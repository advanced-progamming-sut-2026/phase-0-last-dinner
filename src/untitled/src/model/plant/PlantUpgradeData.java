package model.plant;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class PlantUpgradeData {
    private int currentLevel;
    private int maximumLevel;
    private int seedPackets;
    private int requiredSeedPackets;
    private int requiredCoins;
    private List<PlantUpgradeEffect> upgradeEffects;

    public PlantUpgradeData(List<PlantUpgradeEffect> upgradeEffects) {
        this(1, effectCount(upgradeEffects) + 1, 0, 0, 0, upgradeEffects);
    }

    public PlantUpgradeData(
            int currentLevel,
            int maximumLevel,
            int seedPackets,
            int requiredSeedPackets,
            int requiredCoins,
            List<PlantUpgradeEffect> upgradeEffects
    ) {
        this.upgradeEffects = copyEffects(upgradeEffects);
        this.maximumLevel = Math.max(1, maximumLevel);
        this.currentLevel = Math.max(1, Math.min(currentLevel, this.maximumLevel));
        this.seedPackets = Math.max(0, seedPackets);
        this.requiredSeedPackets = Math.max(0, requiredSeedPackets);
        this.requiredCoins = Math.max(0, requiredCoins);
    }

    public boolean canUpgrade() {
        return this.currentLevel < this.maximumLevel
                && this.nextUpgradeEffect() != null
                && this.seedPackets >= this.requiredSeedPackets;
    }

    public PlantUpgradeEffect upgrade() {
        if (!this.canUpgrade()) {
            return null;
        }

        PlantUpgradeEffect effect = this.nextUpgradeEffect();
        this.seedPackets -= this.requiredSeedPackets;
        this.currentLevel++;
        return effect;
    }

    public String getCurrentLevelEffect() {
        PlantUpgradeEffect effect = this.effectForLevel(this.currentLevel);
        return effect == null ? null : effect.getDescription();
    }

    public String getNextLevelEffect() {
        PlantUpgradeEffect effect = this.nextUpgradeEffect();
        return effect == null ? null : effect.getDescription();
    }

    public boolean hasSpecialEffect(PlantUpgradeSpecialEffect specialEffect) {
        if (specialEffect == null) {
            return false;
        }

        int appliedEffectCount = Math.max(0, this.currentLevel - 1);

        for (int i = 0; i < appliedEffectCount && i < this.upgradeEffects.size(); i++) {
            if (this.upgradeEffects.get(i).hasSpecialEffect(specialEffect)) {
                return true;
            }
        }

        return false;
    }

    public PlantUpgradeData copy() {
        return new PlantUpgradeData(
                this.currentLevel,
                this.maximumLevel,
                this.seedPackets,
                this.requiredSeedPackets,
                this.requiredCoins,
                this.upgradeEffects
        );
    }

    public void addSeedPackets(int amount) {
        if (amount > 0) {
            this.seedPackets += amount;
        }
    }

    private PlantUpgradeEffect nextUpgradeEffect() {
        return this.effectForLevel(this.currentLevel + 1);
    }

    private PlantUpgradeEffect effectForLevel(int level) {
        int effectIndex = level - 2;

        if (effectIndex < 0 || effectIndex >= this.upgradeEffects.size()) {
            return null;
        }

        return this.upgradeEffects.get(effectIndex);
    }

    private static int effectCount(List<PlantUpgradeEffect> upgradeEffects) {
        return upgradeEffects == null ? 0 : upgradeEffects.size();
    }

    private static List<PlantUpgradeEffect> copyEffects(List<PlantUpgradeEffect> upgradeEffects) {
        if (upgradeEffects == null || upgradeEffects.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<>(upgradeEffects));
    }
}
