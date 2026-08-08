package model.collection;

import model.zombie.ZombieDefinition;

@FunctionalInterface
public interface ZombieEncounterListener {
    void onZombieEncountered(ZombieDefinition definition);
}