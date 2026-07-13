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
import model.zombie.behavior.FlyingBehavior;

import java.util.List;

// arm shodan va effect giah haye enfajari ya terrain remover ro modiriat mikone
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
    private boolean waterTargetsOnly;
    // in state baraye effect takhiri mesl khordan grave estefade mishe
    private long terrainRemovalDelayTicks;
    private long terrainRemovalElapsedTicks;
    private boolean terrainRemovalStarted;

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
        if (this.terrainRemovalStarted) {
            this.terrainRemovalElapsedTicks++;

            if (this.terrainRemovalElapsedTicks >= this.terrainRemovalDelayTicks) {
                this.finishTerrainRemoval(plant, board, TerrainType.GRAVE);
            }
            return;
        }

        if (!this.triggeredByContact || plant == null || board == null) {
            return;
        }

        if (this.armedTicks < this.armDelayTicks) {
            this.armedTicks++;
            return;
        }

        if (!this.getContactZombies(plant, board).isEmpty()) {
            this.activate(plant, board);
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null || this.consumed || this.terrainRemovalStarted) {
            return;
        }

        if (this.waterTargetsOnly && !this.isWaterTile(board, plant.getPosition())) {
            return;
        }

        List<Zombie> zombies;

        if (this.explosivePattern == ExplosivePattern.TERRAIN_ONLY) {
            this.finishTerrainRemoval(plant, board, TerrainType.FROZEN);
            return;
        }

        if (this.explosivePattern == ExplosivePattern.GRAVE_ONLY) {
            if (this.terrainRemovalDelayTicks > 0) {
                this.terrainRemovalStarted = true;
            } else {
                this.finishTerrainRemoval(plant, board, TerrainType.GRAVE);
            }
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
            zombies = this.getContactZombies(plant, board);

            if (zombies.size() > this.targetCount) {
                zombies.subList(this.targetCount, zombies.size()).clear();
            }
        } else {
            zombies = board.getZombiesInRadius(plant.getPosition(), this.effectRadius);
        }

        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.isHypnotized()
                    || (this.waterTargetsOnly && !this.isSubmergedWaterZombie(zombie, board))
                    || (!this.waterTargetsOnly && zombie.hasCondition(ZombieCondition.SUBMERGED))) {
                continue;
            }

            this.applyCondition(zombie);

            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                if (this.waterTargetsOnly) {
                    board.getCombatSystem().killZombieIgnoringAllegiance(zombie);
                } else {
                    board.getCombatSystem().killZombie(zombie);
                }
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

    public void setWaterTargetsOnly(boolean waterTargetsOnly) {
        this.waterTargetsOnly = waterTargetsOnly;
    }

    public void setTerrainRemovalDelayTicks(long delayTicks) {
        this.terrainRemovalDelayTicks = Math.max(0, delayTicks);
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
        copy.waterTargetsOnly = this.waterTargetsOnly;
        copy.terrainRemovalDelayTicks = this.terrainRemovalDelayTicks;
        copy.terrainRemovalElapsedTicks = this.terrainRemovalElapsedTicks;
        copy.terrainRemovalStarted = this.terrainRemovalStarted;
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

        if (this.terrainRemovalDelayTicks > 0) {
            this.terrainRemovalDelayTicks = effect.upgradeInterval(this.terrainRemovalDelayTicks);
        }

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

    private void finishTerrainRemoval(Plant plant, Board board, TerrainType terrainType) {
        if (plant == null || board == null || this.consumed) {
            return;
        }

        this.consumed = true;
        this.clearTerrain(plant, board, terrainType);
        this.explodeAfterFinishIfNeeded(plant, board);
        board.removePlant(plant);
    }

    private void explodeAfterFinishIfNeeded(Plant plant, Board board) {
        if (!this.explodeOnFinish || plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        int parsedDamage = DamageExpressionParser.isInstantKill(this.damageExpression)
                ? 0
                : DamageExpressionParser.parseTotalDamage(this.damageExpression);
        int damage = Math.max(300, parsedDamage);

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

    private List<Zombie> getContactZombies(Plant plant, Board board) {
        List<Zombie> zombies = new java.util.ArrayList<>();

        if (plant == null || plant.getPosition() == null || board == null) {
            return zombies;
        }

        for (Zombie zombie : board.getZombiesInLane(plant.getPosition())) {
            if (zombie != null && !zombie.isDead() && !zombie.isHypnotized()
                    && zombie.getPosition() != null
                    && zombie.findBehavior(FlyingBehavior.class) == null
                    && !zombie.hasCondition(ZombieCondition.FLYING)
                    && ((this.waterTargetsOnly && this.isSubmergedWaterZombie(zombie, board))
                    || (!this.waterTargetsOnly && !zombie.hasCondition(ZombieCondition.SUBMERGED)))
                    && Math.abs(zombie.getExactX() - plant.getPosition().getX()) <= 1.0) {
                zombies.add(zombie);
            }
        }

        zombies.sort((first, second) -> Double.compare(
                Math.abs(first.getExactX() - plant.getPosition().getX()),
                Math.abs(second.getExactX() - plant.getPosition().getX())
        ));
        return zombies;
    }

    private boolean isSubmergedWaterZombie(Zombie zombie, Board board) {
        return zombie != null
                && zombie.getPosition() != null
                && zombie.hasCondition(ZombieCondition.SUBMERGED)
                && this.isWaterTile(board, zombie.getPosition());
    }

    private boolean isWaterTile(Board board, Position position) {
        Tile tile = board == null ? null : board.getTile(position);
        return tile != null && tile.getTerrainType() == TerrainType.WATER;
    }
}
