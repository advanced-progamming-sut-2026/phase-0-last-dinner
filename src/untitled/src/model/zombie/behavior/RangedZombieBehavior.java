package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
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
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        Plant target = board.getNearestPlant(zombie.getPosition());

        if (target == null) {
            return;
        }

        if (this.abilityType == RangedAbilityType.MAGIC_TRANSFORM) {
            target.transform();
        } else if (this.abilityType == RangedAbilityType.FISHING_HOOK) {
            this.pullPlantTowardZombie(zombie, target, board);
        } else if (this.abilityType == RangedAbilityType.OCTOPUS) {
            target.disable();
        } else if (this.abilityType == RangedAbilityType.SNOWBALL) {
            target.disable();
            board.getCombatSystem().applyDamageToPlant(target, 25);
        }
    }

    private void pullPlantTowardZombie(Zombie zombie, Plant plant, Board board) {
        if (zombie == null || plant == null || plant.getPosition() == null || zombie.getPosition() == null) {
            return;
        }

        int delta = zombie.getPosition().getX() > plant.getPosition().getX() ? 1 : -1;
        Position destination = new Position(plant.getPosition().getX() + delta, plant.getPosition().getY());

        board.movePlant(plant, destination);
    }
}
