package model.mechanism;
import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.plant.PlantCategory;
import model.plant.Projectile;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final int rowCount = 5;
    private final int columnCount = 9;
    @Getter
    private List<Tile> tiles;
    private List<LawnMower> lawnMowers;
    @Getter
    @Setter
    private SunSystem sunSystem;
    @Getter
    private List<Projectile> projectiles = new ArrayList<>();
    @Getter
    @Setter
    private CombatSystem combatSystem;
    public Board(List<Tile> tiles) {
        this.tiles = tiles;
        this.lawnMowers = new ArrayList<>();
    }
    public void addProjectile(Projectile projectile) {
        if (projectile == null) {
            return;
        }

        this.projectiles.add(projectile);
    }

    public void addZombie(Zombie zombie, Position position) {
        if (zombie == null || position == null) {
            return;
        }

        Tile tile = this.getTile(position);

        if (tile == null) {
            return;
        }

        zombie.setPosition(position);
        zombie.setBoard(this);
        tile.addZombie(zombie);
    }

    public boolean moveZombie(Zombie zombie, Position destination) {
        if (zombie == null || destination == null) {
            return false;
        }

        Tile destinationTile = this.getTile(destination);

        if (destinationTile == null) {
            return false;
        }

        Tile sourceTile = this.getTile(zombie.getPosition());

        if (sourceTile != null) {
            sourceTile.removeZombie(zombie);
        }

        zombie.setPosition(destination);
        zombie.setBoard(this);
        destinationTile.addZombie(zombie);
        return true;
    }

    public List<Plant> getPlantsAt(Position position) {
        List<Plant> plantsAtPosition = new ArrayList<>();

        if (position == null) {
            return plantsAtPosition;
        }

        Tile tile = this.getTile(position);

        if (tile == null || tile.getPlants() == null) {
            return plantsAtPosition;
        }

        plantsAtPosition.addAll(tile.getPlants());
        return plantsAtPosition;
    }
    public List<Zombie> getZombiesInRadius(Position center, int radius) {
        List<Zombie> zombiesInRadius = new ArrayList<>();

        if (center == null || this.tiles == null) {
            return zombiesInRadius;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getZombies() == null) {
                continue;
            }

            int deltaX = Math.abs(tile.getPosition().getX() - center.getX());
            int deltaY = Math.abs(tile.getPosition().getY() - center.getY());

            if (deltaX <= radius && deltaY <= radius) {
                zombiesInRadius.addAll(tile.getZombies());
            }
        }

        return zombiesInRadius;
    }

    public List<Zombie> getZombiesAt(Position position) {
        List<Zombie> zombiesAtPosition = new ArrayList<>();

        if (position == null) {
            return zombiesAtPosition;
        }

        Tile tile = this.getTile(position);

        if (tile == null || tile.getZombies() == null) {
            return zombiesAtPosition;
        }

        zombiesAtPosition.addAll(tile.getZombies());

        return zombiesAtPosition;
    }

    public List<Plant> getPlantsInRadius(Position center, int radius) {
        List<Plant> plantsInRadius = new ArrayList<>();

        if (center == null || this.tiles == null) {
            return plantsInRadius;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getPlants() == null) {
                continue;
            }

            int deltaX = Math.abs(tile.getPosition().getX() - center.getX());
            int deltaY = Math.abs(tile.getPosition().getY() - center.getY());

            if (deltaX <= radius && deltaY <= radius) {
                plantsInRadius.addAll(tile.getPlants());
            }
        }

        return plantsInRadius;
    }

    public List<Plant> getPlantsByCategory(PlantCategory category) {
        List<Plant> plantsByCategory = new ArrayList<>();

        if (category == null || this.tiles == null) {
            return plantsByCategory;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPlants() == null) {
                continue;
            }

            for (Plant plant : tile.getPlants()) {
                if (plant != null && plant.getCategories() != null && plant.getCategories().contains(category)) {
                    plantsByCategory.add(plant);
                }
            }
        }

        return plantsByCategory;
    }

    public List<Plant> getPlantsByName(String name) {
        List<Plant> plantsByName = new ArrayList<>();

        if (name == null || this.tiles == null) {
            return plantsByName;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPlants() == null) {
                continue;
            }

            for (Plant plant : tile.getPlants()) {
                if (plant != null && name.equals(plant.getName())) {
                    plantsByName.add(plant);
                }
            }
        }

        return plantsByName;
    }

    public List<Plant> getAllPlants() {
        List<Plant> allPlants = new ArrayList<>();

        if (this.tiles == null) {
            return allPlants;
        }

        for (Tile tile : this.tiles) {
            if (tile != null && tile.getPlants() != null) {
                allPlants.addAll(tile.getPlants());
            }
        }

        return allPlants;
    }

    public List<Plant> getPlantsInLane(Position position) {
        List<Plant> plantsInLane = new ArrayList<>();

        if (position == null || this.tiles == null) {
            return plantsInLane;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getPlants() == null) {
                continue;
            }

            if (tile.getPosition().getY() == position.getY()) {
                plantsInLane.addAll(tile.getPlants());
            }
        }

        return plantsInLane;
    }

    public Plant getNearestPlant(Position position) {
        Plant nearestPlant = null;
        int nearestDistance = Integer.MAX_VALUE;

        if (position == null || this.tiles == null) {
            return null;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getPlants() == null) {
                continue;
            }

            int distance = this.getSquaredDistance(tile.getPosition(), position);

            for (Plant plant : tile.getPlants()) {
                if (plant != null && distance < nearestDistance) {
                    nearestPlant = plant;
                    nearestDistance = distance;
                }
            }
        }

        return nearestPlant;
    }

    public List<Zombie> getAllZombies() {
        List<Zombie> allZombies = new ArrayList<>();

        if (this.tiles == null) {
            return allZombies;
        }

        for (Tile tile : this.tiles) {
            if (tile != null && tile.getZombies() != null) {
                allZombies.addAll(tile.getZombies());
            }
        }

        return allZombies;
    }

    public List<Zombie> getZombiesInLane(Position position) {
        List<Zombie> zombiesInLane = new ArrayList<>();

        if (position == null || this.tiles == null) {
            return zombiesInLane;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getZombies() == null) {
                continue;
            }

            if (tile.getPosition().getY() == position.getY()) {
                zombiesInLane.addAll(tile.getZombies());
            }
        }

        return zombiesInLane;
    }

    public List<Zombie> getZombiesInFrontAndBack(Position position, int range) {
        List<Zombie> zombies = new ArrayList<>();

        if (position == null || this.tiles == null) {
            return zombies;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getZombies() == null) {
                continue;
            }

            if (tile.getPosition().getY() != position.getY()) {
                continue;
            }

            int deltaX = Math.abs(tile.getPosition().getX() - position.getX());

            if (deltaX > 0 && deltaX <= range) {
                zombies.addAll(tile.getZombies());
            }
        }

        return zombies;
    }

    public Zombie getNearestZombie(Position position) {
        Zombie nearestZombie = null;
        int nearestDistance = Integer.MAX_VALUE;

        if (position == null || this.tiles == null) {
            return null;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getZombies() == null) {
                continue;
            }

            int distance = this.getSquaredDistance(tile.getPosition(), position);

            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && distance < nearestDistance) {
                    nearestZombie = zombie;
                    nearestDistance = distance;
                }
            }
        }

        return nearestZombie;
    }

    public List<Zombie> getNearestZombies(Position position, int limit) {
        List<Zombie> zombies = this.getAllZombies();

        if (position == null || zombies.isEmpty() || limit <= 0) {
            return new ArrayList<>();
        }

        zombies.sort((first, second) -> Integer.compare(
                this.getSquaredDistanceOrMax(first.getPosition(), position),
                this.getSquaredDistanceOrMax(second.getPosition(), position)
        ));

        if (zombies.size() <= limit) {
            return zombies;
        }

        return new ArrayList<>(zombies.subList(0, limit));
    }

    public Tile getTile(Position position) {
        if (position == null || this.tiles == null) {
            return null;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }

            if (tile.getPosition().getX() == position.getX()
                    && tile.getPosition().getY() == position.getY()) {
                return tile;
            }
        }

        return null;
    }

    private int getSquaredDistance(Position first, Position second) {
        int deltaX = first.getX() - second.getX();
        int deltaY = first.getY() - second.getY();

        return deltaX * deltaX + deltaY * deltaY;
    }

    private int getSquaredDistanceOrMax(Position first, Position second) {
        if (first == null || second == null) {
            return Integer.MAX_VALUE;
        }

        return this.getSquaredDistance(first, second);
    }

}
