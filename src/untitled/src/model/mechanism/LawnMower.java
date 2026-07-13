package model.mechanism;

import lombok.Getter;
import model.zombie.Zombie;
import model.zombie.ZombieType;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LawnMower {
    private int row;
    private boolean active;
    private boolean used;

    public LawnMower() {
        this(0);
    }

    public LawnMower(int row) {
        this.row = row;
    }

    public List<Zombie> trigger(List<Zombie> zombies) {
        List<Zombie> killedZombies = new ArrayList<>();

        if (!this.canTrigger() || zombies == null) {
            return killedZombies;
        }

        this.active = true;
        this.used = true;

        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null || zombie.isDead()) {
                continue;
            }

            if (zombie.getPosition().getY() == this.row) {
                if (zombie.getDefinition() != null
                        && zombie.getDefinition().getType() == ZombieType.BOSS) {
                    continue;
                }

                zombie.die();
                killedZombies.add(zombie);
            }
        }

        this.active = false;
        return killedZombies;
    }

    public boolean canTrigger() {
        return !this.used;
    }
}
