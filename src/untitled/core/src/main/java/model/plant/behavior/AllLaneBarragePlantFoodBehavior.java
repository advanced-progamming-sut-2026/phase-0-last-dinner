package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.plant.PlantUpgradeEffect;
import model.plant.Projectile;

import java.util.ArrayList;
import java.util.List;

// az sotoon giah dar hame lane ha barrage projectile mifreste
public class AllLaneBarragePlantFoodBehavior implements PlantFoodBehavior {
    private Projectile projectileTemplate;
    private int volleyCount;

    public AllLaneBarragePlantFoodBehavior(Projectile projectileTemplate, int volleyCount) {
        this.projectileTemplate = projectileTemplate;
        this.volleyCount = Math.max(1, volleyCount);
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null || this.projectileTemplate == null) {
            return;
        }

        List<Integer> rows = new ArrayList<>();

        for (Tile tile : board.getTiles()) {
            if (tile != null && tile.getPosition() != null && !rows.contains(tile.getPosition().getY())) {
                rows.add(tile.getPosition().getY());
            }
        }

        for (Integer row : rows) {
            Position spawn = new Position(plant.getPosition().getX(), row);

            for (int volley = 0; volley < this.volleyCount; volley++) {
                Projectile projectile = this.projectileTemplate.copyAt(spawn, 1, 0);
                projectile.setSourcePlant(plant);
                board.addProjectile(projectile);
            }
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        return new AllLaneBarragePlantFoodBehavior(
                this.projectileTemplate == null ? null : this.projectileTemplate.copyAt(null),
                this.volleyCount
        );
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null || this.projectileTemplate == null) {
            return;
        }

        this.projectileTemplate.addDamageBonus(effect.getDamageBonus());
        this.projectileTemplate.addPierceBonus(effect.getPierceBonus());
        this.projectileTemplate.addPlantFoodChanceBonus(effect.getPlantFoodChanceBonusPercent());
    }
}
