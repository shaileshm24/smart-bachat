package com.ametsa.smartbachat.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gamification summary for dashboard.
 * Placeholder structure for Phase 2 implementation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamificationSummary {

    /**
     * Current consecutive day streak.
     */
    private Integer currentStreak;

    /**
     * Longest streak achieved.
     */
    private Integer longestStreak;

    /**
     * Recently earned badges.
     */
    private List<BadgeItem> recentBadges;

    /**
     * Active challenges.
     */
    private List<ChallengeItem> activeChallenges;

    /**
     * User's current level.
     */
    private Integer level;

    /**
     * User's experience points.
     */
    private Integer xp;

    /**
     * Total badges earned.
     */
    private Integer totalBadges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeItem {
        private String id;
        private String name;
        private String icon;
        private String description;
        private String earnedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeItem {
        private String id;
        private String name;
        private String description;
        private Integer progressPercent;
        private String deadline;
        private Double rewardXp;
    }
}

