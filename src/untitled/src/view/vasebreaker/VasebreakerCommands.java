package view.vasebreaker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum VasebreakerCommands {
    START(
            "^\\s*vasebreaker\\s+start"
                    + "(?:\\s+-s\\s+(?<stage>\\d+))?"
                    + "\\s*$"
    ),

    BREAK_VASE(
            "^\\s*vasebreaker\\s+break"
                    + "\\s+-l\\s+"
                    + "\\(\\s*(?<x>\\d+)"
                    + "\\s*,\\s*(?<y>\\d+)\\s*\\)"
                    + "\\s*$"
    ),

    COLLECT_SEED_PACKET(
            "^\\s*vasebreaker\\s+collect"
                    + "\\s+-l\\s+"
                    + "\\(\\s*(?<x>\\d+)"
                    + "\\s*,\\s*(?<y>\\d+)\\s*\\)"
                    + "\\s*$"
    ),

    PLANT_SEED_PACKET(
            "^\\s*vasebreaker\\s+plant"
                    + "\\s+-p\\s+"
                    + "(?<plantName>\"[^\"]+\"|.+?)"
                    + "\\s+-l\\s+"
                    + "\\(\\s*(?<x>\\d+)"
                    + "\\s*,\\s*(?<y>\\d+)\\s*\\)"
                    + "\\s*$"
    ),

    SHOW(
            "^\\s*vasebreaker\\s+show\\s*$"
    ),

    ADVANCE_TIME(
            "^\\s*vasebreaker\\s+advance"
                    + "\\s+-t\\s+(?<ticks>\\d+)"
                    + "\\s*$"
    ),

    HELP(
            "^\\s*vasebreaker\\s+help\\s*$"
    ),

    BACK(
            "^\\s*back\\s+to\\s+minigame\\s+menu\\s*$"
    );

    private final Pattern pattern;

    VasebreakerCommands(String regex) {
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