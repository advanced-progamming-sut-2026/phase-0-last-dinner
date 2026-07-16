package model.minigame.izombieminigame;

public class IZombieStageConfig {

    public static final int BOARD_ROW_COUNT = 5;
    public static final int AVAILABLE_ZOMBIE_COUNT = 5;

    private final int stageNumber;
    private final int startingSun;
    private final int redLineColumn;

    private final int sunProductionAmount;
    private final long initialSunProductionIntervalTicks;
    private final long minimumSunProductionIntervalTicks;
    private final long sunProductionIntervalDecreaseTicks;

    public IZombieStageConfig(
            int stageNumber,
            int startingSun,
            int redLineColumn,
            int sunProductionAmount,
            long initialSunProductionIntervalTicks,
            long minimumSunProductionIntervalTicks,
            long sunProductionIntervalDecreaseTicks
    ) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException(
                    "Stage number must be between 1 and 3."
            );
        }

        if (startingSun < 0) {
            throw new IllegalArgumentException(
                    "Starting sun cannot be negative."
            );
        }

        if (redLineColumn < 1 || redLineColumn > 9) {
            throw new IllegalArgumentException(
                    "Red line column must be between 1 and 9."
            );
        }

        if (sunProductionAmount <= 0) {
            throw new IllegalArgumentException(
                    "Sun production amount must be positive."
            );
        }

        if (initialSunProductionIntervalTicks <= 0) {
            throw new IllegalArgumentException(
                    "Initial sun production interval must be positive."
            );
        }

        if (minimumSunProductionIntervalTicks <= 0) {
            throw new IllegalArgumentException(
                    "Minimum sun production interval must be positive."
            );
        }

        if (minimumSunProductionIntervalTicks
                > initialSunProductionIntervalTicks) {
            throw new IllegalArgumentException(
                    "Minimum interval cannot be greater than initial interval."
            );
        }

        if (sunProductionIntervalDecreaseTicks < 0) {
            throw new IllegalArgumentException(
                    "Interval decrease cannot be negative."
            );
        }

        this.stageNumber = stageNumber;
        this.startingSun = startingSun;
        this.redLineColumn = redLineColumn;
        this.sunProductionAmount = sunProductionAmount;
        this.initialSunProductionIntervalTicks =
                initialSunProductionIntervalTicks;
        this.minimumSunProductionIntervalTicks =
                minimumSunProductionIntervalTicks;
        this.sunProductionIntervalDecreaseTicks =
                sunProductionIntervalDecreaseTicks;
    }

    public boolean isZombiePlacementColumn(int column) {
        return column > redLineColumn && column <= 9;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public int getStartingSun() {
        return startingSun;
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public int getSunProductionAmount() {
        return sunProductionAmount;
    }

    public long getInitialSunProductionIntervalTicks() {
        return initialSunProductionIntervalTicks;
    }

    public long getMinimumSunProductionIntervalTicks() {
        return minimumSunProductionIntervalTicks;
    }

    public long getSunProductionIntervalDecreaseTicks() {
        return sunProductionIntervalDecreaseTicks;
    }
}