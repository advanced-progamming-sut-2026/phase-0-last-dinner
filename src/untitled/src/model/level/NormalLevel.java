package model.level;

public class NormalLevel extends Level {
    public NormalLevel() {
        super(LevelType.NORMAL);
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
