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

    public LeaderboardEntry(int rank, User user) {
        this.rank = rank;
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.lastChapter = user.getLastCompletedChapterType() != null
                ? user.getLastCompletedChapterType().name()
                : user.getChapter() == null || user.getChapter().getChapter() == null
                        ? "NOT_STARTED"
                        : user.getChapter().getChapter().name();
        this.lastLevel = user.getLastCompletedLevel() > 0
                ? user.getLastCompletedLevel()
                : user.getChapter() == null
                        ? 0
                        : Math.max(0, user.getLevel() - 1);
        this.completedMinigames = Math.max(0, user.getCompletedMinigames());
        this.completedDailyQuests = Math.max(0, user.getCompletedDailyQuests());
        this.completedNonDailyQuests = Math.max(0, user.getCompletedNonDailyQuests());
        this.meowPoints = user.getMaxObtainedMeowPoints();
    }
}
