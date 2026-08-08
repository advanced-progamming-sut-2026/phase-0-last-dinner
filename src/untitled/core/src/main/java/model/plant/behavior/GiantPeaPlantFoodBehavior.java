package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.Projectile;
import model.plant.ProjectileType;

public class GiantPeaPlantFoodBehavior implements PlantFoodBehavior {
    private int giantPeaDamage;
    private int regularBurstCount;
    private boolean oneGiantPeaPerHead;
    private int fixedGiantPeaCount;

    public GiantPeaPlantFoodBehavior(int giantPeaDamage, int regularBurstCount, boolean oneGiantPeaPerHead) {
        this.giantPeaDamage = Math.max(1, giantPeaDamage);
        this.regularBurstCount = Math.max(0, regularBurstCount);
        this.oneGiantPeaPerHead = oneGiantPeaPerHead;
        this.fixedGiantPeaCount = 1;
    }

    public GiantPeaPlantFoodBehavior(int giantPeaDamage, int regularBurstCount, int giantPeaCount) {
        this.giantPeaDamage = Math.max(1, giantPeaDamage);
        this.regularBurstCount = Math.max(0, regularBurstCount);
        this.oneGiantPeaPerHead = false;
        this.fixedGiantPeaCount = Math.max(1, giantPeaCount);
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null) {
            return;
        }

        for (int i = 0; i < this.regularBurstCount; i++) {
            plant.useAbility();
        }

        int giantPeaCount = this.oneGiantPeaPerHead
                ? this.getHeadCount(plant, board)
                : this.fixedGiantPeaCount;

        for (int i = 0; i < giantPeaCount; i++) {
            Projectile giantPea = new Projectile(
                    String.valueOf(this.giantPeaDamage),
                    plant.getPosition(),
                    1.0,
                    ProjectileType.NORMAL,
                    null
            );
            giantPea.setPeaBased(true);
            giantPea.setSourcePlant(plant);
            board.addProjectile(giantPea);
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        if (this.oneGiantPeaPerHead) {
            return new GiantPeaPlantFoodBehavior(this.giantPeaDamage, this.regularBurstCount, true);
        }

        return new GiantPeaPlantFoodBehavior(
                this.giantPeaDamage,
                this.regularBurstCount,
                this.fixedGiantPeaCount
        );
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect != null) {
            this.giantPeaDamage += Math.max(0, effect.getDamageBonus()) * 20;
        }
    }

    private int getHeadCount(Plant plant, Board board) {
        int count = 0;

        for (Plant stackedPlant : board.getPlantsAt(plant.getPosition())) {
            if (stackedPlant != null && plant.getName() != null
                    && plant.getName().equalsIgnoreCase(stackedPlant.getName())) {
                count++;
            }
        }

        return Math.max(1, Math.min(5, count));
    }
}
