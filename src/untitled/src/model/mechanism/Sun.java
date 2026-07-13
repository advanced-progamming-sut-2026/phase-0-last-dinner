package model.mechanism;

import lombok.Getter;
import model.Plant;

@Getter

public class Sun {
    private SunType type;
    private Position position;
    private boolean falling;
    private long spawnTick;
    private long landingTick;
    private boolean collected;
    private int value;
    private Plant producer;

    public Sun(SunType type, Position position, long spawnTick) {
        this(type, position, spawnTick, type == null ? 0 : type.getValue(), null);
    }

    public Sun(SunType type, Position position, long spawnTick, int value, Plant producer) {
        this.type = type;
        this.position = position;
        this.spawnTick = spawnTick;
        this.landingTick = type == SunType.PLANT_PRODUCED ? spawnTick : spawnTick + 50;
        this.falling = type != SunType.PLANT_PRODUCED;
        this.collected = false;
        this.value = Math.max(0, value);
        this.producer = producer;
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
