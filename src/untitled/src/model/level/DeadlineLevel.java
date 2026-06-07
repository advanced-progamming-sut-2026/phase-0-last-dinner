package model.level;

public class DeadlineLevel extends Level {
    private double deadlineX;

    public DeadlineLevel() {
        super(LevelType.DEADLINE);
    }

    public boolean hasZombieCrossedDeadline() {
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
