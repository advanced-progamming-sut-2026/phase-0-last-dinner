package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

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
            if (zombie != null && zombie.isHypnotized() && !behavior.runsWhileHypnotized()) {
                continue;
            }

            behavior.onTick(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (this.shouldSkipWhileHypnotized(zombie, behavior)) {
                continue;
            }

            behavior.attack(zombie, plant, board);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (this.shouldSkipWhileHypnotized(zombie, behavior)) {
                continue;
            }

            behavior.activate(zombie, board);
        }
    }

    @Override
    public void applyPlantUpgrade(PlantUpgradeEffect effect) {
        for (ZombieBehavior behavior : this.behaviors) {
            behavior.applyPlantUpgrade(effect);
        }
    }

    @Override
    public void onDeath(Zombie zombie, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            behavior.onDeath(zombie, board);
        }
    }

    @Override
    public boolean canMove(Zombie zombie, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (this.shouldSkipWhileHypnotized(zombie, behavior)) {
                continue;
            }

            if (!behavior.canMove(zombie, board)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (this.shouldSkipWhileHypnotized(zombie, behavior)) {
                continue;
            }

            if (!behavior.canAttackPlant(zombie, plant, board)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canBeHitBy(Zombie zombie, Projectile projectile) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (!behavior.canBeHitBy(zombie, projectile)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        // avalin behavior ke projectile ro consume kone damage va handler badi ro migire
        for (ZombieBehavior behavior : this.behaviors) {
            if (behavior.onProjectileHit(zombie, projectile, board)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptsCondition(Zombie zombie, ZombieCondition condition, Projectile projectile) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (!behavior.acceptsCondition(zombie, condition, projectile)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getMovementDirection(Zombie zombie) {
        for (ZombieBehavior behavior : this.behaviors) {
            if (this.shouldSkipWhileHypnotized(zombie, behavior)) {
                continue;
            }

            int direction = behavior.getMovementDirection(zombie);
            if (direction != -1) {
                return direction;
            }
        }
        return -1;
    }

    private boolean shouldSkipWhileHypnotized(Zombie zombie, ZombieBehavior behavior) {
        // behavior hostile dar hypnosis skip mishe magar khodesh ejaze bede
        return zombie != null && zombie.isHypnotized() && !behavior.runsWhileHypnotized();
    }
}
