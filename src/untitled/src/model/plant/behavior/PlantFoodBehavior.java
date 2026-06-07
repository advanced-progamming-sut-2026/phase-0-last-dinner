package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public interface PlantFoodBehavior {
    void activate(Plant plant, Board board);
}
