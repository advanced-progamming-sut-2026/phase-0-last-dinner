package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public class ExplosiveBehavior implements PlantBehavior {
    private String damageExpression;
    private int effectRadius;
    private boolean triggeredByContact;

    @Override
    public void onTick(Plant plant, Board board) {
    }

    @Override
    public void activate(Plant plant, Board board) {
    }
}
