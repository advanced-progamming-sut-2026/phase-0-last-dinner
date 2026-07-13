package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// chand zombie tasadofi submerged ro faghat rooye tile haye water hazf mikone
public class TangleKelpPlantFoodBehavior implements PlantFoodBehavior {
    private int targetCount;

    public TangleKelpPlantFoodBehavior(int targetCount) {
        this.targetCount = Math.max(1, targetCount);
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (!this.isOnWater(plant, board) || board.getCombatSystem() == null) {
            return;
        }

        List<Zombie> waterZombies = new ArrayList<>();

        for (Zombie zombie : board.getAllZombies()) {
            if (this.isSubmergedWaterEnemy(zombie, board)) {
                waterZombies.add(zombie);
            }
        }

        Collections.shuffle(waterZombies);
        int count = Math.min(this.targetCount, waterZombies.size());

        for (int i = 0; i < count; i++) {
            board.getCombatSystem().killZombieIgnoringAllegiance(waterZombies.get(i));
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        return new TangleKelpPlantFoodBehavior(this.targetCount);
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect != null) {
            this.targetCount += Math.max(0, effect.getTargetCountBonus());
        }
    }

    private boolean isOnWater(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null) {
            return false;
        }

        Tile tile = board.getTile(plant.getPosition());
        return tile != null && tile.getTerrainType() == TerrainType.WATER;
    }

    private boolean isSubmergedWaterEnemy(Zombie zombie, Board board) {
        if (zombie == null || zombie.isDead() || zombie.isHypnotized()
                || zombie.getPosition() == null || !zombie.hasCondition(ZombieCondition.SUBMERGED)) {
            return false;
        }

        Tile tile = board.getTile(zombie.getPosition());
        return tile != null && tile.getTerrainType() == TerrainType.WATER;
    }
}
