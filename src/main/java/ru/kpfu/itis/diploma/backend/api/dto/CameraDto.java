package ru.kpfu.itis.diploma.backend.api.dto;

import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.model.Camera;
import ru.kpfu.itis.diploma.backend.service.hardware.CameraDevice;
import ru.kpfu.itis.diploma.backend.service.hardware.model.DeviceExposureTimeProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CameraDto {
    private String displayName;
    private BriquetteSide position;

    private String deviceName;
    private String deviceId;
    private String deviceSerial;
    private String deviceModel;
    private String deviceType;
    private boolean connected = false;
    private DeviceExposureTimeProperty exposureTimeProperty;

    public CameraDto(Camera camera) {
        this.displayName = camera.getDisplayName();
        this.position = camera.getPosition();
        this.deviceName = camera.getDeviceName();
        this.deviceSerial = camera.getDeviceSerial();
        this.deviceId = camera.getDeviceId();
        this.deviceModel = camera.getDeviceModel();
        this.deviceType = camera.getDeviceType();
        this.exposureTimeProperty = camera.getExposureTime();
    }

    public CameraDto(CameraDevice device) {
        this.deviceName = device.getDisplayName();
        this.deviceSerial = device.getSerialNumber();
        this.deviceId = device.getId();
        this.deviceModel = device.getModel();
        this.deviceType = device.getType();
        this.connected = true;
    }
}
