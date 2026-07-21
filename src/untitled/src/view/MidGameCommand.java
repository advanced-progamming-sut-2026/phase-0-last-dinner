package view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MidGameCommand {
    ADVANCE_TIME("advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks"),
    SHOW_MAP("show\\s+map"),
    SHOW_SUN_AMOUNT("show\\s+sun\\s+amount"),
    SHOW_PLANTS_STATUS("show\\s+plants\\s+status"),
    SHOW_TILE_STATUS("show\\s+tile\\s+status\\s+-l\\s+(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?"),
    COLLECT_SUN("collect\\s+sun\\s+-l\\s+(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?"),
    PLANT_PLANT("plant\\s+plant\\s+-t\\s+(?<type>\\\"[^\\\"]+\\\"|\\S+)\\s+-l\\s+"
            + "(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?"),
    PLANT_IMITATER("plant\\s+imitater\\s+-t\\s+(?<type>\\\"[^\\\"]+\\\"|\\S+)\\s+-l\\s+"
            + "(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?"),
    PLUCK_PLANT("pluck\\s+plant\\s+-l\\s+(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?"),
    FEED_PLANT("feed\\s+plant\\s+-l\\s+(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?"),
    CHEAT_ADD_SUNS("cheat\\s+add\\s+-n\\s+(?<count>\\d+)\\s+suns"),
    CHEAT_REMOVE_COOLDOWN("cheat\\s+remove-cooldown"),
    CHEAT_ADD_PLANT_FOOD("cheat\\s+add-plant-food"),
    RELEASE_THE_NUKE("release\\s+the\\s+nuke"),
    ZOMBIES_INFO("zombies\\s+info"),
    SPAWN_ZOMBIE("cheat\\s+spawn-zombie\\s+-t\\s+(?<type>\\\"[^\\\"]+\\\"|\\S+)\\s+-l\\s+"
            + "(?:\\(|<)?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?");

    private final Pattern pattern;

    MidGameCommand(String regex) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public Matcher getMatcher(String input) {
        if (input == null) return null;
        Matcher matcher = pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}
