package model.minigame.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

public class ZombotanyPeashooterBehavior implements ZombieBehavior {
    private Projectile projectileTemplate;
    private long shootingIntervalTicks;

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
