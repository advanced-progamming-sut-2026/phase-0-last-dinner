package model.minigame.vasebreakerminigame;

import lombok.Getter;
import model.mechanism.Position;

@Getter
public class VasebreakerActionResult {
    private final VasebreakerActionStatus status;

    private final Position position;

    private final VaseContentType contentType;

    private final DroppedSeedPacket droppedSeedPacket;

    private final boolean zombieReleased;

    private final boolean won;

    private final boolean lost;

    private final int advancedTicks;

    private final int stageNumber;

    private final String plantName;

    private VasebreakerActionResult(
            VasebreakerActionStatus status,
            Position position,
            VaseContentType contentType,
            DroppedSeedPacket droppedSeedPacket,
            boolean zombieReleased,
            boolean won,
            boolean lost,
            int advancedTicks,
            int stageNumber,
            String plantName
    ) {
        this.status = status;
        this.position = position;
        this.contentType = contentType;
        this.droppedSeedPacket = droppedSeedPacket;
        this.zombieReleased = zombieReleased;
        this.won = won;
        this.lost = lost;
        this.advancedTicks = advancedTicks;
        this.stageNumber = stageNumber;
        this.plantName = plantName;
    }

    public static VasebreakerActionResult started(
            int stageNumber,
            boolean won,
            boolean lost
    ) {
        return create(
                VasebreakerActionStatus.STARTED,
                null,
                null,
                null,
                false,
                won,
                lost,
                0,
                stageNumber,
                null
        );
    }

    public static VasebreakerActionResult started(
            boolean won,
            boolean lost
    ) {
        return started(1, won, lost);
    }

    public static VasebreakerActionResult invalidStage(int stageNumber) {
        return create(
                VasebreakerActionStatus.INVALID_STAGE,
                null,
                null,
                null,
                false,
                false,
                false,
                0,
                stageNumber,
                null
        );
    }

    public static VasebreakerActionResult stageLocked(int stageNumber) {
        return create(
                VasebreakerActionStatus.STAGE_LOCKED,
                null,
                null,
                null,
                false,
                false,
                false,
                0,
                stageNumber,
                null
        );
    }

    public static VasebreakerActionResult gameNotStarted() {
        return simple(
                VasebreakerActionStatus.GAME_NOT_STARTED,
                null
        );
    }

    public static VasebreakerActionResult gameAlreadyFinished(
            boolean won,
            boolean lost
    ) {
        return create(
                VasebreakerActionStatus.GAME_ALREADY_FINISHED,
                null,
                null,
                null,
                false,
                won,
                lost,
                0,
                0,
                null
        );
    }

    public static VasebreakerActionResult invalidPosition(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.INVALID_POSITION,
                position
        );
    }

    public static VasebreakerActionResult noVase(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.NO_VASE_AT_POSITION,
                position
        );
    }

    public static VasebreakerActionResult vaseBroken(
            Position position,
            VaseContentType contentType,
            DroppedSeedPacket droppedSeedPacket,
            boolean zombieReleased,
            boolean won,
            boolean lost
    ) {
        String plantName = null;

        if (droppedSeedPacket != null
                && droppedSeedPacket.getPlantDefinition() != null) {
            plantName = droppedSeedPacket
                    .getPlantDefinition()
                    .getName();
        }

        return create(
                VasebreakerActionStatus.VASE_BROKEN,
                position,
                contentType,
                droppedSeedPacket,
                zombieReleased,
                won,
                lost,
                0,
                0,
                plantName
        );
    }

    public static VasebreakerActionResult noSeedPacket(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.NO_SEED_PACKET_AT_POSITION,
                position
        );
    }

    public static VasebreakerActionResult seedPacketNotAvailable(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.SEED_PACKET_NOT_AVAILABLE,
                position
        );
    }

    public static VasebreakerActionResult noCollectedSeedPacket(
            String plantName
    ) {
        return create(
                VasebreakerActionStatus.NO_COLLECTED_SEED_PACKET,
                null,
                null,
                null,
                false,
                false,
                false,
                0,
                0,
                plantName
        );
    }

    public static VasebreakerActionResult seedPacketCollected(
            Position position,
            String plantName,
            boolean won,
            boolean lost
    ) {
        return create(
                VasebreakerActionStatus.SEED_PACKET_COLLECTED,
                position,
                null,
                null,
                false,
                won,
                lost,
                0,
                0,
                plantName
        );
    }

    public static VasebreakerActionResult seedPacketCollected(
            Position position,
            boolean won,
            boolean lost
    ) {
        return seedPacketCollected(
                position,
                null,
                won,
                lost
        );
    }

    public static VasebreakerActionResult tileOccupied(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.TILE_OCCUPIED,
                position
        );
    }

    public static VasebreakerActionResult tileHasUnbrokenVase(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.TILE_HAS_UNBROKEN_VASE,
                position
        );
    }

    public static VasebreakerActionResult timeAdvanced(
            int ticks,
            boolean won,
            boolean lost
    ) {
        return create(
                VasebreakerActionStatus.TIME_ADVANCED,
                null,
                null,
                null,
                false,
                won,
                lost,
                ticks,
                0,
                null
        );
    }

    public static VasebreakerActionResult plantFromPacket(
            Position position,
            String plantName,
            boolean won,
            boolean lost
    ) {
        return create(
                VasebreakerActionStatus.PLANT_FROM_PACKET,
                position,
                null,
                null,
                false,
                won,
                lost,
                0,
                0,
                plantName
        );
    }

    public static VasebreakerActionResult plantFromPacket(
            Position position,
            boolean won,
            boolean lost
    ) {
        return plantFromPacket(
                position,
                null,
                won,
                lost
        );
    }

    public static VasebreakerActionResult invalidAction(
            Position position
    ) {
        return simple(
                VasebreakerActionStatus.INVALID_ACTION,
                position
        );
    }

    private static VasebreakerActionResult simple(
            VasebreakerActionStatus status,
            Position position
    ) {
        return create(
                status,
                position,
                null,
                null,
                false,
                false,
                false,
                0,
                0,
                null
        );
    }

    private static VasebreakerActionResult create(
            VasebreakerActionStatus status,
            Position position,
            VaseContentType contentType,
            DroppedSeedPacket droppedSeedPacket,
            boolean zombieReleased,
            boolean won,
            boolean lost,
            int advancedTicks,
            int stageNumber,
            String plantName
    ) {
        return new VasebreakerActionResult(
                status,
                position,
                contentType,
                droppedSeedPacket,
                zombieReleased,
                won,
                lost,
                advancedTicks,
                stageNumber,
                plantName
        );
    }
}