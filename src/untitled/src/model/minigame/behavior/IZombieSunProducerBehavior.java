package model.minigame.behavior;

import model.Plant;
import model.mechanism.Board;
import model.minigame.IZombieMiniGame;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

public class IZombieSunProducerBehavior implements ZombieBehavior {
    private IZombieMiniGame miniGame;
    private int sunAmount;
    private long initialProductionIntervalTicks;
    private long minimumProductionIntervalTicks;

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
