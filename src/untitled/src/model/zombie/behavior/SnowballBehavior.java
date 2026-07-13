package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.zombie.Zombie;

import java.util.IdentityHashMap;
import java.util.Map;

public class SnowballBehavior implements ZombieBehavior {
    private int range;
    private long throwIntervalTicks;
    private long ticksSinceThrow;
    private Map<Plant, Integer> snowballHits = new IdentityHashMap<>();

    public SnowballBehavior(int range, long throwIntervalTicks) {
        this.range = Math.max(1, range);
        this.throwIntervalTicks = Math.max(1, throwIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.blockProjectilesAtFrozenPlants(board);
        this.ticksSinceThrow++;
        if (this.ticksSinceThrow >= this.throwIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceThrow = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        Plant target = this.findNearestPlantInLane(zombie, board);
        if (target == null) {
            return;
        }

        int hits = this.snowballHits.containsKey(target) ? this.snowballHits.get(target) + 1 : 1;
        this.snowballHits.put(target, hits);
        if (hits >= 3) {
            target.disable();
        }
    }

    public void thaw(Plant plant) {
        if (plant != null && this.snowballHits.remove(plant) != null) {
            plant.enable();
        }
    }

    private void blockProjectilesAtFrozenPlants(Board board) {
        if (board == null) {
            return;
        }

        for (Map.Entry<Plant, Integer> entry : this.snowballHits.entrySet()) {
            Plant plant = entry.getKey();
            if (entry.getValue() < 3 || plant == null || plant.isDead() || plant.getPosition() == null) {
                continue;
            }

            for (Projectile projectile : board.getProjectiles()) {
                if (projectile != null && !projectile.isLobbed()
                        && projectile.getPosition() != null
                        && projectile.getPosition().getY() == plant.getPosition().getY()
                        && Math.abs(projectile.getExactX() - plant.getPosition().getX()) <= 0.5) {
                    if (projectile.getType() == model.plant.ProjectileType.FIRE) {
                        plant.enable();
                        entry.setValue(0);
                    } else {
                        projectile.expire();
                    }
                }
            }
        }
    }

    private Plant findNearestPlantInLane(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return null;
        }

        Plant nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Plant plant : board.getPlantsInLane(zombie.getPosition())) {
            if (plant == null || plant.isDead() || plant.getPosition() == null) {
                continue;
            }

            int distance = zombie.getPosition().getX() - plant.getPosition().getX();
            if (distance >= 0 && distance <= this.range && distance < nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
