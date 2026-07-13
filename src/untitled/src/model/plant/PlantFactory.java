package model.plant;

import model.Plant;
import model.plant.behavior.PlantBehavior;
import model.plant.behavior.PlantFoodBehavior;

import java.util.Locale;

public class PlantFactory {
    private static final int TICKS_PER_SECOND = 10;

    private final PlantUpgradeEffectParser upgradeEffectParser = new PlantUpgradeEffectParser();
    private final PlantBehaviorFactory behaviorFactory = new PlantBehaviorFactory();
    private final PlantFoodBehaviorFactory plantFoodBehaviorFactory = new PlantFoodBehaviorFactory();

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
        return new Plant(
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
                this.createUpgradeData(definition),
                this.createLifespanTicks(definition)
        );
    }

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
                copiedDefinition.getLevelUpEffects(),
                copiedDefinition.getActionIntervalSeconds(),
                copiedDefinition.getRechargeSeconds()
        );

        return this.create(imitatedDefinition);
    }

    private PlantUpgradeData createUpgradeData(PlantDefinition definition) {
        if (definition == null || definition.getLevelUpEffects() == null
                || definition.getLevelUpEffects().isEmpty()) {
            return null;
        }

        return new PlantUpgradeData(this.upgradeEffectParser.parseAll(definition.getLevelUpEffects()));
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
