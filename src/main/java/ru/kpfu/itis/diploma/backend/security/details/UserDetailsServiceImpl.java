package ru.kpfu.itis.diploma.backend.security.details;

import ru.kpfu.itis.diploma.backend.model.User;
import ru.kpfu.itis.diploma.backend.repo.UserRepo;
import ru.kpfu.itis.diploma.backend.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    public AuthorizedUser loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user = userRepo.findFirstByLoginAndDeletedIsFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
        return new AuthorizedUser(
                user.getId(),
                user.getLogin(),
                user.getPassword(),
                user.getDeleted(),
                user.getRoles().stream()
                        .map(Role::getAuthority)
                        .collect(Collectors.toList())
        );
    }
}
