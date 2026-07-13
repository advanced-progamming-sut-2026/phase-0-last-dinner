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
    @Getter
    private List<LawnMower> lawnMowers;
    @Getter
    private boolean brainEaten;
    @Getter
    @Setter
    private SunSystem sunSystem;
    @Getter
    @Setter
    private PlantFoodSystem plantFoodSystem;
    @Getter
    private List<Projectile> projectiles = new ArrayList<>();
    @Getter
    @Setter
    private CombatSystem combatSystem;
    public Board() {
        this(createDefaultTiles());
    }

    public Board(List<Tile> tiles) {
        this.tiles = tiles == null ? createDefaultTiles() : tiles;
        this.lawnMowers = createDefaultLawnMowers();
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

        if (destination.getX() < 0) {
            return this.handleZombieAtHouse(zombie);
        }

        Tile destinationTile = this.getTile(destination);

        if (destinationTile == null) {
            return false;
        }

        Tile sourceTile = this.getTile(zombie.getPosition());

        if (sourceTile != null) {
            sourceTile.removeZombie(zombie);
        }

        zombie.setTilePosition(destination);
        zombie.setBoard(this);
        destinationTile.addZombie(zombie);
        return true;
    }

    public boolean handleZombieAtHouse(Zombie zombie) {
        if (zombie == null || zombie.isDead() || zombie.getPosition() == null) {
            return false;
        }

        LawnMower lawnMower = this.getLawnMower(zombie.getPosition().getY());

        if (lawnMower != null && lawnMower.canTrigger()) {
            List<Zombie> killedZombies = lawnMower.trigger(this.getZombiesInLane(zombie.getPosition()));

            for (Zombie killedZombie : killedZombies) {
                if (this.combatSystem != null) {
                    this.combatSystem.killZombie(killedZombie);
                } else {
                    this.removeZombie(killedZombie);
                }
            }

            if (zombie.isDead()) {
                return true;
            }
        }

        this.brainEaten = true;
        return false;
    }

    public LawnMower getLawnMower(int row) {
        for (LawnMower lawnMower : this.lawnMowers) {
            if (lawnMower != null && lawnMower.getRow() == row) {
                return lawnMower;
            }
        }

        return null;
    }

    public boolean isInsideBoard(Position position) {
        return position != null
                && position.getX() >= 0
                && position.getX() < this.columnCount
                && position.getY() >= 0
                && position.getY() < this.rowCount;
    }

    public boolean removeZombie(Zombie zombie) {
        if (zombie == null || zombie.getPosition() == null) {
            return false;
        }

        Tile tile = this.getTile(zombie.getPosition());

        if (tile == null || !tile.removeZombie(zombie)) {
            return false;
        }

        zombie.setBoard(null);
        return true;
    }

    public boolean movePlant(Plant plant, Position destination) {
        if (plant == null || destination == null) {
            return false;
        }

        Tile destinationTile = this.getTile(destination);

        if (destinationTile == null || !destinationTile.canPlacePlant(plant)) {
            return false;
        }

        Tile sourceTile = this.getTile(plant.getPosition());

        if (sourceTile != null) {
            sourceTile.removePlant(plant);
        }

        plant.setPosition(destination);
        plant.setBoard(this);
        destinationTile.addPlant(plant);
        return true;
    }

    public boolean removePlant(Plant plant) {
        if (plant == null || plant.getPosition() == null) {
            return false;
        }

        Tile tile = this.getTile(plant.getPosition());

        if (tile == null || !tile.removePlant(plant)) {
            return false;
        }

        plant.setPosition(null);
        plant.setBoard(null);
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

    public List<Plant> getPlantsInZombieAttackRange(Position position, int range) {
        List<Plant> plantsInRange = new ArrayList<>();

        if (position == null || this.tiles == null || range < 0) {
            return plantsInRange;
        }

        for (Tile tile : this.tiles) {
            if (tile == null || tile.getPosition() == null || tile.getPosition().getY() != position.getY()
                    || tile.getPlants() == null || tile.getPlants().isEmpty()) {
                continue;
            }

            Plant plant = tile.getPlants().get(tile.getPlants().size() - 1);

            if (plant == null || plant.getPosition() == null || plant.isDead() || plant.isTransformed()) {
                continue;
            }

            int deltaX = position.getX() - plant.getPosition().getX();

            if (deltaX >= 0 && deltaX <= range) {
                plantsInRange.add(plant);
            }
        }

        plantsInRange.sort((first, second) -> Integer.compare(
                Math.abs(first.getPosition().getX() - position.getX()),
                Math.abs(second.getPosition().getX() - position.getX())
        ));

        return plantsInRange;
    }

    public Plant getNearestPlantInZombieAttackRange(Position position, int range) {
        List<Plant> plants = this.getPlantsInZombieAttackRange(position, range);
        return plants.isEmpty() ? null : plants.get(0);
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

    public boolean setTerrain(Position position, TerrainType terrainType) {
        if (position == null || terrainType == null || this.tiles == null) {
            return false;
        }

        for (int i = 0; i < this.tiles.size(); i++) {
            Tile tile = this.tiles.get(i);

            if (tile == null || tile.getPosition() == null) {
                continue;
            }

            if (tile.getPosition().getX() == position.getX()
                    && tile.getPosition().getY() == position.getY()) {
                Tile replacement = new Tile(tile.getPosition(), terrainType);

                for (Plant plant : tile.getPlants()) {
                    replacement.addPlant(plant);
                }

                for (Zombie zombie : tile.getZombies()) {
                    replacement.addZombie(zombie);
                }

                this.tiles.set(i, replacement);
                return true;
            }
        }

        return false;
    }

    public int placeTerrainNear(Position center, TerrainType terrainType, int count) {
        if (center == null || terrainType == null || count <= 0) {
            return 0;
        }

        int placed = 0;

        for (int radius = 0; radius <= Math.max(this.rowCount, this.columnCount) && placed < count; radius++) {
            for (int deltaY = -radius; deltaY <= radius && placed < count; deltaY++) {
                for (int deltaX = -radius; deltaX <= radius && placed < count; deltaX++) {
                    if (Math.abs(deltaX) != radius && Math.abs(deltaY) != radius) {
                        continue;
                    }

                    Position candidate = new Position(center.getX() + deltaX, center.getY() + deltaY);
                    Tile tile = this.getTile(candidate);

                    if (tile == null || tile.getTerrainType() == terrainType
                            || !tile.getPlants().isEmpty() || !tile.getZombies().isEmpty()) {
                        continue;
                    }

                    if (this.setTerrain(candidate, terrainType)) {
                        placed++;
                    }
                }
            }
        }

        return placed;
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

    private static List<Tile> createDefaultTiles() {
        List<Tile> defaultTiles = new ArrayList<>();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 9; x++) {
                defaultTiles.add(new Tile(new Position(x, y), TerrainType.CLASSIC));
            }
        }

        return defaultTiles;
    }

    private static List<LawnMower> createDefaultLawnMowers() {
        List<LawnMower> lawnMowers = new ArrayList<>();

        for (int row = 0; row < 5; row++) {
            lawnMowers.add(new LawnMower(row));
        }

        return lawnMowers;
    }

}
