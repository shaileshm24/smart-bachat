package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.PatternType;
import com.ametsa.smartbachat.entity.UserCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCategoryMappingRepository extends JpaRepository<UserCategoryMapping, UUID> {

    /**
     * Find all mappings for a profile.
     */
    List<UserCategoryMapping> findByProfileIdOrderByMatchCountDesc(UUID profileId);

    /**
     * Find all mappings for a profile by category.
     */
    List<UserCategoryMapping> findByProfileIdAndCategory(UUID profileId, String category);

    /**
     * Find a specific mapping by profile, pattern type, and value.
     */
    Optional<UserCategoryMapping> findByProfileIdAndPatternTypeAndPatternValue(
            UUID profileId, PatternType patternType, String patternValue);

    /**
     * Find mappings by pattern type for a profile.
     */
    List<UserCategoryMapping> findByProfileIdAndPatternType(UUID profileId, PatternType patternType);

    /**
     * Check if a mapping exists.
     */
    boolean existsByProfileIdAndPatternTypeAndPatternValue(
            UUID profileId, PatternType patternType, String patternValue);

    /**
     * Find mappings that might match a given value (for quick lookup).
     * Used during transaction categorization.
     */
    @Query("SELECT m FROM UserCategoryMapping m WHERE m.profileId = :profileId " +
           "AND (m.patternType = :patternType OR m.patternType = 'DESCRIPTION_KEYWORD') " +
           "ORDER BY m.matchCount DESC")
    List<UserCategoryMapping> findMatchingMappings(
            @Param("profileId") UUID profileId,
            @Param("patternType") PatternType patternType);

    /**
     * Find all mobile number mappings for a profile.
     */
    @Query("SELECT m FROM UserCategoryMapping m WHERE m.profileId = :profileId " +
           "AND m.patternType = 'MOBILE_NUMBER'")
    List<UserCategoryMapping> findMobileNumberMappings(@Param("profileId") UUID profileId);

    /**
     * Find all UPI ID mappings for a profile.
     */
    @Query("SELECT m FROM UserCategoryMapping m WHERE m.profileId = :profileId " +
           "AND m.patternType = 'UPI_ID'")
    List<UserCategoryMapping> findUpiIdMappings(@Param("profileId") UUID profileId);

    /**
     * Delete all mappings for a profile.
     */
    void deleteByProfileId(UUID profileId);

    /**
     * Count mappings by profile.
     */
    long countByProfileId(UUID profileId);
}

