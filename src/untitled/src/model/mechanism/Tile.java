package model.mechanism;

import lombok.Getter;
import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.PlantTag;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Tile {
    private static final int GRAVE_HEALTH = 700;
    private static final int FROZEN_HEALTH = 600;
    private static final int TICKS_PER_SECOND = 10;
    private static final int FIRE_THAW_DAMAGE_PER_SECOND = 60;

    private Position position;
    private TerrainType terrainType;
    private int terrainHealth;
    private List<Plant> plants;
    private List<Zombie> zombies;
    private int environmentTicks;
    private GraveLootType graveLoot = GraveLootType.NONE;

    public Tile() {
        this(null, TerrainType.CLASSIC);
    }

    public Tile(Position position, TerrainType terrainType) {
        this.position = position;
        this.plants = new ArrayList<>();
        this.zombies = new ArrayList<>();
        this.setTerrainType(terrainType);
    }

    public boolean canPlacePlant(Plant plant) {
        if (plant == null) {
            return false;
        }

        String name = plant.getName() == null
                ? ""
                : plant.getName().toLowerCase(java.util.Locale.ROOT);

        if (name.contains("grave buster")) {
            return this.terrainType == TerrainType.GRAVE;
        }

        if (name.contains("hot potato")) {
            return this.terrainType == TerrainType.FROZEN;
        }

        if (this.terrainType == TerrainType.CRATER
                || this.terrainType == TerrainType.GRAVE
                || this.terrainType == TerrainType.FROZEN
                || this.terrainType == TerrainType.SLIPPERY_UP
                || this.terrainType == TerrainType.SLIPPERY_DOWN) {
            return false;
        }

        if (this.terrainType == TerrainType.WATER) {
            if (name.contains("lily pad")
                    || name.contains("tangle kelp")
                    || name.contains("sea-shroom")) {
                return true;
            }

            for (Plant existingPlant : this.plants) {
                if (existingPlant != null && "lily pad".equalsIgnoreCase(existingPlant.getName())) {
                    return true;
                }
            }

            return plant.getTags() != null && plant.getTags().contains(PlantTag.WATER);
        }

        return !name.contains("lily pad")
                && !name.contains("tangle kelp")
                && !name.contains("sea-shroom");
    }

    public void addPlant(Plant plant) {
        if (plant != null) {
            this.plants.add(plant);
            plant.setTerrainDisabled(this.terrainType == TerrainType.FROZEN
                    && !this.isTerrainRemover(plant));
        }
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null) {
            this.zombies.add(zombie);
            zombie.setTerrainFrozen(this.terrainType == TerrainType.FROZEN);
        }
    }

    public boolean removePlant(Plant plant) {
        if (plant == null || this.plants == null || !this.plants.remove(plant)) {
            return false;
        }

        plant.setTerrainDisabled(false);
        return true;
    }

    public boolean removeZombie(Zombie zombie) {
        if (zombie == null || this.zombies == null || !this.zombies.remove(zombie)) {
            return false;
        }

        zombie.setTerrainFrozen(false);
        return true;
    }

    public void setTerrainType(TerrainType terrainType) {
        this.terrainType = terrainType == null ? TerrainType.CLASSIC : terrainType;

        if (this.terrainType == TerrainType.GRAVE) {
            this.terrainHealth = GRAVE_HEALTH;
        } else if (this.terrainType == TerrainType.FROZEN) {
            this.terrainHealth = FROZEN_HEALTH;
        } else {
            this.terrainHealth = 0;
        }

        this.environmentTicks = 0;
        this.updateOccupantState();
    }
    public void setGraveLoot(GraveLootType graveLoot) {
        this.graveLoot = graveLoot == null ? GraveLootType.NONE : graveLoot;
    }

    public GraveLootType collectGraveLoot() {
        GraveLootType loot = this.graveLoot;
        this.graveLoot = GraveLootType.NONE;
        return loot;
    }

    // terrain masir projectile mostaghim ro ghabl az target migire
    public boolean intercept(Projectile projectile) {
        if (!this.isBlockingTerrain() || projectile == null || projectile.isLobbed()) {
            return false;
        }

        if (this.terrainType == TerrainType.FROZEN && projectile.getType() == ProjectileType.FIRE) {
            this.setTerrainType(TerrainType.CLASSIC);
            return true;
        }

        int damage = DamageExpressionParser.isInstantKill(projectile.getDamageExpression())
                ? this.terrainHealth
                : DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
        this.damageTerrain(damage);
        return true;
    }

    public void onEnvironmentTick(Board board) {
        if (this.terrainType != TerrainType.FROZEN || board == null
                || ++this.environmentTicks < TICKS_PER_SECOND) {
            return;
        }

        this.environmentTicks = 0;

        if (this.hasAdjacentFirePlant(board)) {
            this.damageTerrain(FIRE_THAW_DAMAGE_PER_SECOND);
        }
    }

    public void damageTerrain(int damage) {
        if (!this.isBlockingTerrain() || damage <= 0) {
            return;
        }

        this.terrainHealth -= damage;

        if (this.terrainHealth <= 0) {
            this.setTerrainType(TerrainType.CLASSIC);
        }
    }

    private boolean isBlockingTerrain() {
        return this.terrainType == TerrainType.GRAVE || this.terrainType == TerrainType.FROZEN;
    }

    private void updateOccupantState() {
        if (this.plants != null) {
            for (Plant plant : this.plants) {
                if (plant != null) {
                    plant.setTerrainDisabled(this.terrainType == TerrainType.FROZEN
                            && !this.isTerrainRemover(plant));
                }
            }
        }

        if (this.zombies != null) {
            for (Zombie zombie : this.zombies) {
                if (zombie != null) {
                    zombie.setTerrainFrozen(this.terrainType == TerrainType.FROZEN);
                }
            }
        }
    }

    private boolean hasAdjacentFirePlant(Board board) {
        if (this.position == null) {
            return false;
        }

        for (Plant plant : board.getPlantsInRadius(this.position, 1)) {
            if (plant == null || plant.isDead() || plant.isDisabled() || plant.getPosition() == null
                    || plant.getTags() == null || !plant.getTags().contains(PlantTag.FIRE)) {
                continue;
            }

            int deltaX = Math.abs(plant.getPosition().getX() - this.position.getX());
            int deltaY = Math.abs(plant.getPosition().getY() - this.position.getY());

            if (deltaX <= 1 && deltaY <= 1 && deltaX + deltaY > 0) {
                return true;
            }
        }

        return false;
    }

    private boolean isTerrainRemover(Plant plant) {
        if (plant == null || plant.getName() == null) {
            return false;
        }

        String name = plant.getName().trim().toLowerCase(java.util.Locale.ROOT);
        return name.contains("hot potato") || name.contains("grave buster");
    }
}
