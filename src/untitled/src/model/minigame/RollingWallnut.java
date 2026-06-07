package model.minigame;

import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.Zombie;

public class RollingWallnut implements Tickable {
    private BowlingWallnutType type;
    private Position position;
    private double directionAngle;
    private int damage;
    private boolean moving;

    @Override
    public void onTick() {
    }

    public void collideWithZombie(Zombie zombie) {
    }

    public void bounce() {
    }

    public void explode() {
    }
}
