package model.minigame.zombotanyminigame;

import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;

import java.util.List;
import java.util.Map;

public interface ZombotanyIntegration {

    boolean isReady();

    void prepareStage(
            ZombotanyStageConfig stageConfig
    );

    boolean plant(
            String plantName,
            Position position
    );

    int collectSun(Position position);

    boolean usePlantFood(Position position);

    void advanceOneTick();

    int getSunAmount();

    int getPlantFoodAmount();

    int getCurrentWaveNumber();

    int getWaveCount();

    int getAliveZombieCount();

    boolean areAllWavesFinished();

    boolean isBrainEaten();

    List<PlantDefinition> getAvailablePlants();

    List<ZombieDefinition> getAvailableZombies();

    Map<ZombieDefinition, ZombotanyTrait>
    getZombieTraits();

    ZombotanyTrait getTrait(Zombie zombie);

    Board getBoard();
}