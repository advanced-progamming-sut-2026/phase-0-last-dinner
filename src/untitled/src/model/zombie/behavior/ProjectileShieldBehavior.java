package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;

public class ProjectileShieldBehavior implements ZombieBehavior {
    private ProjectileType blockedType;

    public ProjectileShieldBehavior(ProjectileType blockedType) {
        this.blockedType = blockedType;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    @Override
    public boolean canBeHitBy(Zombie zombie, Projectile projectile) {
        return !this.blocks(projectile);
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        return this.blocks(projectile);
    }

    private boolean blocks(Projectile projectile) {
        if (projectile == null) {
            return false;
        }

        if (this.blockedType == ProjectileType.LOBBED) {
            return projectile.isLobbed();
        }

        return projectile.getType() == this.blockedType;
    }
}
