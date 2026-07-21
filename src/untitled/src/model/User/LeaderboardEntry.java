package model.User;

import lombok.Getter;

@Getter
// etelaate yek radife leaderboard ro negah midare
public class LeaderboardEntry {
    private final int rank;
    private final String username;
    private final String nickname;
    private final int meowPoints;

    public LeaderboardEntry(int rank, User user) {
        this.rank = rank;
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.meowPoints = user.getMaxObtainedMeowPoints();
    }
}
