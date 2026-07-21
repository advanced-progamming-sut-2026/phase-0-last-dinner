package model.chapters;

import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChapterAncientEgypt extends Chapter {
    private static final int DEFAULT_ZOMBIE_ENTRY_COLUMN = 8;
    private static final int MIN_TORNADO_COLUMNS_AHEAD = 1;
    private static final int MAX_TORNADO_COLUMNS_AHEAD = 4;
    private static final double TORNADO_CHANCE = 70;
    private Random random = new Random();

    public ChapterAncientEgypt() {
        super(ChapterType.ANCIENT_EGYPT);
    }
    @Override
    public Board buildBoard() {
        List<Tile> tiles = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                TerrainType type = isGravePosition(col, row)
                        ? TerrainType.GRAVE
                        : TerrainType.CLASSIC;
                tiles.add(new Tile(new Position(col, row), type));
            }
        }
        return new Board(tiles);
    }
    private boolean isGravePosition(int col, int row) {
        return (col == 7 || col == 5) && (row == 1 || row == 4);
    }
    @Override
    public Position resolveZombieSpawnPosition(int row, boolean isFinalWave) {
        if (isFinalWave && this.random.nextInt(100) < TORNADO_CHANCE) {
            return spawnTornado(row);
        }

        return super.resolveZombieSpawnPosition(row, isFinalWave);
    }
    public Position spawnTornado(int row) {
        int columnsAhead = MIN_TORNADO_COLUMNS_AHEAD
                + this.random.nextInt(MAX_TORNADO_COLUMNS_AHEAD - MIN_TORNADO_COLUMNS_AHEAD + 1);
        return new Position(DEFAULT_ZOMBIE_ENTRY_COLUMN - columnsAhead, row);
    }
}
