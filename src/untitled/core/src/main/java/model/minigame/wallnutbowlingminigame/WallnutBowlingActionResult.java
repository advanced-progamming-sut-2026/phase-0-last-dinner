package model.minigame.wallnutbowlingminigame;

import lombok.Getter;
import model.mechanism.Position;

@Getter
public class WallnutBowlingActionResult {
    private final WallnutBowlingActionStatus status;

    private final BowlingWallnutType wallnutType;

    private final Position position;

    private final int stageNumber;

    private final int conveyorIndex;

    private final int advancedTicks;

    private final boolean won;

    private final boolean lost;

    private WallnutBowlingActionResult(
            WallnutBowlingActionStatus status,
            BowlingWallnutType wallnutType,
            Position position,
            int stageNumber,
            int conveyorIndex,
            int advancedTicks,
            boolean won,
            boolean lost
    ) {
        this.status = status;
        this.wallnutType = wallnutType;
        this.position = position;
        this.stageNumber = stageNumber;
        this.conveyorIndex = conveyorIndex;
        this.advancedTicks = advancedTicks;
        this.won = won;
        this.lost = lost;
    }

    public static WallnutBowlingActionResult started(
            int stageNumber
    ) {
        return create(
                WallnutBowlingActionStatus.STARTED,
                null,
                null,
                stageNumber,
                -1,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult generated(
            BowlingWallnutType type
    ) {
        return create(
                WallnutBowlingActionStatus.WALLNUT_GENERATED,
                type,
                null,
                0,
                -1,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult placed(
            BowlingWallnutType type,
            Position position,
            int conveyorIndex,
            boolean won,
            boolean lost
    ) {
        return create(
                WallnutBowlingActionStatus.WALLNUT_PLACED,
                type,
                position,
                0,
                conveyorIndex,
                0,
                won,
                lost
        );
    }

    public static WallnutBowlingActionResult
    noWallnutAvailable() {
        return simple(
                WallnutBowlingActionStatus.NO_WALLNUT_AVAILABLE
        );
    }

    public static WallnutBowlingActionResult
    invalidConveyorIndex(
            int conveyorIndex
    ) {
        return create(
                WallnutBowlingActionStatus.INVALID_CONVEYOR_INDEX,
                null,
                null,
                0,
                conveyorIndex,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult invalidStage(
            int stageNumber
    ) {
        return create(
                WallnutBowlingActionStatus.INVALID_STAGE,
                null,
                null,
                stageNumber,
                -1,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult stageLocked(
            int stageNumber
    ) {
        return create(
                WallnutBowlingActionStatus.STAGE_LOCKED,
                null,
                null,
                stageNumber,
                -1,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult
    gameNotStarted() {
        return simple(
                WallnutBowlingActionStatus.GAME_NOT_STARTED
        );
    }

    public static WallnutBowlingActionResult
    gameAlreadyFinished(
            boolean won,
            boolean lost
    ) {
        return create(
                WallnutBowlingActionStatus.GAME_ALREADY_FINISHED,
                null,
                null,
                0,
                -1,
                0,
                won,
                lost
        );
    }

    public static WallnutBowlingActionResult
    invalidPosition(
            Position position
    ) {
        return create(
                WallnutBowlingActionStatus.INVALID_POSITION,
                null,
                position,
                0,
                -1,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult
    outsidePlantingArea(
            Position position
    ) {
        return create(
                WallnutBowlingActionStatus.OUTSIDE_PLANTING_AREA,
                null,
                position,
                0,
                -1,
                0,
                false,
                false
        );
    }

    public static WallnutBowlingActionResult
    integrationNotReady() {
        return simple(
                WallnutBowlingActionStatus.INTEGRATION_NOT_READY
        );
    }

    public static WallnutBowlingActionResult timeAdvanced(
            int ticks,
            boolean won,
            boolean lost
    ) {
        return create(
                WallnutBowlingActionStatus.TIME_ADVANCED,
                null,
                null,
                0,
                -1,
                ticks,
                won,
                lost
        );
    }

    public static WallnutBowlingActionResult
    invalidAction() {
        return simple(
                WallnutBowlingActionStatus.INVALID_ACTION
        );
    }

    private static WallnutBowlingActionResult simple(
            WallnutBowlingActionStatus status
    ) {
        return create(
                status,
                null,
                null,
                0,
                -1,
                0,
                false,
                false
        );
    }

    private static WallnutBowlingActionResult create(
            WallnutBowlingActionStatus status,
            BowlingWallnutType wallnutType,
            Position position,
            int stageNumber,
            int conveyorIndex,
            int advancedTicks,
            boolean won,
            boolean lost
    ) {
        return new WallnutBowlingActionResult(
                status,
                wallnutType,
                position,
                stageNumber,
                conveyorIndex,
                advancedTicks,
                won,
                lost
        );
    }
}