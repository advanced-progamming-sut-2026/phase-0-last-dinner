package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PlantPickCommand {
    SHOW_ALL_PLANTS("^\\s*show\\s+all\\s+plants\\s*$"),
    SHOW_AVAILABLE_PLANTS("^\\s*show\\s+available\\s+plants\\s*$"),
    ADD_PLANT("^\\s*add\\s+plant\\s+-t\\s+(?<type>\"[^\"]+\"|'[^']+'|.+?)\\s*$"),
    REMOVE_PLANT("^\\s*remove\\s+plant\\s+-t\\s+(?<type>\"[^\"]+\"|'[^']+'|.+?)\\s*$"),
    BOOST_PLANT("^\\s*boost\\s+plant\\s+-t\\s+(?<type>\"[^\"]+\"|'[^']+'|.+?)\\s*$"),
    START_GAME("^\\s*start\\s+game\\s*$");

    private final Pattern pattern;

    PlantPickCommand(String regex) {
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
