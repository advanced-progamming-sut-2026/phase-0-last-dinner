package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class BlockPusherBehavior implements ZombieBehavior {
    private int maximumBlockHealth;
    private int currentBlockHealth;
    private int blocksRemaining;

    public BlockPusherBehavior(int blockHealth) {
        this(blockHealth, 1);
    }

    public BlockPusherBehavior(int blockHealth, int blockCount) {
        this.maximumBlockHealth = Math.max(0, blockHealth);
        this.currentBlockHealth = this.maximumBlockHealth;
        this.blocksRemaining = Math.max(0, blockCount);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (this.blocksRemaining <= 0 || zombie == null || zombie.getPosition() == null
                || board == null || board.getCombatSystem() == null) {
            return;
        }

        for (Zombie other : board.getZombiesInLane(zombie.getPosition())) {
            if (other == null || other == zombie || other.isDead() || other.getPosition() == null
                    || !other.hasCondition(ZombieCondition.HYPNOTIZED)) {
                continue;
            }

            int distance = Math.abs(other.getPosition().getX() - zombie.getPosition().getX());
            if (distance <= 1) {
                board.getCombatSystem().killZombie(other);
                break;
            }
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (this.blocksRemaining > 0 && plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        if (this.blocksRemaining <= 0 || projectile == null) {
            return false;
        }

        int damage = DamageExpressionParser.isInstantKill(projectile.getDamageExpression())
                ? this.currentBlockHealth
                : DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
        this.currentBlockHealth -= Math.max(0, damage);

        if (this.currentBlockHealth <= 0) {
            this.blocksRemaining--;
            this.currentBlockHealth = this.blocksRemaining > 0 ? this.maximumBlockHealth : 0;
        }
        return true;
    }
}
