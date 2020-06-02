package ru.kpfu.itis.diploma.backend.repo;

import ru.kpfu.itis.diploma.backend.model.analyze.AnalyzeProfile;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AnalyzeProfileRepo extends CrudRepository<AnalyzeProfile, Long> {
    @NotNull
    @Override
    List<AnalyzeProfile> findAll();
}
