package model.minigame.izombieminigame.multiplayer;

import model.Plant;
import model.mechanism.Position;
import model.minigame.izombieminigame.PlantZombieIZombieIntegration;
import model.plant.PlantDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlantZombieIZombieMultiplayerIntegration extends PlantZombieIZombieIntegration implements
    IZombieMultiplayerIntegration {

    private static final String[][] PLANTS_BY_STAGE = {
        {
            "Peashooter",
            "Wall-nut",
            "Snow Pea",
            "Repeater",
            "Bonk Choy"
        },
        {
            "Peashooter",
            "Repeater",
            "Snow Pea",
            "Wall-nut",
            "Tall-nut"
        },
        {
            "Repeater",
            "Threepeater",
            "Snow Pea",
            "Tall-nut",
            "Fume-shroom"
        }
    };

    public PlantZombieIZombieMultiplayerIntegration() {
        super();
    }

    @Override
    public void prepareMultiplayerStage(int stageNumber) {
        requireValidStage(stageNumber);

        super.prepareStage(stageNumber);

        removeAutomaticallyPlacedPlants();
    }

    @Override
    public List<PlantDefinition> chooseAvailablePlants(int stageNumber) {
        requireValidStage(stageNumber);

        List<PlantDefinition> selectedPlants = new ArrayList<>();

        for (String plantName : PLANTS_BY_STAGE[stageNumber - 1]) {
            PlantDefinition definition = findPlantIgnoringCase(plantName);

            if (definition != null) {
                selectedPlants.add(definition);
            }
        }

        if (selectedPlants.size() != 5) {
            throw new IllegalStateException("IZombie multiplayer requires exactly five plants.");
        }

        return Collections.unmodifiableList(selectedPlants);
    }

    @Override
    public int getPlantSunCost(PlantDefinition definition) {
        if (definition == null) {
            return -1;
        }

        return Math.max(0, definition.getCost());
    }

    @Override
    public boolean isPlantPlacementBlocked(PlantDefinition definition, Position position) {
        if (!isReady() || definition == null || position == null || !getBoard().isInsideBoard(position)) {
            return true;
        }

        Plant plant = getPlantFactory().create(definition);

        return plant == null || !getPlantingSystem().canPlant(plant, position);
    }

    @Override
    public boolean placePlant(PlantDefinition definition, Position position) {
        if (isPlantPlacementBlocked(definition, position)) {
            return false;
        }

        Plant plant = getPlantFactory().create(definition);

        if (plant == null) {
            return false;
        }

        return getPlantingSystem().plantWithoutCost(plant, position);
    }

    private void removeAutomaticallyPlacedPlants() {
        if (getBoard() == null) {
            return;
        }

        List<Plant> existingPlants = new ArrayList<>(getBoard().getAllPlants());

        for (Plant plant : existingPlants) {
            getBoard().removePlant(plant);
        }
    }

    private PlantDefinition findPlantIgnoringCase(String plantName) {
        if (plantName == null || getPlantDefinitions() == null) {
            return null;
        }

        for (PlantDefinition definition : getPlantDefinitions().findAll()) {
            if (definition != null && definition.getName() != null && definition.getName().equalsIgnoreCase(plantName))
                return definition;
        }

        return null;
    }

    private void requireValidStage(int stageNumber) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException("IZombie stage must be between 1 and 3.");
        }
    }
}
