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

    public Sun(SunType type, Position position, long spawnTick) {
        this.type = type;
        this.position = position;
        this.spawnTick = spawnTick;
        this.landingTick = spawnTick + 50;
        this.falling = true;
        this.collected = false;
    }

    public boolean isFalling() {
        return falling;
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        this.collected = true;
    }

    public void reachGround() {
        this.falling = false;
        if (this.type == SunType.RADIOACTIVE) {
            this.type = SunType.NORMAL;
        }
    }
}
