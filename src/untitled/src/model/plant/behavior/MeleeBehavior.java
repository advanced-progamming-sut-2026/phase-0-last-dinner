package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;

import java.util.List;

public class MeleeBehavior implements PlantBehavior {
    private String damageExpression;
    private int range;
    private long attackIntervalTicks;
    private long ticksSinceLastAttack;
    private MeleePattern meleePattern;
    private long digestTicks;
    private long remainingDigestTicks;
    private long ageTicks;

    public MeleeBehavior(
            String damageExpression,
            int range,
            long attackIntervalTicks,
            MeleePattern meleePattern,
            long digestTicks
    ) {
        this.damageExpression = damageExpression;
        this.range = range;
        this.attackIntervalTicks = attackIntervalTicks;
        this.meleePattern = meleePattern;
        this.digestTicks = digestTicks;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        this.ageTicks++;

        if (this.remainingDigestTicks > 0) {
            this.remainingDigestTicks--;
            return;
        }

        this.ticksSinceLastAttack++;

        if (this.ticksSinceLastAttack >= this.attackIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastAttack = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        List<Zombie> zombies;

        int activeRange = this.activeRange();

        if (this.meleePattern == MeleePattern.FRONT_AND_BACK) {
            zombies = board.getZombiesInFrontAndBack(plant.getPosition(), activeRange);
        } else if (this.meleePattern == MeleePattern.SINGLE_TARGET) {
            zombies = new java.util.ArrayList<>();
            Zombie target = this.findFrontTarget(plant, board, activeRange);

            if (target != null) {
                zombies.add(target);
            }
        } else {
            zombies = board.getZombiesInRadius(plant.getPosition(), activeRange);
        }

        String activeDamage = DamageExpressionParser.selectExpressionAt(
                this.damageExpression,
                this.rampStage()
        );

        for (Zombie zombie : zombies) {
            if (DamageExpressionParser.isInstantKill(activeDamage)) {
                board.getCombatSystem().killZombie(zombie);
                this.remainingDigestTicks = this.digestTicks;
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(activeDamage);
                board.getCombatSystem().applyDamageToZombie(zombie, damage);
            }
        }
    }

    @Override
    public PlantBehavior copy() {
        MeleeBehavior copy = new MeleeBehavior(
                this.damageExpression,
                this.range,
                this.attackIntervalTicks,
                this.meleePattern,
                this.digestTicks
        );
        copy.ageTicks = this.ageTicks;
        return copy;
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, effect.getDamageBonus());
        this.range += effect.getRangeBonus();
        this.attackIntervalTicks = effect.upgradeInterval(this.attackIntervalTicks);
        this.digestTicks = Math.max(0, this.digestTicks - effect.getDigestReductionTicks());
    }

    private Zombie findFrontTarget(Plant plant, Board board, int activeRange) {
        if (plant.getPosition() == null) {
            return null;
        }

        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Zombie zombie : board.getZombiesInLane(plant.getPosition())) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }

            double distance = zombie.getPosition().getX() - plant.getPosition().getX();

            if (distance >= 0 && distance <= activeRange && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private int rampStage() {
        if (this.meleePattern != MeleePattern.RAMPING_RADIUS) {
            return 0;
        }

        if (this.ageTicks >= 720) {
            return 2;
        }

        if (this.ageTicks >= 240) {
            return 1;
        }

        return 0;
    }

    private int activeRange() {
        return this.range + (this.meleePattern == MeleePattern.RAMPING_RADIUS ? this.rampStage() : 0);
    }
}
