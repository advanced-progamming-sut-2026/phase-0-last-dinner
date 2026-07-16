package model.minigame.beghouledminigame;

import model.mechanism.Position;
import model.plant.PlantDefinition;
import java.util.List;
import java.util.Set;

public interface BeghouledIntegration {

    boolean isReady();

    void prepareStage(
            int stageNumber,
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    );

    PlantDefinition findPlantDefinition(
            String plantName
    );

    List<PlantUpgradeOption> createUpgradeOptions(
            int stageNumber
    );

    PlantDefinition getPlantAt(
            Position position
    );

    boolean swapPlants(
            Position first,
            Position second
    );

    void removePlants(
            Set<Position> positions
    );

    void collapseAndRefill(
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    );

    void resetBoard(
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    );

    Set<Position> findDestroyedPlantPositions(
            Set<Position> craters
    );

    boolean createCrater(
            Position position
    );

    int upgradePlants(
            PlantUpgradeOption upgradeOption
    );

    void advanceOneTick();

    void destroyAllZombies();

    boolean isBrainEaten();
}