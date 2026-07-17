package model.chapters;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.mechanism.Board;import model.mechanism.Position;import model.mechanism.TerrainType;import model.mechanism.Tile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.ArrayList;import java.util.List;
import java.util.Random;

@Getter
@Setter
public class ChapterIceCaves extends Chapter{
    private final Random random = new Random();
    private ArrayList<Tile> frozenTiles;
    private ArrayList<Tile> slipperyTiles;
    private ArrayList<Zombie> frozenZombies; // جاهایی که باید زامبی یخ زده باشه رو توی این نگه میدارم
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
    public void spawnIceWind(Board board) {
        if (board == null || this.random.nextDouble() >= 0.5) {
            return;
        }

        int affectedRowCount = 1 + this.random.nextInt(5);
        List<Integer> rows = new ArrayList<>();
        for (int row = 0; row < 5; row++) {
            rows.add(row);
        }
        java.util.Collections.shuffle(rows, this.random);

        for (int i = 0; i < affectedRowCount; i++) {
            int row = rows.get(i);
            List<Plant> plantsInRow = board.getPlantsInLane(new Position(0, row));

            for (Plant plant : plantsInRow) {
                if (plant != null && !plant.isDead()) {
                    plant.addFreezeLevel();
                }
            }
        }
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

    @Override
    public void onWaveStart(Board board, model.mechanism.Wave wave) {
        this.spawnIceWind(board);
    }

    public void slip(Zombie zombie) {
        if (zombie == null || zombie.getBoard() == null || zombie.getPosition() == null) {
            return;
        }

        if (zombie.getConditions() != null && zombie.getConditions().contains(ZombieCondition.FLYING)) {
            return;
        }

        Board board = zombie.getBoard();
        Tile currentTile = board.getTile(zombie.getPosition());

        if (currentTile == null) {
            return;
        }

        int rowDelta;
        if (currentTile.getTerrainType() == TerrainType.SLIPPERY_UP) {
            rowDelta = -1;
        } else if (currentTile.getTerrainType() == TerrainType.SLIPPERY_DOWN) {
            rowDelta = 1;
        } else {
            return;
        }

        Position destination = new Position(
                zombie.getPosition().getX(),
                zombie.getPosition().getY() + rowDelta
        );

        board.moveZombie(zombie, destination);
    }

}
