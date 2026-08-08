package view.shop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ShopCommands {

    SHOP_LIST("^\\s*shop\\s+list\\s*$"),

    SHOP_DAILY("^\\s*shop\\s+daily\\s*$"),

    SHOP_BUY("^\\s*shop\\s+buy\\s+-i\\s+(?<itemId>[a-zA-Z0-9_-]+)"
                    + "\\s+-n\\s+(?<count>-?\\d+)(?:\\s+-t\\s+(?<plantType>"
                    + "\"[^\"]+\"|'[^']+'|.+?" + "))?\\s*$");

    private final Pattern pattern;

    ShopCommands(String regex) {
        this.pattern = Pattern.compile(
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