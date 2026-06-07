package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public interface PlantBehavior {
    void onTick(Plant plant, Board board);

    void activate(Plant plant, Board board);
}
