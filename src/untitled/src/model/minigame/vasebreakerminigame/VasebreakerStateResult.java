package model.minigame.vasebreakerminigame;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class VasebreakerStateResult {
    private final int stageNumber;

    private final long currentTick;

    private final List<Vase> vases;

    private final List<DroppedSeedPacket> droppedSeedPackets;

    private final List<DroppedSeedPacket> collectedSeedPackets;

    private final boolean started;

    private final boolean won;

    private final boolean lost;

    public VasebreakerStateResult(
            int stageNumber,
            long currentTick,
            List<Vase> vases,
            List<DroppedSeedPacket> droppedSeedPackets,
            List<DroppedSeedPacket> collectedSeedPackets,
            boolean started,
            boolean won,
            boolean lost
    ) {
        this.stageNumber = stageNumber;
        this.currentTick = currentTick;

        this.vases = immutableCopy(vases);

        this.droppedSeedPackets = immutableCopy(
                droppedSeedPackets
        );

        this.collectedSeedPackets = immutableCopy(
                collectedSeedPackets
        );

        this.started = started;
        this.won = won;
        this.lost = lost;
    }

    public VasebreakerStateResult(
            long currentTick,
            List<Vase> vases,
            List<DroppedSeedPacket> droppedSeedPackets,
            boolean won,
            boolean lost
    ) {
        this(
                1,
                currentTick,
                vases,
                droppedSeedPackets,
                Collections.emptyList(),
                true,
                won,
                lost
        );
    }

    public int getRemainingVaseCount() {
        int count = 0;

        for (Vase vase : vases) {
            if (vase != null && !vase.isBroken()) {
                count++;
            }
        }

        return count;
    }

    public int getBrokenVaseCount() {
        int count = 0;

        for (Vase vase : vases) {
            if (vase != null && vase.isBroken()) {
                count++;
            }
        }

        return count;
    }

    public int getAvailableDroppedPacketCount() {
        int count = 0;

        for (DroppedSeedPacket packet : droppedSeedPackets) {
            if (packet != null
                    && packet.isAvailable(currentTick)) {
                count++;
            }
        }

        return count;
    }

    public int getCollectedPacketCount() {
        int count = 0;

        for (DroppedSeedPacket packet : collectedSeedPackets) {
            if (packet != null && packet.isPlantable()) {
                count++;
            }
        }

        return count;
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