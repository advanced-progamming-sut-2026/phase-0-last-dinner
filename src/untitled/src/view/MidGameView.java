package view;

import controller.MidGameController;
import model.Plant;
import model.mechanism.*;
import model.zombie.Zombie;

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
            if (matcher == null) continue;

            switch (command) {
                case ADVANCE_TIME: {
                    int count = Integer.parseInt(matcher.group("count"));
                    observer.onAdvanceTimeRequested(count);
                    break;
                }
                case SHOW_MAP: {
                    showMap(observer.onShowMapRequested());
                    break;
                }
                case SHOW_SUN_AMOUNT: {
                    System.out.println("Sun: " + observer.onShowSunAmountRequested());
                    break;
                }
                case SHOW_PLANTS_STATUS: {
                    showPlantsStatus(observer.onShowPlantsStatusRequested());
                    break;
                }
                case SHOW_TILE_STATUS: {
                    int x = Integer.parseInt(matcher.group("x")) - 1;
                    int y = Integer.parseInt(matcher.group("y")) - 1;
                    showTileStatus(observer.onShowTileStatusRequested(x, y));
                    break;
                }
                case COLLECT_SUN: {
                    int x = Integer.parseInt(matcher.group("x")) - 1;
                    int y = Integer.parseInt(matcher.group("y")) - 1;
                    if (!observer.onCollectSunRequested(x, y)) {
                        System.out.println("No sun at (" + (x + 1) + ", " + (y + 1) + ").");
                    }
                    break;
                }
                case PLANT_PLANT: {
                    String type = matcher.group("type");
                    int x = Integer.parseInt(matcher.group("x")) - 1;
                    int y = Integer.parseInt(matcher.group("y")) - 1;
                    if (!observer.onPlantPlantRequested(type, x, y)) {
                        System.out.println("Cannot plant " + type + " at (" + (x + 1) + ", " + (y + 1) + ").");
                    }
                    break;
                }
                case PLUCK_PLANT: {
                    int x = Integer.parseInt(matcher.group("x")) - 1;
                    int y = Integer.parseInt(matcher.group("y")) - 1;
                    if (!observer.onPluckPlantRequested(x, y)) {
                        System.out.println("No plant at (" + (x + 1) + ", " + (y + 1) + ").");
                    }
                    break;
                }
                case FEED_PLANT: {
                    int x = Integer.parseInt(matcher.group("x")) - 1;
                    int y = Integer.parseInt(matcher.group("y")) - 1;
                    if (!observer.onFeedPlantRequested(x, y)) {
                        System.out.println("Cannot feed plant at (" + (x + 1) + ", " + (y + 1) + ").");
                    }
                    break;
                }
                case CHEAT_ADD_SUNS: {
                    int count = Integer.parseInt(matcher.group("count"));
                    observer.onCheatAddSunsRequested(count);
                    break;
                }
                case CHEAT_REMOVE_COOLDOWN: {
                    observer.onCheatRemoveCooldownRequested();
                    break;
                }
                case CHEAT_ADD_PLANT_FOOD: {
                    observer.onCheatAddPlantFoodRequested();
                    break;
                }
                case RELEASE_THE_NUKE: {
                    observer.onReleaseTheNukeRequested();
                    break;
                }
                case ZOMBIES_INFO: {
                    showZombiesInfo(observer.onZombiesInfoRequested());
                    break;
                }
                case SPAWN_ZOMBIE: {
                    String type = matcher.group("type");
                    int x = Integer.parseInt(matcher.group("x")) - 1;
                    int y = Integer.parseInt(matcher.group("y")) - 1;
                    if (!observer.onSpawnZombieRequested(type, x, y)) {
                        System.out.println("Could not spawn zombie.");
                    }
                    break;
                }
                default: {
                    System.out.println("Invalid command.");
                    break;
                }
            }
            return;
        }

        System.out.println("Invalid command.");
    }

    private void showMap(Board board) {
        if (board == null) {
            System.out.println("No board available.");
            return;
        }

        System.out.println("Wave: " + observer.getCurrentWaveNumber());
        System.out.println("Sun: " + observer.onShowSunAmountRequested());
        System.out.println("Plant Food: " + observer.onShowPlantFoodCountRequested());
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
                terrain = "†";
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
        System.out.println("Plants:");
        if (tile.getPlants().isEmpty()) {
            System.out.println("  none");
        } else {
            for (Plant plant : tile.getPlants()) {
                System.out.println("  - " + plant.getName()
                        + " | hp: " + plant.getHealth()
                        + "/" + plant.getMaximumHealth());
            }
        }
        System.out.println("Zombies:");
        if (tile.getZombies().isEmpty()) {
            System.out.println("  none");
        } else {
            for (Zombie zombie : tile.getZombies()) {
                System.out.println("  - " + zombie.getDefinition().getDisplayName()
                        + " | hp: " + zombie.getHealth());
            }
        }
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
