package view;

import controller.MidGameController;
import model.Plant;
import model.mechanism.*;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class MidGameView implements CommandHandler, GameEventListener {
    private MidGameViewObserver observer;

    public void setObserver(MidGameViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        for (MidGameCommand command : MidGameCommand.values()) {
            Matcher matcher = command.getMatcher(input);
            if (matcher != null) {
                executeCommand(command, matcher);
                return;
            }
        }

        System.out.println("Invalid command.");
    }

    private void executeCommand(MidGameCommand command, Matcher matcher) {
        switch (command) {
            case ADVANCE_TIME:
                observer.onAdvanceTimeRequested(readNumber(matcher, "count"));
                break;
            case SHOW_MAP:
                showMap(observer.onShowMapRequested());
                break;
            case SHOW_SUN_AMOUNT:
                System.out.println("Sun: " + observer.onShowSunAmountRequested());
                break;
            case SHOW_PLANTS_STATUS:
                showPlantsStatus(observer.onShowPlantsStatusRequested());
                break;
            case SHOW_TILE_STATUS:
                showTileStatus(observer.onShowTileStatusRequested(readX(matcher), readY(matcher)));
                break;
            case COLLECT_SUN:
                collectSun(matcher);
                break;
            case PLANT_PLANT:
                plant(matcher, false);
                break;
            case PLANT_IMITATER:
                plant(matcher, true);
                break;
            case PLUCK_PLANT:
                pluckPlant(matcher);
                break;
            case FEED_PLANT:
                feedPlant(matcher);
                break;
            case CHEAT_ADD_SUNS:
            case CHEAT_REMOVE_COOLDOWN:
            case CHEAT_ADD_PLANT_FOOD:
            case RELEASE_THE_NUKE:
                executeCheatCommand(command, matcher);
                break;
            case ZOMBIES_INFO:
                showZombiesInfo(observer.onZombiesInfoRequested());
                break;
            case SPAWN_ZOMBIE:
                spawnZombie(matcher);
                break;
            default:
                System.out.println("Invalid command.");
                break;
        }
    }

    private void executeCheatCommand(MidGameCommand command, Matcher matcher) {
        switch (command) {
            case CHEAT_ADD_SUNS:
                observer.onCheatAddSunsRequested(readNumber(matcher, "count"));
                break;
            case CHEAT_REMOVE_COOLDOWN:
                observer.onCheatRemoveCooldownRequested();
                break;
            case CHEAT_ADD_PLANT_FOOD:
                observer.onCheatAddPlantFoodRequested();
                break;
            default:
                observer.onReleaseTheNukeRequested();
                break;
        }
    }

    private int readNumber(Matcher matcher, String group) {
        return Integer.parseInt(matcher.group(group));
    }

    private int readX(Matcher matcher) {
        return readNumber(matcher, "x") - 1;
    }

    private int readY(Matcher matcher) {
        return readNumber(matcher, "y") - 1;
    }

    private void collectSun(Matcher matcher) {
        int x = readX(matcher);
        int y = readY(matcher);
        if (!observer.onCollectSunRequested(x, y)) {
            System.out.println("No sun at (" + (x + 1) + ", " + (y + 1) + ").");
        }
    }

    private void plant(Matcher matcher, boolean imitater) {
        String type = matcher.group("type");
        int x = readX(matcher);
        int y = readY(matcher);
        boolean planted = imitater
                ? observer.onPlantImitaterRequested(type, x, y)
                : observer.onPlantPlantRequested(type, x, y);
        if (planted) {
            return;
        }
        String prefix = imitater ? "Cannot plant Imitater as " : "Cannot plant ";
        System.out.println(prefix + type + " at (" + (x + 1) + ", " + (y + 1) + ").");
    }

    private void pluckPlant(Matcher matcher) {
        int x = readX(matcher);
        int y = readY(matcher);
        if (!observer.onPluckPlantRequested(x, y)) {
            System.out.println("No plant at (" + (x + 1) + ", " + (y + 1) + ").");
        }
    }

    private void feedPlant(Matcher matcher) {
        int x = readX(matcher);
        int y = readY(matcher);
        if (!observer.onFeedPlantRequested(x, y)) {
            System.out.println("Cannot feed plant at (" + (x + 1) + ", " + (y + 1) + ").");
        }
    }

    private void spawnZombie(Matcher matcher) {
        String type = matcher.group("type");
        if (!observer.onSpawnZombieRequested(type, readX(matcher), readY(matcher))) {
            System.out.println("Could not spawn zombie.");
        }
    }

    private void showMap(Board board) {
        if (board == null) {
            System.out.println("No board available.");
            return;
        }

        System.out.println("Wave: " + observer.getCurrentWaveNumber());
        System.out.println("Sun: " + observer.onShowSunAmountRequested());
        System.out.println("Plant Food: " + observer.onShowPlantFoodCountRequested());
        List<String> conveyorPlants = observer.onShowConveyorPlantsRequested();
        if (conveyorPlants != null && !conveyorPlants.isEmpty()) {
            System.out.println("Conveyor: " + conveyorPlants);
        }
        System.out.println();

        for (int row = 0; row < 5; row++) {
            LawnMower mower = board.getLawnMower(row);
            String mowerStr = (mower != null && mower.isUsed()) ? "[X]" : "[M]";
            System.out.print(mowerStr);

            for (int col = 0; col < 9; col++) {
                Tile tile = board.getTile(new Position(col, row));
                System.out.print(formatTile(tile));
            }
            System.out.println();
        }
    }

    private String formatTile(Tile tile) {
        if (tile == null) return "[     ]";

        String terrain;
        switch (tile.getTerrainType()) {
            case WATER:
                terrain = "~";
                break;
            case GRAVE:
                if (tile.getGraveLoot() == GraveLootType.SUN) {
                    terrain = "S";
                } else if (tile.getGraveLoot() == GraveLootType.PLANT_FOOD) {
                    terrain = "F";
                } else {
                    terrain = "G";
                }
                break;
            case FROZEN:
                terrain = "#";
                break;
            case SLIPPERY_UP:
                terrain = "↑";
                break;
            case SLIPPERY_DOWN:
                terrain = "↓";
                break;
            case NECROMANCY:
                terrain = "N";
                break;
            default:
                terrain = " ";
                break;
        }

        boolean hasPlant = !tile.getPlants().isEmpty();
        boolean hasZombie = !tile.getZombies().isEmpty();

        String content;
        if (hasPlant && hasZombie) {
            content = terrain + "Z+P";
        } else if (hasZombie) {
            content = terrain + " Z ";
        } else if (hasPlant) {
            content = terrain + " P ";
        } else {
            content = terrain + "   ";
        }

        return "[" + content + "]";
    }

    private void showPlantsStatus(List<PlantStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            System.out.println("No plants selected.");
            return;
        }
        for (PlantStatus status : statuses) {
            System.out.print(status.getPlant().getName());
            System.out.print(" | cost: " + status.getSunCost());
            if (status.isAvailable()) {
                System.out.println(" | available");
            } else if (status.getRemainingCooldownTicks() <= 0) {
                System.out.println(" | not enough sun");
            } else {
                System.out.println(" | available in: " + status.getRemainingSeconds() + "s");
            }
        }
    }

    private void showTileStatus(Tile tile) {
        if (tile == null) {
            System.out.println("Invalid position.");
            return;
        }
        System.out.println("Terrain: " + tile.getTerrainType());
        showPlantsOnTile(tile);
        showZombiesOnTile(tile);
    }

    private void showPlantsOnTile(Tile tile) {
        System.out.println("Plants:");
        if (tile.getPlants().isEmpty()) {
            System.out.println("  none");
            return;
        }
        for (Plant plant : tile.getPlants()) {
            System.out.println("  - " + plant.getName()
                    + " | hp: " + plant.getHealth()
                    + "/" + plant.getMaximumHealth());
            System.out.println("    level: " + plant.getLevel());
            System.out.println("    categories: " + plant.getCategories());
            System.out.println("    tags: " + plant.getTags());
            System.out.println("    disabled: " + plant.isDisabled());
        }
    }

    private void showZombiesOnTile(Tile tile) {
        System.out.println("Zombies:");
        if (tile.getZombies().isEmpty()) {
            System.out.println("  none");
            return;
        }
        for (Zombie zombie : tile.getZombies()) {
            showZombieOnTile(zombie);
        }
    }

    private void showZombieOnTile(Zombie zombie) {
        String name = zombie.getDefinition() == null
                ? "Zombie"
                : zombie.getDefinition().getDisplayName();
        System.out.println("  - " + name
                + " | hp: " + zombie.getHealth()
                + "/" + zombie.getMaximumHealth());
        System.out.println("    speed: " + this.formatNumber(zombie.getCurrentSpeed()));
        System.out.println("    effects: " + zombie.getConditions());
        System.out.println("    armor:");
        if (!showZombieArmor(zombie)) {
            System.out.println("      none");
        }
    }

    private boolean showZombieArmor(Zombie zombie) {
        boolean hasArmor = false;
        if (zombie.getArmors() == null) {
            return false;
        }
        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor == null || armor.isDropped() || armor.isDestroyed()
                    || armor.getDefinition() == null) {
                continue;
            }
            hasArmor = true;
            System.out.println("      " + armor.getDefinition().getAlias()
                    + ": " + armor.getCurrentHealth());
        }
        return hasArmor;
    }

    private void showZombiesInfo(List<ZombieStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            System.out.println("No zombies on the board.");
            return;
        }
        for (ZombieStatus status : statuses) {
            System.out.println(status.getZombieType() + ":");
            System.out.println("  position: " + this.formatNumber(status.getExactX() + 1)
                    + ", " + (status.getPosition().getY() + 1));
            System.out.println("  health: " + status.getHealth());
            System.out.println("  armor:");
            for (Map.Entry<String, Integer> armor : status.getArmorHealth().entrySet()) {
                System.out.println("    " + armor.getKey() + ": " + armor.getValue());
            }
            System.out.println("  effects:");
            for (Map.Entry<String, Long> effect : status.getEffectRemainingTicks().entrySet()) {
                String remaining = effect.getValue() == null
                        ? ""
                        : ": " + this.formatNumber(effect.getValue() / 10.0) + "s";
                System.out.println("    " + effect.getKey() + remaining);
            }
        }
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    @Override
    public void onGameEvent(String message) {
        System.out.println(message);
    }
}
