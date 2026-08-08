package model.plant;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

// etelaat ertegha collection ke be giah sakhte shode vasl mishe
@Getter
public class PlantUpgradeData {
    private final int currentLevel;
    private final int maximumLevel;
    private final int seedPackets;
    // in do meghdar hazine raftan az level feli be level baad hastan
    private final int requiredSeedPackets;
    private final int requiredCoins;
    private final int availableCoins;
    private final List<PlantUpgradeEffect> upgradeEffects;

    public PlantUpgradeData(List<PlantUpgradeEffect> upgradeEffects) {
        this(
                1,
                effectCount(upgradeEffects) + 1,
                0,
                PlantUpgradeService.BASE_SEED_PACKET_COST,
                PlantUpgradeService.BASE_COIN_COST,
                0,
                upgradeEffects
        );
    }

    public PlantUpgradeData(
            int currentLevel,
            int maximumLevel,
            int seedPackets,
            int requiredSeedPackets,
            int requiredCoins,
            List<PlantUpgradeEffect> upgradeEffects
    ) {
        this(
                currentLevel,
                maximumLevel,
                seedPackets,
                requiredSeedPackets,
                requiredCoins,
                0,
                upgradeEffects
        );
    }

    public PlantUpgradeData(
            int currentLevel,
            int maximumLevel,
            int seedPackets,
            int requiredSeedPackets,
            int requiredCoins,
            int availableCoins,
            List<PlantUpgradeEffect> upgradeEffects
    ) {
        this.upgradeEffects = upgradeEffects == null
                ? Collections.<PlantUpgradeEffect>emptyList()
                : Collections.unmodifiableList(upgradeEffects);
        this.maximumLevel = Math.max(1, maximumLevel);
        this.currentLevel = Math.max(1, Math.min(currentLevel, this.maximumLevel));
        this.seedPackets = Math.max(0, seedPackets);
        this.requiredSeedPackets = Math.max(0, requiredSeedPackets);
        this.requiredCoins = Math.max(0, requiredCoins);
        this.availableCoins = Math.max(0, availableCoins);
    }

    public boolean canUpgrade() {
        return this.currentLevel < this.maximumLevel
                && this.nextUpgradeEffect() != null
                && this.seedPackets >= this.requiredSeedPackets
                && this.availableCoins >= this.requiredCoins;
    }

    public PlantUpgradeEffect nextUpgradeEffect() {
        return this.effectForLevel(this.currentLevel + 1);
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
}
