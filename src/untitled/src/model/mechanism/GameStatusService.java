package model.mechanism;

import model.Plant;

import java.util.ArrayList;
import java.util.List;

public class GameStatusService {
    private Board board;
    private WaveManager waveManager;
    private SunSystem sunSystem;
    private PlantFoodSystem plantFoodSystem;
    private PlantCooldownManager cooldownManager;

    public GameStatusService() {
    }

    public GameStatusService(
            Board board,
            WaveManager waveManager,
            SunSystem sunSystem,
            PlantFoodSystem plantFoodSystem,
            PlantCooldownManager cooldownManager
    ) {
        this.board = board;
        this.waveManager = waveManager;
        this.sunSystem = sunSystem;
        this.plantFoodSystem = plantFoodSystem;
        this.cooldownManager = cooldownManager;
    }

    public int getCurrentWaveNumber() {
        Wave currentWave = this.waveManager == null ? null : this.waveManager.getCurrentWave();
        return currentWave == null ? 0 : currentWave.getNumber();
    }

    public int getSunAmount() {
        return this.sunSystem == null ? 0 : this.sunSystem.getSunAmount();
    }

    public int getPlantFoodAmount() {
        return this.plantFoodSystem == null ? 0 : this.plantFoodSystem.getPlantFoodAmount();
    }

    public Board getMapStatus() {
        return this.board;
    }

    public List<PlantStatus> getPlantsStatus(List<Plant> selectedPlants) {
        List<PlantStatus> statuses = new ArrayList<>();

        if (selectedPlants == null) {
            return statuses;
        }

        for (Plant plant : selectedPlants) {
            if (plant == null) {
                continue;
            }

            boolean available = this.cooldownManager == null || this.cooldownManager.isAvailable(plant);
            long remainingTicks = this.cooldownManager == null
                    ? 0
                    : this.cooldownManager.getRemainingTicks(plant);
            statuses.add(new PlantStatus(plant, available, remainingTicks));
        }

        return statuses;
    }

    public Tile getTileStatus(Position position) {
        if (position == null || this.board == null) {
            return null;
        }

        return this.board.getTile(position);
    }
}
