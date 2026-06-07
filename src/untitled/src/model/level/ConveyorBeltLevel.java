package model.level;

import model.Plant;

import java.util.List;

public class ConveyorBeltLevel extends Level {
    private List<Plant> conveyorPlants;
    private List<Plant> availablePlants;
    private long plantGenerationIntervalTicks;

    public ConveyorBeltLevel() {
        super(LevelType.CONVEYOR_BELT);
    }

    public Plant generateRandomPlant() {
        return null;
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
