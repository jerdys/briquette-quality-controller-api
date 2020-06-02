package ru.kpfu.itis.diploma.backend.service.report;

import ru.kpfu.itis.diploma.backend.api.form.ArchiveFilterForm;
import ru.kpfu.itis.diploma.backend.api.form.StatisticsFilterForm;
import ru.kpfu.itis.diploma.backend.model.ArchiveEvent;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.model.Statistics;
import ru.kpfu.itis.diploma.backend.service.ArchiveService;
import ru.kpfu.itis.diploma.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReportService {
    private static final String[] ARCHIVE_HEADINGS = {"Дата", "Тип", "Стороны"};
    private static final String[] STATISTICS_HEADINGS = {"Дата", "Брикетов всего", "Брикетов с браком", "Процент брака"};
    private static final int PAGE_SIZE = 1000;

    private final ArchiveService archiveService;
    private final StatisticsService statisticsService;

    @SneakyThrows
    public byte[] generate(final ArchiveFilterForm form) {
        form.setPageSize(PAGE_SIZE);
        form.setPage(0);

        final XSSFWorkbook workbook = new XSSFWorkbook();
        List<ArchiveEvent> archiveEvents;

        int rowCount = 0;
        final XSSFSheet sheet = workbook.createSheet("Архив" + (form.getPage() + 1));
        final Row headings = sheet.createRow(rowCount++);

        for (int i = 0; i < ARCHIVE_HEADINGS.length; i++) {
            headings.createCell(i).setCellValue(ARCHIVE_HEADINGS[i]);
        }

        do {
            archiveEvents = archiveService.findAll(form);

            for (final ArchiveEvent archiveEvent : archiveEvents) {
                final Row row = sheet.createRow(rowCount++);
                row.createCell(0).setCellValue(
                        archiveEvent.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yy"))
                );
                row.createCell(1).setCellValue(archiveEvent.getType().getRussianNaming());

                if (archiveEvent.getBriquetteReport() != null) {
                    row.createCell(2)
                            .setCellValue(
                                    archiveEvent.getBriquetteReport()
                                            .getSides()
                                            .keySet()
                                            .stream()
                                            .map(BriquetteSide::getRussianNaming)
                                            .collect(Collectors.joining(", "))
                            );
                }
            }

            form.setPage(form.getPage() + 1);
        } while (archiveEvents.size() == form.getPageSize());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            workbook.write(outputStream);
        } catch (final IOException e) {
            log.warn("Error writing to output stream {}", e.toString());
            throw e;
        }

        return outputStream.toByteArray();
    }

    public byte[] generate(final StatisticsFilterForm form) {
        form.setPageSize(PAGE_SIZE);
        form.setPage(0);

        final XSSFWorkbook workbook = new XSSFWorkbook();
        List<Statistics> statistics;

        int rowCount = 0;
        final XSSFSheet sheet = workbook.createSheet("Статистика" + (form.getPage() + 1));
        final Row headings = sheet.createRow(rowCount++);

        for (int i = 0; i < STATISTICS_HEADINGS.length; i++) {
            headings.createCell(i).setCellValue(STATISTICS_HEADINGS[i]);
        }

        do {
            statistics = statisticsService.find(form);

            for (final Statistics statistic : statistics) {
                final Row row = sheet.createRow(rowCount++);
                row.createCell(0).setCellValue(
                        statistic.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yy"))
                );
                row.createCell(1).setCellValue(statistic.getNormal() + statistic.getDefective());
                row.createCell(2).setCellValue(statistic.getDefective());
                row.createCell(3).setCellValue(String.format(
                        "%.1f%%",
                        (double) statistic.getDefective() / (statistic.getDefective() + statistic.getNormal()) * 100.0
                ));

            }

            form.setPage(form.getPage() + 1);
        } while (statistics.size() == form.getPageSize());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            workbook.write(outputStream);
        } catch (final IOException e) {
            log.warn("Error writing to output stream {}", e.toString());
        }

        return outputStream.toByteArray();
    }
}
