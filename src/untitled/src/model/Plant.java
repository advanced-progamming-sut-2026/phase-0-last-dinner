package model;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.plant.PlantUpgradeData;
import model.plant.PlantUpgradeEffect;
import model.plant.PlantUpgradeSpecialEffect;
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
    private boolean disabled;
    private boolean transformed;
    private long lifespanTicks;
    private boolean upgradeDeathEffectUsed;
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
        this(
                name,
                maximumHealth,
                level,
                sunCost,
                cooldownTicks,
                actionIntervalSeconds,
                categories,
                tags,
                behavior,
                plantFoodBehavior,
                upgradeData,
                0
        );
    }

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
            PlantUpgradeData upgradeData,
            long lifespanTicks
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
        this.lifespanTicks = Math.max(0, lifespanTicks);
    }

    @Override
    public void onTick() {
        if (this.isDisabled()) {
            return;
        }

        if (this.behavior != null) {
            this.behavior.onTick(this, this.board);
        }

        this.tickLifespan();
    }

    public void useAbility() {
        if (this.isDisabled()) {
            return;
        }

        if (this.behavior != null) {
            this.behavior.activate(this, this.board);
        }
    }

    public void receivePlantFood() {
        if (this.isDisabled()) {
            return;
        }

        if (this.plantFoodBehavior != null) {
            this.plantFoodBehavior.activate(this, this.board);
        }
    }

    public boolean upgrade() {
        if (this.upgradeData == null || !this.upgradeData.canUpgrade()) {
            return false;
        }

        PlantUpgradeEffect effect = this.upgradeData.upgrade();

        if (effect == null) {
            return false;
        }

        this.applyUpgradeEffect(effect);
        this.level = this.upgradeData.getCurrentLevel();
        return true;
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
                this.upgradeData == null ? null : this.upgradeData.copy(),
                this.lifespanTicks
        );

        copy.health = this.health;
        copy.setPosition(position);
        copy.setBoard(this.board);
        return copy;
    }

    public void disable() {
        this.disabled = true;
    }

    public void transform() {
        this.transformed = true;
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
        this.transformed = false;
    }

    public boolean isDisabled() {
        return this.disabled || this.transformed;
    }

    public boolean isDead() {
        return this.health <= 0;
    }

    public boolean hasUpgradeSpecialEffect(PlantUpgradeSpecialEffect specialEffect) {
        return this.upgradeData != null && this.upgradeData.hasSpecialEffect(specialEffect);
    }

    public void activateUpgradeDeathEffects(Board board) {
        if (this.upgradeDeathEffectUsed || board == null || board.getCombatSystem() == null) {
            return;
        }

        if (this.hasUpgradeSpecialEffect(PlantUpgradeSpecialEffect.AOE_ON_DEATH)) {
            this.upgradeDeathEffectUsed = true;

            for (model.zombie.Zombie zombie : board.getZombiesInRadius(this.position, 1)) {
                board.getCombatSystem().applyDamageToZombie(zombie, 300);
            }
        }
    }

    private void applyUpgradeEffect(PlantUpgradeEffect effect) {
        this.addBonusHealth(effect.getHealthBonus());
        this.sunCost = Math.max(0, this.sunCost - effect.getSunCostReduction());
        this.cooldownTicks = Math.max(1, this.cooldownTicks - effect.getCooldownReductionTicks());
        this.lifespanTicks += effect.getLifespanBonusTicks();
        this.actionIntervalSeconds = Math.max(
                0.1,
                effect.upgradeInterval(Math.round(this.actionIntervalSeconds * 10)) / 10.0
        );

        if (this.behavior != null) {
            this.behavior.applyUpgrade(effect);
        }

        if (this.plantFoodBehavior != null) {
            this.plantFoodBehavior.applyUpgrade(effect);
        }
    }

    private void tickLifespan() {
        if (this.lifespanTicks <= 0) {
            return;
        }

        this.lifespanTicks--;

        if (this.lifespanTicks == 0 && this.board != null) {
            this.board.removePlant(this);
        }
    }

}
