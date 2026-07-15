package model.minigame.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

public class ZombotanyPeashooterBehavior implements ZombieBehavior {

    private final Projectile projectileTemplate;
    private final long shootingIntervalTicks;

    private long ticksSinceLastShot;

    public ZombotanyPeashooterBehavior() {
        this(
                createDefaultProjectile(),
                15
        );
    }

    public ZombotanyPeashooterBehavior(
            Projectile projectileTemplate,
            long shootingIntervalTicks
    ) {
        if (projectileTemplate == null) {
            throw new IllegalArgumentException(
                    "Peashooter projectile cannot be null."
            );
        }

        this.projectileTemplate = projectileTemplate;
        this.shootingIntervalTicks =
                Math.max(1, shootingIntervalTicks);

        this.ticksSinceLastShot = 0;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (!canShoot(zombie, board)) {
            return;
        }

        ticksSinceLastShot++;

        if (ticksSinceLastShot < shootingIntervalTicks) {
            return;
        }

        if (shoot(zombie, board)) {
            ticksSinceLastShot = 0;
        }
    }

    @Override
    public void activate(
            Zombie zombie,
            Board board
    ) {
        if (shoot(zombie, board)) {
            ticksSinceLastShot = 0;
        }
    }

    private boolean shoot(
            Zombie zombie,
            Board board
    ) {
        if (!canShoot(zombie, board)
                || !hasPlantInFront(zombie, board)) {
            return false;
        }

        Position origin = zombie.getPosition();

        Projectile projectile =
                projectileTemplate.copyAt(
                        origin,
                        -1,
                        0
                );

        projectile.reflectTowardPlants(origin);

        board.addProjectile(projectile);
        return true;
    }

    private boolean canShoot(
            Zombie zombie,
            Board board
    ) {
        return zombie != null
                && !zombie.isDead()
                && zombie.getPosition() != null
                && board != null;
    }

    private boolean hasPlantInFront(
            Zombie zombie,
            Board board
    ) {
        for (Plant plant
                : board.getPlantsInLane(
                zombie.getPosition()
        )) {
            if (plant == null
                    || plant.isDead()
                    || plant.getPosition() == null) {
                continue;
            }

            if (plant.getPosition().getX()
                    < zombie.getPosition().getX()) {
                return true;
            }
        }

        return false;
    }

    private static Projectile createDefaultProjectile() {
        Projectile projectile = new Projectile(
                "20",
                new Position(0, 0),
                0.75,
                ProjectileType.NORMAL,
                null
        );

        projectile.setPeaBased(true);
        projectile.setMaxRange(9);
        projectile.setRemainingTicks(100);

        return projectile;
    }
}