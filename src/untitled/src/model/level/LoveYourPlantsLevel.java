package model.level;

public class LoveYourPlantsLevel extends Level {
    private int maximumDestroyedPlants;
    private int destroyedPlantCount;

    public LoveYourPlantsLevel() {
        super(LevelType.LOVE_YOUR_PLANTS);
    }

    public void recordDestroyedPlant() {
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
