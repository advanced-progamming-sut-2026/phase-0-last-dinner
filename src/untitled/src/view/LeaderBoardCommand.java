package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum LeaderBoardCommand {
    SHOW("^\\s*menu\\s+leaderboard\\s*$");

    private final Pattern pattern;

    LeaderBoardCommand(String regex) {
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
