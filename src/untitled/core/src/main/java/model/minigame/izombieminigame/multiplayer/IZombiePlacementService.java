package model.minigame.izombieminigame.multiplayer;

import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

import java.util.Objects;

public class IZombiePlacementService {

    private final IZombieMultiplayerIntegration integration;
    private final IZombieMatchLoadout loadout;

    public IZombiePlacementService(IZombieMultiplayerIntegration integration, IZombieMatchLoadout loadout) {
        this.integration = Objects.requireNonNull(integration);
        this.loadout = Objects.requireNonNull(loadout);
    }

    public IZombieMatchActionResult placePlant(String unitKey, int column, int row) {
        if (!IZombieMatchRules.isInsideBoard(column, row)) {
            return IZombieMatchActionResult.failure("Selected position is outside the board.");
        }

        if (!IZombieMatchRules.isPlantPlacementPosition(column, row)) {
            return IZombieMatchActionResult.failure("Plants can only be placed on the plant side.");
        }

        IZombieMatchResources resources = loadout.plantResources();

        String resolvedKey = resources.resolveUnitKey(unitKey);

        if (resolvedKey == null) {
            return IZombieMatchActionResult.failure("Selected plant is not available in this match.");
        }

        if (!resources.canAfford(resolvedKey)) {
            return IZombieMatchActionResult.failure("Not enough sun to place this plant.");
        }

        if (!resources.isReady(resolvedKey)) {
            return IZombieMatchActionResult.failure("This plant is still on cooldown.");
        }

        PlantDefinition definition = loadout.findPlant(resolvedKey);

        if (definition == null) {
            return IZombieMatchActionResult.failure("Plant definition was not found.");
        }

        Position position = new Position(column, row);

        if (integration.isPlantPlacementBlocked(definition, position)) {
            return IZombieMatchActionResult.failure("Plant cannot be placed on this tile.");
        }

        boolean placed = integration.placePlant(definition, position);

        if (!placed) {
            return IZombieMatchActionResult.failure("Server could not place the selected plant.");
        }

        resources.commitUse(resolvedKey);

        return IZombieMatchActionResult.success();
    }

    public IZombieMatchActionResult placeZombie(String unitKey, int column, int row) {
        if (!IZombieMatchRules.isInsideBoard(column, row)) {
            return IZombieMatchActionResult.failure("Selected position is outside the board.");
        }

        if (!IZombieMatchRules.isZombiePlacementPosition(column, row)) {
            return IZombieMatchActionResult.failure("Zombies can only be placed on the zombie side.");
        }

        IZombieMatchResources resources = loadout.zombieResources();

        String resolvedKey = resources.resolveUnitKey(unitKey);

        if (resolvedKey == null) {
            return IZombieMatchActionResult.failure("Selected zombie is not available in this match.");
        }

        if (!resources.canAfford(resolvedKey)) {
            return IZombieMatchActionResult.failure("Not enough sun to place this zombie.");
        }

        if (!resources.isReady(resolvedKey)) {
            return IZombieMatchActionResult.failure("This zombie is still on cooldown.");
        }

        ZombieDefinition definition = loadout.findZombie(resolvedKey);

        if (definition == null) {
            return IZombieMatchActionResult.failure("Zombie definition was not found.");
        }

        Position integrationPosition = new Position(column + 1, row + 1);

        if (integration.isZombiePlacementBlocked(integrationPosition)) {
            return IZombieMatchActionResult.failure("Zombie cannot be placed on this tile.");
        }

        boolean placed = integration.placeZombie(definition, integrationPosition);

        if (!placed) {
            return IZombieMatchActionResult.failure("Server could not place the selected zombie.");
        }

        resources.commitUse(resolvedKey);

        return IZombieMatchActionResult.success();
    }
}
