package model.plant;

import lombok.AllArgsConstructor;

import java.util.List;
@AllArgsConstructor
public class PlantUpgradeData {
    private int currentLevel;
    private int maximumLevel;
    private int seedPackets;
    private int requiredSeedPackets;
    private int requiredCoins;
    private List<String> levelUpEffects;

    public boolean canUpgrade() {
        return this.currentLevel < this.maximumLevel
                && this.seedPackets >= this.requiredSeedPackets;
    }
    public void upgrade() {
        if (!this.canUpgrade()) {
            return;
        }

        this.seedPackets -= this.requiredSeedPackets;
        this.currentLevel++;
    }

    public String getCurrentLevelEffect() {
        if (this.levelUpEffects == null || this.levelUpEffects.isEmpty()) {
            return null;
        }

        int effectIndex = this.currentLevel - 1;
//index effect yeki kamtar az levele
        if (effectIndex < 0 || effectIndex >= this.levelUpEffects.size()) {
            return null;
        }

        return this.levelUpEffects.get(effectIndex);
    }
}
