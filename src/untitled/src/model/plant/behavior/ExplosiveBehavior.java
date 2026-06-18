package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.zombie.Zombie;

import java.util.List;

public class ExplosiveBehavior implements PlantBehavior, OnPlantingBehavior {
    private String damageExpression;
    private int effectRadius;
    private boolean triggeredByContact;
    private ExplosivePattern explosivePattern;
    private long armDelayTicks;
    private long armedTicks;
    private boolean activateOnPlanting;

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

        if (this.explosivePattern == ExplosivePattern.FULL_LANE) {
            zombies = board.getZombiesInLane(plant.getPosition());
        } else if (this.explosivePattern == ExplosivePattern.FULL_BOARD) {
            zombies = board.getAllZombies();
        } else if (this.explosivePattern == ExplosivePattern.CONTACT_SINGLE) {
            zombies = board.getZombiesAt(plant.getPosition());
        } else {
            zombies = board.getZombiesInRadius(plant.getPosition(), this.effectRadius);
        }

        for (Zombie zombie : zombies) {
            if (DamageExpressionParser.isInstantKill(this.damageExpression)) {
                board.getCombatSystem().killZombie(zombie);
            } else {
                int damage = DamageExpressionParser.parseTotalDamage(this.damageExpression);
                board.getCombatSystem().applyDamageToZombie(zombie, damage);
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
}
