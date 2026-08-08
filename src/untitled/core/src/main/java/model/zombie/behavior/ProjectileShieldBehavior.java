package model.zombie.behavior;

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
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        // true yani projectile consume mishe va damage mamooli ejra nemishe
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
