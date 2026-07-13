package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantTag;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.Locale;

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
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.ignoresGroundObstacles = true;
    }

    @Override
    public boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        if (!this.ignoresGroundObstacles || plant == null || plant.getName() == null) {
            return true;
        }

        String name = plant.getName().toLowerCase(Locale.ROOT);
        if (name.contains("tall-nut") || name.contains("tall nut")
                || plant.getTags() != null && (plant.getTags().contains(PlantTag.TRAP)
                || plant.getTags().contains(PlantTag.MOVE_ZOMBIES))) {
            return true;
        }

        return false;
    }
}
