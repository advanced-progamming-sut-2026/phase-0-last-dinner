package view.greenhouse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GreenhouseCommands {

    SHOW_GREENHOUSE("^\\s*show\\s+greenhouse\\s*$"),

    PLANT_POT("^\\s*plant\\s+pot\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),

    COLLECT("^\\s*collect\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),

    GROW("^\\s*grow\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),

    BUY_POT("^\\s*buy\\s+pot\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"),

    ENTER_SHOP("^\\s*enter\\s+shop\\s*$");

    private final Pattern pattern;

    GreenhouseCommands(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);

        return matcher.matches() ? matcher : null;
    }
}
