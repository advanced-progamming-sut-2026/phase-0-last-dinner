package model.minigame.behavior;

import lombok.Getter;
import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

@Getter
public class ZombotanyWallnutBehavior implements ZombieBehavior {

    private final double healthMultiplier;
    private final double speedMultiplier;

    private boolean modifiersApplied;

    public ZombotanyWallnutBehavior() {
        this(
                4.0,
                0.5
        );
    }

    public ZombotanyWallnutBehavior(
            double healthMultiplier,
            double speedMultiplier
    ) {
        this.healthMultiplier =
                Math.max(1.0, healthMultiplier);

        this.speedMultiplier =
                Math.max(0.1, speedMultiplier);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        applyModifiers(zombie);
    }

    @Override
    public void activate(
            Zombie zombie,
            Board board
    ) {
        applyModifiers(zombie);
    }

    private void applyModifiers(Zombie zombie) {
        if (modifiersApplied
                || zombie == null
                || zombie.isDead()) {
            return;
        }

        zombie.multiplyHealth(this.healthMultiplier);

        zombie.setCurrentSpeed(
                zombie.getCurrentSpeed()
                        * speedMultiplier
        );

        modifiersApplied = true;
    }
}
