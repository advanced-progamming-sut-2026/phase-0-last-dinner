package model.mechanism;

import model.Plant;

import java.util.ArrayList;import java.util.List;

public class GameStatusService {
    private Board board;
    private WaveManager waveManager;
    private SunSystem sunSystem;
    private PlantFoodSystem plantFoodSystem;
    private PlantCooldownManager cooldownManager;

    public GameStatusService(Board board, WaveManager waveManager,
                             SunSystem sunSystem, PlantFoodSystem plantFoodSystem,
                             PlantCooldownManager cooldownManager) {
        this.board = board;
        this.waveManager = waveManager;
        this.sunSystem = sunSystem;
        this.plantFoodSystem = plantFoodSystem;
        this.cooldownManager = cooldownManager;
    }

    public int getCurrentWaveNumber() {
        Wave current = waveManager.getCurrentWave();
        return current == null ? 0 : current.getNumber();
    }

    public int getSunAmount() {
        return sunSystem.getSunAmount();
    }

    public int getPlantFoodAmount() {
        return plantFoodSystem.getPlantFoodAmount();
    }
    public Board getMapStatus() {
        return board; // توی کنترلر خواسته های داک هندل میشه
    }

    public List<PlantStatus> getPlantsStatus(List<Plant> selectedPlants) {
        List<PlantStatus> statuses = new ArrayList<>();
        if (selectedPlants == null) return statuses;

        for (Plant plant : selectedPlants) {
            if (plant == null) continue;
            boolean available = cooldownManager.isAvailable(plant);
            long remainingTicks = cooldownManager.getRemainingTicks(plant);
            statuses.add(new PlantStatus(plant, available, remainingTicks));
        }

        return statuses;
    }

    public Tile getTileStatus(Position position) {
        if (position == null) return null;
        return board.getTile(position);
    }
}
