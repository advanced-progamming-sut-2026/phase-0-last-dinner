package model.level;

import model.zombie.Zombie;

public class BossLevel extends Level {
    private Zombie boss;

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
