package model.mechanism;
import lombok.Getter;import lombok.Setter;import model.Plant;import view.GameEventListener;@Getter
@Setter
public class PlantFoodSystem {
    private final int maximumPlantFood = 3;
    private int plantFoodAmount;
    private Board board;
    private GameEventListener listener;

    public PlantFoodSystem(Board board) {
        this.board = board;
        this.plantFoodAmount = 0;
    }
    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }

    public boolean addPlantFood() {
        if (plantFoodAmount >= maximumPlantFood) return false;
        plantFoodAmount++;
        fireEvent("The glowing zombie dropeed a plant food; you have "
                + plantFoodAmount + " plant foods now.");
        return true;
    }

    public boolean feedPlant(Position position) {
        if (position == null || plantFoodAmount <= 0) return false;

        Tile tile = board.getTile(position);
        if (tile == null || tile.getPlants() == null || tile.getPlants().isEmpty()) {
            return false;
        }
        Plant plant = tile.getPlants().get(tile.getPlants().size() - 1);
        plant.receivePlantFood();
        plantFoodAmount--;
        return true;
    }
    public void cheatAddPlantFood() {
        if (plantFoodAmount < maximumPlantFood) {
            plantFoodAmount++;
        }
    }
}
