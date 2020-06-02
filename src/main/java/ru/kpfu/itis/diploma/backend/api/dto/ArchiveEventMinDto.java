package ru.kpfu.itis.diploma.backend.api.dto;

import ru.kpfu.itis.diploma.backend.model.ArchiveEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveEventMinDto {
    private Long id;
    private LocalDate date;
    private ArchiveEvent.Type type;

    public ArchiveEventMinDto(ArchiveEvent event) {
        id = event.getId();
        date = event.getDate();
        type = event.getType();
    }
}
