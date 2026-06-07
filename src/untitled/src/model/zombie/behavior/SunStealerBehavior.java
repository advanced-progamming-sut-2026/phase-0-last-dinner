package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.SunSystem;
import model.zombie.Zombie;

public class SunStealerBehavior implements ZombieBehavior {
    private int stealAmount;
    private SunSystem sunSystem;

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
