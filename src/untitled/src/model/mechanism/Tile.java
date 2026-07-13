package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.plant.PlantTag;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Tile {
    private Position position;
    private TerrainType terrainType;
    private List<Plant> plants;
    private List<Zombie> zombies;

    public Tile() {
        this(null, TerrainType.CLASSIC);
    }

    public Tile(Position position, TerrainType terrainType) {
        this.position = position;
        this.terrainType = terrainType;
        this.plants = new ArrayList<>();
        this.zombies = new ArrayList<>();
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
                || this.terrainType == TerrainType.FROZEN) {
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
        }
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null) {
            this.zombies.add(zombie);
        }
    }

    public boolean removePlant(Plant plant) {
        return plant != null && this.plants != null && this.plants.remove(plant);
    }

    public Plant removeTopPlant() {
        if (this.plants == null || this.plants.isEmpty()) {
            return null;
        }

        return this.plants.remove(this.plants.size() - 1);
    }

    public boolean removeZombie(Zombie zombie) {
        return zombie != null && this.zombies != null && this.zombies.remove(zombie);
    }
}
