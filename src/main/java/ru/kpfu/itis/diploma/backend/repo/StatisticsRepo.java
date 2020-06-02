package ru.kpfu.itis.diploma.backend.repo;

import ru.kpfu.itis.diploma.backend.model.Statistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsRepo extends CrudRepository<Statistics, LocalDate> {

    List<Statistics> findAllByDateBetweenOrderByDate(LocalDate from, LocalDate to, Pageable pageable);
}
