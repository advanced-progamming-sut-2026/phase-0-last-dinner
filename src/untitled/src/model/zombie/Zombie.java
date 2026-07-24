package model.zombie;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tickable;
import model.mechanism.Tile;
import model.mechanism.Wave;
import model.plant.Projectile;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import model.zombie.behavior.CompositeZombieBehavior;

@Getter
public class Zombie implements Tickable {
    private static final int TICKS_PER_SECOND = 10;

    private ZombieDefinition definition;
    @Setter
    private Position position;
    private int health;
    private int maximumHealth;
    @Setter
    private double currentSpeed;
    private boolean glowing;
    private boolean attacking;
    private boolean dead;
    private List<ZombieArmor> armors;
    private List<ZombieCondition> conditions;
    private Map<ZombieCondition, Long> conditionRemainingTicks;
    private ZombieBehavior behavior;
    @Setter
    private Board board;
    @Setter
    private Wave wave;
    // exact x harekate kasri ro negah midare va position tile ro round mikone
    private double exactX;
    private int poisonDamagePerTick;
    @Setter
    private Plant poisonSourcePlant;
    // behavior marg va remove reward do marhale joda hastan
    private boolean deathProcessed;
    private boolean deathBehaviorCalled;
    private boolean terrainFrozen;

    public Zombie(
            ZombieDefinition definition,
            Position position,
            int health,
            double currentSpeed,
            List<ZombieArmor> armors,
            List<ZombieCondition> conditions,
            ZombieBehavior behavior
    ) {
        this.definition = definition;
        this.setPosition(position);
        this.health = health;
        this.maximumHealth = Math.max(0, health);
        this.currentSpeed = currentSpeed;
        this.armors = armors == null ? new ArrayList<ZombieArmor>() : armors;
        this.conditions = conditions == null ? new ArrayList<ZombieCondition>() : conditions;
        this.behavior = behavior;
    }

    @Override
    public void onTick() {
        if (this.isDead()) {
            return;
        }

        this.tickConditions();

        if (this.terrainFrozen || this.hasCondition(ZombieCondition.FROZEN)
                || this.hasCondition(ZombieCondition.STUNNED)
                || this.hasCondition(ZombieCondition.TRANSFORMED)) {
            return;
        }

        if (this.behavior != null) {
            this.behavior.onTick(this, this.board);
        }
    }

    public void move() {
        if (this.isMovementBlocked()) {
            return;
        }

        double speed = Math.max(0, this.currentSpeed);

        if (this.hasCondition(ZombieCondition.CHILLED)) {
            speed = speed / 2;
        }

        int direction = this.behavior == null
                ? (this.hasCondition(ZombieCondition.HYPNOTIZED) ? 1 : -1)
                : this.behavior.getMovementDirection(this);
        this.exactX += direction * speed / TICKS_PER_SECOND;

        if (this.exactX < 0) {
            if (this.board != null) {
                this.board.handleZombieAtHouse(this);
            }
            return;
        }

        if (this.exactX > 8) {
            if (direction > 0) {
                this.die();
            }
            return;
        }

        Position destination = new Position((int) Math.round(this.exactX), this.position.getY());

        if (destination.getX() != this.position.getX()) {
            destination = this.applySlipperyLaneShift(destination);

            if (this.board != null) {
                this.board.moveZombie(this, destination);
            } else {
                this.position = destination;
            }
        }
    }

    private boolean isMovementBlocked() {
        return this.dead
                || this.position == null
                || this.attacking
                || this.terrainFrozen
                || this.hasCondition(ZombieCondition.FROZEN)
                || this.hasCondition(ZombieCondition.STUNNED)
                || this.hasCondition(ZombieCondition.TRANSFORMED)
                || (this.behavior != null && !this.behavior.canMove(this, this.board));
    }

    public void attack(Plant plant) {
        if (this.behavior != null && this.behavior.canAttackPlant(this, plant, this.board)) {
            this.behavior.attack(this, plant, this.board);
        }
    }

    public void activateAbility() {
        if (this.behavior != null) {
            this.behavior.activate(this, this.board);
        }
    }

