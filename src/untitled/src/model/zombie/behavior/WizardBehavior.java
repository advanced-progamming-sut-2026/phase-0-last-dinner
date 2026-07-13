package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class WizardBehavior implements ZombieBehavior {
    private long castIntervalTicks;
    private long ticksSinceCast;
    private List<Plant> transformedPlants = new ArrayList<>();

    public WizardBehavior(long castIntervalTicks) {
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
        this.transform(plant);
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return;
        }

        Plant nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Plant plant : board.getPlantsInRadius(zombie.getPosition(), 4)) {
            if (plant == null || plant.isDead() || plant.isDisabled() || plant.getPosition() == null) {
                continue;
            }

            int deltaX = plant.getPosition().getX() - zombie.getPosition().getX();
            int deltaY = plant.getPosition().getY() - zombie.getPosition().getY();
            int distance = deltaX * deltaX + deltaY * deltaY;

            if (distance < nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }

        this.transform(nearest);
    }

    @Override
    public boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        return false;
    }

    @Override
    public void onDeath(Zombie zombie, Board board) {
        for (Plant plant : this.transformedPlants) {
            if (plant != null && !plant.isDead()) {
                plant.enable();
            }
        }
        this.transformedPlants.clear();
    }

    private void transform(Plant plant) {
        if (plant != null && !plant.isDead() && !plant.isDisabled()) {
            plant.transform();
            this.transformedPlants.add(plant);
        }
    }
}
