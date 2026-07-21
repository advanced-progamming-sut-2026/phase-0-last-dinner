package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public interface ZombieBehavior {
    default void onTick(Zombie zombie, Board board) {
    }

    default void attack(Zombie zombie, Plant plant, Board board) {
    }

    default void activate(Zombie zombie, Board board) {
    }

    default void applyPlantUpgrade(PlantUpgradeEffect effect) {
    }

    default void multiplyDamage(double multiplier) {
    }

    default void onDeath(Zombie zombie, Board board) {
    }

    default boolean runsWhileHypnotized() {
        return false;
    }

    default boolean canMove(Zombie zombie, Board board) {
        return true;
    }

    default boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        return true;
    }

    default boolean canBeHitBy(Zombie zombie, Projectile projectile) {
        return true;
    }

    default boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        return false;
    }

    default boolean acceptsCondition(Zombie zombie, ZombieCondition condition, Projectile projectile) {
        return true;
    }

    default int getMovementDirection(Zombie zombie) {
        return zombie != null && zombie.hasCondition(ZombieCondition.HYPNOTIZED) ? 1 : -1;
    }
}
