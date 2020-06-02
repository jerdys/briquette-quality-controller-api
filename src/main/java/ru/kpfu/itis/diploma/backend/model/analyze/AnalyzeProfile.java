package ru.kpfu.itis.diploma.backend.model.analyze;

import ru.kpfu.itis.diploma.backend.model.AbstractEntity;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import java.util.Map;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeProfile extends AbstractEntity {
    private String name;
    @Embedded
    private Size size;
    private Integer minSmudgeArea;
    private Integer minSummarySmudgeArea;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    @MapKey(name = "side")
    @JoinColumn(name = "profile_id")
    private Map<BriquetteSide, SideSettings> sideSettings;

    /**
     * See {@link ru.kpfu.itis.diploma.backend.service.analyze.AlgorithmParameters.BriquetteSize}
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Size {
        @Column(name = "length", nullable = false)
        private int length;
        @Column(name = "length_tolerance", nullable = false)
        private int lengthTolerance;
        @Column(name = "depth", nullable = false)
        private int depth;
        @Column(name = "depth_tolerance", nullable = false)
        private int depthTolerance;
        @Column(name = "height", nullable = false)
        private int height;
        @Column(name = "height_tolerance", nullable = false)
        private int heightTolerance;
    }
}