    public void takeDamage(int amount) {
        if (amount <= 0 || this.dead) {
            return;
        }

        int remainingDamage = amount;
        ZombieArmor activeArmor;

        while (remainingDamage > 0 && (activeArmor = this.getActiveArmor()) != null) {
            remainingDamage = activeArmor.absorbDamage(remainingDamage);
        }

        if (remainingDamage > 0) {
            this.applyDamageToHealth(remainingDamage);
        }
    }

    public void takeDirectDamage(int amount) {
        if (amount > 0 && !this.dead) {
            this.applyDamageToHealth(amount);
        }
    }

    public void addHealth(int amount) {
        if (amount > 0 && !this.dead) {
            this.health += amount;
        }
    }

    public void multiplyHealth(double multiplier) {
        if (multiplier <= 0 || this.dead) {
            return;
        }

        this.health = Math.max(1, (int) Math.round(this.health * multiplier));
        this.maximumHealth = Math.max(1, (int) Math.round(this.maximumHealth * multiplier));
    }

    public void applyDifficulty(double multiplier) {
        if (multiplier <= 0) {
            return;
        }

        this.multiplyHealth(multiplier);
        if (this.behavior != null) {
            this.behavior.multiplyDamage(multiplier);
        }
    }

    public void addPoisonDamagePerTick(int amount) {
        if (amount > 0 && !this.dead) {
            this.poisonDamagePerTick = Math.max(this.poisonDamagePerTick, amount);
        }
    }

    public void die() {
        if (this.dead) {
            return;
        }

        this.dead = true;

        if (!this.deathBehaviorCalled && this.behavior != null) {
            this.deathBehaviorCalled = true;
            this.behavior.onDeath(this, this.board);
        }
    }

    public void addCondition(ZombieCondition condition) {
        if (condition == null || this.dead) {
            return;
        }

        if (!this.acceptsCondition(condition, null)) {
            return;
        }

        this.addConditionInternal(condition);
    }

    private void addConditionInternal(ZombieCondition condition) {
        if (condition == null || this.dead) {
            return;
        }

        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }

