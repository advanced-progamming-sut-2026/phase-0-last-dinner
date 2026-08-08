package view;

public interface SettingViewObserver {
    String onChangeDifficultyRequested(int level);
    int getCurrentDifficulty();
}