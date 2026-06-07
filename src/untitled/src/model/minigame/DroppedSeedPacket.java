package model.minigame;

import model.mechanism.Position;
import model.plant.PlantDefinition;

public class DroppedSeedPacket {
    private PlantDefinition plantDefinition;
    private Position position;
    private long expirationTick;
    private boolean collected;

    public boolean isExpired(long currentTick) {
        return false;
    }

    public void collect() {
    }
}
