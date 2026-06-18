package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.zombie.Zombie;

public class ProjectileReflectorBehavior implements ZombieBehavior {
    private boolean reflecting;

    public ProjectileReflectorBehavior(boolean reflecting) {
        this.reflecting = reflecting;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.reflecting = true;
    }

    public Projectile reflect(Projectile projectile) {
        if (!this.reflecting || projectile == null) {
            return projectile;
        }

        return projectile.copyAt(projectile.getPosition());
    }
}
