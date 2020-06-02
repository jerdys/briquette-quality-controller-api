package ru.kpfu.itis.diploma.backend.api.dto;

import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import ru.kpfu.itis.diploma.backend.model.Statistics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorEventDto {
    private long time;
    private Map<BriquetteSide, List<SmudgeDescriptionDto>> briquetteDescription;
    private Statistics statistics;
}
