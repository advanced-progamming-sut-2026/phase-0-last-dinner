package controller;

import model.Menu.MenuType;
import model.User.User;
import model.mechanism.DifficultyConfig;
import view.SettingViewObserver;

public class SettingController implements MenuController, SettingViewObserver {
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    private final DifficultyConfig difficultyConfig;

    public SettingController(User user) {
        this.difficultyConfig = new DifficultyConfig(user);
    }

    @Override
    public String onChangeDifficultyRequested(int level) {
        if (level < MIN_DIFFICULTY || level > MAX_DIFFICULTY) {
            return "Invalid difficulty level. Must be between 1 and 5.";
        }
        difficultyConfig.setLevel(level);
        return "Difficulty changed to " + level + ".";
    }

    @Override
    public int getCurrentDifficulty() {
        return difficultyConfig.getLevel();
    }

    public double getMultiplier() {
        return difficultyConfig.getMultiplier();
    }

    public double getInverseMultiplier() {
        return difficultyConfig.getInverseMultiplier();
    }

    @Override
    public void changeMenu() {}

    @Override
    public MenuType getCurrentMenu() {
        return MenuType.SETTINGS_MENU;
    }
}
