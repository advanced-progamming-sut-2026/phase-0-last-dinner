package model.zombie;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.List;

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
    private ZombieBehavior behavior;
    @Setter
    private Board board;

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
        if (this.behavior != null) {
            this.behavior.onTick(this, this.board);
        }
    }

    public void move() {
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

    public boolean isDead() {
        return this.dead || this.health <= 0;
    }
}
