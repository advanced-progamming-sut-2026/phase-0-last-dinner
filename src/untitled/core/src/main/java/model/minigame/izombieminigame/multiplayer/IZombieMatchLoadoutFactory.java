package model.minigame.izombieminigame.multiplayer;

import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;
import network.izombie.protocol.IZombieRole;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IZombieMatchLoadoutFactory {
    private static final int REQUIRED_UNIT_COUNT = 5;

    private final IZombieMultiplayerIntegration integration;
    private final int stageNumber;

    public IZombieMatchLoadoutFactory(IZombieMultiplayerIntegration integration, int stageNumber) {
        if (integration == null) {
            throw new IllegalArgumentException("Multiplayer integration is required.");
        }

        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException("IZombie stage must be between 1 and 3.");
        }

        this.integration = integration;
        this.stageNumber = stageNumber;
    }

    public IZombieMatchLoadout create() {
        this.integration.prepareMultiplayerStage(this.stageNumber);

        List<PlantDefinition> plants = this.integration.chooseAvailablePlants(this.stageNumber);
        List<ZombieDefinition> zombies = this.integration.chooseAvailableZombies(this.stageNumber);

        validateUnitCount(plants, "plants");
        validateUnitCount(zombies, "zombies");

        Map<String, PlantDefinition> plantDefinitions = createPlantDefinitions(plants);
        Map<String, ZombieDefinition> zombieDefinitions = createZombieDefinitions(zombies);

        IZombieMatchResources plantResources = createPlantResources(plantDefinitions);
        IZombieMatchResources zombieResources = createZombieResources(zombieDefinitions);

        return new IZombieMatchLoadout(plantResources, zombieResources, plantDefinitions, zombieDefinitions);
    }

    private IZombieMatchResources createPlantResources(Map<String, PlantDefinition> definitions) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        Map<String, Integer> cooldowns = new LinkedHashMap<>();

        for (Map.Entry<String, PlantDefinition> entry : definitions.entrySet()) {
            PlantDefinition definition = entry.getValue();

            costs.put(entry.getKey(), this.integration.getPlantSunCost(definition));

            cooldowns.put(entry.getKey(), IZombieMatchRules.getPlantCooldownTicks(definition));
        }

        return new IZombieMatchResources(IZombieRole.PLANTS, IZombieMatchRules.getStartingSun(IZombieRole.PLANTS),
            costs, cooldowns);
    }

    private IZombieMatchResources createZombieResources(Map<String, ZombieDefinition> definitions) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        Map<String, Integer> cooldowns = new LinkedHashMap<>();

        for (Map.Entry<String, ZombieDefinition> entry : definitions.entrySet()) {
            ZombieDefinition definition = entry.getValue();

            costs.put(entry.getKey(), this.integration.getZombieSunCost(definition, this.stageNumber));

            cooldowns.put(entry.getKey(), IZombieMatchRules.getZombieCooldownTicks(definition));
        }

        return new IZombieMatchResources(IZombieRole.ZOMBIES, IZombieMatchRules.getStartingSun(IZombieRole.ZOMBIES),
            costs, cooldowns);
    }

    private Map<String, PlantDefinition> createPlantDefinitions(List<PlantDefinition> definitions) {
        Map<String, PlantDefinition> result = new LinkedHashMap<>();

        for (PlantDefinition definition : definitions) {
            if (definition == null || definition.getName() == null || definition.getName().trim().isEmpty()) {
                throw new IllegalStateException("Every available plant must have a name.");
            }

            String name = definition.getName().trim();
            result.put(name, definition);
        }

        if (result.size() != REQUIRED_UNIT_COUNT) {
            throw new IllegalStateException("Available plant names must be unique.");
        }

        return result;
    }

    private Map<String, ZombieDefinition> createZombieDefinitions(List<ZombieDefinition> definitions) {
        Map<String, ZombieDefinition> result = new LinkedHashMap<>();

        for (ZombieDefinition definition : definitions) {
            if (definition == null || definition.getAlias() == null || definition.getAlias().trim().isEmpty()) {
                throw new IllegalStateException("Every available zombie must have an alias.");
            }

            String alias = definition.getAlias().trim();
            result.put(alias, definition);
        }

        if (result.size() != REQUIRED_UNIT_COUNT) {
            throw new IllegalStateException("Available zombie aliases must be unique.");
        }

        return result;
    }

    private void validateUnitCount(List<?> units, String unitType) {
        if (units == null || units.size() != REQUIRED_UNIT_COUNT) {
            throw new IllegalStateException("IZombie multiplayer requires exactly five " + unitType + ".");
        }
    }
}
