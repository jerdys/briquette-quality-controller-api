package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.api.dto.CameraDto;
import ru.kpfu.itis.diploma.backend.api.form.EditCameraForm;
import ru.kpfu.itis.diploma.backend.exception.NotFoundException;
import ru.kpfu.itis.diploma.backend.model.Camera;
import ru.kpfu.itis.diploma.backend.repo.CameraRepo;
import ru.kpfu.itis.diploma.backend.service.hardware.BaumerService;
import ru.kpfu.itis.diploma.backend.service.hardware.CameraDevice;
import ru.kpfu.itis.diploma.backend.service.hardware.model.DeviceExposureTimeProperty;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class CameraService {
    private final CameraRepo cameraRepo;
    private final BaumerService baumerService;

    public List<CameraDto> all() {
        final ArrayList<CameraDto> dtoList = cameraRepo.findAll()
                .stream()
                .map(CameraDto::new)
                .collect(Collectors.toCollection(ArrayList::new));

        final List<CameraDevice> deviceList = baumerService.getDeviceList();
        log.debug(() -> new ParameterizedMessage(
                "Connected {} devices: {}",
                deviceList.size(),
                Arrays.toString(deviceList.toArray())
        ));
        for (CameraDevice device : deviceList) {
            CameraDto cameraDto = dtoList.stream()
                    .filter(camera -> !camera.isConnected())
                    .filter(camera -> isSameDevice(camera, device))
                    .findFirst()
                    .orElse(null);
            if (cameraDto != null) {
                cameraDto.setConnected(true);
                final DeviceExposureTimeProperty exposureTime = device.getExposureTime();
                if (exposureTime != null) {
                    cameraDto.setExposureTimeProperty(exposureTime);
                }
            } else {
                cameraDto = new CameraDto(device);
                dtoList.add(cameraDto);
            }
        }

        return dtoList;
    }

    public synchronized CameraDto configure(EditCameraForm form) {
        final Optional<Camera> cameraOptional = cameraRepo.findFirstByDeviceId(form.getDeviceId());
        final Optional<CameraDevice> deviceOptional = baumerService.findDevice(form.getDeviceId());
        final Camera camera;
        if (cameraOptional.isEmpty()) {
            final CameraDevice device = deviceOptional
                    .orElseThrow(() -> new NotFoundException(Camera.class, form.getDeviceId()));
            camera = new Camera()
                    .setDeviceName(device.getDisplayName())
                    .setDeviceId(device.getId())
                    .setDeviceSerial(device.getSerialNumber())
                    .setDeviceModel(device.getModel())
                    .setDeviceType(device.getType());
        } else {
            camera = cameraOptional.get();
        }

        if (deviceOptional.isPresent()) {
            final CameraDevice device = deviceOptional.get();
            final DeviceExposureTimeProperty exposureTime = device.getExposureTime();
            if (exposureTime != null) {
                camera.setExposureTime(exposureTime);
            }
            if (form.getExposureTime() != null) {
                try {
                    device.setExposureTime(form.getExposureTime());
                } catch (Throwable t) {
                    log.warn("Unable to update exposure time: {}", t.toString());
                }
            }
        }
        if (form.getExposureTime() != null) {
            camera.setExposureTimeValue(form.getExposureTime());
        }

        camera.setDisplayName(form.getDisplayName())
                .setPosition(form.getPosition());

        cameraRepo.save(camera);
        return new CameraDto(camera).setConnected(deviceOptional.isPresent());
    }

    private boolean isSameDevice(@NonNull CameraDto camera, @NonNull CameraDevice device) {
        return camera.getDeviceId() != null && camera.getDeviceId().equals(device.getId());
    }
}
