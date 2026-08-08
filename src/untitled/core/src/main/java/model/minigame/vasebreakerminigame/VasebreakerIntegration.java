package model.minigame.vasebreakerminigame;

import model.mechanism.Board;

import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;

public interface VasebreakerIntegration {

    boolean isReady();

    void prepareStage(int stageNumber);

    PlantDefinition choosePlantDefinition(
            int stageNumber
    );

    ZombieDefinition chooseRegularZombieDefinition(
            int stageNumber
    );

    ZombieDefinition chooseGargantuarDefinition(
            int stageNumber
    );

    boolean releaseZombie(
            ZombieDefinition zombieDefinition,
            Position position
    );

    boolean plantFromSeedPacket(
            PlantDefinition plantDefinition,
            Position position
    );

    boolean isPlantingPositionOccupied(
            Position position
    );

    boolean hasAliveVasebreakerZombies();

    void advanceOneTick();

    boolean isBrainEaten();

    default Board getBoard() {
        return null;
    }
}
