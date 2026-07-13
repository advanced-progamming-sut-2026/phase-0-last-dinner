package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.Projectile;
import model.zombie.ArmorFlag;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomingBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private long homingIntervalTicks;
    private long ticksSinceLastHoming;
    private HomingTargetMode targetMode;
    private String damageExpression;
    private boolean priorityUp;
    private final Random random = new Random();

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
            Zombie armoredTarget = this.selectArmoredTarget(plant, board);

            if (armoredTarget != null) {
                ZombieArmor armor = this.findMetallicArmor(armoredTarget);

                if (armor != null) {
                    armor.drop();
                }
            }
            return;
        }

        if (this.targetMode == HomingTargetMode.HYPNOSIS) {
            Zombie target = this.selectTarget(plant, board);

            if (target != null) {
                target.addCondition(model.zombie.ZombieCondition.HYPNOTIZED);
            }
            return;
        }

        if (this.projectileTemplate == null) {
            return;
        }

        Zombie target = this.selectTarget(plant, board);

        if (target != null) {
            board.addProjectile(this.projectileTemplate.copyAtTarget(plant.getPosition(), target));
        }
    }

    @Override
    public PlantBehavior copy() {
        Projectile projectileCopy = this.projectileTemplate == null
                ? null
                : this.projectileTemplate.copyAt(null);
        HomingBehavior copy = new HomingBehavior(
                projectileCopy,
                this.homingIntervalTicks,
                this.targetMode,
                this.damageExpression
        );
        copy.priorityUp = this.priorityUp;
        return copy;
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

        if (this.targetMode != HomingTargetMode.PRIORITY) {
            return board.getNearestZombie(plant.getPosition());
        }

        if (!this.priorityUp) {
            List<Zombie> candidates = new ArrayList<>();

            for (Zombie zombie : board.getAllZombies()) {
                if (zombie != null && !zombie.isDead()) {
                    candidates.add(zombie);
                }
            }

            return candidates.isEmpty() ? null : candidates.get(this.random.nextInt(candidates.size()));
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

    private Zombie selectArmoredTarget(Plant plant, Board board) {
        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.isDead() || this.findMetallicArmor(zombie) == null
                    || zombie.getPosition() == null) {
                continue;
            }

            double distance = plant.getPosition() == null
                    ? 0
                    : Math.abs(zombie.getPosition().getX() - plant.getPosition().getX());

            if (distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private ZombieArmor findMetallicArmor(Zombie zombie) {
        if (zombie == null || zombie.getArmors() == null) {
            return null;
        }

        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor == null || armor.isDestroyed() || armor.isDropped() || armor.getDefinition() == null
                    || armor.getDefinition().getFlags() == null) {
                continue;
            }

            if (armor.getDefinition().getFlags().contains(ArmorFlag.METALLIC)) {
                return armor;
            }
        }

        return null;
    }
}
