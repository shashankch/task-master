package com.taskmaster.user.domain.port;

import com.taskmaster.user.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for User persistence operations.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
