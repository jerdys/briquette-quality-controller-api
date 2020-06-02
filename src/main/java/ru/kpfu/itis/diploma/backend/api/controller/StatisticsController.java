package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.api.form.StatisticsFilterForm;
import ru.kpfu.itis.diploma.backend.model.Statistics;
import ru.kpfu.itis.diploma.backend.service.StatisticsService;
import ru.kpfu.itis.diploma.backend.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatisticsController {

    private final ReportService reportService;
    private final StatisticsService statisticsService;

    @RequestMapping(value = "/analyze/statistics/reset", method = RequestMethod.POST)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Statistics reset() {
        return statisticsService.resetToday();
    }

    @RequestMapping(value = "/analyze/statistics", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<Statistics> get(final StatisticsFilterForm form) {
        return statisticsService.find(form);
    }

    @RequestMapping(value = "/analyze/statistics/report", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<byte[]> report(final StatisticsFilterForm form) throws IOException {
        final byte[] report = reportService.generate(form);
        final HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Disposition", "attachment; filename=statistics-report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(report);
    }
}
