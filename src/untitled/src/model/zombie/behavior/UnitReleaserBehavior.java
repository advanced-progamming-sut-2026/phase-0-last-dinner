package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;

import java.util.List;

public class UnitReleaserBehavior implements ZombieBehavior {
    private ZombieDefinition releasedUnitDefinition;
    private ZombieFactory zombieFactory;
    private int releaseCount;
    private double releaseHealthThreshold;
    private List<Zombie> releasedUnits;
    private boolean released;

    public UnitReleaserBehavior(
            ZombieDefinition releasedUnitDefinition,
            ZombieFactory zombieFactory,
            int releaseCount,
            double releaseHealthThreshold
    ) {
        this.releasedUnitDefinition = releasedUnitDefinition;
        this.zombieFactory = zombieFactory;
        this.releaseCount = releaseCount;
        this.releaseHealthThreshold = releaseHealthThreshold;
        this.releasedUnits = new java.util.ArrayList<>();
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (this.released || zombie == null || zombie.getDefinition() == null) {
            return;
        }

        if (zombie.getHealth() <= zombie.getDefinition().getHitpoints() * this.releaseHealthThreshold) {
            this.activate(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (this.released || zombie == null || board == null
                || this.zombieFactory == null || this.releasedUnitDefinition == null) {
            return;
        }

        for (int i = 0; i < this.releaseCount; i++) {
            Zombie releasedUnit = this.zombieFactory.create(this.releasedUnitDefinition, zombie.getPosition());
            this.releasedUnits.add(releasedUnit);
            board.addZombie(releasedUnit, zombie.getPosition());
        }

        this.released = true;
    }
}
