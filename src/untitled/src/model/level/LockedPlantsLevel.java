package model.level;

import model.Plant;
public class LockedPlantsLevel extends Level {
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
