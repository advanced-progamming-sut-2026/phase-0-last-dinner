package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class FlyingBehavior implements ZombieBehavior {
    private boolean ignoresGroundObstacles;

    public FlyingBehavior(boolean ignoresGroundObstacles) {
        this.ignoresGroundObstacles = ignoresGroundObstacles;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie != null) {
            zombie.addCondition(ZombieCondition.FLYING);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        // Gaz gereftan ro BasicZombieBehavior handle mikone; in class faghat flying state ro mide.
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.ignoresGroundObstacles = true;
    }
}
