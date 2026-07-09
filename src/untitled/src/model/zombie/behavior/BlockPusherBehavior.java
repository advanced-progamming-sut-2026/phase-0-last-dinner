package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class BlockPusherBehavior implements ZombieBehavior {
    private int blockHealth;
    private boolean blockAvailable;

    public BlockPusherBehavior(int blockHealth) {
        this.blockHealth = blockHealth;
        this.blockAvailable = blockHealth > 0;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (!this.blockAvailable || zombie == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        Plant target = board.getNearestPlantInZombieAttackRange(zombie.getPosition(), 1);

        if (target != null) {
            board.getCombatSystem().destroyPlant(target);
            this.blockAvailable = false;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }
}
