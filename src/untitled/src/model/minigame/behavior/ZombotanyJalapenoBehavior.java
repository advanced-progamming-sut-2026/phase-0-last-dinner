package model.minigame.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

public class ZombotanyJalapenoBehavior implements ZombieBehavior {
    private long explosionDelayTicks;
    private long enteredBoardTick;

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    public void burnRow(Zombie zombie, Board board) {
    }
}
