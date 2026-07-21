package model.level;

public class TimedWarLevel extends Level {
    public TimedWarLevel() {
        super(LevelType.TIMED_WAR);
    }

    public void recordProgress(int amount) {
    }

    public long getRemainingTicks() {
        return 0;
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
