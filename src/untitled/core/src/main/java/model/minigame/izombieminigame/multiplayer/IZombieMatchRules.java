package model.minigame.izombieminigame.multiplayer;

import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieType;
import network.izombie.protocol.IZombieRole;

public final class IZombieMatchRules {
    public static final int BOARD_COLUMN_COUNT = 9;
    public static final int BOARD_ROW_COUNT = 5;

    public static final int TICKS_PER_SECOND = 10;
    public static final int MATCH_DURATION_SECONDS = 120;
    public static final long MATCH_DURATION_TICKS = (long) MATCH_DURATION_SECONDS * TICKS_PER_SECOND;

    public static final int STARTING_PLANT_SUN = 500;
    public static final int STARTING_ZOMBIE_SUN = 500;

    public static final int PASSIVE_SUN_AMOUNT = 50;
    public static final int PASSIVE_SUN_INTERVAL_TICKS = 50;

    public static final int FIRST_ZOMBIE_COLUMN = 5;

    private IZombieMatchRules() {
    }

    public static int getStartingSun(IZombieRole role) {
        if (role == IZombieRole.PLANTS) {
            return STARTING_PLANT_SUN;
        }

        if (role == IZombieRole.ZOMBIES) {
            return STARTING_ZOMBIE_SUN;
        }

        throw new IllegalArgumentException("Player role is required.");
    }

    public static boolean shouldProducePassiveSun(long serverTick) {
        return serverTick > 0 && serverTick % PASSIVE_SUN_INTERVAL_TICKS == 0;
    }

    public static boolean isInsideBoard(int column, int row) {
        return column >= 0 && column < BOARD_COLUMN_COUNT && row >= 0 && row < BOARD_ROW_COUNT;
    }

    public static boolean isPlantPlacementPosition(int column, int row) {
        return isInsideBoard(column, row) && column < FIRST_ZOMBIE_COLUMN;
    }

    public static boolean isZombiePlacementPosition(int column, int row) {
        return isInsideBoard(column, row) && column >= FIRST_ZOMBIE_COLUMN;
    }

    public static int getPlantCooldownTicks(PlantDefinition definition) {
        if (definition == null) {
            return 0;
        }

        double rechargeSeconds = Math.max(0, definition.getRechargeSeconds());

        return (int) Math.ceil(rechargeSeconds * TICKS_PER_SECOND);
    }

    public static int getZombieCooldownTicks(ZombieDefinition definition) {
        if (definition == null || definition.getType() == null) {
            return 20;
        }

        ZombieType type = definition.getType();

        return switch (type) {
            case ANIMAL -> 30;
            case ARMORED -> 40;
            case SPECIAL -> 50;
            case GARGANTUAR -> 80;
            case BOSS -> 100;
            default -> 20;
        };
    }
}
