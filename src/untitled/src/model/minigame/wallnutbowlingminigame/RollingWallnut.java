package model.minigame.wallnutbowlingminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.mechanism.Tickable;

@Getter
public class RollingWallnut implements Tickable {
    private static final int MIN_X = 1;
    private static final int MAX_X = 9;

    private static final int MIN_Y = 1;
    private static final int MAX_Y = 5;

    private static final int EXPLOSION_RADIUS = 1;

    private final BowlingWallnutType type;

    private final int damage;

    private final int explosionDamage;

    private final int movementIntervalTicks;

    private final WallnutBowlingIntegration integration;

    private Position position;

    private double directionAngle;

    private int verticalDirection;

    private int collisionCount;

    private int ticksSinceLastMove;

    private boolean initialPositionChecked;

    private boolean moving;

    private boolean exploded;

    public RollingWallnut(
            BowlingWallnutType type,
            Position position,
            int damage,
            int explosionDamage,
            int movementIntervalTicks,
            WallnutBowlingIntegration integration
    ) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Wallnut type cannot be null."
            );
        }

        if (position == null) {
            throw new IllegalArgumentException(
                    "Wallnut position cannot be null."
            );
        }

        this.type = type;
        this.position = position;

        this.damage = Math.max(0, damage);

        this.explosionDamage = Math.max(
                0,
                explosionDamage
        );

        this.movementIntervalTicks = Math.max(
                1,
                movementIntervalTicks
        );

        if (integration == null) {
            this.integration =
                    new PlantZombieWallnutBowlingIntegration();
        } else {
            this.integration = integration;
        }

        this.directionAngle = 0;
        this.verticalDirection = 0;
        this.collisionCount = 0;
        this.ticksSinceLastMove = 0;

        this.moving = true;
        this.exploded = false;
    }

    @Override
    public void onTick() {
        if (!moving || position == null) {
            return;
        }

        ticksSinceLastMove++;

        if (ticksSinceLastMove
                < movementIntervalTicks) {
            return;
        }

        ticksSinceLastMove = 0;

        if (!initialPositionChecked) {
            initialPositionChecked = true;
            if (integration.isReady() && integration.hasZombieAt(position)) {
                collideWithZombie();
                return;
            }
        }

        moveOneStep();

        if (!moving || position == null) {
            return;
        }

        checkZombieCollision();
    }

    private void moveOneStep() {
        int nextX = position.getX() + 1;
        int nextY = position.getY()
                + verticalDirection;

        if (nextY < MIN_Y || nextY > MAX_Y) {
            bounceFromBoundary();

            nextY = position.getY()
                    + verticalDirection;
        }

        if (nextX > MAX_X) {
            stop();
            return;
        }

        position = new Position(
                nextX,
                nextY
        );
    }

    private void checkZombieCollision() {
        if (!integration.isReady()) {
            return;
        }

        if (!integration.hasZombieAt(position)) {
            return;
        }

        collideWithZombie();
    }

    public void collideWithZombie() {
        if (!moving || position == null) {
            return;
        }

        switch (type) {
            case BOWLING_WALLNUT:
                integration.damageFirstZombieAt(
                        position,
                        damage
                );

                collisionCount++;

                bounce();
                break;

            case EXPLODE_O_NUT:
                explode();
                break;

            case GIANT_WALLNUT:
                integration.crushZombiesAt(
                        position
                );

                collisionCount++;
                break;
        }
    }

    public void bounce() {
        if (!moving) {
            return;
        }

        if (verticalDirection == 0) {
            /*
             * The first zombie collision changes the
             * straight path by 45 degrees.
             *
             * The starting row determines whether the
             * wallnut initially moves upward or downward.
             */
            if (position.getY() <= 3) {
                verticalDirection = 1;
            } else {
                verticalDirection = -1;
            }
        } else {
            /*
             * Switching from +45 to -45, or the reverse,
             * produces the required 90-degree turn.
             */
            verticalDirection *= -1;
        }

        updateDirectionAngle();
    }

    private void bounceFromBoundary() {
        if (verticalDirection == 0) {
            return;
        }

        verticalDirection *= -1;

        updateDirectionAngle();
    }

    private void updateDirectionAngle() {
        if (verticalDirection > 0) {
            directionAngle = 45;
        } else if (verticalDirection < 0) {
            directionAngle = -45;
        } else {
            directionAngle = 0;
        }
    }

    public void explode() {
        if (!moving || exploded || position == null) {
            return;
        }

        exploded = true;
        moving = false;

        if (integration.isReady()) {
            integration.explodeAt(
                    position,
                    EXPLOSION_RADIUS,
                    explosionDamage
            );
        }
    }

    public void stop() {
        moving = false;
    }

    public boolean isOutsideBoard() {
        if (position == null) {
            return true;
        }

        return position.getX() < MIN_X
                || position.getX() > MAX_X
                || position.getY() < MIN_Y
                || position.getY() > MAX_Y;
    }
}
