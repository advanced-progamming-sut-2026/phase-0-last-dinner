package view.beghouled;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum BeghouledCommands {

    START(
            "^\\s*beghouled\\s+start"
                    + "(?:\\s+-s\\s+(?<stage>\\d+))?"
                    + "\\s*$"
    ),

    SWAP(
            "^\\s*beghouled\\s+swap"
                    + "\\s+-f\\s+\\("
                    + "\\s*(?<firstX>\\d+)\\s*,"
                    + "\\s*(?<firstY>\\d+)\\s*"
                    + "\\)"
                    + "\\s+-t\\s+\\("
                    + "\\s*(?<secondX>\\d+)\\s*,"
                    + "\\s*(?<secondY>\\d+)\\s*"
                    + "\\)"
                    + "\\s*$"
    ),

    UPGRADE(
            "^\\s*beghouled\\s+upgrade"
                    + "\\s+-p\\s+(?<plant>.+?)"
                    + "\\s*$"
    ),

    SHOW(
            "^\\s*beghouled\\s+show\\s*$"
    ),

    ADVANCE_TIME(
            "^\\s*beghouled\\s+advance"
                    + "\\s+-t\\s+(?<ticks>\\d+)"
                    + "\\s*$"
    ),

    HELP(
            "^\\s*beghouled\\s+help\\s*$"
    ),

    BACK(
            "^\\s*back\\s+to\\s+minigame\\s+menu\\s*$"
    );

    private final Pattern pattern;

    BeghouledCommands(String regex) {
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
        return matcher.matches() ? matcher : null;
    }
}