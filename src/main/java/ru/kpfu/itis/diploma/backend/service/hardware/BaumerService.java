package ru.kpfu.itis.diploma.backend.service.hardware;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface BaumerService {
    Optional<CameraDevice> findDevice(String deviceId);

    List<CameraDevice> getDeviceList();

    void handleConnectedDevices(Consumer<CameraDevice> consumer);
}
