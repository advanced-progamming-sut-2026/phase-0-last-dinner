package model.minigame;

import model.plant.PlantDefinition;

public class PlantUpgradeOption {
    private PlantDefinition sourcePlant;
    private PlantDefinition targetPlant;
    private int sunCost;

    public boolean canUpgrade(int availableSun) {
        return false;
    }
}
