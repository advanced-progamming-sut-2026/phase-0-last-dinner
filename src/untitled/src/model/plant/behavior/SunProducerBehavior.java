package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;

public class SunProducerBehavior implements PlantBehavior, OnPlantingBehavior {
    private int sunAmount;
    private long productionIntervalTicks;
    private long ticksSinceLastProduction;
    private SunProductionMode productionMode;

    public SunProducerBehavior(int sunAmount, long productionIntervalTicks) {
        this.sunAmount = sunAmount;
        this.productionIntervalTicks = productionIntervalTicks;
        this.productionMode = SunProductionMode.PERIODIC;
    }

    public SunProducerBehavior(int sunAmount, long productionIntervalTicks, SunProductionMode productionMode) {
        this.sunAmount = sunAmount;
        this.productionIntervalTicks = productionIntervalTicks;
        this.productionMode = productionMode;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        if (this.productionMode == SunProductionMode.INSTANT_ON_PLANTING) {
            return;
        }

        this.ticksSinceLastProduction++;

        if (this.ticksSinceLastProduction >= this.productionIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastProduction = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (board == null || board.getSunSystem() == null) {
            return;
        }

        if (this.productionMode == SunProductionMode.INSTANT_ON_PLANTING) {
            board.getSunSystem().addSun(this.sunAmount);
            return;
        }

        board.getSunSystem().addPlantSun(plant.getPosition());
    }

    @Override
    public boolean shouldActivateOnPlanting() {
        return this.productionMode == SunProductionMode.INSTANT_ON_PLANTING;
    }
}
