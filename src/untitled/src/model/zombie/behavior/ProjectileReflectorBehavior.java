package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.DamageExpressionParser;
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

    public boolean reflect(Projectile projectile, Zombie zombie, Board board) {
        if (!this.reflecting || projectile == null || zombie == null || board == null
                || board.getCombatSystem() == null) {
            return false;
        }

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

        return true;
    }
}
