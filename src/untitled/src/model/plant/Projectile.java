package model.plant;

import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.Zombie;

public class Projectile implements Tickable {
    private String damageExpression;
    private Position position;
    private double speed;
    private ProjectileType type;
    private Zombie target;

    @Override
    public void onTick() {
    }

    public void move() {
    }

    public void hit(Zombie zombie) {
    }
}
