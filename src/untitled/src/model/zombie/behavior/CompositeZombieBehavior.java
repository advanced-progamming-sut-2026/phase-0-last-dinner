package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
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

    public <T extends ZombieBehavior> T findBehavior(Class<T> behaviorType) {
        if (behaviorType == null) {
            return null;
        }

        for (ZombieBehavior behavior : this.behaviors) {
            if (behaviorType.isInstance(behavior)) {
                return behaviorType.cast(behavior);
            }

            if (behavior instanceof CompositeZombieBehavior) {
                T nestedBehavior = ((CompositeZombieBehavior) behavior).findBehavior(behaviorType);

                if (nestedBehavior != null) {
                    return nestedBehavior;
                }
            }
        }

        return null;
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

    @Override
    public void applyPlantUpgrade(PlantUpgradeEffect effect) {
        for (ZombieBehavior behavior : this.behaviors) {
            behavior.applyPlantUpgrade(effect);
        }
    }
}
