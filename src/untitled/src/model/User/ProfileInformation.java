package model.User;

import lombok.Getter;

@Getter
// etelaate namayeshe profile ro negah midare
public class ProfileInformation {
    private final String username;
    private final String nickname;
    private final int gamesPlayed;
    private final int coins;
    private final int diamonds;
    private final int completedLevels;
    private final int maximumMeowPoints;

    public ProfileInformation(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.gamesPlayed = user.getGamesPlayed();
        this.coins = user.getGold();
        this.diamonds = user.getDiamond();
        this.completedLevels = Math.max(0, user.getLevel() - 1);
        this.maximumMeowPoints = user.getMaxObtainedMeowPoints();
    }
}
