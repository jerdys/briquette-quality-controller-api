package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;

import static ru.kpfu.itis.diploma.backend.model.AbstractEntity.GENERATOR;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SequenceGenerator(name = GENERATOR, sequenceName = "smudge_info_seq", allocationSize = 100)
public class SmudgeInfo extends AbstractEntity {
    private double centerX;
    private double centerY;
    private double width;
    private double height;
    private double direction;
}
