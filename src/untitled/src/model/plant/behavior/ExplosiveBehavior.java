package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.List;

public class ExplosiveBehavior implements PlantBehavior, OnPlantingBehavior {
    private String damageExpression;
    private int effectRadius;
    private boolean triggeredByContact;
    private ExplosivePattern explosivePattern;
    private long armDelayTicks;
    private long armedTicks;
    private boolean activateOnPlanting;
    private int targetCount;
    private ZombieCondition conditionOnHit;
    private long conditionDurationTicks;
    private boolean explodeOnFinish;
    private boolean consumed;
    private boolean createsCrater;
    private boolean meltsLane;
    private Projectile secondaryProjectileTemplate;
    private int secondaryProjectileCount;

    public ExplosiveBehavior(
            String damageExpression,
            int effectRadius,
            boolean triggeredByContact,
            ExplosivePattern explosivePattern,
            long armDelayTicks,
            boolean activateOnPlanting
    ) {
        this.damageExpression = damageExpression;
        this.effectRadius = effectRadius;
        this.triggeredByContact = triggeredByContact;
        this.explosivePattern = explosivePattern;
        this.armDelayTicks = armDelayTicks;
        this.activateOnPlanting = activateOnPlanting;
        this.targetCount = 1;
    }


    @Override
    public void onTick(Plant plant, Board board) {
        if (!this.triggeredByContact || plant == null || board == null) {
            return;
        }

        if (this.armedTicks < this.armDelayTicks) {
            this.armedTicks++;
            return;
        }

        if (!board.getZombiesAt(plant.getPosition()).isEmpty()) {
            this.activate(plant, board);
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null || this.consumed) {
            return;
        }

        List<Zombie> zombies;

        if (this.explosivePattern == ExplosivePattern.TERRAIN_ONLY) {
            this.consumed = true;
            this.clearTerrain(plant, board, TerrainType.FROZEN);
            this.explodeAfterFinishIfNeeded(plant, board);
            board.removePlant(plant);
            return;
        }

        if (this.explosivePattern == ExplosivePattern.GRAVE_ONLY) {
            this.consumed = true;
            this.clearTerrain(plant, board, TerrainType.GRAVE);
            this.explodeAfterFinishIfNeeded(plant, board);
            board.removePlant(plant);
            return;
        }

        if (board.getCombatSystem() == null) {
            return;
        }

        this.consumed = true;

        if (this.explosivePattern == ExplosivePattern.FULL_LANE) {
            zombies = board.getZombiesInLane(plant.getPosition());
        } else if (this.explosivePattern == ExplosivePattern.FULL_BOARD) {
            zombies = board.getAllZombies();
        } else if (this.explosivePattern == ExplosivePattern.CONTACT_SINGLE) {
            zombies = this.targetCount > 1
                    ? board.getNearestZombies(plant.getPosition(), this.targetCount)
                    : board.getZombiesAt(plant.getPosition());
        } else {
            zombies = board.getZombiesInRadius(plant.getPosition(), this.effectRadius);
        }

        for (Zombie zombie : zombies) {
            this.applyCondition(zombie);

            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(this.damageExpression);

                if (damage > 0) {
                    board.getCombatSystem().applyDamageToZombie(zombie, damage);
                }
            }
        }

