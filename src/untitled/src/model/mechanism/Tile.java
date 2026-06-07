package model.mechanism;

import model.Plant;
import model.zombie.Zombie;

import java.util.List;

public class Tile {
    private Position position;
    private TerrainType terrainType;
    private List<Plant> plants;
    private List<Zombie> zombies;

    public boolean canPlacePlant(Plant plant) {
        return false;
    }

    public void addPlant(Plant plant) {
    }

    public Plant removePlant() {
        return null;
    }
}
