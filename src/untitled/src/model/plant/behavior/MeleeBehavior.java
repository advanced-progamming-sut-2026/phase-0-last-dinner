package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
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

        if (this.meleePattern == MeleePattern.FRONT_AND_BACK) {
            zombies = board.getZombiesInFrontAndBack(plant.getPosition(), this.range);
        } else if (this.meleePattern == MeleePattern.SINGLE_TARGET) {
            zombies = new java.util.ArrayList<>();
            Zombie target = board.getNearestZombie(plant.getPosition());

            if (target != null) {
                zombies.add(target);
            }
        } else {
            zombies = board.getZombiesInRadius(plant.getPosition(), this.range);
        }

        for (Zombie zombie : zombies) {
            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie);
                this.remainingDigestTicks = this.digestTicks;
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(this.damageExpression);
                board.getCombatSystem().applyDamageToZombie(zombie, damage);
            }
        }
    }
}
