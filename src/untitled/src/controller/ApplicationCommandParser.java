package controller;

import model.Menu.MenuType;
import model.level.LevelType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ApplicationCommandParser {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");
    private final Set<Integer> quotedTokenIndexes = new HashSet<>();

    List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        this.quotedTokenIndexes.clear();
        if (input == null) {
            return tokens;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(input.trim());
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                this.quotedTokenIndexes.add(tokens.size());
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                this.quotedTokenIndexes.add(tokens.size());
                tokens.add(matcher.group(2));
            } else {
                tokens.add(matcher.group(3));
            }
        }
        return tokens;
    }

    String valueAfter(List<String> tokens, String flag, int offset) {
        for (int i = 0; i < tokens.size(); i++) {
            if (!this.quotedTokenIndexes.contains(i)
                    && flag.equalsIgnoreCase(tokens.get(i)) && i + offset < tokens.size()) {
                String value = tokens.get(i + offset);
                return !this.quotedTokenIndexes.contains(i + offset)
                        && this.isCommandFlag(value) ? null : value;
            }
        }
        return null;
    }

    boolean hasMissingValue(String... values) {
        for (String value : values) {
            if (value == null || value.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    boolean matches(List<String> tokens, String... expected) {
        return tokens.size() == expected.length && this.startsWith(tokens, expected);
    }

    boolean startsWith(List<String> tokens, String... expected) {
        if (tokens.size() < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(tokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    boolean containsIgnoreCase(List<String> tokens, String value) {
        for (int i = 0; i < tokens.size(); i++) {
            if (!this.quotedTokenIndexes.contains(i)
                    && value.equalsIgnoreCase(tokens.get(i))) {
                return true;
            }
        }
        return false;
    }

    String join(List<String> tokens, int startIndex) {
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i < tokens.size(); i++) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(tokens.get(i));
        }
        return result.toString();
    }

    LevelType parseLevelType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Level type is required.");
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return LevelType.valueOf(normalized);
    }

    MenuType parseMenuType(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("menu", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
        MenuType menuType = parseMainMenuType(normalized);
        if (menuType != null) {
            return menuType;
        }
        menuType = parseGameMenuType(normalized);
        if (menuType != null) {
            return menuType;
        }
        throw new IllegalArgumentException("Menu name is invalid");
    }

    private MenuType parseMainMenuType(String value) {
        switch (value) {
            case "signup":
                return MenuType.SIGNUP_MENU;
            case "login":
                return MenuType.LOGIN_MENU;
            case "main":
                return MenuType.MAIN_MENU;
            case "game":
            case "play":
                return MenuType.GAME_MENU;
            case "settings":
            case "setting":
                return MenuType.SETTINGS_MENU;
            case "network":
                return MenuType.NETWORK_MENU;
            case "news":
                return MenuType.NEWS_MENU;
            case "profile":
                return MenuType.PROFILE_MENU;
            default:
                return null;
        }
    }

    private MenuType parseGameMenuType(String value) {
        switch (value) {
            case "collection":
                return MenuType.COLLECTION_MENU;
            case "greenhouse":
                return MenuType.GREENHOUSE_MENU;
            case "leaderboard":
                return MenuType.LEADERBOARD_MENU;
            case "meowpoint":
            case "scoring":
                return MenuType.MEOW_POINT_MENU;
            case "travellog":
                return MenuType.TRAVEL_LOG_MENU;
            case "plantpick":
            case "plantselection":
                return MenuType.PLANT_PICK_MENU;
            case "midgame":
            case "gameplay":
                return MenuType.MID_GAME_MENU;
            default:
                return null;
        }
    }

    private boolean isCommandFlag(String value) {
        if (value == null) {
            return false;
        }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "-u":
            case "-p":
            case "-n":
            case "-e":
            case "-g":
            case "-q":
            case "-a":
            case "-c":
            case "-t":
            case "-l":
            case "-o":
            case "-stay-logged-in":
                return true;
            default:
                return false;
        }
    }
}
