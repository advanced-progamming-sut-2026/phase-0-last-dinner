package model.zombie.behavior;

import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class ColdResistantBehavior implements ZombieBehavior {
    @Override
    public boolean acceptsCondition(Zombie zombie, ZombieCondition condition, Projectile projectile) {
        return condition != ZombieCondition.CHILLED && condition != ZombieCondition.FROZEN;
    }
}
