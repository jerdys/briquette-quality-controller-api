package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statistics {
    @Id
    private LocalDate date;
    @Builder.Default
    @Column(nullable = false)
    private int normal = 0;
    @Builder.Default
    @Column(nullable = false)
    private int defective = 0;

    public void incrementNormal() {
        normal++;
    }

    public void incrementDefective() {
        defective++;
    }
}
