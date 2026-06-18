package model.mechanism;

public class GameClock {
    private long currentTick;
    private final int ticksPerSecond = 10;

    public long getCurrentTick() {
        return currentTick;
    }

    public int getTicksPerSecond() {
        return ticksPerSecond;
    }

    public void advance(int tickCount) {
        currentTick += tickCount;
    }

    public double getElapsedSeconds() {
        return (double) currentTick / ticksPerSecond;
    }
}