        this.applyTerrainEffect(plant, board);
        this.launchSecondaryProjectiles(plant, board);
        board.removePlant(plant);
    }

    @Override
    public boolean shouldActivateOnPlanting() {
        return this.activateOnPlanting;
    }

    public void armNow() {
        this.armedTicks = this.armDelayTicks;
    }

    public void setConditionOnHit(ZombieCondition conditionOnHit, long conditionDurationTicks) {
        this.conditionOnHit = conditionOnHit;
        this.conditionDurationTicks = Math.max(0, conditionDurationTicks);
    }

    public void setCreatesCrater(boolean createsCrater) {
        this.createsCrater = createsCrater;
    }

    public void setMeltsLane(boolean meltsLane) {
        this.meltsLane = meltsLane;
    }

    public void setSecondaryProjectileBurst(Projectile projectileTemplate, int projectileCount) {
        this.secondaryProjectileTemplate = projectileTemplate;
        this.secondaryProjectileCount = Math.max(0, projectileCount);
    }

    @Override
    public PlantBehavior copy() {
        ExplosiveBehavior copy = new ExplosiveBehavior(
                this.damageExpression,
                this.effectRadius,
                this.triggeredByContact,
                this.explosivePattern,
                this.armDelayTicks,
                this.activateOnPlanting
        );
        copy.armedTicks = this.armedTicks;
        copy.targetCount = this.targetCount;
        copy.conditionOnHit = this.conditionOnHit;
        copy.conditionDurationTicks = this.conditionDurationTicks;
        copy.explodeOnFinish = this.explodeOnFinish;
        copy.createsCrater = this.createsCrater;
        copy.meltsLane = this.meltsLane;
        copy.secondaryProjectileTemplate = this.secondaryProjectileTemplate == null
                ? null
                : this.secondaryProjectileTemplate.copyAt(null);
        copy.secondaryProjectileCount = this.secondaryProjectileCount;
        return copy;
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, effect.getDamageBonus());
        this.effectRadius += effect.getRangeBonus();
        this.armDelayTicks = Math.max(0, this.armDelayTicks - effect.getArmDelayReductionTicks());
        this.conditionDurationTicks += effect.getDurationBonusTicks();
        this.targetCount += effect.getTargetCountBonus();

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.CAN_CRUSH_EXTRA_TARGET)) {
            this.targetCount = Math.max(this.targetCount, 2);
        }

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.EXPLODE_ON_FINISH)) {
            this.explodeOnFinish = true;
        }

        if (effect.hasSpecialEffect(PlantUpgradeSpecialEffect.MELT_AREA)) {
            this.effectRadius = Math.max(this.effectRadius, 1);
        }
    }

    private void applyCondition(Zombie zombie) {
        if (zombie == null || this.conditionOnHit == null) {
            return;
        }

        if (this.conditionDurationTicks > 0) {
            zombie.addCondition(this.conditionOnHit, this.conditionDurationTicks);
        } else {
            zombie.addCondition(this.conditionOnHit);
        }
    }

    private void clearTerrain(Plant plant, Board board, TerrainType terrainType) {
        if (plant == null || board == null || plant.getPosition() == null || terrainType == null) {
            return;
        }

        for (int deltaY = -this.effectRadius; deltaY <= this.effectRadius; deltaY++) {
            for (int deltaX = -this.effectRadius; deltaX <= this.effectRadius; deltaX++) {
                Position position = new Position(
                        plant.getPosition().getX() + deltaX,
                        plant.getPosition().getY() + deltaY
                );
                Tile tile = board.getTile(position);

                if (tile != null && tile.getTerrainType() == terrainType) {
                    board.setTerrain(position, TerrainType.CLASSIC);
                }
            }
        }
    }

    private void explodeAfterFinishIfNeeded(Plant plant, Board board) {
        if (!this.explodeOnFinish || plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        int damage = Math.max(300, DamageExpressionParser.parseTotalDamage(this.damageExpression));

        for (Zombie zombie : board.getZombiesInRadius(plant.getPosition(), Math.max(1, this.effectRadius))) {
            board.getCombatSystem().applyDamageToZombie(zombie, damage);
        }
    }

    private void applyTerrainEffect(Plant plant, Board board) {
        if (plant.getPosition() == null) {
            return;
        }

        if (this.meltsLane) {
            for (Tile tile : board.getTiles()) {
                if (tile != null && tile.getPosition() != null
                        && tile.getPosition().getY() == plant.getPosition().getY()
                        && tile.getTerrainType() == TerrainType.FROZEN) {
                    board.setTerrain(tile.getPosition(), TerrainType.CLASSIC);
                }
            }
        }

        if (this.createsCrater) {
            board.setTerrain(plant.getPosition(), TerrainType.CRATER);
        }
    }

    private void launchSecondaryProjectiles(Plant plant, Board board) {
        if (this.secondaryProjectileTemplate == null || this.secondaryProjectileCount <= 0
                || plant.getPosition() == null) {
            return;
        }

        int[][] directions = {
                {1, 0}, {-1, 0}, {1, 1}, {1, -1},
                {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}
        };

        for (int i = 0; i < this.secondaryProjectileCount; i++) {
            int[] direction = directions[i % directions.length];
            board.addProjectile(this.secondaryProjectileTemplate.copyAt(
                    plant.getPosition(),
                    direction[0],
                    direction[1]
            ));
        }
    }
}
