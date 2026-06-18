package model.plant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.Zombie;
@Getter
@Setter
@AllArgsConstructor
public class Projectile implements Tickable {
    private String damageExpression;
    private Position position;
    private double speed;
    private ProjectileType type;
    private Zombie target;

    public Projectile copyAt(Position position) {
        return new Projectile(
                this.damageExpression,
                position,
                this.speed,
                this.type,
                this.target
        );
    }

    public Projectile copyAtTarget(Position position, Zombie target) {
        return new Projectile(
                this.damageExpression,
                position,
                this.speed,
                this.type,
                target
        );
    }

    @Override
    public void onTick() {
        this.move();
    }

    public void move() {
    }

    public void hit(Zombie zombie) {
        this.target = zombie;
    }
}
