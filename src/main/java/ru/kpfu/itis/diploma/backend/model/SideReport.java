package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import java.util.List;

import static ru.kpfu.itis.diploma.backend.model.AbstractEntity.GENERATOR;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SequenceGenerator(name = GENERATOR, sequenceName = "side_report_seq", allocationSize = 100)
public class SideReport extends AbstractEntity {
    @Enumerated
    @Column(nullable = false)
    private BriquetteSide side;
    private LocalDateTime firstFrameTime;
    private LocalDateTime lastFrameTime;
    private double avgSpeed;
    private double direction;
    @OneToMany(cascade = CascadeType.PERSIST)
    private List<ArchivedFrame> frames;
    // @OneToOne
    // private SettingsInfo settings;
    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn
    private List<SmudgeInfo> smudges;
    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn
    private List<RawSmudgeInfo> rawSmudges;
    // @ManyToOne
    // @JoinColumn
    // @Getter(AccessLevel.PROTECTED)
    // @Setter(AccessLevel.PROTECTED)
    // private BriquetteReport briquetteReport;
}
