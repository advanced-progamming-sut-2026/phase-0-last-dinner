package model.mechanism;

import lombok.Getter;

@Getter

public class Sun {
    private SunType type;
    private Position position;
    private boolean falling;
    private long spawnTick;
    private long landingTick;
    private boolean collected;

    public boolean isFalling() {
        return false;
    }

    public boolean isCollected() {
        return false;
    }

    public void collect() {
    }

    public void reachGround() {
    }
}
