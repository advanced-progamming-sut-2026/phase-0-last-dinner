package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.plant.ShootingDirection;

public class ShooterBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private ShootingDirection direction;
    private int forwardShotCount;
    private int backwardShotCount;
    private int laneCount;
    private long shootIntervalTicks;
    private long ticksSinceLastShot;
    private ShooterPattern shooterPattern;

    public ShooterBehavior(
            Projectile projectileTemplate,
            ShootingDirection direction,
            int forwardShotCount,
            int backwardShotCount,
            int laneCount,
            long shootIntervalTicks,
            ShooterPattern shooterPattern
    ) {
        this.projectileTemplate = projectileTemplate;
        this.direction = direction;
        this.forwardShotCount = forwardShotCount;
        this.backwardShotCount = backwardShotCount;
        this.laneCount = laneCount;
        this.shootIntervalTicks = shootIntervalTicks;
        this.shooterPattern = shooterPattern;
    }

    @Override
    public void onTick(Plant plant, Board board) {
        this.ticksSinceLastShot++;

        if (this.ticksSinceLastShot >= this.shootIntervalTicks) {
            this.activate(plant, board);
            this.ticksSinceLastShot = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (this.shooterPattern == ShooterPattern.THREE_LANE
                || this.shooterPattern == ShooterPattern.FOUR_DIAGONAL
                || this.shooterPattern == ShooterPattern.STAR_FIVE) {
            this.shootPattern(plant, board);
            return;
        }

        if (this.direction == ShootingDirection.FORWARD || this.direction == ShootingDirection.BOTH) {
            this.shootForward(plant, board);
        }

        if (this.direction == ShootingDirection.BACKWARD || this.direction == ShootingDirection.BOTH) {
            this.shootBackward(plant, board);
        }
    }

    private void shootForward(Plant plant, Board board) {
        if (board == null || this.projectileTemplate == null || plant == null) {
            return;
        }

        for (int i = 0; i < this.forwardShotCount; i++) {
            board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
        }
    }

    private void shootBackward(Plant plant, Board board) {
        if (board == null || this.projectileTemplate == null || plant == null) {
            return;
        }

        for (int i = 0; i < this.backwardShotCount; i++) {
            board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
        }
    }

    private void shootPattern(Plant plant, Board board) {
        if (board == null || this.projectileTemplate == null || plant == null) {
            return;
        }

        int shotCount = Math.max(1, this.laneCount);

        if (this.shooterPattern == ShooterPattern.FOUR_DIAGONAL) {
            shotCount = 4;
        } else if (this.shooterPattern == ShooterPattern.STAR_FIVE) {
            shotCount = 5;
        }

        for (int i = 0; i < shotCount; i++) {
            board.addProjectile(this.projectileTemplate.copyAt(plant.getPosition()));
        }
    }
}
