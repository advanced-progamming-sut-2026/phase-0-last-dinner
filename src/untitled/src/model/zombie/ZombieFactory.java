package model.zombie;

import model.mechanism.Position;
import model.zombie.behavior.ZombieBehavior;

public class ZombieFactory {
    private ZombieBehaviorFactory behaviorFactory;
    private ZombieArmorFactory armorFactory;

    public Zombie create(ZombieDefinition definition, Position spawnPosition) {
        return null;
    }

}
