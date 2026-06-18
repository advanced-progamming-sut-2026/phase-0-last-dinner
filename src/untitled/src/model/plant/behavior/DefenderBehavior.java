package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public class DefenderBehavior implements PlantBehavior {
    private DefenderMode defenderMode;
    private String damageExpression;

    public DefenderBehavior() {
        this(DefenderMode.BASIC, "0");
    }

    public DefenderBehavior(DefenderMode defenderMode, String damageExpression) {
        this.defenderMode = defenderMode;
        this.damageExpression = damageExpression;
    }

    @Override
    public void onTick(Plant plant, Board board) {
    }

    @Override
    public void activate(Plant plant, Board board) {
    }
}
