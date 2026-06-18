package model;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.plant.PlantUpgradeData;
import model.plant.behavior.PlantBehavior;
import model.plant.behavior.PlantFoodBehavior;

import java.util.Set;

@Getter
public class Plant implements Tickable {
    private String name;
    private int health;
    private int maximumHealth;
    private int level;
    private int sunCost;
    private long cooldownTicks;
    private double actionIntervalSeconds;
    @Setter
    private Position position;
    private Set<PlantCategory> categories;
    private Set<PlantTag> tags;
    private PlantBehavior behavior;
    private PlantFoodBehavior plantFoodBehavior;
    private PlantUpgradeData upgradeData;
    @Setter
    private Board board;

    public Plant(
            String name,
            int maximumHealth,
            int level,
            int sunCost,
            long cooldownTicks,
            double actionIntervalSeconds,
            Set<PlantCategory> categories,
            Set<PlantTag> tags,
            PlantBehavior behavior,
            PlantFoodBehavior plantFoodBehavior,
            PlantUpgradeData upgradeData
    ) {
        this.name = name;
        this.maximumHealth = maximumHealth;
        this.health = maximumHealth;
        this.level = level;
        this.sunCost = sunCost;
        this.cooldownTicks = cooldownTicks;
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.categories = categories;
        this.tags = tags;
        this.behavior = behavior;
        this.plantFoodBehavior = plantFoodBehavior;
        this.upgradeData = upgradeData;
    }

    @Override
    public void onTick() {
        if (this.behavior != null) {
            this.behavior.onTick(this, this.board);
        }
    }

    public void useAbility() {
        if (this.behavior != null) {
            this.behavior.activate(this, this.board);
        }
    }

    public void receivePlantFood() {
        if (this.plantFoodBehavior != null) {
            this.plantFoodBehavior.activate(this, this.board);
        }
    }

    public void upgrade() {
        if (this.upgradeData != null && this.upgradeData.canUpgrade()) {
            this.upgradeData.upgrade();
            this.level++;
        }
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }

        this.health -= amount;

        if (this.health < 0) {
            this.health = 0;
        }
    }

    public void heal(int amount) {
        if (amount <= 0 || this.isDead()) {
            return;
        }

        this.health += amount;

        if (this.health > this.maximumHealth) {
            this.health = this.maximumHealth;
        }
    }

    public void healToFull() {
        if (this.isDead()) {
            return;
        }

        this.health = this.maximumHealth;
    }

    public void addBonusHealth(int amount) {
        if (amount <= 0) {
            return;
        }

        this.maximumHealth += amount;
        this.health += amount;
    }

    public Plant copyForPlantFood(Position position) {
        Plant copy = new Plant(
                this.name,
                this.maximumHealth,
                this.level,
                this.sunCost,
                this.cooldownTicks,
                this.actionIntervalSeconds,
                this.categories,
                this.tags,
                this.behavior,
                this.plantFoodBehavior,
                this.upgradeData
        );

        copy.health = this.health;
        copy.setPosition(position);
        copy.setBoard(this.board);
        return copy;
    }

    public boolean isDead() {
        return this.health <= 0;
    }

}
