package model.plant;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@Getter
public class PlantUpgradeEffect {
    private final String description;
    private final int healthBonus;
    private final int sunCostReduction;
    private final long cooldownReductionTicks;
    private final long actionIntervalReductionTicks;
    private final int attackSpeedBonusPercent;
    private final int damageBonus;
    private final int rangeBonus;
    private final long durationBonusTicks;
    private final long lifespanBonusTicks;
    private final long armDelayReductionTicks;
    private final long digestReductionTicks;
    private final int sunProductionBonus;
    private final int sunDropBonus;
    private final int targetCountBonus;
    private final int pierceBonus;
    private final int bounceBonus;
    private final int plantFoodChanceBonusPercent;
    private final int poisonDamageBonusPerTick;
    private final Set<PlantUpgradeSpecialEffect> specialEffects;

    private PlantUpgradeEffect(Builder builder) {
        this.description = builder.description;
        this.healthBonus = builder.healthBonus;
        this.sunCostReduction = builder.sunCostReduction;
        this.cooldownReductionTicks = builder.cooldownReductionTicks;
        this.actionIntervalReductionTicks = builder.actionIntervalReductionTicks;
        this.attackSpeedBonusPercent = builder.attackSpeedBonusPercent;
        this.damageBonus = builder.damageBonus;
        this.rangeBonus = builder.rangeBonus;
        this.durationBonusTicks = builder.durationBonusTicks;
        this.lifespanBonusTicks = builder.lifespanBonusTicks;
        this.armDelayReductionTicks = builder.armDelayReductionTicks;
        this.digestReductionTicks = builder.digestReductionTicks;
        this.sunProductionBonus = builder.sunProductionBonus;
        this.sunDropBonus = builder.sunDropBonus;
        this.targetCountBonus = builder.targetCountBonus;
        this.pierceBonus = builder.pierceBonus;
        this.bounceBonus = builder.bounceBonus;
        this.plantFoodChanceBonusPercent = builder.plantFoodChanceBonusPercent;
        this.poisonDamageBonusPerTick = builder.poisonDamageBonusPerTick;
        this.specialEffects = copySpecialEffects(builder.specialEffects);
    }

    public static Builder builder(String description) {
        return new Builder(description);
    }

    public boolean hasSpecialEffect(PlantUpgradeSpecialEffect specialEffect) {
        return specialEffect != null && this.specialEffects.contains(specialEffect);
    }

    public long upgradeInterval(long currentIntervalTicks) {
        long upgradedInterval = currentIntervalTicks - this.actionIntervalReductionTicks;

        if (this.attackSpeedBonusPercent > 0) {
            upgradedInterval = Math.round(upgradedInterval * (100.0 - this.attackSpeedBonusPercent) / 100.0);
        }

        return Math.max(1, upgradedInterval);
    }

    private static Set<PlantUpgradeSpecialEffect> copySpecialEffects(Set<PlantUpgradeSpecialEffect> specialEffects) {
        if (specialEffects == null || specialEffects.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(EnumSet.copyOf(specialEffects));
    }

    public static class Builder {
        private final String description;
        private int healthBonus;
        private int sunCostReduction;
        private long cooldownReductionTicks;
        private long actionIntervalReductionTicks;
        private int attackSpeedBonusPercent;
        private int damageBonus;
        private int rangeBonus;
        private long durationBonusTicks;
        private long lifespanBonusTicks;
        private long armDelayReductionTicks;
        private long digestReductionTicks;
        private int sunProductionBonus;
        private int sunDropBonus;
        private int targetCountBonus;
        private int pierceBonus;
        private int bounceBonus;
        private int plantFoodChanceBonusPercent;
        private int poisonDamageBonusPerTick;
        private final Set<PlantUpgradeSpecialEffect> specialEffects = EnumSet.noneOf(PlantUpgradeSpecialEffect.class);

        private Builder(String description) {
            this.description = description == null ? "" : description.trim();
        }

        public Builder addHealthBonus(int amount) {
            this.healthBonus += Math.max(0, amount);
            return this;
        }

        public Builder addSunCostReduction(int amount) {
            this.sunCostReduction += Math.max(0, amount);
            return this;
        }

        public Builder addCooldownReductionTicks(long ticks) {
            this.cooldownReductionTicks += Math.max(0, ticks);
            return this;
        }

        public Builder addActionIntervalReductionTicks(long ticks) {
            this.actionIntervalReductionTicks += Math.max(0, ticks);
            return this;
        }

        public Builder addAttackSpeedBonusPercent(int percent) {
            this.attackSpeedBonusPercent += Math.max(0, percent);
            return this;
        }

        public Builder addDamageBonus(int amount) {
            this.damageBonus += Math.max(0, amount);
            return this;
        }

        public Builder addRangeBonus(int amount) {
            this.rangeBonus += Math.max(0, amount);
            return this;
        }

        public Builder addDurationBonusTicks(long ticks) {
            this.durationBonusTicks += Math.max(0, ticks);
            return this;
        }

        public Builder addLifespanBonusTicks(long ticks) {
            this.lifespanBonusTicks += Math.max(0, ticks);
            return this;
        }

        public Builder addArmDelayReductionTicks(long ticks) {
            this.armDelayReductionTicks += Math.max(0, ticks);
            return this;
        }

        public Builder addDigestReductionTicks(long ticks) {
            this.digestReductionTicks += Math.max(0, ticks);
            return this;
        }

        public Builder addSunProductionBonus(int amount) {
            this.sunProductionBonus += Math.max(0, amount);
            return this;
        }

        public Builder addSunDropBonus(int amount) {
            this.sunDropBonus += Math.max(0, amount);
            return this;
        }

        public Builder addTargetCountBonus(int amount) {
            this.targetCountBonus += Math.max(0, amount);
            return this;
        }

        public Builder addPierceBonus(int amount) {
            this.pierceBonus += Math.max(0, amount);
            return this;
        }

        public Builder addBounceBonus(int amount) {
            this.bounceBonus += Math.max(0, amount);
            return this;
        }

        public Builder addPlantFoodChanceBonusPercent(int percent) {
            this.plantFoodChanceBonusPercent += Math.max(0, percent);
            return this;
        }

        public Builder addPoisonDamageBonusPerTick(int amount) {
            this.poisonDamageBonusPerTick += Math.max(0, amount);
            return this;
        }

        public Builder addSpecialEffect(PlantUpgradeSpecialEffect specialEffect) {
            if (specialEffect != null) {
                this.specialEffects.add(specialEffect);
            }

            return this;
        }

        public PlantUpgradeEffect build() {
            return new PlantUpgradeEffect(this);
        }
    }
}
