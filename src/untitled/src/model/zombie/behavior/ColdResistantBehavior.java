package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class ColdResistantBehavior implements ZombieBehavior {
    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    @Override
    public boolean acceptsCondition(Zombie zombie, ZombieCondition condition, Projectile projectile) {
        return condition != ZombieCondition.CHILLED && condition != ZombieCondition.FROZEN;
    }
}
