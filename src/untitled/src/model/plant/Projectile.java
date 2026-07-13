package model.plant;

import lombok.Getter;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.ArrayList;
import java.util.List;

// state harekat va asar yek projectile dar board ro negah midare
@Getter
public class Projectile implements Tickable {
    private String damageExpression;
    private Position position;
    private double speed;
    private ProjectileType type;
    private Zombie target;
    private int pierceCount;
    private int bounceCount;
    private int plantFoodChancePercent;
    private long conditionDurationTicks;
    private int poisonDamagePerTick;
    private int stunChancePercent;
    private int splashRadius;
    private int maxRange;
    // target haye sabt shode jeloye hit dobare dar yek masir ro migiran
    private List<Zombie> hitZombies;
    private int horizontalDirection;
    private int verticalDirection;
    // mokhtasat double harekat narm ro negah midaran va position tile gerde shode ast
    private double exactX;
    private double exactY;
    private double originX;
    private double originY;
    private double travelledDistance;
    private long remainingTicks;
    private boolean expired;
    private boolean lobbed;
    private boolean peaBased;
    private boolean hostileToPlants;

    public Projectile(
            String damageExpression,
            Position position,
            double speed,
            ProjectileType type,
            Zombie target
    ) {
        this(damageExpression, position, speed, type, target, 0, 0, 0, 0, 0, 0, 0, 0, new ArrayList<>());
    }

    public Projectile(
            String damageExpression,
            Position position,
            double speed,
            ProjectileType type,
            Zombie target,
            int pierceCount,
            int bounceCount,
            int plantFoodChancePercent,
            long conditionDurationTicks,
            int poisonDamagePerTick,
            int stunChancePercent,
            int splashRadius,
            int maxRange,
            List<Zombie> hitZombies
    ) {
        this.damageExpression = damageExpression;
        this.position = position;
        this.speed = speed;
        this.type = type;
        this.target = target;
        this.pierceCount = Math.max(0, pierceCount);
        this.bounceCount = Math.max(0, bounceCount);
        this.plantFoodChancePercent = Math.max(0, plantFoodChancePercent);
        this.conditionDurationTicks = Math.max(0, conditionDurationTicks);
        this.poisonDamagePerTick = Math.max(0, poisonDamagePerTick);
        this.stunChancePercent = Math.max(0, stunChancePercent);
        this.splashRadius = Math.max(0, splashRadius);
        this.maxRange = Math.max(0, maxRange);
        this.hitZombies = hitZombies == null ? new ArrayList<>() : hitZombies;
        this.horizontalDirection = 1;
        this.verticalDirection = 0;
        this.remainingTicks = 100;
        this.setPosition(position);
    }

    public Projectile copyAt(Position position) {
        return this.copyAt(position, this.horizontalDirection, this.verticalDirection);
    }

    public Projectile copyAt(Position position, int horizontalDirection, int verticalDirection) {
        Projectile copy = new Projectile(
                this.damageExpression,
                position,
                this.speed,
                this.type,
                this.target,
                this.pierceCount,
                this.bounceCount,
                this.plantFoodChancePercent,
                this.conditionDurationTicks,
                this.poisonDamagePerTick,
                this.stunChancePercent,
                this.splashRadius,
                this.maxRange,
                new ArrayList<>()
        );

        copy.horizontalDirection = normalizeDirection(horizontalDirection, 1);
        copy.verticalDirection = normalizeDirection(verticalDirection, 0);
        copy.lobbed = this.lobbed;
        copy.peaBased = this.peaBased;
        copy.hostileToPlants = this.hostileToPlants;
        copy.remainingTicks = this.remainingTicks;
        return copy;
    }

    public Projectile copyAtTarget(Position position, Zombie target) {
        Projectile copy = new Projectile(
                this.damageExpression,
                position,
                this.speed,
                this.type,
                target,
                this.pierceCount,
                this.bounceCount,
                this.plantFoodChancePercent,
                this.conditionDurationTicks,
                this.poisonDamagePerTick,
                this.stunChancePercent,
                this.splashRadius,
                this.maxRange,
                new ArrayList<>()
        );

        copy.horizontalDirection = this.horizontalDirection;
        copy.verticalDirection = this.verticalDirection;
        copy.lobbed = this.lobbed;
        copy.peaBased = this.peaBased;
        copy.hostileToPlants = this.hostileToPlants;
        copy.remainingTicks = this.remainingTicks;
        return copy;
    }

    public void addDamageBonus(int bonusDamage) {
        this.damageExpression = DamageExpressionParser.addFlatDamage(this.damageExpression, bonusDamage);
    }

    public void addPierceBonus(int amount) {
        this.pierceCount += Math.max(0, amount);
    }

    public void addBounceBonus(int amount) {
        this.bounceCount += Math.max(0, amount);
    }

    public void addPlantFoodChanceBonus(int amount) {
        this.plantFoodChancePercent += Math.max(0, amount);
    }

    public void addConditionDuration(long ticks) {
        this.conditionDurationTicks += Math.max(0, ticks);
    }

