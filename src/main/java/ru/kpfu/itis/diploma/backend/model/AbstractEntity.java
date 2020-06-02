package ru.kpfu.itis.diploma.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Objects;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class AbstractEntity {
    public static final String GENERATOR = "seq_gen";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = GENERATOR)
    private Long id;

    @Override
    public boolean equals(Object o) {
        return o != null &&
                (
                        this == o
                                || o.getClass().isAssignableFrom(this.getClass())
                                && Objects.equals(this.id, ((AbstractEntity) o).id)
                );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
