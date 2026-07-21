package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameCommand {
    ENTER_CHAPTER("menu\\s+enter\\s+chapter\\s+-c\\s+(?<name>\\S+)"),
    GREENHOUSE("menu\\s+greenhouse"),
    TRAVEL_LOG("menu\\s+travel-log"),
    LEADERBOARD("menu\\s+leaderboard"),
    COIN_WALLET("menu\\s+coin-wallet"),
    GEM_WALLET("menu\\s+gem-wallet"),
    CHEAT_ADD("menu\\s+cheat\\s+add\\s+(?<count>\\d+)\\s+(?<currency>coin|diamond)");

    private final Pattern pattern;

    GameCommand(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null) return null;
        Matcher matcher = pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}