        if (!this.conditions.contains(condition)) {
            this.conditions.add(condition);
        }
    }

    public void addCondition(ZombieCondition condition, long durationTicks) {
        this.addCondition(condition, durationTicks, null);
    }

    public void addCondition(ZombieCondition condition, long durationTicks, Projectile projectile) {
        if (!this.acceptsCondition(condition, projectile)) {
            return;
        }

        this.addConditionInternal(condition);

        if (condition == null || durationTicks <= 0 || this.dead || !this.hasCondition(condition)) {
            return;
        }

        if (this.conditionRemainingTicks == null) {
            this.conditionRemainingTicks = new EnumMap<>(ZombieCondition.class);
        }

        long currentTicks = this.conditionRemainingTicks.containsKey(condition)
                ? this.conditionRemainingTicks.get(condition)
                : 0;
        this.conditionRemainingTicks.put(condition, Math.max(currentTicks, durationTicks));
    }

    public boolean hasCondition(ZombieCondition condition) {
        return condition != null && this.conditions != null && this.conditions.contains(condition);
    }

    public void removeCondition(ZombieCondition condition) {
        if (condition == null) {
            return;
        }

        if (this.conditions != null) {
            this.conditions.remove(condition);
        }

        if (this.conditionRemainingTicks != null) {
            this.conditionRemainingTicks.remove(condition);
        }

        if (condition == ZombieCondition.POISONED) {
            this.poisonDamagePerTick = 0;
            this.poisonSourcePlant = null;
        }
    }

    public boolean acceptsCondition(ZombieCondition condition, Projectile projectile) {
        if (condition == null) {
            return false;
        }

        if ((condition == ZombieCondition.CHILLED || condition == ZombieCondition.FROZEN)
                && this.definition != null
                && this.definition.getChapter() == ZombieChapter.FROSTBITE_CAVES) {
            return false;
        }

        if (this.definition != null && this.definition.getConditionResistances() != null) {
            for (ConditionResistance resistance : this.definition.getConditionResistances()) {
                if (resistance != null && resistance.getCondition() == condition && resistance.isImmune()) {
                    return false;
                }
            }
        }

        return this.behavior == null || this.behavior.acceptsCondition(this, condition, projectile);
    }

    public ZombieArmor getActiveArmor() {
        if (this.armors == null) {
            return null;
        }

        for (ZombieArmor armor : this.armors) {
            if (armor != null && !armor.isDestroyed() && !armor.isDropped()) {
                return armor;
            }
        }

        return null;
    }

    public void addArmor(ZombieArmor armor) {
        if (armor != null) {
            this.armors.add(armor);
        }
    }

    public <T extends ZombieBehavior> T findBehavior(Class<T> behaviorType) {
        if (behaviorType == null || this.behavior == null) {
            return null;
        }

        if (behaviorType.isInstance(this.behavior)) {
            return behaviorType.cast(this.behavior);
        }

        if (this.behavior instanceof model.zombie.behavior.CompositeZombieBehavior) {
            return ((model.zombie.behavior.CompositeZombieBehavior) this.behavior).findBehavior(behaviorType);
        }

        return null;
    }

    public boolean isDead() {
        return this.dead || this.health <= 0;
    }

    public boolean isHypnotized() {
        return this.hasCondition(ZombieCondition.HYPNOTIZED);
    }

    public boolean markDeathProcessed() {
        if (this.deathProcessed) {
            return false;
        }

        this.deathProcessed = true;
        return true;
    }

    public void setPosition(Position position) {
        this.position = position;

        if (position != null) {
            this.exactX = position.getX();
        }
    }

    public void setTilePosition(Position position) {
        this.position = position;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = Math.max(0, currentSpeed);
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setTerrainFrozen(boolean terrainFrozen) {
        this.terrainFrozen = terrainFrozen;
    }

    private void applyDamageToHealth(int amount) {
        this.health -= amount;

        if (this.health <= 0) {
            this.health = 0;
            this.die();
        }
    }

    private Position applySlipperyLaneShift(Position destination) {
        if (this.board == null || destination == null) {
            return destination;
        }

        if (this.hasCondition(ZombieCondition.FLYING)) {
            return destination;
        }

        Tile tile = this.board.getTile(destination);

        if (tile == null) {
            return destination;
        }

        int destinationRow = destination.getY();

        if (tile.getTerrainType() == TerrainType.SLIPPERY_UP) {
            destinationRow--;
        } else if (tile.getTerrainType() == TerrainType.SLIPPERY_DOWN) {
            destinationRow++;
        }

        destinationRow = Math.max(0, Math.min(4, destinationRow));
        return new Position(destination.getX(), destinationRow);
    }

    private void tickConditions() {
        if (this.conditionRemainingTicks == null || this.conditionRemainingTicks.isEmpty()) {
            return;
        }

        List<ZombieCondition> expiredConditions = new ArrayList<>();

        for (Map.Entry<ZombieCondition, Long> entry : this.conditionRemainingTicks.entrySet()) {
            long remainingTicks = entry.getValue() - 1;

            if (remainingTicks <= 0) {
                expiredConditions.add(entry.getKey());
            } else {
                entry.setValue(remainingTicks);
            }
        }

        for (ZombieCondition condition : expiredConditions) {
            this.removeCondition(condition);
        }
    }

    public void addBehavior(
            ZombieBehavior additionalBehavior
    ) {
        if (additionalBehavior == null) {
            return;
        }

        if (behavior == null) {
            behavior = additionalBehavior;
            return;
        }

        if (behavior instanceof CompositeZombieBehavior) {
            CompositeZombieBehavior compositeBehavior =
                    (CompositeZombieBehavior) behavior;

            compositeBehavior.addBehavior(
                    additionalBehavior
            );

            return;
        }

        CompositeZombieBehavior compositeBehavior =
                new CompositeZombieBehavior();

        compositeBehavior.addBehavior(behavior);
        compositeBehavior.addBehavior(
                additionalBehavior
        );

        behavior = compositeBehavior;
    }
}
