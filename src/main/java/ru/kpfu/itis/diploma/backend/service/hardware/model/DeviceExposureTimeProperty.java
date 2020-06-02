package ru.kpfu.itis.diploma.backend.service.hardware.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@SuppressWarnings("JpaDataSourceORMInspection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class DeviceExposureTimeProperty {
    @Column(name = "exposure_value")
    private Double value;
    @Column(name = "exposure_min")
    private Double min;
    @Column(name = "exposure_max")
    private Double max;
    @Column(name = "exposure_unit")
    private String unit;
}
