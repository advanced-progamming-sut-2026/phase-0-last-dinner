package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;

public class BarrelObstacleBehavior implements ZombieBehavior {
    private static final int RELEASED_IMP_COUNT = 2;

    private final ZombieDefinition impDefinition;
    private final ZombieFactory zombieFactory;
    private boolean impsReleased;

    public BarrelObstacleBehavior(
            ZombieDefinition impDefinition,
            ZombieFactory zombieFactory
    ) {
        this.impDefinition = impDefinition;
        this.zombieFactory = zombieFactory;
    }

    @Override
    public boolean canMove(Zombie barrel, Board board) {
        return false;
    }

    @Override
    public boolean canAttackPlant(Zombie barrel, Plant plant, Board board) {
        return false;
    }

    @Override
    public boolean onProjectileHit(Zombie barrel, Projectile projectile, Board board) {
        if (barrel == null || barrel.isDead() || projectile == null || projectile.isLobbed()) {
            return false;
        }

        if (DamageExpressionParser.isInstantKill(projectile.getDamageExpression())) {
            barrel.die();
        } else {
            int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
            barrel.takeDirectDamage(Math.max(0, damage));
        }

        // barrel shot piercing ro ham motevaghef mikone
        return true;
    }

    @Override
    public boolean canBeHitBy(Zombie barrel, Projectile projectile) {
        return projectile != null && !projectile.isLobbed();
    }

    @Override
    public void onDeath(Zombie barrel, Board board) {
        if (this.impsReleased || barrel == null || barrel.getPosition() == null
                || board == null || this.impDefinition == null || this.zombieFactory == null) {
            return;
        }

        this.impsReleased = true;
        Position barrelPosition = barrel.getPosition();

        for (int i = 0; i < RELEASED_IMP_COUNT; i++) {
            Position spawnPosition = new Position(barrelPosition.getX(), barrelPosition.getY());
            Zombie imp = this.zombieFactory.create(this.impDefinition, spawnPosition);

            if (imp != null) {
                board.addZombie(imp, spawnPosition);

                if (barrel.getWave() != null) {
                    barrel.getWave().addZombie(imp);
                }
            }
        }
    }
}
