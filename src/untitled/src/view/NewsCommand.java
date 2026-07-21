package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum NewsCommand {
    SHOW_UNREAD("^\\s*menu\\s+news\\s+show-unread\\s*$"),
    SHOW_ALL("^\\s*menu\\s+news\\s+show-all\\s*$");

    private final Pattern pattern;

    NewsCommand(String regex) {
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
