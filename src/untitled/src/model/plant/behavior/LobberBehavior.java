package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.Projectile;

public class LobberBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private long lobIntervalTicks;
    private long ticksSinceLastLob;
    private boolean areaDamage;

    public LobberBehavior(Projectile projectileTemplate, long lobIntervalTicks, boolean areaDamage) {
        this.projectileTemplate = projectileTemplate;
        this.lobIntervalTicks = lobIntervalTicks;
        this.areaDamage = areaDamage;

        if (this.projectileTemplate != null && areaDamage) {
            this.projectileTemplate.setSplashRadius(1);
        }
    }

    @Override
    public void onTick(Plant plant, Board board) {
        this.ticksSinceLastLob++;

        if (this.ticksSinceLastLob >= this.lobIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastLob = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null || this.projectileTemplate == null) {
            return;
        }

        board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        if (this.projectileTemplate != null) {
            this.projectileTemplate.addDamageBonus(effect.getDamageBonus());
            this.projectileTemplate.addPierceBonus(effect.getPierceBonus());
            this.projectileTemplate.addBounceBonus(effect.getBounceBonus());
            this.projectileTemplate.addConditionDuration(effect.getDurationBonusTicks());
            this.projectileTemplate.addPlantFoodChanceBonus(effect.getPlantFoodChanceBonusPercent());
            this.projectileTemplate.addPoisonDamagePerTick(effect.getPoisonDamageBonusPerTick());

            if (effect.getRangeBonus() > 0) {
                this.projectileTemplate.addSplashRadius(effect.getRangeBonus());
            }

            if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.BUTTER_CHANCE_UP)) {
                this.projectileTemplate.addStunChanceBonus(5);
            }
        }

        this.lobIntervalTicks = effect.upgradeInterval(this.lobIntervalTicks);
    }
}
