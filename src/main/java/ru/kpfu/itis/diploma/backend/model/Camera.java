package ru.kpfu.itis.diploma.backend.model;

import ru.kpfu.itis.diploma.backend.service.hardware.model.DeviceExposureTimeProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Camera {
    private String displayName;
    @Enumerated(EnumType.STRING)
    private BriquetteSide position;
    private String deviceName;
    @Id
    private String deviceId;
    private String deviceSerial;
    private String deviceModel;
    private String deviceType;

    @Embedded
    private DeviceExposureTimeProperty exposureTime;

    public String getDisplayName() {
        return displayName != null ? displayName : deviceName + " (" + deviceId + ":" + deviceSerial + ")";
    }

    public void setExposureTimeValue(Double exposureTime) {
        if (this.exposureTime == null) {
            this.exposureTime = DeviceExposureTimeProperty.builder()
                    .value(exposureTime)
                    .build();
        } else {
            this.exposureTime.setValue(Math.min(
                    this.exposureTime.getMax(),
                    Math.max(
                            this.exposureTime.getMin(),
                            exposureTime
                    )
            ));
        }
    }
}
