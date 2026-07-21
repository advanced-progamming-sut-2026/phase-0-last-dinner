package view;

import java.util.regex.Matcher;

public class SettingView implements GameEventListener {
    private SettingViewObserver observer;

    public void setObserver(SettingViewObserver observer) {
        this.observer = observer;
    }

    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println("Setting controller is not connected.");
            return;
        }

        Matcher matcher;

        matcher = SettingCommand.CHANGE_DIFFICULTY.getMatcher(input);
        if (matcher != null) {
            int level = Integer.parseInt(matcher.group("level"));
            System.out.println(observer.onChangeDifficultyRequested(level));
            return;
        }

        System.out.println("Invalid settings command.");
    }

    @Override
    public void onGameEvent(String message) {
        System.out.println(message);
    }
}