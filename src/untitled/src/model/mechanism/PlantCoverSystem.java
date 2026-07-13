package model.mechanism;

import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ProjectileType;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

// cover haye yakh va octopus ro joda az hp giah modiriat mikone
public class PlantCoverSystem {
    private static final int SNOWBALLS_TO_FREEZE = 3;
    private static final int FROZEN_COVER_HEALTH = 600;
    private static final int OCTOPUS_HEALTH = 300;
    private static final int TICKS_PER_SECOND = 10;
    private static final int FIRE_THAW_DAMAGE_PER_SECOND = 60;

    // identity map state ro be haman instance giah vasl mikone
    private final Map<Plant, Integer> snowballHits = new IdentityHashMap<>();
    private final Map<Plant, Cover> covers = new IdentityHashMap<>();
    private int environmentTicks;

    public void hitWithSnowball(Plant plant) {
        if (!this.canReceiveCover(plant) || this.covers.containsKey(plant)) {
            return;
        }

        int hitCount = this.snowballHits.containsKey(plant) ? this.snowballHits.get(plant) + 1 : 1;
        this.snowballHits.put(plant, hitCount);

        if (hitCount >= SNOWBALLS_TO_FREEZE) {
            this.covers.put(plant, new Cover(CoverType.FROZEN, FROZEN_COVER_HEALTH));
            plant.setCovered(true);
        }
    }

    public void coverWithOctopus(Plant plant) {
        if (!this.canReceiveCover(plant) || this.covers.containsKey(plant)) {
            return;
        }

        this.snowballHits.remove(plant);
        this.covers.put(plant, new Cover(CoverType.OCTOPUS, OCTOPUS_HEALTH));
        plant.setCovered(true);
    }

    public void onTick(Board board) {
        if (board == null || ++this.environmentTicks < TICKS_PER_SECOND) {
            return;
        }

        this.environmentTicks = 0;
        Iterator<Map.Entry<Plant, Cover>> iterator = this.covers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Plant, Cover> entry = iterator.next();
            Plant plant = entry.getKey();
            Cover cover = entry.getValue();

            if (!this.isPlacedPlant(plant)) {
                iterator.remove();
                this.snowballHits.remove(plant);
                if (plant != null) {
                    plant.setCovered(false);
                }
                continue;
            }

            if (cover.type == CoverType.FROZEN && this.hasAdjacentFirePlant(board, plant)) {
                cover.health -= FIRE_THAW_DAMAGE_PER_SECOND;

                if (cover.health <= 0) {
                    this.removeCover(iterator, plant);
                }
            }
        }
    }

    // projectile mamooli ro ghabl az barkhord ba giah be cover mizane
    public boolean intercept(Projectile projectile) {
        if (projectile == null || projectile.isLobbed() || projectile.getPosition() == null) {
            return false;
        }

        Iterator<Map.Entry<Plant, Cover>> iterator = this.covers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Plant, Cover> entry = iterator.next();
            Plant plant = entry.getKey();

            if (!this.isPlacedPlant(plant)) {
                iterator.remove();
                this.snowballHits.remove(plant);
                if (plant != null) {
                    plant.setCovered(false);
                }
                continue;
            }

            if (!this.collides(projectile, plant)) {
                continue;
            }

            Cover cover = entry.getValue();

            if (cover.type == CoverType.FROZEN && projectile.getType() == ProjectileType.FIRE) {
                this.removeCover(iterator, plant);
                return true;
            }

            int damage = DamageExpressionParser.isInstantKill(projectile.getDamageExpression())
                    ? cover.health
                    : DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
            cover.health -= Math.max(0, damage);

            if (cover.health <= 0) {
                this.removeCover(iterator, plant);
            }

            return true;
        }

        return false;
    }

    public void removePlant(Plant plant) {
        this.snowballHits.remove(plant);
        this.covers.remove(plant);

        if (plant != null) {
            plant.setCovered(false);
        }
    }

    public boolean isCovered(Plant plant) {
        return plant != null && this.covers.containsKey(plant);
    }

    public int getCoverHealth(Plant plant) {
        Cover cover = this.covers.get(plant);
        return cover == null ? 0 : Math.max(0, cover.health);
    }

    private void removeCover(Iterator<Map.Entry<Plant, Cover>> iterator, Plant plant) {
        iterator.remove();
        this.snowballHits.remove(plant);

        if (!plant.isDead()) {
            plant.setCovered(false);
        }
    }

    private boolean collides(Projectile projectile, Plant plant) {
        return plant.getPosition() != null
                && projectile.getPosition().getY() == plant.getPosition().getY()
                && Math.abs(projectile.getExactX() - plant.getPosition().getX()) <= 0.5;
    }

    private boolean isPlacedPlant(Plant plant) {
        return plant != null && !plant.isDead() && plant.getPosition() != null;
    }

    private boolean canReceiveCover(Plant plant) {
        return this.isPlacedPlant(plant) && !plant.isDisabled();
    }

    private boolean hasAdjacentFirePlant(Board board, Plant frozenPlant) {
        if (frozenPlant.getPosition() == null) {
            return false;
        }

        for (Plant plant : board.getPlantsInRadius(frozenPlant.getPosition(), 1)) {
            if (plant == null || plant == frozenPlant || plant.isDead() || plant.isDisabled()
                    || plant.getPosition() == null || plant.getTags() == null
                    || !plant.getTags().contains(model.plant.PlantTag.FIRE)) {
                continue;
            }

            int deltaX = Math.abs(plant.getPosition().getX() - frozenPlant.getPosition().getX());
            int deltaY = Math.abs(plant.getPosition().getY() - frozenPlant.getPosition().getY());

            if (deltaX <= 1 && deltaY <= 1 && deltaX + deltaY > 0) {
                return true;
            }
        }

        return false;
    }

    private enum CoverType {
        FROZEN,
        OCTOPUS
    }

    private static final class Cover {
        private final CoverType type;
        private int health;

        private Cover(CoverType type, int health) {
            this.type = type;
            this.health = health;
        }
    }
}
