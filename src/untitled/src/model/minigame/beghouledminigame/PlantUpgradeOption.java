package model.minigame.beghouledminigame;

import lombok.Getter;
import model.plant.PlantDefinition;

@Getter
public class PlantUpgradeOption {

    private final PlantDefinition sourcePlant;

    private final PlantDefinition targetPlant;

    private final int sunCost;

    public PlantUpgradeOption(
            PlantDefinition sourcePlant,
            PlantDefinition targetPlant,
            int sunCost
    ) {
        if (sourcePlant == null) {
            throw new IllegalArgumentException(
                    "Source plant cannot be null."
            );
        }

        if (targetPlant == null) {
            throw new IllegalArgumentException(
                    "Target plant cannot be null."
            );
        }

        if (sunCost <= 0) {
            throw new IllegalArgumentException(
                    "Upgrade sun cost must be positive."
            );
        }

        this.sourcePlant = sourcePlant;
        this.targetPlant = targetPlant;
        this.sunCost = sunCost;
    }

    public boolean canUpgrade(int availableSun) {
        return availableSun >= sunCost;
    }

    public boolean matchesSourcePlant(
            String plantName
    ) {
        if (plantName == null
                || sourcePlant.getName() == null) {

            return false;
        }

        return sourcePlant.getName()
                .equalsIgnoreCase(
                        plantName.trim()
                );
    }

}