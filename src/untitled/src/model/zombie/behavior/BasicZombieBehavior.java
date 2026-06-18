package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class BasicZombieBehavior implements ZombieBehavior {
    private int eatDamagePerSecond;

    public BasicZombieBehavior() {
        this(0);
    }

    public BasicZombieBehavior(int eatDamagePerSecond) {
        this.eatDamagePerSecond = eatDamagePerSecond;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie != null) {
            zombie.move();
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant == null || board == null || board.getCombatSystem() == null) {
            return;
        }

        board.getCombatSystem().applyDamageToPlant(plant, this.eatDamagePerSecond);
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }
}
