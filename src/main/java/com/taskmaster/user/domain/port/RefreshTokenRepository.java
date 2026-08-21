package com.taskmaster.user.domain.port;

import com.taskmaster.user.domain.model.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for RefreshToken persistence operations.
 */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyId(String familyId);

    void revokeAllByFamilyId(String familyId);

    void deleteAllByUserId(UUID userId);
}
