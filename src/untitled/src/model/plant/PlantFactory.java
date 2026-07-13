package model.plant;

import model.Plant;
import model.plant.behavior.PlantBehavior;
import model.plant.behavior.PlantFoodBehavior;

import java.util.List;
import java.util.Locale;

// definition ro be giah runtime ba behavior va ertegha haye feli tabdil mikone
public class PlantFactory {
    private static final int TICKS_PER_SECOND = 10;

    private final PlantUpgradeEffectParser upgradeEffectParser = new PlantUpgradeEffectParser();
    private final PlantBehaviorFactory behaviorFactory = new PlantBehaviorFactory();
    private final PlantFoodBehaviorFactory plantFoodBehaviorFactory = new PlantFoodBehaviorFactory();
    private final PlantUpgradeService upgradeService;

    public PlantFactory() {
        this(null);
    }

    public PlantFactory(PlantUpgradeService upgradeService) {
        this.upgradeService = upgradeService;
    }

    public Plant create(PlantDefinition definition) {
        return this.create(
                definition,
                this.behaviorFactory.create(definition),
                this.plantFoodBehaviorFactory.create(definition)
        );
    }

    public Plant create(
            PlantDefinition definition,
            PlantBehavior behavior,
            PlantFoodBehavior plantFoodBehavior
    ) {
        return this.build(
                definition,
                behavior,
                plantFoodBehavior,
                this.createUpgradeData(definition)
        );
    }

    private Plant build(
            PlantDefinition definition,
            PlantBehavior behavior,
            PlantFoodBehavior plantFoodBehavior,
            PlantUpgradeData upgradeData
    ) {
        Plant plant = new Plant(
                definition.getName(),
                definition.getBaseHealth(),
                1,
                definition.getCost(),
                this.secondsToTicks(definition.getRechargeSeconds()),
                definition.getActionIntervalSeconds(),
                definition.getCategories(),
                definition.getTags(),
                behavior,
                plantFoodBehavior,
                upgradeData,
                this.createLifespanTicks(definition)
        );

        plant.applyStoredUpgrades();
        return plant;
    }

    // behavior giah maghsad ro ba ertegha haye khod imitater misaze
    public Plant createImitater(PlantDefinition imitater, PlantDefinition copiedDefinition) {
        if (imitater == null || copiedDefinition == null
                || !this.normalize(imitater.getName()).contains("imitater")
                || this.normalize(copiedDefinition.getName()).contains("imitater")) {
            throw new IllegalArgumentException("Imitater requires a non-Imitater plant definition to copy");
        }

        PlantDefinition imitatedDefinition = new PlantDefinition(
                "Imitater: " + copiedDefinition.getName(),
                copiedDefinition.getCategories(),
                copiedDefinition.getTags(),
                copiedDefinition.getCost(),
                copiedDefinition.getBaseHealth(),
                copiedDefinition.getDamageExpression(),
                copiedDefinition.getBaseAbilityDescription(),
                copiedDefinition.getPlantFoodEffectDescription(),
                imitater.getLevelUpEffects(),
                copiedDefinition.getActionIntervalSeconds(),
                copiedDefinition.getRechargeSeconds()
        );

        return this.build(
                imitatedDefinition,
                this.behaviorFactory.create(imitatedDefinition),
                this.plantFoodBehaviorFactory.create(imitatedDefinition),
                this.createUpgradeData(imitater)
        );
    }

    private PlantUpgradeData createUpgradeData(PlantDefinition definition) {
        if (definition == null || definition.getLevelUpEffects() == null
                || definition.getLevelUpEffects().isEmpty()) {
            return null;
        }

        List<PlantUpgradeEffect> effects =
                this.upgradeEffectParser.parseAll(definition.getLevelUpEffects());

        if (this.upgradeService == null) {
            return new PlantUpgradeData(effects);
        }

        return this.upgradeService.createUpgradeData(definition, effects);
    }

    private long createLifespanTicks(PlantDefinition definition) {
        String name = this.normalize(definition.getName());

        if (name.contains("sea-shroom") || name.contains("puff-shroom")) {
            return this.secondsToTicks(60);
        }

        return 0;
    }

    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private long secondsToTicks(double seconds) {
        return seconds <= 0 ? 1 : Math.round(seconds * TICKS_PER_SECOND);
    }
}
