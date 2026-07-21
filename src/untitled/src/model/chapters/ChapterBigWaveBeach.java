package model.chapters;

import model.Plant;
import model.mechanism.*;
import model.plant.PlantTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ChapterBigWaveBeach extends Chapter{
    private int minWaterColumn = 4;
    private ArrayList<Tile> waterTiles;
    private ArrayList<Tile> lowBeachTiles;
    private final Random random = new Random();
    private int waterLevel; // اینو برای این اضافه کردم ببینیم تا کدوم ستون اب هست
    public ChapterBigWaveBeach() {
        super(ChapterType.BIG_WAVE_BEACH);
        this.waterTiles = new ArrayList<>();
        this.lowBeachTiles = new ArrayList<>();
        this.waterLevel = 6;
    }
    @Override
    public Board buildBoard() {
        this.waterTiles.clear();
        this.lowBeachTiles.clear();
        List<Tile> tiles = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                TerrainType type;
                if (isLowBeach(col, row)) {
                    type = TerrainType.LOW_BEACH;
                } else if (col >= waterLevel) {
                    type = TerrainType.WATER;
                } else {
                    type = TerrainType.CLASSIC;
                }
                Tile tile = new Tile(new Position(col, row), type);
                if (type == TerrainType.WATER) waterTiles.add(tile);
                if (type == TerrainType.LOW_BEACH) lowBeachTiles.add(tile);
                tiles.add(tile);
            }
        }
        return new Board(tiles);
    }
    public void changeWaterLevel(Board board) {
        if (board == null) {
            return;
        }

        int range = 8 - this.minWaterColumn + 1;
        int newWaterLevel = this.minWaterColumn + this.random.nextInt(range);

        if (newWaterLevel == this.waterLevel) {
            return;
        }

        this.waterLevel = newWaterLevel;
        this.applyWaterLevel(board);
    }

    @Override
    public void onWaveStart(Board board, Wave wave) {
        this.changeWaterLevel(board);
    }
    private void applyWaterLevel(Board board) {
        for (Tile tile : board.getTiles()) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }

            boolean shouldBeSubmerged = tile.getPosition().getX() >= this.waterLevel;

            if (this.lowBeachTiles.contains(tile)) {
                if (shouldBeSubmerged) {
                    tile.setTerrainType(TerrainType.WATER);
                    if (!this.waterTiles.contains(tile)) {
                        this.waterTiles.add(tile);
                    }
                    this.destroyIncompatiblePlants(board, tile);
                } else if (tile.getTerrainType() == TerrainType.WATER) {
                    tile.setTerrainType(TerrainType.LOW_BEACH);
                    this.waterTiles.remove(tile);
                }
                continue;
            }

            boolean isCurrentlyWater = tile.getTerrainType() == TerrainType.WATER;

            if (shouldBeSubmerged && !isCurrentlyWater) {
                tile.setTerrainType(TerrainType.WATER);

                if (!this.waterTiles.contains(tile)) {
                    this.waterTiles.add(tile);
                }

                this.destroyIncompatiblePlants(board, tile);
            } else if (!shouldBeSubmerged && isCurrentlyWater) {
                tile.setTerrainType(TerrainType.CLASSIC);
                this.waterTiles.remove(tile);
            }
        }
    }
    private boolean isLowBeach(int col, int row) {
        return col == 5 && (row == 1 || row == 3);
    }
    private static final double LOW_BEACH_ZOMBIE_CHANCE = 0.25;

    @Override
    public Position resolveZombieSpawnPosition(int row, boolean isFinalWave) {
        List<Tile> submergedLowBeachTilesInRow = new ArrayList<>();

        for (Tile tile : this.lowBeachTiles) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }

            int col = tile.getPosition().getX();

            if (isLowBeach(col, row) && this.isLowBeachSubmerged(tile)) {
                submergedLowBeachTilesInRow.add(tile);
            }
        }

        if (!submergedLowBeachTilesInRow.isEmpty()
                && this.random.nextDouble() < LOW_BEACH_ZOMBIE_CHANCE) {
            Tile chosenTile = submergedLowBeachTilesInRow.get(
                    this.random.nextInt(submergedLowBeachTilesInRow.size())
            );
            return chosenTile.getPosition();
        }

        return super.resolveZombieSpawnPosition(row, isFinalWave);
    }
    public boolean isLowBeachSubmerged(Tile lowBeachTile) {
        return lowBeachTile != null
                && lowBeachTile.getPosition() != null
                && lowBeachTile.getPosition().getX() >= this.waterLevel;
    }

    private void destroyIncompatiblePlants(Board board, Tile tile) {
        if (tile.getPlants() == null || tile.getPlants().isEmpty()) {
            return;
        }

        List<Plant> plantsToRemove = new ArrayList<>(tile.getPlants());

        for (Plant plant : plantsToRemove) {
            if (plant == null || this.canSurviveOnWater(tile, plant)) {
                continue;
            }

            if (board.getCombatSystem() != null) {
                board.getCombatSystem().destroyPlant(plant);
            } else {
                board.removePlant(plant);
            }
        }
    }
    private boolean canSurviveOnWater(Tile tile, Plant plant) {
        if (plant.getTags() != null && plant.getTags().contains(PlantTag.WATER)) {
            return true;
        }

        String name = plant.getName() == null ? "" : plant.getName().toLowerCase(Locale.ROOT);

        if (name.contains("lily pad") || name.contains("tangle kelp") || name.contains("sea-shroom")) {
            return true;
        }

        for (Plant other : tile.getPlants()) {
            if (other != null && other != plant && "lily pad".equalsIgnoreCase(other.getName())) {
                return true;
            }
        }

        return false;
    }
}
