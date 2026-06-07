package model.zombie;

import java.util.List;

public class ZombieDefinition {
    private String alias;
    private String displayName;
    private String description;
    private ZombieType type;
    private ZombieChapter chapter;
    private int hitpoints;
    private int eatDamagePerSecond;
    private double speed;
    private int wavePointCost;
    private int weight;
    private boolean canSpawnPlantFood;
    private List<ZombieArmorDefinition> armorDefinitions;
    private List<ConditionResistance> conditionResistances;
}
