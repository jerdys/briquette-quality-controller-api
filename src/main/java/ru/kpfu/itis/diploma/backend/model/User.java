package ru.kpfu.itis.diploma.backend.model;

import ru.kpfu.itis.diploma.backend.security.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Set;

import static ru.kpfu.itis.diploma.backend.model.AbstractEntity.GENERATOR;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SequenceGenerator(name = GENERATOR, sequenceName = "user_seq", allocationSize = 100)
public class User extends AbstractEntity {
    private String name;
    private String lastName;
    private String patronymic;
    private String login;
    private String password;
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    @Builder.Default
    private Boolean deleted = false;

    public boolean getDeleted() {
        return deleted != null && deleted;
    }
}
