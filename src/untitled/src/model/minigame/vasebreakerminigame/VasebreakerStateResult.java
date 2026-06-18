package model.minigame.vasebreakerminigame;

import lombok.Getter;

import java.util.List;

@Getter
public class VasebreakerStateResult {
    private final long currentTick;
    private final List<Vase> vases;
    private final List<DroppedSeedPacket> droppedSeedPackets;
    private final boolean won;
    private final boolean lost;

    public VasebreakerStateResult(
            long currentTick,
            List<Vase> vases,
            List<DroppedSeedPacket> droppedSeedPackets,
            boolean won,
            boolean lost
    ) {
        this.currentTick = currentTick;
        this.vases = vases;
        this.droppedSeedPackets = droppedSeedPackets;
        this.won = won;
        this.lost = lost;
    }
}

