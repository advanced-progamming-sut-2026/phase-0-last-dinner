package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ShootingDirection;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;

public class ShooterBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private ShootingDirection direction;
    private int forwardShotCount;
    private int backwardShotCount;
    private int laneCount;
    private long shootIntervalTicks;
    private long ticksSinceLastShot;
    private ShooterPattern shooterPattern;
    // har bulb cooldown joda dare ta nobati shlik beshe
    private final long[] bulbCooldowns = new long[3];

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

        if (this.projectileTemplate != null && this.shooterPattern == ShooterPattern.SHORT_RANGE) {
            this.projectileTemplate.setMaxRange(3);
        }
    }

    @Override
    public void onTick(Plant plant, Board board) {
        if (this.shooterPattern == ShooterPattern.STACKED_FORWARD
                && !this.isStackLeader(plant, board)) {
            return;
        }

        this.tickBulbCooldowns();
        this.ticksSinceLastShot++;

        if (this.ticksSinceLastShot >= Math.max(1, this.shootIntervalTicks)) {
            this.activate(plant, board);
            this.ticksSinceLastShot = 0;
        }
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || board == null || plant.getPosition() == null || this.projectileTemplate == null) {
            return;
        }

        if (this.shooterPattern == ShooterPattern.THREE_LANE) {
            this.shootThreeLanes(plant, board);
            return;
        }

        if (this.shooterPattern == ShooterPattern.FOUR_DIAGONAL) {
            this.shootVectors(plant, board, new int[][]{{1, -1}, {1, 1}, {-1, -1}, {-1, 1}});
            return;
        }

        if (this.shooterPattern == ShooterPattern.STAR_FIVE) {
            this.shootVectors(plant, board, new int[][]{{1, 0}, {1, -1}, {1, 1}, {-1, -1}, {-1, 1}});
            return;
        }

        if (this.shooterPattern == ShooterPattern.BOUNCING) {
            this.shootAvailableBulb(plant, board);
            return;
        }

        if (this.direction == ShootingDirection.FORWARD || this.direction == ShootingDirection.BOTH) {
            this.shootStraight(plant, board, 1, this.forwardShotCount);
        }

        if (this.direction == ShootingDirection.BACKWARD || this.direction == ShootingDirection.BOTH) {
            this.shootStraight(plant, board, -1, this.backwardShotCount);
        }
    }

    private void shootStraight(Plant plant, Board board, int horizontalDirection, int shotCount) {
        if (!this.hasTargetAlongPath(plant.getPosition(), board, horizontalDirection, 0)) {
            return;
        }

        for (int i = 0; i < Math.max(0, shotCount); i++) {
            Projectile projectile = this.createProjectileForPlant(plant, board, plant.getPosition(), horizontalDirection, 0);
            board.addProjectile(projectile);
        }
    }

    private void shootThreeLanes(Plant plant, Board board) {
        int halfLaneCount = Math.max(0, this.laneCount / 2);

        for (int deltaY = -halfLaneCount; deltaY <= halfLaneCount; deltaY++) {
            Position spawn = new Position(plant.getPosition().getX(), plant.getPosition().getY() + deltaY);

            if (board.getTile(spawn) != null && this.hasTargetAlongPath(spawn, board, 1, 0)) {
                board.addProjectile(this.createProjectileForPlant(plant, board, spawn, 1, 0));
            }
        }
    }

    private void shootVectors(Plant plant, Board board, int[][] directions) {
        for (int[] vector : directions) {
            if (this.hasTargetAlongPath(plant.getPosition(), board, vector[0], vector[1])) {
                board.addProjectile(this.createProjectileForPlant(
                        plant,
                        board,
                        plant.getPosition(),
                        vector[0],
                        vector[1]
                ));
            }
        }
    }

    private void shootAvailableBulb(Plant plant, Board board) {
        if (!this.hasTargetAlongPath(plant.getPosition(), board, 1, 0)) {
            return;
        }

        for (int bulbIndex = this.bulbCooldowns.length - 1; bulbIndex >= 0; bulbIndex--) {
            if (this.bulbCooldowns[bulbIndex] > 0) {
                continue;
            }

            Projectile projectile = this.projectileTemplate.copyAt(plant.getPosition(), 1, 0);
            projectile.setDamageExpression(DamageExpressionParser.selectExpressionAt(
                    this.projectileTemplate.getDamageExpression(),
                    bulbIndex
            ));
            board.addProjectile(projectile);
            this.bulbCooldowns[bulbIndex] = this.bulbRechargeTicks(bulbIndex);
            return;
        }
    }

    private Projectile createProjectileForPlant(
            Plant plant,
            Board board,
            Position spawn,
            int horizontalDirection,
            int verticalDirection
    ) {
        Projectile projectile = this.projectileTemplate.copyAt(spawn, horizontalDirection, verticalDirection);

        if (this.shooterPattern == ShooterPattern.STACKED_FORWARD) {
            int stackIndex = Math.max(0, this.samePlantCount(plant, board) - 1);
            projectile.setDamageExpression(DamageExpressionParser.selectExpressionAt(
                    this.projectileTemplate.getDamageExpression(),
                    stackIndex
            ));
        } else if (verticalDirection == 0 && this.forwardShotCount > 1
                && DamageExpressionParser.parseHitCount(this.projectileTemplate.getDamageExpression())
                == this.forwardShotCount) {
            projectile.setDamageExpression(String.valueOf(
                    DamageExpressionParser.parseDamagePerHit(this.projectileTemplate.getDamageExpression())
            ));
        }

        return projectile;
    }

    private int samePlantCount(Plant plant, Board board) {
        int count = 0;

        for (Plant stackedPlant : board.getPlantsAt(plant.getPosition())) {
            if (stackedPlant != null && plant.getName() != null && plant.getName().equals(stackedPlant.getName())) {
                count++;
            }
        }

        return Math.max(1, count);
    }

    private boolean isStackLeader(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null) {
            return false;
        }

        for (Plant stackedPlant : board.getPlantsAt(plant.getPosition())) {
            if (stackedPlant != null && plant.getName() != null
                    && plant.getName().equals(stackedPlant.getName())) {
                return stackedPlant == plant;
            }
        }

        return false;
    }

    private boolean hasTargetAlongPath(Position origin, Board board, int horizontalDirection, int verticalDirection) {
        if (origin == null || board == null) {
            return false;
        }

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()
                    || zombie.getPosition() == null) {
                continue;
            }

            double deltaX = zombie.getPosition().getX() - origin.getX();
            int deltaY = zombie.getPosition().getY() - origin.getY();

            if (verticalDirection == 0 && deltaY == 0 && deltaX * horizontalDirection >= 0) {
                return true;
            }

            if (verticalDirection != 0 && deltaX * horizontalDirection >= 0
                    && deltaY * verticalDirection >= 0
                    && Math.abs(Math.abs(deltaX) - Math.abs(deltaY)) < 0.5) {
                return true;
            }
        }

        return false;
    }

    private void tickBulbCooldowns() {
        for (int i = 0; i < this.bulbCooldowns.length; i++) {
            if (this.bulbCooldowns[i] > 0) {
                this.bulbCooldowns[i]--;
            }
        }
    }

    private long bulbRechargeTicks(int bulbIndex) {
        if (bulbIndex == 2) {
            return 100;
        }

        return bulbIndex == 1 ? 50 : 20;
    }

    @Override
    public PlantBehavior copy() {
        Projectile projectileCopy = this.projectileTemplate == null
                ? null
                : this.projectileTemplate.copyAt(null);
        return new ShooterBehavior(
                projectileCopy,
                this.direction,
                this.forwardShotCount,
                this.backwardShotCount,
                this.laneCount,
                this.shootIntervalTicks,
                this.shooterPattern
        );
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        if (this.projectileTemplate != null) {
            this.projectileTemplate.addDamageBonus(effect.getDamageBonus());
            this.projectileTemplate.addPierceBonus(effect.getPierceBonus());
            this.projectileTemplate.addBounceBonus(effect.getBounceBonus());
            this.projectileTemplate.addRangeBonus(effect.getRangeBonus());
            this.projectileTemplate.addConditionDuration(effect.getDurationBonusTicks());
            this.projectileTemplate.addPoisonDamagePerTick(effect.getPoisonDamageBonusPerTick());
            this.projectileTemplate.addPlantFoodChanceBonus(effect.getPlantFoodChanceBonusPercent());
        }

        this.shootIntervalTicks = effect.upgradeInterval(this.shootIntervalTicks);
    }
}
