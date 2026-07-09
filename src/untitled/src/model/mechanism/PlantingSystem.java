package model.mechanism;

import model.Plant;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.PlantTag;
import model.plant.behavior.OnPlantingBehavior;

public class PlantingSystem {
    private Board board;
    private SunSystem sunSystem;
    private PlantCooldownManager cooldownManager;

    public PlantingSystem() {
    }

    public PlantingSystem(Board board, SunSystem sunSystem, PlantCooldownManager cooldownManager) {
        this.board = board;
        this.sunSystem = sunSystem;
        this.cooldownManager = cooldownManager;
    }

    public boolean canPlant(Plant plant, Position position) {
        if (plant == null || position == null || this.board == null) {
            return false;
        }

        if (this.sunSystem != null && this.sunSystem.getSunAmount() < plant.getSunCost()) {
            return false;
        }

        if (this.cooldownManager != null && !this.cooldownManager.isAvailable(plant)) {
            return false;
        }

        Tile tile = this.board.getTile(position);

        if (tile == null || !tile.canPlacePlant(plant)) {
            return false;
        }

        return this.canStackOnTile(plant, tile);
    }

    public void plant(Plant plant, Position position) {
        if (!this.canPlant(plant, position)) {
            return;
        }

        plant.setPosition(position);
        plant.setBoard(this.board);

        Tile tile = this.board.getTile(position);

        if (tile == null) {
            return;
        }

        tile.addPlant(plant);
        this.spendSun(plant);
        this.startCooldown(plant);

        if (plant.getBehavior() instanceof OnPlantingBehavior) {
            OnPlantingBehavior onPlantingBehavior = (OnPlantingBehavior) plant.getBehavior();

            if (onPlantingBehavior.shouldActivateOnPlanting()) {
                plant.useAbility();
            }
        }

        this.activateUpgradeSpecialsOnPlanting(plant);
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
        if (this.cooldownManager != null) {
            this.cooldownManager.removeAllCooldowns();
        }
    }

    private boolean canStackOnTile(Plant plant, Tile tile) {
        if (tile.getPlants() == null || tile.getPlants().isEmpty()) {
            return true;
        }

        Plant topPlant = tile.getPlants().get(tile.getPlants().size() - 1);

        // Stack faghat vaghti mojaze ke yeki az do plant stack-support bashe.
        return this.hasStackTag(topPlant) || this.hasStackTag(plant);
    }

    private boolean hasStackTag(Plant plant) {
        return plant != null
                && plant.getTags() != null
                && plant.getTags().contains(PlantTag.STACK);
    }

    private void spendSun(Plant plant) {
        if (this.sunSystem != null) {
            this.sunSystem.addSun(-plant.getSunCost());
        }
    }

    private void startCooldown(Plant plant) {
        if (this.cooldownManager != null) {
            this.cooldownManager.startCooldown(plant);
        }
    }

    private void activateUpgradeSpecialsOnPlanting(Plant plant) {
        if (plant == null) {
            return;
        }

        if (plant.hasUpgradeSpecialEffect(PlantUpgradeSpecialEffect.PLANT_FOOD_ON_PLANTING)) {
            plant.receivePlantFood();
        }

        if (plant.hasUpgradeSpecialEffect(PlantUpgradeSpecialEffect.RESET_FAMILY_COOLDOWNS)
                && this.cooldownManager != null) {
            this.cooldownManager.resetAllCooldowns();
        }
    }
}
