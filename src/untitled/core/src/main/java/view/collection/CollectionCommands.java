package view.collection;

import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public enum CollectionCommands {
    SHOW_PLANTS("^\\s*menu\\s+collection\\s+show-plants\\s*$"),

    SHOW_ALL_PLANTS("^\\s*menu\\s+collection\\s+show-all-plants\\s*$"),

    SHOW_ZOMBIES("^\\s*menu\\s+collection\\s+show-zombies\\s*$"),

    SHOW_ALL_ZOMBIES("^\\s*menu\\s+collection\\s+show-all-zombies\\s*$"),

    SHOW_PLANT("^\\s*menu\\s+collection\\s+show-plant\\s+-p\\s+(?<plantName>\"[^\"]+\"|'[^']+'|.+?)\\s*$"),

    SHOW_ZOMBIE("^\\s*menu\\s+collection\\s+show-zombie\\s+-z\\s+(?<zombieName>\"[^\"]+\"|'[^']+'|.+?)\\s*$"),

    UPGRADE_PLANT("^\\s*menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(?<plantName>\"[^\"]+\"|'[^']+'|.+?)\\s*$"),

    PURCHASE_PLANT("^\\s*menu\\s+collection\\s+purchase-plant\\s+-p\\s+(?<plantName>\"[^\"]+\"|'[^']+'|.+?)\\s*$");

    private final Pattern pattern;

    CollectionCommands(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null)
            return null;

        Matcher matcher = this.pattern.matcher(input);

        return matcher.matches() ? matcher : null;
    }
}