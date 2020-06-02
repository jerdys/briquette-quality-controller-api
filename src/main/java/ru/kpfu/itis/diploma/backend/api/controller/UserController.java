package ru.kpfu.itis.diploma.backend.api.controller;

import ru.kpfu.itis.diploma.backend.api.dto.ProfileDto;
import ru.kpfu.itis.diploma.backend.api.dto.UserMinDto;
import ru.kpfu.itis.diploma.backend.api.form.EditUserForm;
import ru.kpfu.itis.diploma.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @RequestMapping(value = "/users", method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileDto editUser(@RequestBody EditUserForm form) {
        return userService.put(form);
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserMinDto> getAllUsers() {
        return userService.findAll()
                .stream()
                .map(UserMinDto::new)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileDto getUser(@PathVariable long userId) {
        return new ProfileDto(userService.get(userId));
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable long userId) {
        userService.delete(userId);
    }
}
