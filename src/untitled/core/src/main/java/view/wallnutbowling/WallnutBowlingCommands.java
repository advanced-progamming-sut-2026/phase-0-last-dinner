package view.wallnutbowling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum WallnutBowlingCommands {
    START(
            "^\\s*wallnut(?:\\s+|-)bowling"
                    + "\\s+start"
                    + "(?:\\s+-s\\s+(?<stage>\\d+))?"
                    + "\\s*$"
    ),

    PLACE_WALLNUT(
            "^\\s*wallnut(?:\\s+|-)bowling"
                    + "\\s+place"
                    + "\\s+-i\\s+(?<index>\\d+)"
                    + "\\s+-l\\s+"
                    + "\\(\\s*(?<x>\\d+)"
                    + "\\s*,\\s*(?<y>\\d+)\\s*\\)"
                    + "\\s*$"
    ),

    SHOW(
            "^\\s*wallnut(?:\\s+|-)bowling"
                    + "\\s+show\\s*$"
    ),

    ADVANCE_TIME(
            "^\\s*wallnut(?:\\s+|-)bowling"
                    + "\\s+advance"
                    + "\\s+-t\\s+(?<ticks>\\d+)"
                    + "\\s*$"
    ),

    HELP(
            "^\\s*wallnut(?:\\s+|-)bowling"
                    + "\\s+help\\s*$"
    ),

    BACK(
            "^\\s*back\\s+to\\s+minigame"
                    + "\\s+menu\\s*$"
    );

    private final Pattern pattern;

    WallnutBowlingCommands(String regex) {
        this.pattern = Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE
        );
    }

    public Matcher getMatcher(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            return matcher;
        }

        return null;
    }
}