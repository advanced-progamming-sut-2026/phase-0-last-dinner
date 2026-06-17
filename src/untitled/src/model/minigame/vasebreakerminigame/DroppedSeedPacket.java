package model.minigame.vasebreakerminigame;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.plant.PlantDefinition;

@Getter
@Setter

public class DroppedSeedPacket {
    private PlantDefinition plantDefinition;
    private Position position;
    private long expirationTick;
    private boolean collected;

    public DroppedSeedPacket(
            PlantDefinition plantDefinition,
            Position position,
            long expirationTick
    ) {
        this.plantDefinition = plantDefinition;
        this.position = position;
        this.expirationTick = expirationTick;
        this.collected = false;
    }

    public boolean isExpired(long currentTick) {
        return this.expirationTick > 0 && currentTick >= this.expirationTick;
    }

    public void collect() {
        this.collected = true;
    }

    public boolean isAt(Position position) {
        return this.position.equals(position);
    }

    public boolean isAvailable(long currentTick) {
        return !this.collected && !isExpired(currentTick);
    }
}
