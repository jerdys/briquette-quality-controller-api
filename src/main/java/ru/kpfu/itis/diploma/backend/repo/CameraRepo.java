package ru.kpfu.itis.diploma.backend.repo;

import ru.kpfu.itis.diploma.backend.model.Camera;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CameraRepo extends CrudRepository<Camera, String> {
    @NotNull
    @Override
    List<Camera> findAll();

    Optional<Camera> findFirstByDeviceId(String deviceId);
}
