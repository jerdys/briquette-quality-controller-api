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
@SequenceGenerator(name = GENERATOR, sequenceName = "archived_frame_seq", allocationSize = 100)
public class ArchivedFrame extends AbstractEntity {
    private BriquetteSide side;
    private String filePath;
    private long timestamp;
    private int width;
    private int height;
}
