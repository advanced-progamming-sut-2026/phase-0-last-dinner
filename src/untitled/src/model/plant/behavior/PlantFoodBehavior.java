package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;

public interface PlantFoodBehavior {
    void activate(Plant plant, Board board);

    default boolean canActivate() {
        return true;
    }

    default PlantFoodBehavior copy() {
        return this;
    }

    default void applyUpgrade(PlantUpgradeEffect effect) {
    }
}
