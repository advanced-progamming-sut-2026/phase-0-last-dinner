package view;

import java.util.regex.Matcher;

public class SettingView implements GameEventListener {
    private static final int DEFAULT_DIFFICULTY = 3;
    private SettingViewObserver observer;

    public void setObserver(SettingViewObserver observer) {
        this.observer = observer;
    }

    public String handleCommand(String input) {
        if (observer == null) {
            return "Setting controller is not connected.";
        }

        Matcher matcher = SettingCommand.CHANGE_DIFFICULTY.getMatcher(input);
        if (matcher != null) {
            int level = Integer.parseInt(matcher.group("level"));
            return observer.onChangeDifficultyRequested(level);
        }

        return "Invalid settings command.";
    }

    public int getCurrentDifficulty() {
        return observer != null ? observer.getCurrentDifficulty() : DEFAULT_DIFFICULTY;
    }

    @Override
    public void onGameEvent(String message) {
        System.out.println(message);
    }
}
