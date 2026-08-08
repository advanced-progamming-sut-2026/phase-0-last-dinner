package model.plant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class PlantDefinition {
    private String name;
    private Set<PlantCategory> categories;
    private Set<PlantTag> tags;
    private int cost;
    private int baseHealth;
    private String damageExpression;
    private String baseAbilityDescription;
    private String plantFoodEffectDescription;
    private List<String> levelUpEffects;
    private double actionIntervalSeconds;
    private double rechargeSeconds;
}