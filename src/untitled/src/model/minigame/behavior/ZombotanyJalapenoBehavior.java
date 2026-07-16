package model.minigame.behavior;

import lombok.Getter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ZombotanyJalapenoBehavior
        implements ZombieBehavior {

    private final long explosionDelayTicks;

    private long enteredBoardTick;
    private boolean exploded;

    public ZombotanyJalapenoBehavior() {
        this(60);
    }

    public ZombotanyJalapenoBehavior(
            long explosionDelayTicks
    ) {
        this.explosionDelayTicks =
                Math.max(1, explosionDelayTicks);

        this.enteredBoardTick = 0;
        this.exploded = false;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (exploded
                || zombie == null
                || zombie.isDead()
                || board == null) {
            return;
        }

        enteredBoardTick++;

        if (enteredBoardTick >= explosionDelayTicks) {
            burnRow(zombie, board);
        }
    }

    @Override
    public void attack(
            Zombie zombie,
            Plant plant,
            Board board
    ) {
        if (exploded
                || zombie == null
                || plant == null
                || board == null) {
            return;
        }
        burnRow(zombie, board);
    }

    @Override
    public void activate(
            Zombie zombie,
            Board board
    ) {
        burnRow(zombie, board);
    }

    public void burnRow(
            Zombie zombie,
            Board board
    ) {
        if (exploded
                || zombie == null
                || zombie.getPosition() == null
                || board == null) {
            return;
        }

        exploded = true;

        CombatSystem combatSystem =
                board.getCombatSystem();

        List<Plant> plantsInLane =
                new ArrayList<>(
                        board.getPlantsInLane(
                                zombie.getPosition()
                        )
                );

        for (Plant plant : plantsInLane) {
            if (plant == null || plant.isDead()) {
                continue;
            }

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
}