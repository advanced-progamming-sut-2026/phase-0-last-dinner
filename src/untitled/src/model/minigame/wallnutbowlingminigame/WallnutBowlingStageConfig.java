package model.minigame.wallnutbowlingminigame;

import lombok.Getter;

@Getter
public class WallnutBowlingStageConfig {
    private final int stageNumber;

    private final int plantingBoundaryColumn;

    private final int conveyorCapacity;

    private final int initialWallnutCount;

    private final long generationIntervalTicks;

    private final int movementIntervalTicks;

    private final int normalWallnutChance;

    private final int explodeONutChance;

    private final int giantWallnutChance;

    public WallnutBowlingStageConfig(
            int stageNumber,
            int plantingBoundaryColumn,
            int conveyorCapacity,
            int initialWallnutCount,
            long generationIntervalTicks,
            int movementIntervalTicks,
            int normalWallnutChance,
            int explodeONutChance,
            int giantWallnutChance
    ) {
        validatePositive(
                stageNumber,
                "Stage number"
        );

        validatePositive(
                plantingBoundaryColumn,
                "Planting boundary column"
        );

        validatePositive(
                conveyorCapacity,
                "Conveyor capacity"
        );

        validatePositive(
                initialWallnutCount,
                "Initial wallnut count"
        );

        validatePositive(
                generationIntervalTicks,
                "Generation interval"
        );

        validatePositive(
                movementIntervalTicks,
                "Movement interval"
        );

        validateChance(
                normalWallnutChance,
                "Normal wallnut chance"
        );

        validateChance(
                explodeONutChance,
                "Explode-O-Nut chance"
        );

        validateChance(
                giantWallnutChance,
                "Giant wallnut chance"
        );

        int totalChance =
                normalWallnutChance
                        + explodeONutChance
                        + giantWallnutChance;

        if (totalChance != 100) {
            throw new IllegalArgumentException(
                    "Wallnut chances must add up to 100."
            );
        }

        if (initialWallnutCount > conveyorCapacity) {
            throw new IllegalArgumentException(
                    "Initial wallnut count cannot be "
                            + "greater than conveyor capacity."
            );
        }

        this.stageNumber = stageNumber;

        this.plantingBoundaryColumn =
                plantingBoundaryColumn;

        this.conveyorCapacity =
                conveyorCapacity;

        this.initialWallnutCount =
                initialWallnutCount;

        this.generationIntervalTicks =
                generationIntervalTicks;

        this.movementIntervalTicks =
                movementIntervalTicks;

        this.normalWallnutChance =
                normalWallnutChance;

        this.explodeONutChance =
                explodeONutChance;

        this.giantWallnutChance =
                giantWallnutChance;
    }

    private void validatePositive(
            long value,
            String fieldName
    ) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive."
            );
        }
    }

    private void validateChance(
            int chance,
            String fieldName
    ) {
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be between 0 and 100."
            );
        }
    }
}