package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.plant.Projectile;
import model.zombie.Zombie;

@Getter
@Setter
public class ZombieKillEvent {
    private final Zombie zombie;
    private final Projectile projectile;
    private final long tick;

    public ZombieKillEvent(Zombie zombie, Projectile projectile, long tick) {
        this.zombie = zombie;
        this.projectile = projectile;
        this.tick = tick;
    }
}
