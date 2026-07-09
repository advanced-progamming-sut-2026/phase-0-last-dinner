package model.plant;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
    private List<Zombie> hitZombies;

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
    }

    public Projectile copyAt(Position position) {
        return new Projectile(
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
    }

    public Projectile copyAtTarget(Position position, Zombie target) {
        return new Projectile(
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

        int deltaX = Math.abs(targetPosition.getX() - this.position.getX());
        int deltaY = Math.abs(targetPosition.getY() - this.position.getY());
        return deltaX + deltaY <= this.maxRange;
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
    }

    public void hit(Zombie zombie) {
        this.target = zombie;
    }
}
