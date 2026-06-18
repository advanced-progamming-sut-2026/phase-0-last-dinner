package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class RangedZombieBehavior implements ZombieBehavior {
    private RangedAbilityType abilityType;
    private int range;
    private long actionIntervalTicks;
    private long ticksSinceLastAction;

    public RangedZombieBehavior(RangedAbilityType abilityType, int range, long actionIntervalTicks) {
        this.abilityType = abilityType;
        this.range = range;
        this.actionIntervalTicks = actionIntervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceLastAction++;

        if (this.ticksSinceLastAction >= this.actionIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceLastAction = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().applyDamageToPlant(plant, 1);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        Plant target = board.getNearestPlant(zombie.getPosition());

        if (target == null) {
            return;
        }

        if (this.abilityType == RangedAbilityType.MAGIC_TRANSFORM) {
            board.getCombatSystem().destroyPlant(target);
        }
    }
}
