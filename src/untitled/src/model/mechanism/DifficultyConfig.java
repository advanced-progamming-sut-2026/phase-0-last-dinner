package model.mechanism;

import model.User.User;

public class DifficultyConfig {
    private static final int DEFAULT = 3;
    private User user;

    public DifficultyConfig(User user) {
        this.user = user;
    }

    public int getLevel() {
        if (user == null || user.getDifficultyLevel() < 1 || user.getDifficultyLevel() > 5) {
            return DEFAULT;
        }
        return user.getDifficultyLevel();
    }

    public void setLevel(int level) {
        if (user != null) user.setDifficultyLevel(level);
    }

    public double getMultiplier() {
        return (double) getLevel() / DEFAULT;
    }

    public double getInverseMultiplier() {
        return (double) DEFAULT / getLevel();
    }
}
