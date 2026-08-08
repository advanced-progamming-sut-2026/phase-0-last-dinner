package model.User;

import java.util.Locale;

public enum LeaderboardSortField {
    USERNAME,
    PROGRESS,
    MINIGAMES,
    DAILY_QUESTS,
    NON_DAILY_QUESTS,
    MEOW_POINTS;

    public static LeaderboardSortField from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEOW_POINTS;
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');

        if ("username".equals(normalized) || "user".equals(normalized)) {
            return USERNAME;
        }
        if ("progress".equals(normalized) || "level".equals(normalized)
                || "chapter".equals(normalized)) {
            return PROGRESS;
        }
        if ("minigames".equals(normalized) || "mini-games".equals(normalized)) {
            return MINIGAMES;
        }
        if ("daily-quests".equals(normalized) || "daily".equals(normalized)) {
            return DAILY_QUESTS;
        }
        if ("non-daily-quests".equals(normalized) || "non-daily".equals(normalized)
                || "quests".equals(normalized)) {
            return NON_DAILY_QUESTS;
        }
        if ("meow-points".equals(normalized) || "meow".equals(normalized)
                || "points".equals(normalized)) {
            return MEOW_POINTS;
        }

        throw new IllegalArgumentException("Leaderboard sort field is invalid.");
    }
}
