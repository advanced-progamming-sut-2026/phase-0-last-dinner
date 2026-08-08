package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.plant.Projectile;
import model.zombie.Zombie;

@Getter
@Setter
public class ZombieKillEvent {
    private final Zombie zombie;
    private final Projectile projectile;
    private final long tick;
    private final Plant sourcePlant;

    public ZombieKillEvent(Zombie zombie, Projectile projectile, long tick) {
        this(zombie, projectile, tick, projectile == null ? null : projectile.getSourcePlant());
    }

    public ZombieKillEvent(
            Zombie zombie,
            Projectile projectile,
            long tick,
            Plant sourcePlant
    ) {
        this.zombie = zombie;
        this.projectile = projectile;
        this.tick = tick;
        this.sourcePlant = sourcePlant;
    }
}
