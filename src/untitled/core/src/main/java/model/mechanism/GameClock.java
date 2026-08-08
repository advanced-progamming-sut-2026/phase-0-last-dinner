package model.mechanism;

import lombok.Getter;

@Getter
public class GameClock {
    private long currentTick;
    private final int ticksPerSecond = 10;

    public void advance(int tickCount) {
        currentTick += tickCount;
    }

    public double getElapsedSeconds() {
        return (double) currentTick / ticksPerSecond;
    }
}
