package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class CompositeZombieBehavior implements ZombieBehavior {
    private List<ZombieBehavior> behaviors = new ArrayList<>();

    public void addBehavior(ZombieBehavior behavior) {
        if (behavior != null) {
            this.behaviors.add(behavior);
        }
    }

    public boolean isEmpty() {
        return this.behaviors.isEmpty();
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            behavior.onTick(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            behavior.attack(zombie, plant, board);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            behavior.activate(zombie, board);
        }
    }
}
