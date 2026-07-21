package model.minigame.izombieminigame;

import model.mechanism.Board;

import model.mechanism.Position;
import model.zombie.ZombieDefinition;

import java.util.List;

public interface IZombieIntegration {

    boolean isReady();

    void prepareStage(int stageNumber);

    List<ZombieDefinition> chooseAvailableZombies(
            int stageNumber
    );

    int getZombieSunCost(
            ZombieDefinition zombieDefinition,
            int stageNumber
    );

    void spawnInitialSunProducerZombies(
            int stageNumber,
            IZombieMiniGame miniGame
    );

    boolean isZombiePlacementBlocked(
            Position position
    );

    boolean placeZombie(
            ZombieDefinition zombieDefinition,
            Position position
    );

    void advanceOneTick();

    boolean hasAlivePlayerZombies();

    boolean isBrainEaten(int row);

    default Board getBoard() {
        return null;
    }
}
