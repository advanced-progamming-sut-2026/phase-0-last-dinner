package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;

public interface PlantBehavior {
    void onTick(Plant plant, Board board);

    void activate(Plant plant, Board board);

    default PlantBehavior copy() {
        return this;
    }

    default void onDamaged(Plant plant, Board board, int damage) {
    }

    default void onDeath(Plant plant, Board board) {
    }

    default void applyUpgrade(PlantUpgradeEffect effect) {
    }
}
