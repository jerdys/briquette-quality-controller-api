package ru.kpfu.itis.diploma.backend.repo;

import ru.kpfu.itis.diploma.backend.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends CrudRepository<User, Long> {
    Optional<User> findFirstByIdAndDeletedIsFalse(Long id);
    Optional<User> findFirstByLoginAndDeletedIsFalse(String login);
    List<User> findAllByDeletedIsFalse();
}
