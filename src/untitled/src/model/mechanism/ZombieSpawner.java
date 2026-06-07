package model.mechanism;

import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.behavior.ZombieBehavior;

import java.util.List;

public class ZombieSpawner {
    private ZombieFactory zombieFactory;
    private ZombieDefinitionRepository definitionRepository;
    private Board board;

    public List<Zombie> spawnWave(Wave wave) {
        return null;
    }

    public Zombie spawnZombie(
            ZombieDefinition definition,
            ZombieBehavior behavior,
            int row
    ) {
        return null;
    }

    public int chooseRandomRow() {
        return 0;
    }

    public ZombieDefinition chooseZombieDefinition(int remainingCost) {
        return null;
    }
}
