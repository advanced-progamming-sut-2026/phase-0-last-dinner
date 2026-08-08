package model.collection;

import lombok.Getter;
import model.zombie.ConditionResistance;
import model.zombie.ZombieArmorDefinition;
import model.zombie.ZombieChapter;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ZombieCollectionState {
    private final String alias;
    private final String displayName;
    private final boolean encountered;
    private final String description;
    private final ZombieType type;
    private final ZombieChapter chapter;
    private final int hitpoints;
    private final int eatDamagePerSecond;
    private final double speed;
    private final int wavePointCost;
    private final int weight;
    private final boolean canSpawnPlantFood;
    private final List<ZombieArmorDefinition> armorDefinitions;
    private final List<ConditionResistance> conditionResistances;

    public ZombieCollectionState(
            String alias,
            String displayName,
            boolean encountered,
            String description,
            ZombieType type,
            ZombieChapter chapter,
            int hitpoints,
            int eatDamagePerSecond,
            double speed,
            int wavePointCost,
            int weight,
            boolean canSpawnPlantFood,
            List<ZombieArmorDefinition> armorDefinitions,
            List<ConditionResistance> conditionResistances
    ) {
        this.alias = alias;
        this.displayName = displayName;
        this.encountered = encountered;
        this.description = description;
        this.type = type;
        this.chapter = chapter;
        this.hitpoints = hitpoints;
        this.eatDamagePerSecond = eatDamagePerSecond;
        this.speed = speed;
        this.wavePointCost = wavePointCost;
        this.weight = weight;
        this.canSpawnPlantFood = canSpawnPlantFood;
        this.armorDefinitions = armorDefinitions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<>(armorDefinitions)
                );
        this.conditionResistances = conditionResistances == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<>(conditionResistances)
                );
    }

    public static ZombieCollectionState from(
            ZombieDefinition definition,
            boolean encountered
    ) {
        if (definition == null) {
            return null;
        }

        return new ZombieCollectionState(
                definition.getAlias(),
                definition.getDisplayName(),
                encountered,
                definition.getDescription(),
                definition.getType(),
                definition.getChapter(),
                definition.getHitpoints(),
                definition.getEatDamagePerSecond(),
                definition.getSpeed(),
                definition.getWavePointCost(),
                definition.getWeight(),
                definition.isCanSpawnPlantFood(),
                definition.getArmorDefinitions(),
                definition.getConditionResistances()
        );
    }
}
