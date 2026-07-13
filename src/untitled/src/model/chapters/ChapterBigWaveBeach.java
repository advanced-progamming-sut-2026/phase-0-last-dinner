package model.chapters;

import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;

import java.util.ArrayList;
import java.util.List;

public class ChapterBigWaveBeach extends Chapter{
    private ArrayList<Tile> waterTiles;
    private ArrayList<Tile> lowBeachTiles;
    private int waterLevel; // اینو برای این اضافه کردم ببینیم تا کدوم ستون اب هست
    public ChapterBigWaveBeach() {
        super(ChapterType.BIG_WAVE_BEACH);
        this.waterTiles = new ArrayList<>();
        this.lowBeachTiles = new ArrayList<>();
        this.waterLevel = 6;
    }
    @Override
    public Board buildBoard() {
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
    private boolean isLowBeach(int col, int row) {
        return col == 5 && (row == 1 || row == 3);
    }
    public void changeWaterLevel(){}
}
