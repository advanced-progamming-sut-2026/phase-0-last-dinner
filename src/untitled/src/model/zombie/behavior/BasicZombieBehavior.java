package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.List;

public class BasicZombieBehavior implements ZombieBehavior {
    private int eatDamagePerSecond;

    public BasicZombieBehavior() {
        this(0);
    }

    public BasicZombieBehavior(int eatDamagePerSecond) {
        this.eatDamagePerSecond = eatDamagePerSecond;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie != null && zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
            this.attackNearestZombie(zombie, board);
        }

        if (zombie != null) {
            zombie.move();
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (zombie != null && zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
            return;
        }

        if (plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        board.getCombatSystem().applyDamageToPlant(plant, this.eatDamagePerSecond);
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    @Override
    public void applyPlantUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.ZOMBIE_DAMAGE_BUFF)) {
            this.eatDamagePerSecond = Math.max(1, this.eatDamagePerSecond + 25);
        }
    }

    private void attackNearestZombie(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        List<Zombie> zombiesInLane = board.getZombiesInLane(zombie.getPosition());
        Zombie target = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Zombie otherZombie : zombiesInLane) {
            if (otherZombie == null || otherZombie == zombie || otherZombie.isDead()
                    || otherZombie.getPosition() == null) {
                continue;
            }

            int deltaX = otherZombie.getPosition().getX() - zombie.getPosition().getX();

            if (deltaX < 0) {
                continue;
            }

            if (deltaX < nearestDistance) {
                nearestDistance = deltaX;
                target = otherZombie;
            }
        }

        if (target != null && nearestDistance <= 1) {
            board.getCombatSystem().applyDamageToZombie(target, this.eatDamagePerSecond);
        }
    }
}
