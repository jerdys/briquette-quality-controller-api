package ru.kpfu.itis.diploma.backend.model.analyze;

import ru.kpfu.itis.diploma.backend.model.AbstractEntity;
import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SideSettings extends AbstractEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BriquetteSide side;
    @OneToOne(orphanRemoval = true, cascade = CascadeType.PERSIST)
    private Histogram histogram;
    @OneToOne(orphanRemoval = true, cascade = CascadeType.PERSIST)
    private CropBounds bounds;
}
