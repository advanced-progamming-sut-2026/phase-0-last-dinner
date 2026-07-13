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

// state va behavior yek giah sakhte shode dar board ro negah midare
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
    // in flag ha dalil haye joda baraye gheire faal shodan giah hastan
    private boolean disabled;
    private boolean transformed;
    private boolean covered;
    private boolean terrainDisabled;
    // jeloye dobar apply shodan ertegha haye collection ro migire
    private boolean storedUpgradesApplied;
    // meghdar zero yani giah omr mahdood nadare
    private long lifespanTicks;
    private long fullLifespanTicks;
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
        this.fullLifespanTicks = this.lifespanTicks;
    }

    @Override
    public void onTick() {
        if (!this.isDisabled() && this.behavior != null) {
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
        if (!this.canReceivePlantFood()) {
            return;
        }

        this.plantFoodBehavior.activate(this, this.board);
    }

    public boolean canReceivePlantFood() {
        return !this.isDisabled()
                && this.plantFoodBehavior != null
                && this.plantFoodBehavior.canActivate();
    }

    // ertegha dar collection sabt mishe va rooye yek instance anjam nemishe
    @Deprecated
    public boolean upgrade() {
        return false;
    }

    // ertegha haye sabt shode collection ro faghat yek bar apply mikone
    public void applyStoredUpgrades() {
        if (this.storedUpgradesApplied || this.upgradeData == null) {
            return;
        }

        int appliedEffectCount = Math.min(
                this.upgradeData.getCurrentLevel() - 1,
                this.upgradeData.getUpgradeEffects().size()
        );

        for (int i = 0; i < appliedEffectCount; i++) {
            this.applyUpgradeEffect(this.upgradeData.getUpgradeEffects().get(i));
        }

        this.level = this.upgradeData.getCurrentLevel();
        this.storedUpgradesApplied = true;
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }

        boolean wasAlive = !this.isDead();
        this.health -= amount;

        if (this.behavior != null) {
            this.behavior.onDamaged(this, this.board, amount);
        }

        if (this.health < 0) {
            this.health = 0;
        }

        if (wasAlive && this.isDead() && this.behavior != null) {
            this.behavior.onDeath(this, this.board);
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

    // baraye clone plant food yek giah joda ba state feli misaze
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
                this.behavior == null ? null : this.behavior.copy(),
                this.plantFoodBehavior == null ? null : this.plantFoodBehavior.copy(),
                this.upgradeData,
                this.lifespanTicks
        );

        copy.health = this.health;
        copy.fullLifespanTicks = this.fullLifespanTicks;
        copy.storedUpgradesApplied = this.storedUpgradesApplied;
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

    public void setCovered(boolean covered) {
        this.covered = covered;
    }

    public void setTerrainDisabled(boolean terrainDisabled) {
        this.terrainDisabled = terrainDisabled;
    }

    public void resetLifespan() {
        this.lifespanTicks = this.fullLifespanTicks;
    }

    public boolean isDisabled() {
        return this.disabled || this.transformed || this.covered || this.terrainDisabled;
    }

    public boolean isDead() {
        return this.health <= 0;
    }

    public boolean hasUpgradeSpecialEffect(PlantUpgradeSpecialEffect specialEffect) {
        return this.upgradeData != null && this.upgradeData.hasSpecialEffect(specialEffect);
    }

    // asar marg ertegha ro dar har instance faghat yek bar ejra mikone
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
        this.fullLifespanTicks += effect.getLifespanBonusTicks();
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
