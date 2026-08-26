package model.minigame.izombieminigame.multiplayer;

import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record IZombieMatchLoadout(IZombieMatchResources plantResources, IZombieMatchResources zombieResources,
                                  Map<String, PlantDefinition> plantDefinitions,
                                  Map<String, ZombieDefinition> zombieDefinitions) {
    public IZombieMatchLoadout {
        if (plantResources == null || zombieResources == null) {
            throw new IllegalArgumentException("Both player resources are required.");
        }

        plantDefinitions = immutableCopy(plantDefinitions);
        zombieDefinitions = immutableCopy(zombieDefinitions);
    }

    public PlantDefinition findPlant(String plantName) {
        String resolvedName = plantResources.resolveUnitKey(plantName);

        if (resolvedName == null) {
            return null;
        }

        return plantDefinitions.get(resolvedName);
    }

    public ZombieDefinition findZombie(String zombieAlias) {
        String resolvedAlias = zombieResources.resolveUnitKey(zombieAlias);

        if (resolvedAlias == null) {
            return null;
        }

        return zombieDefinitions.get(resolvedAlias);
    }

    private static <T> Map<String, T> immutableCopy(Map<String, T> source) {
        if (source == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
