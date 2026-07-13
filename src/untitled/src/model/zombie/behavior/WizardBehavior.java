package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WizardBehavior implements ZombieBehavior {
    private long castIntervalTicks;
    private long ticksSinceCast;
    private List<Plant> transformedPlants = new ArrayList<>();
    private Random random = new Random();

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
        if (zombie == null || board == null) {
            return;
        }

        List<Plant> eligiblePlants = new ArrayList<>();

        for (Plant plant : board.getAllPlants()) {
            if (plant != null && !plant.isDead() && !plant.isDisabled()
                    && plant.getPosition() != null) {
                eligiblePlants.add(plant);
            }
        }

        if (!eligiblePlants.isEmpty()) {
            this.transform(eligiblePlants.get(this.random.nextInt(eligiblePlants.size())));
        }
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

    public void setRandom(Random random) {
        this.random = random == null ? new Random() : random;
    }

    private void transform(Plant plant) {
        if (plant != null && !plant.isDead() && !plant.isDisabled()) {
            plant.transform();
            this.transformedPlants.add(plant);
        }
    }
}
