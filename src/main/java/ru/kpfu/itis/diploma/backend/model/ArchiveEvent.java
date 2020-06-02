package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import java.time.LocalDate;

import static ru.kpfu.itis.diploma.backend.model.AbstractEntity.GENERATOR;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SequenceGenerator(name = GENERATOR, sequenceName = "archive_event_seq", allocationSize = 100)
public class ArchiveEvent extends AbstractEntity {
    private LocalDate date;
    @OneToOne
    @JoinColumn
    private BriquetteReport briquetteReport;
    @Enumerated(EnumType.STRING)
    private Type type;

    @AllArgsConstructor
    public enum Type {
        DEFECT("ДЕФЕКТ"),
        ERROR("ОШИБКА");

        @Getter
        private final String russianNaming;
    }
}
