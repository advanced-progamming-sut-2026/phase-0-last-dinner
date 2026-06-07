package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class AmphibiousBehavior implements ZombieBehavior {
    private double waterSpeed;
    private double landSpeed;
    private boolean submerged;
    private boolean targetableWhileSubmerged;

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
