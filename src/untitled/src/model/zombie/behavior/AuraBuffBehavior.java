package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

import java.util.List;

public class AuraBuffBehavior implements ZombieBehavior {
    private double speedMultiplier;
    private double damageMultiplier;
    private int effectRadius;
    private List<Zombie> affectedZombies;

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
