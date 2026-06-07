package model.plant;

import java.util.List;

public class PlantUpgradeData {
    private int currentLevel;
    private int maximumLevel;
    private int seedPackets;
    private int requiredSeedPackets;
    private int requiredCoins;
    private List<String> levelUpEffects;

    public boolean canUpgrade() {
        return false;
    }

    public void upgrade() {
    }

    public String getCurrentLevelEffect() {
        return null;
    }
}
