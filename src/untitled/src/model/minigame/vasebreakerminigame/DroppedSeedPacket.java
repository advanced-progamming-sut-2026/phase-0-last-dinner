package model.minigame.vasebreakerminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.plant.PlantDefinition;

@Getter
public class DroppedSeedPacket {
    private final PlantDefinition plantDefinition;

    private final Position position;

    private final long expirationTick;

    private boolean collected;

    private boolean used;

    public DroppedSeedPacket(
            PlantDefinition plantDefinition,
            Position position,
            long expirationTick
    ) {
        this.plantDefinition = plantDefinition;
        this.position = position;
        this.expirationTick = expirationTick;
        this.collected = false;
        this.used = false;
    }

    public boolean isExpired(long currentTick) {
        if (collected) {
            return false;
        }

        return expirationTick > 0
                && currentTick >= expirationTick;
    }

    public boolean isAvailable(long currentTick) {
        return !collected
                && !used
                && !isExpired(currentTick);
    }

    public boolean collect(long currentTick) {
        if (!isAvailable(currentTick)) {
            return false;
        }

        collected = true;
        return true;
    }

    public void collect() {
        if (!used) {
            collected = true;
        }
    }

    public boolean isPlantable() {
        return collected && !used;
    }

    public boolean use() {
        if (!isPlantable()) {
            return false;
        }

        used = true;
        return true;
    }

    public boolean isAt(Position targetPosition) {
        return position != null
                && position.equals(targetPosition);
    }

    public long getRemainingTicks(long currentTick) {
        if (collected || used) {
            return 0;
        }

        return Math.max(
                0,
                expirationTick - currentTick
        );
    }

    public String getPlantName() {
        if (plantDefinition == null) {
            return null;
        }

        return plantDefinition.getName();
    }
}