package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class FlyingBehavior implements ZombieBehavior {
    private boolean ignoresGroundObstacles;

    public FlyingBehavior(boolean ignoresGroundObstacles) {
        this.ignoresGroundObstacles = ignoresGroundObstacles;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie != null) {
            zombie.move();
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
        this.ignoresGroundObstacles = true;
    }
}
