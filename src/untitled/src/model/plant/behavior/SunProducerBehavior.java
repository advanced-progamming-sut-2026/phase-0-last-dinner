package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;

public class SunProducerBehavior implements PlantBehavior, OnPlantingBehavior {
    private int sunAmount;
    private long productionIntervalTicks;
    private long ticksSinceLastProduction;
    private SunProductionMode productionMode;
    private boolean doubleSunChance;
    private boolean doubleSunTurn;

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

        int producedSun = this.sunAmount;

        if (this.doubleSunChance) {
            this.doubleSunTurn = !this.doubleSunTurn;

            if (this.doubleSunTurn) {
                producedSun *= 2;
            }
        }

        if (this.productionMode == SunProductionMode.INSTANT_ON_PLANTING) {
            board.getSunSystem().addSun(producedSun);
            return;
        }

        board.getSunSystem().addSun(producedSun);
    }

    @Override
    public boolean shouldActivateOnPlanting() {
        return this.productionMode == SunProductionMode.INSTANT_ON_PLANTING;
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.sunAmount += effect.getSunProductionBonus();
        this.productionIntervalTicks = effect.upgradeInterval(this.productionIntervalTicks);

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.DOUBLE_SUN_CHANCE)) {
            this.doubleSunChance = true;
        }
    }
}
