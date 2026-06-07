package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;

public class GargantuarBehavior implements ZombieBehavior {
    private ZombieDefinition impDefinition;
    private ZombieFactory zombieFactory;
    private double throwHealthThreshold;
    private boolean impThrown;

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    public Zombie throwImp(Zombie zombie, Board board) {
        return null;
    }
}
