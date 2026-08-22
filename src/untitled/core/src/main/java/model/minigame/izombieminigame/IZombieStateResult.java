package model.minigame.izombieminigame;

import lombok.Getter;
import model.zombie.ZombieDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter

public class IZombieStateResult {

    private final int stageNumber;
    private final int stageCount;

    private final int sunAmount;
    private final int redLineColumn;

    private final List<ZombieDefinition> availableZombies;
    private final Map<ZombieDefinition, Integer> zombieCosts;
    private final Map<Integer, Boolean> brainEatenByRow;

    private final int placedZombieCount;
    private final boolean alivePlayerZombies;

    private final boolean started;
    private final boolean completed;
    private final boolean won;
    private final boolean lost;
    private final Map<ZombieDefinition, Integer> zombieCooldownTicks;

    public IZombieStateResult(
            int stageNumber,
            int stageCount,
            int sunAmount,
            int redLineColumn,
            List<ZombieDefinition> availableZombies,
            Map<ZombieDefinition, Integer> zombieCosts,
            Map<Integer, Boolean> brainEatenByRow,
            Map<ZombieDefinition, Integer> zombieCooldownTicks,
            int placedZombieCount,
            boolean alivePlayerZombies,
            boolean started,
            boolean completed,
            boolean won,
            boolean lost
    ) {
        this.stageNumber = stageNumber;
        this.stageCount = stageCount;
        this.sunAmount = Math.max(0, sunAmount);
        this.redLineColumn = redLineColumn;

        this.availableZombies = availableZombies == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(availableZombies));

        this.zombieCosts = zombieCosts == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(zombieCosts));

        this.brainEatenByRow = brainEatenByRow == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(brainEatenByRow));

        this.zombieCooldownTicks = zombieCooldownTicks == null ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(zombieCooldownTicks));

        this.placedZombieCount = Math.max(0, placedZombieCount);
        this.alivePlayerZombies = alivePlayerZombies;
        this.started = started;
        this.completed = completed;
        this.won = won;
        this.lost = lost;
    }

    public int getZombieCost(ZombieDefinition definition) {
        if (definition == null) {
            return -1;
        }

        return zombieCosts.getOrDefault(definition, -1);
    }

    public boolean canAfford(ZombieDefinition definition) {
        int cost = getZombieCost(definition);

        return cost >= 0 && sunAmount >= cost;
    }

    public boolean isBrainEaten(int row) {
        return brainEatenByRow.getOrDefault(row, false);
    }

    public int getEatenBrainCount() {
        int count = 0;

        for (boolean eaten : brainEatenByRow.values()) {
            if (eaten) {
                count++;
            }
        }

        return count;
    }

    public int getRemainingBrainCount() {
        return Math.max(
                0,
                IZombieStageConfig.BOARD_ROW_COUNT - getEatenBrainCount()
        );
    }

    public boolean hasAlivePlayerZombies() {
        return alivePlayerZombies;
    }

    public int getZombieCooldownTicks(ZombieDefinition definition) {
        if (definition == null)
            return 0;

        return Math.max(0, zombieCooldownTicks.getOrDefault(definition, 0));
    }

    public boolean isZombieReady(ZombieDefinition definition) {
        return getZombieCooldownTicks(definition) <= 0;
    }

    public boolean canPlaceZombie(ZombieDefinition definition) {
        return canAfford(definition) && isZombieReady(definition);
    }
}
