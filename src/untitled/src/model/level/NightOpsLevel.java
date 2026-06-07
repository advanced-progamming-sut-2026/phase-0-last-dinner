package model.level;

public class NightOpsLevel extends Level {
    private boolean skySunEnabled;

    public NightOpsLevel() {
        super(LevelType.NIGHT_OPS);
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
