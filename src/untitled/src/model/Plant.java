package model;

import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.plant.PlantUpgradeData;
import model.plant.behavior.PlantBehavior;
import model.plant.behavior.PlantFoodBehavior;

import java.util.Set;

public class Plant implements Tickable {
    private String name;
    private int health;
    private int maximumHealth;
    private int level;
    private int sunCost;
    private long cooldownTicks;
    private double actionIntervalSeconds;
    private Position position;
    private Set<PlantCategory> categories;
    private Set<PlantTag> tags;
    private PlantBehavior behavior;
    private PlantFoodBehavior plantFoodBehavior;
    private PlantUpgradeData upgradeData;
    private Board board;

    @Override
    public void onTick() {
    }

    public void useAbility() {
    }

    public void receivePlantFood() {
    }

    public void upgrade() {
    }

    public void takeDamage(int amount) {
    }

    public boolean isDead() {
        return false;
    }
}
