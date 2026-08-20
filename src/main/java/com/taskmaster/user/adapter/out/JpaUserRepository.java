package com.taskmaster.user.adapter.out;

import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter implementing UserRepository port using Spring Data JPA.
 */
@Component
public class JpaUserRepository implements UserRepository {

    private final SpringDataJpaUserRepository repository;

    public JpaUserRepository(SpringDataJpaUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmailOrUsername(String email, String username) {
        Optional<User> byEmail = repository.findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return repository.findByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }
}
