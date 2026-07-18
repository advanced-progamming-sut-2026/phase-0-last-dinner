package model.collection;

import lombok.Getter;
import model.plant.PlantCategory;
import model.plant.PlantDefinition;
import model.plant.PlantTag;
import model.plant.PlantUpgradeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class PlantCollectionState {
    private final String name;
    private final boolean unlocked;
    private final int currentLevel;
    private final int maximumLevel;
    private final int seedPackets;
    private final int requiredSeedPackets;
    private final int requiredCoins;
    private final int sunCost;
    private final int baseHealth;
    private final String damageExpression;
    private final double actionIntervalSeconds;
    private final double rechargeSeconds;
    private final Set<PlantCategory> categories;
    private final Set<PlantTag> tags;
    private final String baseAbilityDescription;
    private final String plantFoodEffectDescription;
    private final List<String> levelUpEffects;

    public PlantCollectionState(
            String name,
            boolean unlocked,
            int currentLevel,
            int maximumLevel,
            int seedPackets,
            int requiredSeedPackets,
            int requiredCoins,
            int sunCost,
            int baseHealth,
            String damageExpression,
            double actionIntervalSeconds,
            double rechargeSeconds,
            Set<PlantCategory> categories,
            Set<PlantTag> tags,
            String baseAbilityDescription,
            String plantFoodEffectDescription,
            List<String> levelUpEffects
    ) {
        this.name = name;
        this.unlocked = unlocked;
        this.currentLevel = currentLevel;
        this.maximumLevel = maximumLevel;
        this.seedPackets = seedPackets;
        this.requiredSeedPackets = requiredSeedPackets;
        this.requiredCoins = requiredCoins;
        this.sunCost = sunCost;
        this.baseHealth = baseHealth;
        this.damageExpression = damageExpression;
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.rechargeSeconds = rechargeSeconds;
        this.categories = categories == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(categories));
        this.tags = tags == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        this.baseAbilityDescription = baseAbilityDescription;
        this.plantFoodEffectDescription = plantFoodEffectDescription;
        this.levelUpEffects = levelUpEffects == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(levelUpEffects));
    }

    public static PlantCollectionState from(
            PlantDefinition definition,
            PlantUpgradeService upgradeService,
            boolean unlocked
    ) {
        if (definition == null || upgradeService == null) {
            return null;
        }

        int maximumLevel = upgradeService.getMaximumLevel(definition);
        int currentLevel = Math.min(
                upgradeService.getLevel(definition.getName()),
                maximumLevel
        );
        int seedPackets = upgradeService.getSeedPackets(definition.getName());
        boolean atMaximumLevel = currentLevel >= maximumLevel;

        int requiredSeedPackets = atMaximumLevel
                ? 0
                : upgradeService.requiredSeedPackets(currentLevel);

        int requiredCoins = atMaximumLevel
                ? 0
                : upgradeService.requiredCoins(currentLevel);

        return new PlantCollectionState(
                definition.getName(),
                unlocked,
                currentLevel,
                maximumLevel,
                seedPackets,
                requiredSeedPackets,
                requiredCoins,
                definition.getCost(),
                definition.getBaseHealth(),
                definition.getDamageExpression(),
                definition.getActionIntervalSeconds(),
                definition.getRechargeSeconds(),
                definition.getCategories(),
                definition.getTags(),
                definition.getBaseAbilityDescription(),
                definition.getPlantFoodEffectDescription(),
                definition.getLevelUpEffects()
        );
    }
}