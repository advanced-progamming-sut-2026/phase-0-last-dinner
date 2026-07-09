package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.Projectile;
import model.zombie.Zombie;

public class HomingBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private long homingIntervalTicks;
    private long ticksSinceLastHoming;
    private HomingTargetMode targetMode;
    private String damageExpression;
    private boolean priorityUp;

    public HomingBehavior(
            Projectile projectileTemplate,
            long homingIntervalTicks,
            HomingTargetMode targetMode,
            String damageExpression
    ) {
        this.projectileTemplate = projectileTemplate;
        this.homingIntervalTicks = homingIntervalTicks;
        this.targetMode = targetMode;
        this.damageExpression = damageExpression;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        this.ticksSinceLastHoming++;

        if (this.ticksSinceLastHoming >= this.homingIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastHoming = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null) {
            return;
        }

        if (this.targetMode == HomingTargetMode.ARMOR) {
            return;
        }

        if (this.targetMode == HomingTargetMode.HYPNOSIS && board.getCombatSystem() != null) {
            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(board.getNearestZombie(plant.getPosition()));
            }
            return;
        }

        if (this.projectileTemplate == null) {
            return;
        }

        board.addProjectile(this.projectileTemplate.copyAtTarget(plant.getPosition(), this.selectTarget(plant, board)));
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        if (this.projectileTemplate != null) {
            this.projectileTemplate.addDamageBonus(effect.getDamageBonus());
            this.projectileTemplate.addConditionDuration(effect.getDurationBonusTicks());
            this.projectileTemplate.addPlantFoodChanceBonus(effect.getPlantFoodChanceBonusPercent());
            this.projectileTemplate.addPoisonDamagePerTick(effect.getPoisonDamageBonusPerTick());
        }

        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, effect.getDamageBonus());
        this.homingIntervalTicks = effect.upgradeInterval(this.homingIntervalTicks);

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.TARGET_PRIORITY_UP)) {
            this.priorityUp = true;
        }
    }

    private Zombie selectTarget(Plant plant, Board board) {
        if (plant == null || board == null) {
            return null;
        }

        if (this.targetMode != HomingTargetMode.PRIORITY || !this.priorityUp) {
            return board.getNearestZombie(plant.getPosition());
        }

        Zombie bestTarget = null;
        int bestScore = Integer.MIN_VALUE;

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            int armorScore = zombie.getActiveArmor() == null ? 0 : 10000;
            int score = armorScore + zombie.getHealth();

            if (score > bestScore) {
                bestScore = score;
                bestTarget = zombie;
            }
        }

        return bestTarget == null ? board.getNearestZombie(plant.getPosition()) : bestTarget;
    }
}
