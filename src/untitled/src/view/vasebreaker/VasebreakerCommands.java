package view.vasebreaker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum VasebreakerCommands {
    START("^\\s*vasebreaker\\s+start\\s*$"),

    BREAK_VASE("^\\s*vasebreaker\\s+break\\s+-l\\s+\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),

    COLLECT_SEED_PACKET("^\\s*vasebreaker\\s+collect\\s+-l\\s+\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),

    SHOW("^\\s*vasebreaker\\s+show\\s*$"),

    ADVANCE_TIME("^\\s*vasebreaker\\s+advance\\s+-t\\s+(?<ticks>\\d+)\\s*$"),

    BACK("^\\s*Back\\s+to\\s+minigame\\s+menu\\s*$");

    private final String pattern;

    VasebreakerCommands(String pattern) {
        this.pattern = pattern;
    }

    public Matcher getMatcher(String input) {
        Matcher matcher = Pattern.compile(this.pattern).matcher(input);

        if (matcher.matches()) {
            return matcher;
        }

        return null;
    }
}