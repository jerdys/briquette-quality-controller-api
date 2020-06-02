package ru.kpfu.itis.diploma.backend.api.form;

import ru.kpfu.itis.diploma.backend.model.ArchiveEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveFilterForm {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private ArchiveEvent.Type type;
    private int page = 0;
    private int pageSize = 20;
}
