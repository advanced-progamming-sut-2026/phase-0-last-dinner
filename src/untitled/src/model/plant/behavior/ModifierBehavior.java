package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public class ModifierBehavior implements PlantBehavior {
    private String effectDescription;
    private int effectRadius;

    @Override
    public void onTick(Plant plant, Board board) {
    }

    @Override
    public void activate(Plant plant, Board board) {
    }
}
