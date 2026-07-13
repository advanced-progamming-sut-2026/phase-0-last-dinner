package model.chapters;

import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;

import java.util.ArrayList;
import java.util.List;

public class ChapterMedieval extends Chapter{
    private ArrayList<Tile> necromancyGraves;
    public ChapterMedieval() {
        super(ChapterType.MEDIEVAL);
        this.necromancyGraves = new ArrayList<>();
    }
    @Override
    public Board buildBoard() {
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
    public void spawnGrave() {
    }
}
