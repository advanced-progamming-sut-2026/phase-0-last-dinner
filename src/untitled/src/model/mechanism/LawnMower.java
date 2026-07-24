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

    public List<Zombie> trigger(Board board) {
        List<Zombie> killedZombies = new ArrayList<>();

        if (!this.canTrigger() || board == null) {
            return killedZombies;
        }

        this.active = true;
        this.used = true;

        boolean killedInPass;
        do {
            killedInPass = false;
            for (Zombie zombie : board.getZombiesInLane(new Position(0, this.row))) {
                if (zombie == null || zombie.getPosition() == null || zombie.isDead()) {
                    continue;
                }
                if (zombie.getDefinition() != null
                        && zombie.getDefinition().getType() == ZombieType.BOSS) {
                    continue;
                }

                zombie.die();
                killedZombies.add(zombie);
                killedInPass = true;
            }
        } while (killedInPass);

        this.active = false;
        return killedZombies;
    }

    public boolean canTrigger() {
        return !this.used;
    }
}
