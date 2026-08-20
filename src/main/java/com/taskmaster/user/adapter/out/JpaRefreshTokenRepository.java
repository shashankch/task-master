package com.taskmaster.user.adapter.out;

import com.taskmaster.user.domain.model.RefreshToken;
import com.taskmaster.user.domain.port.RefreshTokenRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter implementing RefreshTokenRepository port.
 */
@Component
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataJpaRefreshTokenRepository repository;

    public JpaRefreshTokenRepository(SpringDataJpaRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return repository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }

    @Override
    public List<RefreshToken> findByFamilyId(String familyId) {
        return repository.findByFamilyId(familyId);
    }

    @Override
    public void revokeAllByFamilyId(String familyId) {
        repository.revokeAllByFamilyId(familyId);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
    }
}
