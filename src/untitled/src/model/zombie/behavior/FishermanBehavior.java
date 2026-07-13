package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.zombie.Zombie;

public class FishermanBehavior implements ZombieBehavior {
    private long castIntervalTicks;
    private long ticksSinceCast;

    public FishermanBehavior(long castIntervalTicks) {
        this.castIntervalTicks = Math.max(1, castIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceCast++;
        if (this.ticksSinceCast >= this.castIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceCast = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        Plant target = this.findHookTarget(zombie, board);
        if (target == null || target.getPosition() == null) {
            return;
        }

        Position destination = new Position(target.getPosition().getX() + 1, target.getPosition().getY());
        if (zombie.getPosition().getX() - target.getPosition().getX() <= 1) {
            if (board.getCombatSystem() != null) {
                board.getCombatSystem().destroyPlant(target);
            }
        } else if (board.getPlantsAt(destination).isEmpty()) {
            board.movePlant(target, destination);
        }
    }

    @Override
    public boolean canMove(Zombie zombie, Board board) {
        return false;
    }

    private Plant findHookTarget(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return null;
        }

        Plant nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Plant plant : board.getPlantsInLane(zombie.getPosition())) {
            if (plant == null || plant.isDead() || plant.getPosition() == null) {
                continue;
            }

            int distance = zombie.getPosition().getX() - plant.getPosition().getX();
            if (distance >= 1 && distance <= 8 && distance < nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
