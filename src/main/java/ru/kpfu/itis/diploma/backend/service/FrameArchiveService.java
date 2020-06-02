package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.model.ArchivedFrame;
import ru.kpfu.itis.diploma.backend.repo.ArchivedFrameRepo;
import ru.kpfu.itis.diploma.backend.service.analyze.BriquetteInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class FrameArchiveService {
    private final ArchivedFrameRepo archivedFrameRepo;
    private final MatOfInt jpegParams = new MatOfInt(
            Imgcodecs.IMWRITE_JPEG_QUALITY,
            75,
            Imgcodecs.IMWRITE_JPEG_OPTIMIZE,
            1
    );

    @Value("${archive-base-path}")
    private String basePath;
    @Value("${enable-archive}")
    private boolean enabled;

    public List<ArchivedFrame> save(BriquetteInfo info) {
        if (!enabled) {
            final List<ArchivedFrame> result = info.getKeyFrames()
                    .stream()
                    .filter(frame -> frame.getImage() != null && !frame.getImage().empty())
                    .map(frame -> ArchivedFrame.builder()
                            .side(info.getSide())
                            .timestamp(frame.getTimestamp())
                            .height(frame.getHeight())
                            .width(frame.getWidth())
                            .build())
                    .collect(Collectors.toList());
            archivedFrameRepo.saveAll(result);
            return result;
        }

        final List<ArchivedFrame> result = new ArrayList<>(info.getKeyFrames().size());
        if (info.getKeyFrames().isEmpty()) {
            return Collections.emptyList();
        }
        final String outPathRel;
        try {
            outPathRel = getRelativeOutputPath(info);
        } catch (IOException e) {
            log.error("Unable to create archive dir: {}", e.toString());
            return Collections.emptyList();
        }
        for (final BriquetteInfo.FrameMeta frame : info.getKeyFrames()) {
            if (frame.getImage() != null && !frame.getImage().empty()) {
                final String filenameRel = outPathRel + "/" + frame.getTimestamp() + ".jpg";
                final String filenameAbs = Paths.get(basePath + "/" + filenameRel).toAbsolutePath().toString();
                final boolean write = Imgcodecs.imwrite(filenameAbs, frame.getImage(), jpegParams);
                if (!write) {
                    log.warn("Failed to save jpg to dir '{}'", filenameAbs);
                } else {
                    result.add(
                            ArchivedFrame.builder()
                                    .side(info.getSide())
                                    .timestamp(frame.getTimestamp())
                                    .height(frame.getHeight())
                                    .width(frame.getWidth())
                                    .filePath(filenameRel)
                                    .build()
                    );
                }
            }
        }
        archivedFrameRepo.saveAll(result);
        return result;
    }

    @NonNull
    private synchronized String getRelativeOutputPath(BriquetteInfo info) throws IOException {
        final LocalDateTime time = info.getFirstFrameTime();
        if (time == null) {
            throw new IllegalStateException("info.firstFrameTime is null");
        }
        final String archivePath = String.format(
                "%s/%s/%s_%s",
                time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                time.format(DateTimeFormatter.ofPattern("HH")),
                time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                info.getSide()
        );
        if (!checkOrCreateDir(basePath + "/" + archivePath)) {
            throw new IOException("Archive directory not found(can't be created, or it's a file)");
        }

        return archivePath;
    }

    private synchronized boolean checkOrCreateDir(String pathStr) throws IOException {
        final Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return Files.isDirectory(path);
    }
}
