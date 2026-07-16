package model.chapters;

import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import java.util.ArrayList;
import java.util.List;
public class ChapterAncientEgypt extends Chapter {
    public void spawnGrave() {
    }

    public void spawnTornado() {
    }
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
}
