package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
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
                    && projectile.getPosition().getX() <= zombie.getPosition().getX()) {
                projectileApproaching = true;
                break;
            }
        }

        this.reflecting = projectileApproaching;
        zombie.setCurrentSpeed(this.reflecting ? this.normalSpeed * 1.1 : this.normalSpeed);
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

    public boolean reflect(Projectile projectile, Zombie zombie, Board board) {
        if (!this.isReflectable(projectile) || zombie == null || board == null
                || board.getCombatSystem() == null) {
            return false;
        }

        this.reflecting = true;

        Plant target = board.getNearestPlant(zombie.getPosition());

        if (target == null) {
            return true;
        }

        if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
            board.getCombatSystem().destroyPlant(target);
            return true;
        }

        int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());

        if (damage > 0) {
            board.getCombatSystem().applyDamageToPlant(target, damage);
        }

        if (projectile.getType() == ProjectileType.ICE && !target.isDead()) {
            target.disable();
        }

        return true;
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        return this.reflect(projectile, zombie, board);
    }

    private boolean isReflectable(Projectile projectile) {
        if (projectile == null || projectile.getType() == null) {
            return false;
        }

        return !projectile.isLobbed()
                && projectile.getType() != ProjectileType.HOMING;
    }
}
