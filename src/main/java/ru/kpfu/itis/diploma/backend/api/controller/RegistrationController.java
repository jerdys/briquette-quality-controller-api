package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.api.dto.ProfileDto;
import ru.kpfu.itis.diploma.backend.api.form.EditUserForm;
import ru.kpfu.itis.diploma.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegistrationController {
    private final UserService userService;

    @RequestMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileDto registerUser(@RequestBody EditUserForm form) {
        return userService.register(form);
    }
}
