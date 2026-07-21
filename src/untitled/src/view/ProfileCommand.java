package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ProfileCommand {
    CHANGE_USERNAME("^\\s*menu\\s+profile\\s+change-username\\s+-u\\s+(?<username>\\S+)\\s*$"),
    CHANGE_NICKNAME("^\\s*menu\\s+profile\\s+change-nickname\\s+-u\\s+(?<nickname>\"[^\"]*\"|'[^']*'|\\S+)\\s*$"),
    CHANGE_EMAIL("^\\s*menu\\s+profile\\s+change-email\\s+-e\\s+(?<email>\\S+)\\s*$"),
    CHANGE_PASSWORD(
            "^\\s*menu\\s+profile\\s+change-password"
                    + "\\s+-p\\s+(?<newPassword>\"[^\"]*\"|'[^']*'|\\S+)"
                    + "\\s+-o\\s+(?<oldPassword>\"[^\"]*\"|'[^']*'|\\S+)\\s*$"
    ),
    SHOW_INFORMATION("^\\s*menu\\s+profile\\s+show-info\\s*$");

    private final Pattern pattern;

    ProfileCommand(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = this.pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}
