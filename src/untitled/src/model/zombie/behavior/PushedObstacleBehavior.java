package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.zombie.Zombie;

public final class PushedObstacleBehavior implements ZombieBehavior {
    @Override
    public boolean canMove(Zombie obstacle, Board board) {
        return false;
    }

    @Override
    public boolean canAttackPlant(Zombie obstacle, Plant plant, Board board) {
        return false;
    }

    @Override
    public boolean canBeHitBy(Zombie obstacle, Projectile projectile) {
        return projectile != null && !projectile.isLobbed();
    }

    @Override
    public boolean onProjectileHit(Zombie obstacle, Projectile projectile, Board board) {
        if (obstacle == null || obstacle.isDead() || projectile == null || projectile.isLobbed()) {
            return false;
        }

        if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
            obstacle.die();
        } else {
            obstacle.takeDirectDamage(Math.max(
                    0,
                    DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression())
            ));
        }

        // true shot straight ro consume mikone hata agar piercing bashe
        return true;
    }
}
