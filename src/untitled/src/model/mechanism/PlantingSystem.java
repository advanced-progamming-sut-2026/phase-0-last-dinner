package model.mechanism;

import model.Plant;
import model.plant.PlantTag;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.behavior.ModifierBehavior;
import model.plant.behavior.OnPlantingBehavior;

import java.util.Locale;

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
            OnPlantingBehavior plantingBehavior = (OnPlantingBehavior) plant.getBehavior();

            if (plantingBehavior.shouldActivateOnPlanting()) {
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

        String newName = this.normalizedName(plant);

        if ("pea pod".equals(newName)) {
            if (tile.getPlants().size() >= 5) {
                return false;
            }

            for (Plant existingPlant : tile.getPlants()) {
                if (!"pea pod".equals(this.normalizedName(existingPlant))) {
                    return false;
                }
            }

            return true;
        }

        for (Plant existingPlant : tile.getPlants()) {
            if ("pea pod".equals(this.normalizedName(existingPlant))) {
                return false;
            }
        }

        if ("lily pad".equals(newName)) {
            return false;
        }

        Plant topPlant = tile.getPlants().get(tile.getPlants().size() - 1);
        String topName = this.normalizedName(topPlant);

        if ("lily pad".equals(topName)) {
            return true;
        }

        if ("pumpkin".equals(topName)) {
            return false;
        }

        if ("pumpkin".equals(newName)) {
            for (Plant existingPlant : tile.getPlants()) {
                if ("pumpkin".equals(this.normalizedName(existingPlant))) {
                    return false;
                }
            }

            return true;
        }

        return this.hasStackTag(plant) || this.hasStackTag(topPlant);
    }

    private boolean hasStackTag(Plant plant) {
        return plant != null
                && plant.getTags() != null
                && plant.getTags().contains(PlantTag.STACK);
    }

    private String normalizedName(Plant plant) {
        return plant == null || plant.getName() == null
                ? ""
                : plant.getName().trim().toLowerCase(Locale.ROOT);
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

        if (!plant.hasUpgradeSpecialEffect(PlantUpgradeSpecialEffect.RESET_FAMILY_COOLDOWNS)
                || this.cooldownManager == null
                || !(plant.getBehavior() instanceof ModifierBehavior)
                || this.board == null) {
            return;
        }

        ModifierBehavior mintBehavior = (ModifierBehavior) plant.getBehavior();

        for (Plant familyPlant : this.board.getAllPlants()) {
            if (familyPlant != plant && mintBehavior.isSameFamily(familyPlant)) {
                this.cooldownManager.resetCooldown(familyPlant);
            }
        }
    }
}
