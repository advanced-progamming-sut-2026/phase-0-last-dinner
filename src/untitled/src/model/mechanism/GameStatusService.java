package model.mechanism;

import model.Plant;

import java.util.List;

public class GameStatusService {
    private Board board;
    private WaveManager waveManager;
    private SunSystem sunSystem;
    private PlantFoodSystem plantFoodSystem;
    private PlantCooldownManager cooldownManager;

    public Board getMapStatus() {
        return null;
    }

    public List<PlantStatus> getPlantsStatus(List<Plant> selectedPlants) {
        return null;
    }

    public Tile getTileStatus(Position position) {
        return null;
    }
}
