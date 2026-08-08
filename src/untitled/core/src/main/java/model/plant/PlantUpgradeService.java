package model.plant;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// pishraft daemi ertegha giah ha ro baraye yek profile negah midare
// hazine ha dar har level ba zarb hazine paye dar level feli hesab mishan
public class PlantUpgradeService {
    public static final int BASE_SEED_PACKET_COST = 10;
    public static final int BASE_COIN_COST = 1000;

    // key az nam normalize shode sakhte mishe ta progress joda nashe
    private final Map<String, PlantProgress> progressByPlant = new HashMap<>();
    @Getter
    private int coins;

    public PlantUpgradeService() {
        this(0);
    }

    public PlantUpgradeService(int initialCoins) {
        this.coins = Math.max(0, initialCoins);
    }

    public PlantUpgradeResult upgrade(PlantDefinition definition) {
        if (definition == null || this.normalize(definition.getName()).isEmpty()) {
            return PlantUpgradeResult.PLANT_NOT_FOUND;
        }

        PlantProgress progress = this.progressFor(definition.getName());
        int maximumLevel = this.maximumLevel(definition);

        if (progress.level >= maximumLevel) {
            return PlantUpgradeResult.MAXIMUM_LEVEL_REACHED;
        }

        int requiredSeedPackets = this.requiredSeedPackets(progress.level);
        int requiredCoins = this.requiredCoins(progress.level);

        if (progress.seedPackets < requiredSeedPackets) {
            return PlantUpgradeResult.NOT_ENOUGH_SEED_PACKETS;
        }

        if (this.coins < requiredCoins) {
            return PlantUpgradeResult.NOT_ENOUGH_COINS;
        }

        progress.seedPackets -= requiredSeedPackets;
        this.coins -= requiredCoins;
        progress.level++;
        return PlantUpgradeResult.SUCCESS;
    }

    public PlantUpgradeResult upgrade(PlantDefinition definition, int availableCoins) {
        this.setCoins(availableCoins);
        return this.upgrade(definition);
    }

    public PlantUpgradeData createUpgradeData(
            PlantDefinition definition,
            List<PlantUpgradeEffect> effects
    ) {
        if (definition == null)
            return null;

        PlantProgress progress = this.progressFor(definition.getName());
        int maximumLevel = effects == null ? 1 : effects.size() + 1;
        int currentLevel = Math.min(progress.level, maximumLevel);
        boolean atMaximumLevel = currentLevel >= maximumLevel;

        return new PlantUpgradeData(
                currentLevel,
                maximumLevel,
                progress.seedPackets,
                atMaximumLevel ? 0 : this.requiredSeedPackets(currentLevel),
                atMaximumLevel ? 0 : this.requiredCoins(currentLevel),
                this.coins,
                effects
        );
    }

    public void addCoins(int amount) {
        if (amount > 0) {
            this.coins = this.safeAdd(this.coins, amount);
        }
    }

    public void setCoins(int amount) {
        this.coins = Math.max(0, amount);
    }

    public void addSeedPackets(String plantName, int amount) {
        if (amount > 0 && !this.normalize(plantName).isEmpty()) {
            PlantProgress progress = this.progressFor(plantName);
            progress.seedPackets = this.safeAdd(progress.seedPackets, amount);
        }
    }

    public int getLevel(String plantName) {
        PlantProgress progress = this.findProgress(plantName);
        return progress == null ? 1 : progress.level;
    }

    public int getSeedPackets(String plantName) {
        PlantProgress progress = this.findProgress(plantName);
        return progress == null ? 0 : progress.seedPackets;
    }

    public int getMaximumLevel(PlantDefinition definition) {
        if (definition == null) {
            return 1;
        }
        return this.maximumLevel(definition);
    }

    public int requiredSeedPackets(int currentLevel) {
        return BASE_SEED_PACKET_COST * Math.max(1, currentLevel);
    }

    public int requiredCoins(int currentLevel) {
        return BASE_COIN_COST * Math.max(1, currentLevel);
    }

    private int maximumLevel(PlantDefinition definition) {
        List<String> effects = definition.getLevelUpEffects();
        return effects == null ? 1 : effects.size() + 1;
    }

    private PlantProgress progressFor(String plantName) {
        String key = this.normalize(plantName);
        PlantProgress progress = this.progressByPlant.get(key);

        if (progress == null) {
            progress = new PlantProgress();
            this.progressByPlant.put(key, progress);
        }

        return progress;
    }

    private PlantProgress findProgress(String plantName) {
        return this.progressByPlant.get(this.normalize(plantName));
    }

    private String normalize(String plantName) {
        return plantName == null ? "" : plantName.trim().toLowerCase(Locale.ROOT);
    }

    private int safeAdd(int value, int amount) {
        long total = (long) value + amount;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static final class PlantProgress {
        private int level = 1;
        private int seedPackets;
    }
}
