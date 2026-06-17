package model.minigame.vasebreakerminigame;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

@Getter
@Setter

public class Vase {
    private Position position;
    private VaseType type;
    private VaseContentType contentType;
    private PlantDefinition plantDefinition;
    private ZombieDefinition zombieDefinition;
    private boolean broken;

    public Vase(
            Position position,
            VaseType type,
            VaseContentType contentType,
            PlantDefinition plantDefinition,
            ZombieDefinition zombieDefinition
    ) {
        this.position = position;
        this.type = type;
        this.contentType = contentType;
        this.plantDefinition = plantDefinition;
        this.zombieDefinition = zombieDefinition;
        this.broken = false;
    }

    public void breakVase() {
        this.broken = true;
    }

    public boolean isAt(Position position) {
        return this.position != null && this.position.equals(position);
    }
}
