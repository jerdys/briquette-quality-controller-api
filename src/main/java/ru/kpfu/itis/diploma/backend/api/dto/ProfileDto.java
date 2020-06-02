package ru.kpfu.itis.diploma.backend.api.dto;

import ru.kpfu.itis.diploma.backend.model.User;
import ru.kpfu.itis.diploma.backend.security.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ProfileDto {
    private Long id;
    private String name;
    private String lastName;
    private String patronymic;
    private String login;
    private List<String> roles;

    public ProfileDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.lastName = user.getLastName();
        this.patronymic = user.getPatronymic();
        this.login = user.getLogin();
        this.roles = user.getRoles() != null
                ? user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList())
                : null;
    }
}
