package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class SnowballBehavior implements ZombieBehavior {
    private final long throwIntervalTicks;
    private long ticksSinceThrow;

    public SnowballBehavior(long throwIntervalTicks) {
        this.throwIntervalTicks = Math.max(1, throwIntervalTicks);
    }

    public SnowballBehavior(int ignoredRange, long throwIntervalTicks) {
        this(throwIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceThrow++;
        if (this.ticksSinceThrow >= this.throwIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceThrow = 0;
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        Plant target = this.findNearestPlantInLane(zombie, board);

        if (target != null) {
            // state cover ro board negah midare ta ba marge thrower hazf nashe
            board.getPlantCoverSystem().hitWithSnowball(target);
        }
    }

    private Plant findNearestPlantInLane(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return null;
        }

        Plant nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Plant plant : board.getPlantsInLane(zombie.getPosition())) {
            if (plant == null || plant.isDead() || plant.isDisabled() || plant.getPosition() == null) {
                continue;
            }

            int distance = zombie.getPosition().getX() - plant.getPosition().getX();
            if (distance >= 0 && distance < nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
