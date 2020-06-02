package ru.kpfu.itis.diploma.backend.repo;

import ru.kpfu.itis.diploma.backend.model.ArchiveEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface ArchiveEventsRepo extends CrudRepository<ArchiveEvent, Long> {
    List<ArchiveEvent> findAllByDateAndType(LocalDate date, ArchiveEvent.Type type, Pageable pageable);

    List<ArchiveEvent> findAllByDate(LocalDate date, Pageable pageable);
}
