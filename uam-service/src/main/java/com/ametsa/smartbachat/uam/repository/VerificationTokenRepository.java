package com.ametsa.smartbachat.uam.repository;

import com.ametsa.smartbachat.uam.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByTokenAndTokenType(String token, String tokenType);

    List<VerificationToken> findByUserIdAndTokenType(UUID userId, String tokenType);

    @Query("SELECT t FROM VerificationToken t WHERE t.user.id = :userId AND t.tokenType = :tokenType " +
           "AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<VerificationToken> findValidTokenByUserAndType(UUID userId, String tokenType, Instant now);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(Instant now);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.user.id = :userId AND t.tokenType = :tokenType")
    int deleteByUserIdAndTokenType(UUID userId, String tokenType);
}

