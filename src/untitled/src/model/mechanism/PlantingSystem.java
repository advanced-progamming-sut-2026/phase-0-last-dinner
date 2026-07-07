package model.mechanism;

import model.Plant;
import model.plant.PlantTag;
import model.plant.behavior.OnPlantingBehavior;

public class PlantingSystem {
    private Board board;
    private SunSystem sunSystem;
    private PlantCooldownManager cooldownManager;

    public PlantingSystem(Board board, SunSystem sunSystem,
                          PlantCooldownManager cooldownManager) {
        this.board = board;
        this.sunSystem = sunSystem;
        this.cooldownManager = cooldownManager;
    }

    public boolean canPlant(Plant plant, Position position) {
        if (plant == null || position == null || board == null) return false;
        if (sunSystem.getSunAmount() < plant.getSunCost()) return false;
        if (!cooldownManager.isAvailable(plant)) return false;
        Tile tile = board.getTile(position);
        if (tile == null || !tile.canPlacePlant(plant)) return false;
        if (!tile.getPlants().isEmpty()) {
            Plant topPlant = tile.getPlants().get(tile.getPlants().size() - 1);
            boolean topAllowsStack = topPlant.getTags().contains(PlantTag.STACK);
            boolean newAllowsStack = plant.getTags().contains(PlantTag.STACK);
            if (!topAllowsStack && !newAllowsStack) return false;
        }
        return true;
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
        sunSystem.addSun(-plant.getSunCost());
        cooldownManager.startCooldown(plant);
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
        cooldownManager.removeAllCooldowns();
    }
}
