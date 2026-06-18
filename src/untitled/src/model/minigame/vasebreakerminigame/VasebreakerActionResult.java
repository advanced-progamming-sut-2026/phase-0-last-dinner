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

    private VasebreakerActionResult(
            VasebreakerActionStatus status,
            Position position,
            VaseContentType contentType,
            DroppedSeedPacket droppedSeedPacket,
            boolean zombieReleased,
            boolean won,
            boolean lost,
            int advancedTicks
    ) {
        this.status = status;
        this.position = position;
        this.contentType = contentType;
        this.droppedSeedPacket = droppedSeedPacket;
        this.zombieReleased = zombieReleased;
        this.won = won;
        this.lost = lost;
        this.advancedTicks = advancedTicks;
    }

    public static VasebreakerActionResult started(boolean won, boolean lost) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.STARTED,
                null,
                null,
                null,
                false,
                won,
                lost,
                0
        );
    }

    public static VasebreakerActionResult noVase(Position position) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.NO_VASE_AT_POSITION,
                position,
                null,
                null,
                false,
                false,
                false,
                0
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
        return new VasebreakerActionResult(
                VasebreakerActionStatus.VASE_BROKEN,
                position,
                contentType,
                droppedSeedPacket,
                zombieReleased,
                won,
                lost,
                0
        );
    }

    public static VasebreakerActionResult noSeedPacket(Position position) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.NO_SEED_PACKET_AT_POSITION,
                position,
                null,
                null,
                false,
                false,
                false,
                0
        );
    }

    public static VasebreakerActionResult seedPacketNotAvailable(Position position) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.SEED_PACKET_NOT_AVAILABLE,
                position,
                null,
                null,
                false,
                false,
                false,
                0
        );
    }

    public static VasebreakerActionResult seedPacketCollected(
            Position position,
            boolean won,
            boolean lost
    ) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.SEED_PACKET_COLLECTED,
                position,
                null,
                null,
                false,
                won,
                lost,
                0
        );
    }

    public static VasebreakerActionResult timeAdvanced(
            int ticks,
            boolean won,
            boolean lost
    ) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.TIME_ADVANCED,
                null,
                null,
                null,
                false,
                won,
                lost,
                ticks
        );
    }

    public static VasebreakerActionResult invalidAction(Position position) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.INVALID_ACTION,
                position,
                null,
                null,
                false,
                false,
                false,
                0
        );
    }

    public static VasebreakerActionResult plantFromPacket(
            Position position,
            boolean won,
            boolean lost
    ) {
        return new VasebreakerActionResult(
                VasebreakerActionStatus.PLANT_FROM_PACKET,
                position,
                null,
                null,
                false,
                won,
                lost,
                0
        );
    }
}