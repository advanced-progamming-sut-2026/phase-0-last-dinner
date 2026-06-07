package model.mechanism;

import model.Plant;

public class PlantingSystem {
    private Board board;
    private SunSystem sunSystem;
    private PlantCooldownManager cooldownManager;

    public boolean canPlant(Plant plant, Position position) {
        return false;
    }

    public void plant(Plant plant, Position position) {
    }

    public void pluck(Position position) {

    }

    public void removeAllCooldowns() {
    }
}
