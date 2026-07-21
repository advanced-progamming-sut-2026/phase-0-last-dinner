package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SettingCommand {
    CHANGE_DIFFICULTY(
            "menu\\s+settings\\s+change-difficulty\\s+-l\\s+(?<level>\\d+)"
    );

    private final Pattern pattern;

    SettingCommand(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null) return null;
        Matcher matcher = pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}