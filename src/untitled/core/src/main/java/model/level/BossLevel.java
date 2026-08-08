package model.level;

public class BossLevel extends Level {
    public BossLevel() {
        super(LevelType.BOSS);
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
