package model.minigame.wallnutbowlingminigame;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class WallnutBowlingStateResult {
    private final int stageNumber;

    private final long currentTick;

    private final List<BowlingWallnutType> conveyorBelt;

    private final List<RollingWallnut> rollingWallnuts;

    private final int plantingBoundaryColumn;

    private final long ticksUntilNextGeneration;

    private final boolean started;

    private final boolean integrationReady;

    private final boolean won;

    private final boolean lost;

    public WallnutBowlingStateResult(
            int stageNumber,
            long currentTick,
            List<BowlingWallnutType> conveyorBelt,
            List<RollingWallnut> rollingWallnuts,
            int plantingBoundaryColumn,
            long ticksUntilNextGeneration,
            boolean started,
            boolean integrationReady,
            boolean won,
            boolean lost
    ) {
        this.stageNumber = stageNumber;
        this.currentTick = currentTick;

        this.conveyorBelt = immutableCopy(
                conveyorBelt
        );

        this.rollingWallnuts = immutableCopy(
                rollingWallnuts
        );

        this.plantingBoundaryColumn =
                plantingBoundaryColumn;

        this.ticksUntilNextGeneration = Math.max(
                0,
                ticksUntilNextGeneration
        );

        this.started = started;
        this.integrationReady = integrationReady;
        this.won = won;
        this.lost = lost;
    }

    public int getAvailableWallnutCount() {
        return conveyorBelt.size();
    }

    public int getMovingWallnutCount() {
        int count = 0;

        for (RollingWallnut wallnut
                : rollingWallnuts) {

            if (wallnut != null
                    && wallnut.isMoving()) {
                count++;
            }
        }

        return count;
    }

    public int getExplodedWallnutCount() {
        int count = 0;

        for (RollingWallnut wallnut
                : rollingWallnuts) {

            if (wallnut != null
                    && wallnut.isExploded()) {
                count++;
            }
        }

        return count;
    }

    public BowlingWallnutType getWallnutAtIndex(
            int userIndex
    ) {
        int listIndex = userIndex - 1;

        if (listIndex < 0
                || listIndex >= conveyorBelt.size()) {
            return null;
        }

        return conveyorBelt.get(listIndex);
    }

    private static <T> List<T> immutableCopy(
            List<T> source
    ) {
        if (source == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(source)
        );
    }
}