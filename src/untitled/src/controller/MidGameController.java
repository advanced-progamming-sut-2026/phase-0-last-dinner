package controller;

import model.mechanism.Board;
import model.mechanism.GameStatusService;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.ZombieSpawner;
import model.mechanism.ZombieStatus;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MidGameController {
    private static final int BOARD_WIDTH = 9;
    private static final int BOARD_HEIGHT = 5;
    private static final int TICKS_PER_SECOND = 10;

    private static final Pattern ZOMBIES_INFO_COMMAND = Pattern.compile(
            "^\\s*zombies\\s+info\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SPAWN_ZOMBIE_COMMAND = Pattern.compile(
            "^\\s*cheat\\s+spawn-zombie"
                    + "\\s+-t\\s+(?<type>\\\"[^\\\"]+\\\"|.+?)"
                    + "\\s+-l\\s+(?:\\(|<)?\\s*(?<x>\\d+)"
                    + "\\s*,\\s*(?<y>\\d+)\\s*(?:\\)|>)?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final GameStatusService statusService;
    private final ZombieSpawner zombieSpawner;

    public MidGameController() {
        this(null, null);
    }

    public MidGameController(PlantZombieGame game) {
        this(
                game == null ? null : game.getGameStatusService(),
                game == null ? null : game.getZombieSpawner()
        );
    }

    public MidGameController(GameStatusService statusService, ZombieSpawner zombieSpawner) {
        this.statusService = statusService;
        this.zombieSpawner = zombieSpawner;
    }

    public String executeCommand(String input) {
        if (input != null && ZOMBIES_INFO_COMMAND.matcher(input).matches()) {
            return this.showAllZombieInfo();
        }

        Matcher spawnMatcher = input == null ? null : SPAWN_ZOMBIE_COMMAND.matcher(input);

        if (spawnMatcher != null && spawnMatcher.matches()) {
            String zombieType = this.removeQuotes(spawnMatcher.group("type"));
            int x = Integer.parseInt(spawnMatcher.group("x"));
            int y = Integer.parseInt(spawnMatcher.group("y"));
            Zombie zombie = this.spawnZombieCheat(zombieType, x, y);

            if (zombie == null) {
                return "Could not spawn zombie. Check the type and location.";
            }

            return this.getZombieTypeName(zombie) + " spawned at " + x + ", " + y + ".";
        }

        return "Invalid mid-game command.";
    }

    public String showAllZombieInfo() {
        List<ZombieStatus> statuses = this.getAllZombieInfo();

        if (statuses.isEmpty()) {
            return "No zombies on the board.";
        }

        StringBuilder output = new StringBuilder();

        for (ZombieStatus status : statuses) {
            if (output.length() > 0) {
                output.append('\n');
            }

            output.append(status.getZombieType()).append(":\n")
                    .append("    position: ")
                    .append(this.formatNumber(status.getExactX() + 1))
                    .append(", ")
                    .append(status.getPosition().getY() + 1)
                    .append("\n    health: ")
                    .append(status.getHealth())
                    .append("\n    armor:\n");

            for (Map.Entry<String, Integer> armor : status.getArmorHealth().entrySet()) {
                output.append("        ").append(armor.getKey()).append(": ")
                        .append(armor.getValue()).append('\n');
            }

            output.append("    effects:");

            for (Map.Entry<String, Long> effect : status.getEffectRemainingTicks().entrySet()) {
                output.append("\n        ").append(effect.getKey());

                if (effect.getValue() != null) {
                    output.append(": ")
                            .append(this.formatSeconds(effect.getValue()))
                            .append('s');
                }
            }
        }

        return output.toString();
    }

    public List<ZombieStatus> getAllZombieInfo() {
        return this.statusService == null
                ? new ArrayList<ZombieStatus>()
                : this.statusService.getZombiesStatus();
    }

    // mokhtasat dastoor az yek shoru mishe
    public Zombie spawnZombieCheat(String zombieType, int x, int y) {
        if (x < 1 || x > BOARD_WIDTH || y < 1 || y > BOARD_HEIGHT
                || this.zombieSpawner == null) {
            return null;
        }

        ZombieDefinition definition = this.findZombieDefinition(zombieType);
        ZombieFactory factory = this.zombieSpawner.getZombieFactory();
        Board board = this.zombieSpawner.getBoard();

        if (definition == null || factory == null || board == null) {
            return null;
        }

        Position internalPosition = new Position(x - 1, y - 1);
        Zombie zombie = factory.create(definition, internalPosition);

        if (zombie == null) {
            return null;
        }

        board.addZombie(zombie, internalPosition);
        return zombie;
    }

    private ZombieDefinition findZombieDefinition(String requestedType) {
        ZombieDefinitionRepository definitions = this.zombieSpawner == null
                ? null
                : this.zombieSpawner.getDefinitionRepository();

        if (definitions == null || requestedType == null || requestedType.trim().isEmpty()) {
            return null;
        }

        String cleanType = this.removeQuotes(requestedType);
        ZombieDefinition aliasMatch = definitions.findByAlias(cleanType);

        if (aliasMatch != null) {
            return aliasMatch;
        }

        String normalizedType = this.normalizeTypeName(cleanType);
        if (normalizedType.isEmpty()) {
            return null;
        }

        ZombieDefinition suffixMatch = null;

        for (ZombieDefinition definition : definitions.findAll()) {
            if (definition == null) {
                continue;
            }

            String displayName = this.normalizeTypeName(definition.getDisplayName());
            String alias = this.normalizeTypeName(definition.getAlias());

            if (normalizedType.equals(displayName) || normalizedType.equals(alias)) {
                return definition;
            }

            if (suffixMatch == null && alias.endsWith(normalizedType)) {
                suffixMatch = definition;
            }
        }

        return suffixMatch;
    }

    private String getZombieTypeName(Zombie zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return "Zombie";
        }

        String displayName = zombie.getDefinition().getDisplayName();
        return displayName == null || displayName.trim().isEmpty()
                ? zombie.getDefinition().getAlias()
                : displayName.trim();
    }

    private String normalizeTypeName(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String removeQuotes(String value) {
        if (value == null) {
            return "";
        }

        String cleanValue = value.trim();

        if (cleanValue.length() >= 2 && cleanValue.startsWith("\"") && cleanValue.endsWith("\"")) {
            return cleanValue.substring(1, cleanValue.length() - 1).trim();
        }

        return cleanValue;
    }

    private String formatSeconds(long ticks) {
        return this.formatNumber((double) ticks / TICKS_PER_SECOND);
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
