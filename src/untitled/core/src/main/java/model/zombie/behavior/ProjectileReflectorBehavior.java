package model.zombie.behavior;

import model.mechanism.Board;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;

public class ProjectileReflectorBehavior implements ZombieBehavior {
    private boolean reflecting;
    private double normalSpeed = -1;

    public ProjectileReflectorBehavior(boolean reflecting) {
        this.reflecting = reflecting;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return;
        }

        if (this.normalSpeed < 0) {
            this.normalSpeed = zombie.getDefinition() == null
                    ? zombie.getCurrentSpeed()
                    : zombie.getDefinition().getSpeed();
        }

        boolean projectileApproaching = false;
        for (Projectile projectile : board.getProjectiles()) {
            if (this.isReflectable(projectile)
                    && projectile.getPosition() != null
                    && projectile.getPosition().getY() == zombie.getPosition().getY()
                    && projectile.getHorizontalDirection() > 0
                    && projectile.getPosition().getX() <= zombie.getPosition().getX()) {
                projectileApproaching = true;
                break;
            }
        }

        this.reflecting = projectileApproaching;
        zombie.setCurrentSpeed(this.reflecting ? this.normalSpeed * 1.1 : this.normalSpeed);
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.reflecting = true;
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        if (!this.isReflectable(projectile) || zombie == null
                || zombie.getPosition() == null || board == null) {
            return false;
        }

        this.reflecting = true;
        // haman projectile baraks mishe ta pipeline collision board hefz beshe
        projectile.reflectTowardPlants(zombie.getPosition());
        return true;
    }

    private boolean isReflectable(Projectile projectile) {
        if (projectile == null || projectile.getType() == null
                || projectile.isHostileToPlants() || projectile.isWaitingForRelease()) {
            return false;
        }

        return !projectile.isLobbed()
                && projectile.getType() != ProjectileType.LOBBED
                && projectile.getType() != ProjectileType.HOMING;
    }
}
