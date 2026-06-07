package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class GraveSummonerBehavior implements ZombieBehavior {
    private int graveCount;
    private long summonIntervalTicks;

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
