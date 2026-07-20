package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MainCommand {
    OPEN_GAME_MENU("menu\\s+game"),
    OPEN_SETTINGS_MENU("menu\\s+settings"),
    OPEN_NEWS_MENU("menu\\s+news"),
    OPEN_PROFILE_MENU("menu\\s+profile"),
    LOGOUT("menu\\s+logout");

    private final Pattern pattern;

    MainCommand(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null) return null;
        Matcher matcher = pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}