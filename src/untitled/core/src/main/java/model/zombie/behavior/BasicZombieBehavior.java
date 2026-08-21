package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.List;

public class BasicZombieBehavior implements ZombieBehavior {
    private static final int TICKS_PER_SECOND = 10;
    private static final double PLANT_ATTACK_DISTANCE = 0.55d;

    private int eatDamagePerSecond;
    private int damageRemainder;

    public BasicZombieBehavior() {
        this(0);
    }

    public BasicZombieBehavior(int eatDamagePerSecond) {
        this.eatDamagePerSecond = eatDamagePerSecond;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie == null || zombie.isDead()) {
            return;
        }

        if (zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
            if (!this.attackNearestZombie(zombie, board)) {
                zombie.setAttacking(false);
                this.moveIfAllowed(zombie, board);
            }
            return;
        }

        Plant target = this.findPlantInContactRange(zombie, board);

        if (target == null || target.isDead()) {
            zombie.setAttacking(false);
            this.moveIfAllowed(zombie, board);
            return;
        }

        ZombieBehavior completeBehavior = zombie.getBehavior();
        boolean canEat = completeBehavior == null || completeBehavior.canAttackPlant(zombie, target, board);
        zombie.setAttacking(canEat);
        if (canEat) {
            zombie.attack(target);
        } else if (completeBehavior != null) {
            completeBehavior.attack(zombie, target, board);
        }

        if (!canEat) {
            this.moveIfAllowed(zombie, board);
        }
    }

    private Plant findPlantInContactRange(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return null;
        }

        for (Plant candidate : board.getPlantsInZombieAttackRange(zombie.getPosition(), 1)) {
            if (candidate == null || candidate.isDead() || candidate.getPosition() == null) {
                continue;
            }
            double distance = zombie.getExactX() - candidate.getPosition().getX();
            if (distance >= 0d && distance <= PLANT_ATTACK_DISTANCE) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean runsWhileHypnotized() {
        return true;
    }

    private void moveIfAllowed(Zombie zombie, Board board) {
        ZombieBehavior completeBehavior = zombie.getBehavior();
        if (completeBehavior == null || completeBehavior.canMove(zombie, board)) {
            zombie.move();
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (zombie != null && zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
            return;
        }

        if (plant == null || plant.isDead() || board == null || board.getCombatSystem() == null) {
            return;
        }

        ZombieBehavior completeBehavior = zombie == null ? null : zombie.getBehavior();
        if (completeBehavior != null && !completeBehavior.canAttackPlant(zombie, plant, board)) {
            return;
        }

        int damage = this.nextDamagePerTick(zombie);
        if (damage > 0) {
            board.getCombatSystem().applyDamageToPlant(plant, damage);
        }
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

    @Override
    public void multiplyDamage(double multiplier) {
        if (multiplier > 0) {
            this.eatDamagePerSecond = Math.max(0, (int) Math.round(this.eatDamagePerSecond * multiplier));
        }
    }

    public void ensureMinimumDamagePerSecond(int minimumDamage) {
        this.eatDamagePerSecond = Math.max(this.eatDamagePerSecond, Math.max(0, minimumDamage));
    }

    private int nextDamagePerTick(Zombie zombie) {
        int damagePerSecond = Math.max(0, this.eatDamagePerSecond);

        if (zombie != null && zombie.hasCondition(ZombieCondition.CHILLED)) {
            damagePerSecond /= 2;
        }

        int total = this.damageRemainder + damagePerSecond;
        int damage = total / TICKS_PER_SECOND;
        this.damageRemainder = total % TICKS_PER_SECOND;
        return damage;
    }

    private boolean attackNearestZombie(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null || board.getCombatSystem() == null) {
            return false;
        }

        List<Zombie> zombiesInLane = board.getZombiesInLane(zombie.getPosition());
        Zombie target = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Zombie otherZombie : zombiesInLane) {
            if (otherZombie == null || otherZombie == zombie || otherZombie.isDead()
                    || otherZombie.isHypnotized()
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
            zombie.setAttacking(true);
            int damage = this.nextDamagePerTick(zombie);
            if (damage > 0) {
                board.getCombatSystem().applyDamageToZombie(target, damage);
            }
            return true;
        }

        return false;
    }
}
