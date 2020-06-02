package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.api.dto.AuthDto;
import ru.kpfu.itis.diploma.backend.api.form.LoginForm;
import ru.kpfu.itis.diploma.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final ApplicationContext applicationContext;

    @RequestMapping(value = "/auth", method = RequestMethod.POST)
    public AuthDto auth(HttpServletRequest request, @RequestBody LoginForm form) {
        return userService.authenticate(request, form);
    }
}
