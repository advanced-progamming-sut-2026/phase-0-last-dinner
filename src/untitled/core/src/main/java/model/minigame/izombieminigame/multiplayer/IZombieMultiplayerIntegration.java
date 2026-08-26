package model.minigame.izombieminigame.multiplayer;

import model.mechanism.Position;
import model.minigame.izombieminigame.IZombieIntegration;
import model.plant.PlantDefinition;

import java.util.List;

public interface IZombieMultiplayerIntegration extends IZombieIntegration {

    void prepareMultiplayerStage(int stageNumber);

    List<PlantDefinition> chooseAvailablePlants(int stageNumber);

    int getPlantSunCost(PlantDefinition definition);

    boolean isPlantPlacementBlocked(PlantDefinition definition, Position position);

    boolean placePlant(PlantDefinition definition, Position position);
}
