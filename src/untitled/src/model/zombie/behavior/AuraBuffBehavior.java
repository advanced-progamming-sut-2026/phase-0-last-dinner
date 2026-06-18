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

    public AuraBuffBehavior(double speedMultiplier, double damageMultiplier, int effectRadius) {
        this.speedMultiplier = speedMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.effectRadius = effectRadius;
        this.affectedZombies = new java.util.ArrayList<>();
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.activate(zombie, board);
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || board == null) {
            return;
        }

        this.affectedZombies.clear();
        this.affectedZombies.addAll(board.getZombiesInRadius(zombie.getPosition(), this.effectRadius));

        for (Zombie affectedZombie : this.affectedZombies) {
            if (affectedZombie != null) {
                affectedZombie.setCurrentSpeed(affectedZombie.getCurrentSpeed() * this.speedMultiplier);
            }
        }
    }
}
