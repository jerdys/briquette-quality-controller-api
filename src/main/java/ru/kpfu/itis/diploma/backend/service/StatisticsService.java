package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.api.form.StatisticsFilterForm;
import ru.kpfu.itis.diploma.backend.model.Statistics;
import ru.kpfu.itis.diploma.backend.repo.StatisticsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepo statisticsRepo;

    public Statistics resetToday() {
        final Statistics statistics = getTodayStatistics()
                .setDefective(0)
                .setNormal(0);
        statisticsRepo.save(statistics);
        return statistics;
    }

    public void incrementNormal() {
        final Statistics statistics = getTodayStatistics();
        statistics.incrementNormal();
        statisticsRepo.save(statistics);
    }

    public void incrementDefective() {
        final Statistics statistics = getTodayStatistics();
        statistics.incrementDefective();
        statisticsRepo.save(statistics);
    }

    public synchronized Statistics getTodayStatistics() {
        return statisticsRepo.findById(LocalDate.now())
                .orElseGet(this::createStatisticsEntity);
    }

    private Statistics createStatisticsEntity() {
        final Statistics statistics = new Statistics(LocalDate.now(), 0, 0);
        return statisticsRepo.save(statistics);
    }

    public List<Statistics> find(final StatisticsFilterForm form) {
        if (form.getTo() == null) {
            form.setTo(LocalDate.now());
        }
        if (form.getFrom() == null) {
            form.setFrom(form.getTo());
        }

        return statisticsRepo.findAllByDateBetweenOrderByDate(
                form.getFrom(),
                form.getTo(),
                PageRequest.of(form.getPage(), form.getPageSize())
        );
    }
}
