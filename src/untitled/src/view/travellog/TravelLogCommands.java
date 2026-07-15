package view.travellog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TravelLogCommands {

    CHANGE_PAGE(
            "^\\s*travel\\s+log\\s+page"
                    + "\\s+(?<page>[a-zA-Z_-]+)"
                    + "\\s*$"
    ),

    SHOW_PAGE(
            "^\\s*travel\\s+log\\s+show\\s*$"
    ),

    PLAY_MINIGAME(
            "^\\s*minigame\\s+play"
                    + "\\s+(?<game>.+?)"
                    + "\\s*$"
    ),

    HELP(
            "^\\s*travel\\s+log\\s+help\\s*$"
    ),

    BACK_TO_MINIGAMES(
            "^\\s*back\\s+to\\s+minigame"
                    + "\\s+menu\\s*$"
    ),

    BACK_TO_GAME(
            "^\\s*back\\s+to\\s+game"
                    + "\\s+menu\\s*$"
    );

    private final Pattern pattern;

    TravelLogCommands(String regex) {
        pattern = Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE
        );
    }

    public Matcher getMatcher(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);

        return matcher.matches()
                ? matcher
                : null;
    }
}