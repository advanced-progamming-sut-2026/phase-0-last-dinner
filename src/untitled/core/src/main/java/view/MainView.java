package view;

import java.util.regex.Matcher;

public class MainView implements CommandHandler, GameEventListener {
    private MainViewObserver observer;

    public void setObserver(MainViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        for (MainCommand command : MainCommand.values()) {
            Matcher matcher = command.getMatcher(input);
            if (matcher == null) continue;

            switch (command) {
                case OPEN_GAME_MENU: {
                    if (!observer.onOpenGameMenuRequested()) {
                        System.out.println("Cannot open game menu.");
                    }
                    break;
                }
                case OPEN_SETTINGS_MENU: {
                    if (!observer.onOpenSettingsMenuRequested()) {
                        System.out.println("Cannot open settings menu.");
                    }
                    break;
                }
                case OPEN_NEWS_MENU: {
                    if (!observer.onOpenNewsMenuRequested()) {
                        System.out.println("Cannot open news menu.");
                    }
                    break;
                }
                case OPEN_PROFILE_MENU: {
                    if (!observer.onOpenProfileMenuRequested()) {
                        System.out.println("Cannot open profile menu.");
                    }
                    break;
                }
                case LOGOUT: {
                    System.out.println(observer.onLogoutRequested());
                    break;
                }
                default: {
                    System.out.println("Invalid command.");
                    break;
                }
            }
            return;
        }

        System.out.println("Invalid command.");
    }

    @Override
    public void onGameEvent(String message) {
        System.out.println(message);
    }
}