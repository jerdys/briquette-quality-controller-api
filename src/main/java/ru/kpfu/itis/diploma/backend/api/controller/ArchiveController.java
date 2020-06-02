package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.api.dto.ArchiveEventMinDto;
import ru.kpfu.itis.diploma.backend.api.form.ArchiveFilterForm;
import ru.kpfu.itis.diploma.backend.model.ArchiveEvent;
import ru.kpfu.itis.diploma.backend.service.ArchiveService;
import ru.kpfu.itis.diploma.backend.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;
    private final ReportService reportService;

    @RequestMapping(value = "/archive/events", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public List<ArchiveEventMinDto> find(final ArchiveFilterForm form) {
        return archiveService.find(form);
    }

    @RequestMapping(value = "/archive/events/{eventId}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ArchiveEvent get(@PathVariable final long eventId) {
        return archiveService.get(eventId);
    }

    @RequestMapping(value = "/archive/events/report", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<byte[]> report(final ArchiveFilterForm form) throws IOException {
        final byte[] report = reportService.generate(form);
        final HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Disposition", "attachment; filename=archive-report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(report);
    }
}
