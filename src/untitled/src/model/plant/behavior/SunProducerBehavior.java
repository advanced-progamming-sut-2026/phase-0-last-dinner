package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;

import java.util.Random;

public class SunProducerBehavior implements PlantBehavior, OnPlantingBehavior {
    private int sunAmount;
    private long productionIntervalTicks;
    private long ticksSinceLastProduction;
    private SunProductionMode productionMode;
    private boolean doubleSunChance;
    private long ageTicks;
    private final Random random = new Random();

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

        this.ageTicks++;

        if (plant == null || board == null || board.getSunSystem() == null
                || board.getSunSystem().hasUncollectedSunFrom(plant)) {
            return;
        }

        this.ticksSinceLastProduction++;

        if (this.ticksSinceLastProduction >= Math.max(1, this.productionIntervalTicks)) {
            this.activate(plant, board);
            this.ticksSinceLastProduction = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (board == null || board.getSunSystem() == null) {
            return;
        }

        int producedSun = this.currentSunAmount();

        if (this.doubleSunChance && this.random.nextBoolean()) {
            producedSun *= 2;
        }

        if (this.productionMode == SunProductionMode.INSTANT_ON_PLANTING) {
            board.getSunSystem().addSun(producedSun);
            board.removePlant(plant);
            return;
        }

        board.getSunSystem().addPlantSun(plant, producedSun);
    }

    @Override
    public boolean shouldActivateOnPlanting() {
        return this.productionMode == SunProductionMode.INSTANT_ON_PLANTING;
    }

    public void growToMaximum() {
        if (this.productionMode == SunProductionMode.RAMPING) {
            this.ageTicks = Math.max(this.ageTicks, 720);
        }
    }

    @Override
    public PlantBehavior copy() {
        SunProducerBehavior copy = new SunProducerBehavior(
                this.sunAmount,
                this.productionIntervalTicks,
                this.productionMode
        );
        copy.doubleSunChance = this.doubleSunChance;
        copy.ageTicks = this.ageTicks;
        return copy;
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

    private int currentSunAmount() {
        if (this.productionMode != SunProductionMode.RAMPING) {
            return this.sunAmount;
        }

        if (this.ageTicks >= 720) {
            return this.sunAmount * 3;
        }

        if (this.ageTicks >= 240) {
            return this.sunAmount * 2;
        }

        return this.sunAmount;
    }
}
