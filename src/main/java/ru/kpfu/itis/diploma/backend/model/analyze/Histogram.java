package ru.kpfu.itis.diploma.backend.model.analyze;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.kpfu.itis.diploma.backend.model.AbstractEntity;
import com.vladmihalcea.hibernate.type.array.IntArrayType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.IntSummaryStatistics;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Histogram extends AbstractEntity {
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "values",
                    column = @Column(
                            name = "red_channel",
                            columnDefinition = "integer[]",
                            nullable = false
                    )
            )
    })
    private HistogramChannel red;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "values",
                    column = @Column(
                            name = "green_channel",
                            columnDefinition = "integer[]",
                            nullable = false
                    )
            )
    })
    private HistogramChannel green;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "values",
                    column = @Column(
                            name = "blue_channel",
                            columnDefinition = "integer[]",
                            nullable = false
                    )
            )
    })
    private HistogramChannel blue;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "values",
                    column = @Column(
                            name = "grayscale",
                            columnDefinition = "integer[]",
                            nullable = false
                    )
            )
    })
    private HistogramChannel grayscale;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @TypeDefs({
            @TypeDef(
                    name = "int-array",
                    typeClass = IntArrayType.class
            )
    })
    public static class HistogramChannel {
        @Type(type = "int-array")
        @Column(columnDefinition = "integer[]", nullable = false)
        private int[] values;
        @Transient
        private transient IntSummaryStatistics statistics;

        @JsonIgnore
        private IntSummaryStatistics getStatistics() {
            if (statistics == null) {
                statistics = Arrays.stream(values)
                        .summaryStatistics();
            }
            return statistics;
        }

        public int get(int i) {
            return values[i];
        }

        public int size() {
            return values.length;
        }
    }
}
