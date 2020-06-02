package ru.kpfu.itis.diploma.backend.api.form;

import ru.kpfu.itis.diploma.backend.security.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class EditUserForm {
    private Long id;
    private String name;
    private String lastName;
    private String patronymic;
    private String login;
    private String password;
    private Set<Role> roles;
}
