package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public class SunProducerBehavior implements PlantBehavior {
    private int sunAmount;
    private long productionIntervalTicks;

    @Override
    public void onTick(Plant plant, Board board) {
    }

    @Override
    public void activate(Plant plant, Board board) {
    }
}
