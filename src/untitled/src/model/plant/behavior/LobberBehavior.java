package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;

import java.util.Random;

public class LobberBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private long lobIntervalTicks;
    private long ticksSinceLastLob;
    private boolean areaDamage;
    private boolean randomButter;
    private final Random random = new Random();

    public LobberBehavior(Projectile projectileTemplate, long lobIntervalTicks, boolean areaDamage) {
        this(projectileTemplate, lobIntervalTicks, areaDamage, false);
    }

    public LobberBehavior(
            Projectile projectileTemplate,
            long lobIntervalTicks,
            boolean areaDamage,
            boolean randomButter
    ) {
        this.projectileTemplate = projectileTemplate;
        this.lobIntervalTicks = lobIntervalTicks;
        this.areaDamage = areaDamage;
        this.randomButter = randomButter;

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

        model.zombie.Zombie target = this.findLaneTarget(plant, board);

        if (target != null) {
            Projectile projectile = this.projectileTemplate.copyAtTarget(plant.getPosition(), target);

            if (this.randomButter) {
                boolean butter = this.random.nextInt(4) == 0;
                projectile.setDamageExpression(DamageExpressionParser.selectExpressionAt(
                        this.projectileTemplate.getDamageExpression(),
                        butter ? 1 : 0
                ));

                if (butter) {
                    projectile.addStunChanceBonus(100);
                }
            }

            board.addProjectile(projectile);
        }
    }

    @Override
    public PlantBehavior copy() {
        Projectile projectileCopy = this.projectileTemplate == null
                ? null
                : this.projectileTemplate.copyAt(null);
        return new LobberBehavior(
                projectileCopy,
                this.lobIntervalTicks,
                this.areaDamage,
                this.randomButter
        );
    }

    private model.zombie.Zombie findLaneTarget(Plant plant, Board board) {
        model.zombie.Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (model.zombie.Zombie zombie : board.getZombiesInLane(plant.getPosition())) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }

            double distance = zombie.getPosition().getX() - plant.getPosition().getX();

            if (distance >= 0 && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }

        return nearest;
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
