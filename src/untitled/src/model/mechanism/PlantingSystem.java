package model.mechanism;

import model.Plant;
import model.plant.behavior.OnPlantingBehavior;

public class PlantingSystem {
    private Board board;
    private SunSystem sunSystem;
    private PlantCooldownManager cooldownManager;

    public boolean canPlant(Plant plant, Position position) {
        return false;
    }

    public void plant(Plant plant, Position position) {
        if (plant == null || position == null || this.board == null) {
            return;
        }

        plant.setPosition(position);
        plant.setBoard(this.board);

        Tile tile = this.board.getTile(position);

        if (tile == null) {
            return;
        }

        tile.addPlant(plant);

        if (plant.getBehavior() instanceof OnPlantingBehavior) {
            OnPlantingBehavior onPlantingBehavior = (OnPlantingBehavior) plant.getBehavior();

            if (onPlantingBehavior.shouldActivateOnPlanting()) {
                plant.useAbility();
            }
        }
    }

    public void pluck(Position position) {
        if (position == null || this.board == null) {
            return;
        }

        Tile tile = this.board.getTile(position);

        if (tile == null) {
            return;
        }

        Plant removedPlant = tile.removeTopPlant();

        if (removedPlant != null) {
            removedPlant.setPosition(null);
            removedPlant.setBoard(null);
        }
    }

    public void removeAllCooldowns() {
    }
}
