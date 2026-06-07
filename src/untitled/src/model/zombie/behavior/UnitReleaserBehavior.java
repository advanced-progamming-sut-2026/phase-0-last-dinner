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

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }
}
