package ru.kpfu.itis.diploma.backend.model.analyze;

import ru.kpfu.itis.diploma.backend.model.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CropBounds extends AbstractEntity {
    @Column(name = "top_indent")
    private Double top;
    @Column(name = "right_indent")
    private Double right;
    @Column(name = "bottom_indent")
    private Double bottom;
    @Column(name = "left_indent")
    private Double left;
}
