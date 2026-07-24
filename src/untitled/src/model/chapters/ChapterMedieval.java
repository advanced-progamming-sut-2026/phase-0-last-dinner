package model.chapters;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.*;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class ChapterMedieval extends Chapter {
    private ArrayList<Tile> necromancyGraves;
    private static final int MIN_GRAVES_PER_WAVE = 1;
    private static final int MAX_GRAVES_PER_WAVE = 3;
    private static final double SUN_LOOT_CHANCE = 0.35;
    private static final double PLANT_FOOD_LOOT_CHANCE = 0.30;
    private static final double NECROMANCY_ZOMBIE_CHANCE = 0.7;
    private final Random random = new Random();
    private GameEventListener listener;
    private ZombieSpawner zombieSpawner;

    public ChapterMedieval() {
        super(ChapterType.MEDIEVAL);
        this.necromancyGraves = new ArrayList<>();
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }

    @Override
    public Board buildBoard() {
        this.necromancyGraves.clear();
        List<Tile> tiles = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                TerrainType type = isNecromancy(col, row)
                        ? TerrainType.NECROMANCY
                        : TerrainType.CLASSIC;
                Tile tile = new Tile(new Position(col, row), type);
                if (type == TerrainType.NECROMANCY) necromancyGraves.add(tile);
                tiles.add(tile);
            }
        }
        // SunSystem باید غیرفعال بشه
        return new Board(tiles);
    }

    private boolean isNecromancy(int col, int row) {
        return (col == 3 || col == 5) && (row == 0 || row == 4);
    }

    @Override
    public void onWaveStart(Board board, Wave wave) {
        if (board != null && board.getSunSystem() != null) {
            board.getSunSystem().setSkySunEnabled(false);
        }

        this.spawnGrave(board);
        this.spawnNecromancyZombies(board, wave);
    }

    public void spawnGrave(Board board) {
        if (board == null) {
            return;
        }

        List<Tile> eligibleTiles = new ArrayList<>();

        for (Tile tile : board.getTiles()) {
            if (tile == null) {
                continue;
            }

            boolean isEmptyGround = tile.getTerrainType() == TerrainType.CLASSIC
                    || tile.getTerrainType() == TerrainType.NECROMANCY;
            boolean hasNoPlant = tile.getPlants() == null || tile.getPlants().isEmpty();
            boolean hasNoZombie = tile.getZombies() == null || tile.getZombies().isEmpty();

            if (isEmptyGround && hasNoPlant && hasNoZombie) {
                eligibleTiles.add(tile);
            }
        }

        if (eligibleTiles.isEmpty()) {
            return;
        }

        Collections.shuffle(eligibleTiles, this.random);

        int graveCount = MIN_GRAVES_PER_WAVE
                + this.random.nextInt(MAX_GRAVES_PER_WAVE - MIN_GRAVES_PER_WAVE + 1);
        graveCount = Math.min(graveCount, eligibleTiles.size());

        for (int i = 0; i < graveCount; i++) {
            Tile tile = eligibleTiles.get(i);
            tile.setTerrainType(TerrainType.GRAVE);
            tile.setGraveLoot(this.rollGraveLoot());

            this.fireEvent("A new grave has formed at position ("
                    + (tile.getPosition().getX() + 1) + ", "
                    + (tile.getPosition().getY() + 1) + ").");
        }
    }
    private GraveLootType rollGraveLoot() {
        double roll = this.random.nextDouble();

        if (roll < SUN_LOOT_CHANCE) {
            return GraveLootType.SUN;
        }

        if (roll < SUN_LOOT_CHANCE + PLANT_FOOD_LOOT_CHANCE) {
            return GraveLootType.PLANT_FOOD;
        }

        return GraveLootType.NONE;
    }

    public void spawnNecromancyZombies(Board board) {
        this.spawnNecromancyZombies(board, null);
    }

    private void spawnNecromancyZombies(Board board, Wave wave) {
        if (board == null || this.zombieSpawner == null) {
            return;
        }

        for (Tile tile : this.necromancyGraves) {
            if (tile == null || tile.getPosition() == null
                    || tile.getTerrainType() != TerrainType.GRAVE) {
                continue;
            }

            if (this.random.nextDouble() >= NECROMANCY_ZOMBIE_CHANCE) {
                continue;
            }

            ZombieDefinition definition = this.zombieSpawner.chooseRandomSpawnableDefinition();

            if (definition == null) {
                continue;
            }

            Zombie zombie = this.zombieSpawner.getZombieFactory().create(definition, tile.getPosition());

            if (zombie == null) {
                continue;
            }

            zombie.applyDifficulty(this.zombieSpawner.getDifficultyConfig().getMultiplier());
            board.addZombie(zombie, tile.getPosition());
            if (wave != null) {
                wave.addZombie(zombie);
            }
            this.fireEvent("A " + definition.getDisplayName()
                    + " rises from a grave at position ("
                    + (tile.getPosition().getX() + 1) + ", "
                    + (tile.getPosition().getY() + 1) + ").");
        }
    }
}
