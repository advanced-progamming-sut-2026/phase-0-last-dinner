package view.izombie;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum IZombieCommands {

    START(
            "^\\s*i[-\\s]*zombie\\s+start\\s*$"
    ),

    PLACE_ZOMBIE(
            "^\\s*i[-\\s]*zombie\\s+place"
                    + "\\s+-z\\s+(?<zombie>.+?)"
                    + "\\s+-l\\s+\\("
                    + "\\s*(?<x>\\d+)\\s*,"
                    + "\\s*(?<y>\\d+)\\s*"
                    + "\\)\\s*$"
    ),

    SHOW(
            "^\\s*i[-\\s]*zombie\\s+show\\s*$"
    ),

    ADVANCE_TIME(
            "^\\s*i[-\\s]*zombie\\s+advance"
                    + "\\s+-t\\s+(?<ticks>\\d+)\\s*$"
    ),

    BACK(
            "^\\s*back\\s+to\\s+minigame\\s+menu\\s*$"
    );

    private final Pattern pattern;

    IZombieCommands(String regex) {
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

        return matcher.matches() ? matcher : null;
    }
}