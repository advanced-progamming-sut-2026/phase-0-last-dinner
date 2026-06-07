package model.level;

import model.Plant;
import model.plant.PlantCategory;

import java.util.List;
import java.util.Set;

public class LockedPlantsLevel extends Level {
    private LockedPlantMode mode;
    private List<Plant> forcedPlants;
    private Set<String> lockedPlantNames;
    private Set<PlantCategory> restrictedFamilies;

    public LockedPlantsLevel() {
        super(LevelType.LOCKED_PLANTS);
    }

    public boolean isPlantSelectable(Plant plant) {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isWinConditionMet() {
        return false;
    }

    @Override
    public boolean isLoseConditionMet() {
        return false;
    }
}