    public void addPoisonDamagePerTick(int amount) {
        this.poisonDamagePerTick += Math.max(0, amount);
    }

    public void addStunChanceBonus(int amount) {
        this.stunChancePercent += Math.max(0, amount);
    }

    public void addSplashRadius(int amount) {
        this.splashRadius += Math.max(0, amount);
    }

    public void addRangeBonus(int amount) {
        if (this.maxRange > 0) {
            this.maxRange += Math.max(0, amount);
        }
    }

    public boolean isInRangeOf(Position targetPosition) {
        if (this.maxRange <= 0 || this.position == null || targetPosition == null) {
            return true;
        }

        double deltaX = targetPosition.getX() - this.originX;
        double deltaY = targetPosition.getY() - this.originY;
        return Math.abs(deltaX) + Math.abs(deltaY) <= this.maxRange;
    }

    public boolean canHit(Zombie zombie) {
        return zombie != null && !this.hitZombies.contains(zombie);
    }

    public void markHit(Zombie zombie) {
        if (zombie != null && !this.hitZombies.contains(zombie)) {
            this.hitZombies.add(zombie);
        }
    }

    public boolean shouldContinueAfterHit() {
        int extraHits = this.pierceCount + this.bounceCount;
        return this.hitZombies.size() <= extraHits;
    }

    public ZombieCondition getConditionFromType() {
        if (this.type == ProjectileType.ICE) {
            return ZombieCondition.CHILLED;
        }

        if (this.type == ProjectileType.POISON) {
            return ZombieCondition.POISONED;
        }

        return null;
    }

    @Override
    public void onTick() {
        this.move();
    }

    public void move() {
        if (this.expired || this.position == null) {
            return;
        }

        if (this.remainingTicks-- <= 0) {
            this.expired = true;
            return;
        }

        double deltaX = this.horizontalDirection;
        double deltaY = this.verticalDirection;

        if (this.target != null && !this.target.isDead() && this.target.getPosition() != null) {
            deltaX = this.target.getExactX() - this.exactX;
            deltaY = this.target.getPosition().getY() - this.exactY;
        }

        double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (length == 0) {
            return;
        }

        double distance = Math.max(0.1, this.speed);
        this.exactX += deltaX / length * distance;
        this.exactY += deltaY / length * distance;
        this.travelledDistance += distance;
        this.position = new Position((int) Math.round(this.exactX), (int) Math.round(this.exactY));

        if ((this.maxRange > 0 && this.travelledDistance > this.maxRange)
                || this.exactX < -1 || this.exactX > 9 || this.exactY < -1 || this.exactY > 5) {
            this.expired = true;
        }
    }

    public void hit(Zombie zombie) {
        this.target = zombie;
    }

    public void expire() {
        this.expired = true;
    }

    public void setPosition(Position position) {
        this.position = position;

        if (position != null) {
            this.exactX = position.getX();
            this.exactY = position.getY();
            this.originX = position.getX();
            this.originY = position.getY();
        }
    }

    public void setTarget(Zombie target) {
        this.target = target;
    }

    public void setType(ProjectileType type) {
        this.type = type == null ? ProjectileType.NORMAL : type;
    }

    public void setDamageExpression(String damageExpression) {
        this.damageExpression = damageExpression == null || damageExpression.trim().isEmpty()
                ? "0"
                : damageExpression.trim();
    }

    public void setPierceCount(int pierceCount) {
        this.pierceCount = Math.max(0, pierceCount);
    }

    public void setBounceCount(int bounceCount) {
        this.bounceCount = Math.max(0, bounceCount);
    }

    public void setConditionDurationTicks(long conditionDurationTicks) {
        this.conditionDurationTicks = Math.max(0, conditionDurationTicks);
    }

    public void setPoisonDamagePerTick(int poisonDamagePerTick) {
        this.poisonDamagePerTick = Math.max(0, poisonDamagePerTick);
    }

    public void setStunChancePercent(int stunChancePercent) {
        this.stunChancePercent = Math.max(0, Math.min(100, stunChancePercent));
    }

    public void setSplashRadius(int splashRadius) {
        this.splashRadius = Math.max(0, splashRadius);
    }

    public void setMaxRange(int maxRange) {
        this.maxRange = Math.max(0, maxRange);
    }

    public void setLobbed(boolean lobbed) {
        this.lobbed = lobbed;
    }

    public void setPeaBased(boolean peaBased) {
        this.peaBased = peaBased;
    }

    public void setRemainingTicks(long remainingTicks) {
        this.remainingTicks = Math.max(1, remainingTicks);
    }

    // projectile ro az noghte barkhord be samte giah ha hostile mikone
    public void reflectTowardPlants(Position reflectionPosition) {
        this.setPosition(reflectionPosition);
        this.target = null;
        this.horizontalDirection = -1;
        this.verticalDirection = 0;
        this.travelledDistance = 0;
        this.hitZombies.clear();
        this.hostileToPlants = true;
    }

    private static int normalizeDirection(int value, int defaultValue) {
        if (value < 0) {
            return -1;
        }

        if (value > 0) {
            return 1;
        }

        return defaultValue;
    }
}
