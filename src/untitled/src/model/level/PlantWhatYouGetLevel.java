package model.level;

public class PlantWhatYouGetLevel extends Level {
    private int initialSunAmount;
    private boolean skySunEnabled;
    private boolean sunProducerSelectionEnabled;
    private boolean cooldownEnabledBeforeWaves;
    private boolean zombieWavesStarted;

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
