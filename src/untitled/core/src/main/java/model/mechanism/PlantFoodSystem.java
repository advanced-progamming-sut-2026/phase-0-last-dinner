package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import view.GameEventListener;

@Getter
@Setter
public class PlantFoodSystem {
    private final int maximumPlantFood = 3;
    private int plantFoodAmount;
    private Board board;
    private GameEventListener listener;

    public PlantFoodSystem() {
    }

    public PlantFoodSystem(Board board) {
        this.board = board;

        if (board != null) {
            board.setPlantFoodSystem(this);
        }
    }

    public boolean addPlantFood() {
        if (this.plantFoodAmount >= this.maximumPlantFood) {
            return false;
        }

        this.plantFoodAmount++;
        this.fireEvent("Plant food added. Current amount: " + this.plantFoodAmount);
        return true;
    }

    public boolean feedPlant(Position position) {
        if (position == null || this.board == null || this.plantFoodAmount <= 0) {
            return false;
        }

        Tile tile = this.board.getTile(position);

        if (tile == null || tile.getPlants() == null || tile.getPlants().isEmpty()) {
            return false;
        }

        Plant plant = tile.getPlants().get(tile.getPlants().size() - 1);

        if (plant == null || !plant.canReceivePlantFood()) {
            return false;
        }

        plant.receivePlantFood();
        this.plantFoodAmount--;
        this.fireEvent("Plant food used on " + plant.getName() + ".");
        return true;
    }

    public void cheatAddPlantFood() {
        if (this.plantFoodAmount < this.maximumPlantFood) {
            this.plantFoodAmount++;
        }
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
