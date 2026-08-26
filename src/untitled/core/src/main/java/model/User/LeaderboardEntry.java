package model.User;

import lombok.Getter;

@Getter
// etelaate yek radife leaderboard ro negah midare
public class LeaderboardEntry {
    private final int rank;
    private final String username;
    private final String nickname;
    private final String lastChapter;
    private final int lastLevel;
    private final int completedMinigames;
    private final int completedDailyQuests;
    private final int completedNonDailyQuests;
    private final int meowPoints;

    public LeaderboardEntry(
            int rank,
            String username,
            String nickname,
            String lastChapter,
            int lastLevel,
            int completedMinigames,
            int completedDailyQuests,
            int completedNonDailyQuests,
            int meowPoints
    ) {
        this.rank = rank;
        this.username = username;
        this.nickname = nickname;
        this.lastChapter = lastChapter;
        this.lastLevel = Math.max(0, lastLevel);
        this.completedMinigames = Math.max(0, completedMinigames);
        this.completedDailyQuests = Math.max(0, completedDailyQuests);
        this.completedNonDailyQuests = Math.max(0, completedNonDailyQuests);
        this.meowPoints = Math.max(0, meowPoints);
    }

    public LeaderboardEntry(int rank, User user) {
        this(
                rank,
                user.getUsername(),
                user.getNickname(),
                user.getLastCompletedChapterType() != null
                ? user.getLastCompletedChapterType().name()
                : user.getChapter() == null || user.getChapter().getChapter() == null
                        ? "NOT_STARTED"
                        : user.getChapter().getChapter().name(),
                user.getLastCompletedLevel() > 0
                ? user.getLastCompletedLevel()
                : user.getChapter() == null
                        ? 0
                        : Math.max(0, user.getLevel() - 1),
                user.getCompletedMinigames(),
                user.getCompletedDailyQuests(),
                user.getCompletedNonDailyQuests(),
                user.getMaxObtainedMeowPoints()
        );
    }
}
