package ru.kpfu.itis.diploma.backend.api.dto;

import ru.kpfu.itis.diploma.backend.model.SmudgeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmudgeDescriptionDto {
    // millimeters
    private double minRadius;
    // millimeters
    private double maxRadius;
    // abs (0..1)
    private double x;
    // abs (0..1)
    private double y;
    private double direction;

    public SmudgeDescriptionDto(SmudgeInfo smudgeInfo) {
        this.x = smudgeInfo.getCenterX();
        this.y = smudgeInfo.getCenterY();
        // FIXME: 20.12.2019 magic number
        final double diag = Math.sqrt(Math.pow(smudgeInfo.getWidth(), 2) + Math.pow(smudgeInfo.getHeight(), 2));
        this.minRadius = 5 + Math.round(diag / 2) * 100;
        this.maxRadius = 5 + Math.round(diag / 2) * 100;
        this.direction = smudgeInfo.getDirection();
    }
}
