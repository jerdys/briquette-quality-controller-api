package ru.kpfu.itis.diploma.backend.service.analyze;

import ru.kpfu.itis.diploma.backend.api.dto.OperatorEventDto;
import ru.kpfu.itis.diploma.backend.api.dto.SmudgeDescriptionDto;
import ru.kpfu.itis.diploma.backend.exception.NotFoundException;
import ru.kpfu.itis.diploma.backend.model.*;
import ru.kpfu.itis.diploma.backend.model.analyze.AnalyzeProfile;
import ru.kpfu.itis.diploma.backend.model.analyze.SideSettings;
import ru.kpfu.itis.diploma.backend.repo.*;
import ru.kpfu.itis.diploma.backend.service.FrameArchiveService;
import ru.kpfu.itis.diploma.backend.service.StatisticsService;
import ru.kpfu.itis.diploma.backend.service.hardware.BaumerService;
import ru.kpfu.itis.diploma.backend.service.hardware.CameraDevice;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class AnalyzeService {
    private final BaumerService baumerService;
    private final ExecutorService executorService;
    private final StatisticsService statisticsService;
    private final BriquetteReportRepo briquetteReportRepo;
    private final ArchiveEventsRepo archiveEventsRepo;
    private final ConcurrentHashMap<CameraDevice, Analyzer> analyzers = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        baumerService.handleConnectedDevices(this::onDeviceConnected);
    }

    private void onDeviceConnected(CameraDevice device) {
        try {
            analyzer(device);
        } catch (Throwable t) {
            log.error("Unable to start analyze for camera {}: {}", device.getId(), t.toString(), t);
        }
    }

    // @Scheduled(fixedDelay = 1000)
    // TODO: 27.02.2020 remove on prod
    private void sendAgain() {
        final BriquetteReport briquetteReport = lastReport.get();
        if (briquetteReport == null) {
            return;
        }
        final OperatorEventDto event = toEvent(briquetteReport);
        for (Consumer<OperatorEventDto> consumer : eventConsumers) {
            executorService.submit(() -> notifyConsumer(event, consumer));
        }
    }

    private void notifyConsumer(OperatorEventDto event,
                                Consumer<OperatorEventDto> consumer) {
        try {
            consumer.accept(event);
        } catch (Throwable t) {
            log.warn("Unhandled exception in event consumer: {}", t.toString(), t);
        }
    }

    public Analyzer analyzer(CameraDevice capture) {
        return analyzers.computeIfAbsent(
                capture,
                // FIXME: 04.03.2020 set correct side
                t -> new Analyzer(capture, this::registerEvent, BriquetteSide.TOP)
        );
    }

    // TODO: 27.02.2020 remove on prod
    private final AtomicReference<BriquetteReport> lastReport = new AtomicReference<>(null);
    private final ConcurrentHashMap<BriquetteSide, BriquetteInfo> sideEvents = new ConcurrentHashMap<>();

    private final FrameArchiveService frameArchiveService;

    private static final boolean __TEST__ = true;

    private synchronized void registerEvent(BriquetteInfo event) {
        if (__TEST__) {
            List<ArchivedFrame> archivedFrames = frameArchiveService.save(event);
            final BriquetteReport report = new BriquetteReport(
                    LocalDateTime.now(),
                    Map.of(event.getSide(), event.getReport()),
                    BriquetteReport.Status.OK
            );
            executorService.submit(() -> {
                saveReport(report);
                sendReport(report);
            });
            return;
        }
        if (sideEvents.containsKey(event.getSide())) {
            // что-то сильно не так
            // проверить все остальные значения, вероятно, брикет проехал, но не было евента с последней камеры
            log.fatal(
                    "Duplicate side event:\n\tOld: {};\n\tNew: {}",
                    sideEvents.get(event.getSide()),
                    event
            );
            sideEvents.clear();
            sideEvents.put(event.getSide(), event);
            // FIXME: 03.03.2020 do something!!!!
        } else if (isFinishFrame(event)) {
            sideEvents.put(event.getSide(), event);
            BriquetteReport.Status status = BriquetteReport.Status.OK;
            if (sideEvents.size() != 6) {
                log.error(
                        "Information about some sides of the briquette is lost. Saved events: {}",
                        sideEvents.values().stream().map(Objects::toString).collect(Collectors.joining("; "))
                );
                status = BriquetteReport.Status.WARN;
            }
            final BriquetteReport report = new BriquetteReport(
                    LocalDateTime.now(),
                    sideEvents.values()
                            .stream()
                            .map(BriquetteInfo::getReport)
                            .collect(Collectors.toMap(SideReport::getSide, Function.identity())),
                    status
            );
            sideEvents.clear();
            executorService.submit(() -> {
                saveReport(report);
                sendReport(report);
            });
        }
    }

    private void sendReport(BriquetteReport report) {
        lastReport.set(report);
        final OperatorEventDto event = toEvent(report);
        for (Consumer<OperatorEventDto> consumer : eventConsumers) {
            executorService.submit(() -> notifyConsumer(event, consumer));
        }
    }

    private void saveReport(BriquetteReport report) {
        briquetteReportRepo.save(report);
        if (smudgesCount(report) > 0) {
            archiveEventsRepo.save(new ArchiveEvent(
                    report.getTime().toLocalDate(),
                    report,
                    ArchiveEvent.Type.DEFECT
            ));
            statisticsService.incrementDefective();
        } else {
            statisticsService.incrementNormal();
        }
    }

    private long smudgesCount(BriquetteReport report) {
        return report.getSides()
                .values()
                .stream()
                .map(SideReport::getSmudges)
                .mapToLong(List::size)
                .count();
    }

    private boolean isFinishFrame(BriquetteInfo event) {
        return (event.getSide() == BriquetteSide.BACK || event.getSide() == BriquetteSide.FRONT)
                && !sideEvents.isEmpty();
    }

    private OperatorEventDto toEvent(BriquetteReport report) {
        return new OperatorEventDto(
                System.currentTimeMillis(),
                report.getSides()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue()
                                        .getSmudges()
                                        .stream()
                                        .map(SmudgeDescriptionDto::new)
                                        .collect(Collectors.toList())
                        )),
                statisticsService.getTodayStatistics()
        );
    }

    private final ConcurrentLinkedQueue<Consumer<OperatorEventDto>> eventConsumers = new ConcurrentLinkedQueue<>();

    public void subscribe(Consumer<OperatorEventDto> consumer) {
        eventConsumers.add(consumer);
    }

    public void unsubscribe(Consumer<OperatorEventDto> consumer) {
        eventConsumers.remove(consumer);
    }

    private final AnalyzeProfileRepo analyzeProfileRepo;

    public List<AnalyzeProfile> getProfiles() {
        return analyzeProfileRepo.findAll();
    }

    public AnalyzeProfile createProfile(AnalyzeProfile settings) {
        settings.setId(null);
        // FIXME: 26.02.2020 validate
        if (settings.getSideSettings() != null) {
            settings.getSideSettings().forEach((side, sideSettings) -> sideSettings.setSide(side));
        }
        return analyzeProfileRepo.save(settings);
    }

    public AnalyzeProfile updateProfile(AnalyzeProfile settings) {
        final AnalyzeProfile profile = analyzeProfileRepo.findById(settings.getId())
                .orElseThrow(() -> new NotFoundException(AnalyzeProfile.class, settings.getId()));

        // FIXME: 26.02.2020 validate
        if (settings.getName() != null) {
            profile.setName(settings.getName());
        }
        if (settings.getSize() != null) {
            profile.setSize(settings.getSize());
        }
        if (settings.getMinSmudgeArea() != null) {
            profile.setMinSmudgeArea(settings.getMinSmudgeArea());
        }
        if (settings.getMinSummarySmudgeArea() != null) {
            profile.setMinSummarySmudgeArea(settings.getMinSummarySmudgeArea());
        }

        for (final Map.Entry<BriquetteSide, SideSettings> entry : settings.getSideSettings().entrySet()) {
            final SideSettings old = profile.getSideSettings().get(entry.getKey());
            if (old == null) {
                entry.getValue().setId(null);
                profile.getSideSettings().put(entry.getKey(), entry.getValue());
            } else {
                update(old, entry.getValue());
            }
        }

        return analyzeProfileRepo.save(profile);
    }

    private void update(@NonNull SideSettings old, @NonNull SideSettings update) {
        if (update.getBounds() != null) {
            update.getBounds().setId(null);
            old.setBounds(update.getBounds());
        }
        if (update.getHistogram() != null) {
            update.getHistogram().setId(null);
            old.setHistogram(update.getHistogram());
        }
    }

    public AnalyzeProfile getProfileById(Long id) {
        return analyzeProfileRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(AnalyzeProfile.class, id));
    }

    public AnalyzeProfile changeActiveProfile(Long profileId) {
        final AnalyzeProfile profile = getProfileById(profileId);
        log.error("Not implemented changeActiveProfile");
        return profile;
    }
}
