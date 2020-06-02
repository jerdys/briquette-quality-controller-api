package ru.kpfu.itis.diploma.backend.service;

import ru.kpfu.itis.diploma.backend.api.dto.AuthDto;
import ru.kpfu.itis.diploma.backend.api.dto.ProfileDto;
import ru.kpfu.itis.diploma.backend.api.form.EditUserForm;
import ru.kpfu.itis.diploma.backend.api.form.LoginForm;
import ru.kpfu.itis.diploma.backend.exception.BadRequestException;
import ru.kpfu.itis.diploma.backend.exception.ForbiddenException;
import ru.kpfu.itis.diploma.backend.exception.NotFoundException;
import ru.kpfu.itis.diploma.backend.model.User;
import ru.kpfu.itis.diploma.backend.repo.UserRepo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @NonNull
    public User get(@NonNull Long id) {
        return userRepo.findFirstByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> new NotFoundException(User.class, id));
    }

    @NonNull
    public User get(@NonNull String login) {
        return userRepo.findFirstByLoginAndDeletedIsFalse(login)
                .orElseThrow(() -> new NotFoundException(User.class, login));
    }

    public AuthDto authenticate(HttpServletRequest request, LoginForm form) {
        final User user = userRepo.findFirstByLoginAndDeletedIsFalse(form.getLogin())
                .orElseThrow(() -> new ForbiddenException("Невеный логин или пароль"));
        if (passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            UsernamePasswordAuthenticationToken authReq
                    = new UsernamePasswordAuthenticationToken(form.getLogin(), form.getPassword());
            Authentication auth = authenticationManager.authenticate(authReq);
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            return new AuthDto(new ProfileDto(user), session.getId());
        } else {
            throw new ForbiddenException("Невеный логин или пароль");
        }
    }

    public ProfileDto register(EditUserForm form) {
        validateLoginUnique(form.getLogin());

        final User newUser = userRepo.save(
                User.builder()
                        .name(form.getName())
                        .lastName(form.getLastName())
                        .patronymic(form.getPatronymic())
                        .login(form.getLogin())
                        .password(passwordEncoder.encode(form.getPassword()))
                        .roles(form.getRoles())
                        .build()
        );

        return new ProfileDto(newUser);
    }

    public ProfileDto put(EditUserForm form) {
        final User user = userRepo.findById(form.getId())
                .orElseThrow(() -> new NotFoundException(User.class, form.getId()));

        if (!user.getLogin().equals(form.getLogin())) {
            validateLoginUnique(form.getLogin());
        }
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        user.setName(form.getName())
                .setLastName(form.getLastName())
                .setPatronymic(form.getPatronymic())
                .setLogin(form.getLogin())
                .setRoles(form.getRoles());

        userRepo.save(user);

        return new ProfileDto(user);
    }

    public void delete(final long userId) {
        final User user = get(userId);

        user.setDeleted(true);

        userRepo.save(user);
    }

    private void validateLoginUnique(String login) {
        if (isExistsByLogin(login)) {
            throw new BadRequestException("Login already exists");
        }
    }

    private boolean isExistsByLogin(String login) {
        return userRepo
                .findFirstByLoginAndDeletedIsFalse(login)
                .isPresent();
    }

    public List<User> findAll() {
        return userRepo.findAllByDeletedIsFalse();
    }
}
