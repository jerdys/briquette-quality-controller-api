package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import java.util.Map;

import static ru.kpfu.itis.diploma.backend.model.AbstractEntity.GENERATOR;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SequenceGenerator(name = GENERATOR, sequenceName = "briquette_report_seq", allocationSize = 100)
public class BriquetteReport extends AbstractEntity {
    private LocalDateTime time;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    @MapKey(name = "side")
    @JoinColumn(name = "briquette_report_id")
    private Map<BriquetteSide, SideReport> sides;
    private Status status;

    public enum Status {
        OK, WARN, FAIL
    }
}
