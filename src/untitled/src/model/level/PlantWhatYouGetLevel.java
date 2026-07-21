package model.level;

public class PlantWhatYouGetLevel extends Level {
    public PlantWhatYouGetLevel() {
        super(LevelType.PLANT_WHAT_YOU_GET);
    }

    public void startZombieWaves() {
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
