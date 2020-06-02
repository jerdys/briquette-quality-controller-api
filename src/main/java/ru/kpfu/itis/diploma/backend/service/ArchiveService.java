package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.api.dto.ArchiveEventMinDto;
import ru.kpfu.itis.diploma.backend.api.form.ArchiveFilterForm;
import ru.kpfu.itis.diploma.backend.exception.NotFoundException;
import ru.kpfu.itis.diploma.backend.model.ArchiveEvent;
import ru.kpfu.itis.diploma.backend.repo.ArchiveEventsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveService {
    private final ArchiveEventsRepo archiveEventsRepo;

    public List<ArchiveEventMinDto> find(final ArchiveFilterForm form) {
        if (form.getDate() == null) {
            form.setDate(LocalDate.now());
        }
        if (form.getType() != null) {
            return archiveEventsRepo.findAllByDateAndType(
                    form.getDate(),
                    form.getType(),
                    PageRequest.of(form.getPage(), form.getPageSize())
            ).stream()
                    .map(ArchiveEventMinDto::new)
                    .collect(Collectors.toList());
        } else {
            return archiveEventsRepo.findAllByDate(
                    form.getDate(),
                    PageRequest.of(form.getPage(), form.getPageSize())
            ).stream()
                    .map(ArchiveEventMinDto::new)
                    .collect(Collectors.toList());
        }
    }

    public List<ArchiveEvent> findAll(final ArchiveFilterForm form) {
        if (form.getDate() == null) {
            form.setDate(LocalDate.now());
        }
        if (form.getType() != null) {
            return archiveEventsRepo.findAllByDateAndType(
                    form.getDate(),
                    form.getType(),
                    PageRequest.of(form.getPage(), form.getPageSize())
            );
        } else {
            return archiveEventsRepo.findAllByDate(
                    form.getDate(),
                    PageRequest.of(form.getPage(), form.getPageSize())
            );
        }
    }

    public ArchiveEvent get(final long eventId) {
        return archiveEventsRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ArchiveEvent.class, eventId));
    }
}
