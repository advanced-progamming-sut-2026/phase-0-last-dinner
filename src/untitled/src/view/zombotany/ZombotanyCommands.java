package view.zombotany;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ZombotanyCommands {

    START(
            "^\\s*zombotany\\s+start"
                    + "(?:\\s+-s\\s+(?<stage>\\d+))?"
                    + "\\s*$"
    ),

    PLANT(
            "^\\s*zombotany\\s+plant"
                    + "\\s+-p\\s+(?<plant>.+?)"
                    + "\\s+-l\\s+\\("
                    + "\\s*(?<x>\\d+)\\s*,"
                    + "\\s*(?<y>\\d+)\\s*"
                    + "\\)\\s*$"
    ),

    COLLECT_SUN(
            "^\\s*zombotany\\s+collect"
                    + "\\s+-l\\s+\\("
                    + "\\s*(?<x>\\d+)\\s*,"
                    + "\\s*(?<y>\\d+)\\s*"
                    + "\\)\\s*$"
    ),

    USE_PLANT_FOOD(
            "^\\s*zombotany\\s+plant[-\\s]*food"
                    + "\\s+-l\\s+\\("
                    + "\\s*(?<x>\\d+)\\s*,"
                    + "\\s*(?<y>\\d+)\\s*"
                    + "\\)\\s*$"
    ),

    ADVANCE_TIME(
            "^\\s*zombotany\\s+advance"
                    + "\\s+-t\\s+(?<ticks>\\d+)"
                    + "\\s*$"
    ),

    SHOW(
            "^\\s*zombotany\\s+show\\s*$"
    ),

    HELP(
            "^\\s*zombotany\\s+help\\s*$"
    ),

    BACK(
            "^\\s*back\\s+to\\s+minigame\\s+menu\\s*$"
    );

    private final Pattern pattern;

    ZombotanyCommands(String regex) {
        pattern = Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE
        );
    }

    public Matcher getMatcher(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher =
                pattern.matcher(input);

        return matcher.matches()
                ? matcher
                : null;
    }
}