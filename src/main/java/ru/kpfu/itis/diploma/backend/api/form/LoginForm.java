package ru.kpfu.itis.diploma.backend.api.form;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginForm {
    private String login;
    private String password;
}
