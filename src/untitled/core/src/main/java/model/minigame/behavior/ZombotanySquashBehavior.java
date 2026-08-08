package model.minigame.behavior;

import lombok.Getter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

@Getter
public class ZombotanySquashBehavior
        implements ZombieBehavior {

    private final double speedMultiplier;

    private boolean speedApplied;
    private boolean squashed;

    public ZombotanySquashBehavior() {
        this(2.0);
    }

    public ZombotanySquashBehavior(
            double speedMultiplier
    ) {
        this.speedMultiplier =
                Math.max(1.0, speedMultiplier);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        applySpeedMultiplier(zombie);
    }

    @Override
    public void attack(
            Zombie zombie,
            Plant plant,
            Board board
    ) {
        if (squashed
                || zombie == null
                || plant == null
                || board == null) {
            return;
        }

        squashed = true;

        CombatSystem combatSystem =
                board.getCombatSystem();

        if (!plant.isDead()) {
            if (combatSystem != null) {
                combatSystem.destroyPlant(plant);
            } else {
                plant.takeDamage(
                        Math.max(1, plant.getHealth())
                );

                if (plant.isDead()) {
                    board.removePlant(plant);
                }
            }
        }
        if (combatSystem != null) {
            combatSystem.killZombieIgnoringAllegiance(
                    zombie
            );
        } else {
            zombie.die();
            board.removeZombie(zombie);
        }
    }

    @Override
    public void activate(
            Zombie zombie,
            Board board
    ) {
        applySpeedMultiplier(zombie);

        if (zombie == null
                || zombie.getPosition() == null
                || board == null) {
            return;
        }

        Plant target =
                board.getNearestPlantInZombieAttackRange(
                        zombie.getPosition(),
                        1
                );

        if (target != null) {
            attack(zombie, target, board);
        }
    }

    private void applySpeedMultiplier(Zombie zombie) {
        if (speedApplied
                || zombie == null
                || zombie.isDead()) {
            return;
        }

        zombie.setCurrentSpeed(
                zombie.getCurrentSpeed()
                        * speedMultiplier
        );

        speedApplied = true;
    }
}