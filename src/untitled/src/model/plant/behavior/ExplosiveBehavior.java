package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
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
        if (plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        List<Zombie> zombies;

        if (this.explosivePattern == ExplosivePattern.TERRAIN_ONLY) {
            this.clearTerrain(plant, board, TerrainType.FROZEN);
            this.explodeAfterFinishIfNeeded(plant, board);
            return;
        }

        if (this.explosivePattern == ExplosivePattern.GRAVE_ONLY) {
            this.clearTerrain(plant, board, TerrainType.GRAVE);
            this.explodeAfterFinishIfNeeded(plant, board);
            return;
        }

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
}
