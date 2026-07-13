package model.chapters;

import model.mechanism.Board;import model.mechanism.Position;import model.mechanism.TerrainType;import model.mechanism.Tile;
import model.zombie.Zombie;

import java.util.ArrayList;import java.util.List;

public class ChapterIceCaves extends Chapter{
    public void spawnIceWind(){}
    public void slip(){}
    private ArrayList<Tile> frozenTiles;
    private ArrayList<Tile> slipperyTiles; // جاهایی که باید زامبی یخ زده باشه رو توی این نگه میدارم
    private ArrayList<Zombie> frozenZombies;
    public ChapterIceCaves() {
        super(ChapterType.ICE_CAVES);
        this.frozenTiles = new ArrayList<>();
        this.slipperyTiles = new ArrayList<>();
        this.frozenZombies = new ArrayList<>();
    }
    @Override
    public Board buildBoard() {
        List<Tile> tiles = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                TerrainType type;
                if (isFrozen(col, row)) {
                    type = TerrainType.FROZEN;
                } else if (isSlipperyUp(col, row)) {
                    type = TerrainType.SLIPPERY_UP;
                } else if (isSlipperyDown(col, row)) {
                    type = TerrainType.SLIPPERY_DOWN;
                } else {
                    type = TerrainType.CLASSIC;
                }
                Tile tile = new Tile(new Position(col, row), type);
                if (type == TerrainType.FROZEN) frozenTiles.add(tile);
                tiles.add(tile);
            }
        }

        Board board = new Board(tiles);
        // این تیکه بعدا برای ایجاد زامبی های مختلف قراره استفاده بشه
        return board;
    }

    private boolean isFrozen(int col, int row) {
        return (col == 7 || col == 6) && (row == 0 || row == 4);
    }

    private boolean isSlipperyUp(int col, int row) {
        return col == 4 && row == 1;
    }
    private boolean isSlipperyDown(int col, int row) {
        return col == 4 && row == 3;
    }

}
