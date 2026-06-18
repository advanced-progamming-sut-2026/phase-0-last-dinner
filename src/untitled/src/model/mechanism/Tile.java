package model.mechanism;

import lombok.Getter;
import model.Plant;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;
@Getter
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
        return plant != null;
    }

    public void addPlant(Plant plant) {
        if (plant == null) {
            return;
        }

        this.plants.add(plant);
    }

    public void addZombie(Zombie zombie) {
        if (zombie == null) {
            return;
        }

        this.zombies.add(zombie);
    }

    public boolean removePlant(Plant plant) {
        if (plant == null || this.plants == null) {
            return false;
        }

        return this.plants.remove(plant);
    }

    public Plant removeTopPlant() {
        if (this.plants == null || this.plants.isEmpty()) {
            return null;
        }

        return this.plants.remove(this.plants.size() - 1);
    }

    public boolean removeZombie(Zombie zombie) {
        if (zombie == null || this.zombies == null) {
            return false;
        }

        return this.zombies.remove(zombie);
    }
}
