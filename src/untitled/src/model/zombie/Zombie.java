package model.zombie;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Zombie implements Tickable {
    private ZombieDefinition definition;
    @Setter
    private Position position;
    private int health;
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
    private double movementProgress;
    private int poisonDamagePerTick;

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
        this.position = position;
        this.health = health;
        this.currentSpeed = currentSpeed;
        this.armors = armors;
        this.conditions = conditions;
        this.behavior = behavior;
    }

    @Override
    public void onTick() {
        this.tickConditions();

        if (this.behavior != null) {
            this.behavior.onTick(this, this.board);
        }
    }

    public void move() {
        if (this.dead || this.position == null) {
            return;
        }

        if (this.hasCondition(ZombieCondition.FROZEN) || this.hasCondition(ZombieCondition.STUNNED)
                || this.hasCondition(ZombieCondition.TRANSFORMED)) {
            return;
        }

        double speed = Math.max(0, this.currentSpeed);

        if (this.hasCondition(ZombieCondition.CHILLED)) {
            speed = speed / 2;
        }

        this.movementProgress += speed;

        if (this.movementProgress < 1) {
            return;
        }

        int steps = (int) this.movementProgress;
        this.movementProgress -= steps;

        for (int i = 0; i < steps && !this.dead; i++) {
            int direction = this.hasCondition(ZombieCondition.HYPNOTIZED) ? 1 : -1;
            Position destination = new Position(this.position.getX() + direction, this.position.getY());

            if (this.board != null && this.board.moveZombie(this, destination)) {
                continue;
            }

            if (this.board == null) {
                this.position = destination;
            } else if (destination.getX() < 0) {
                this.die();
            }
        }
    }

    public void attack(Plant plant) {
        if (this.behavior != null) {
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

        ZombieArmor activeArmor = this.getActiveArmor();

        if (activeArmor != null && !activeArmor.isDestroyed()) {
            activeArmor.takeDamage(amount);
            return;
        }

        this.health -= amount;

        if (this.health <= 0) {
            this.health = 0;
            this.die();
        }
    }

    public void addHealth(int amount) {
        if (amount > 0 && !this.dead) {
            this.health += amount;
        }
    }

    public void addPoisonDamagePerTick(int amount) {
        if (amount > 0 && !this.dead) {
            this.poisonDamagePerTick += amount;
        }
    }

    public void die() {
        this.dead=true;
    }

    public void addCondition(ZombieCondition condition) {
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
        this.addCondition(condition);

        if (condition == null || durationTicks <= 0 || this.dead) {
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
    }

    public void dropActiveArmor() {
        ZombieArmor activeArmor = this.getActiveArmor();

        if (activeArmor != null) {
            activeArmor.drop();
        }
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
}
