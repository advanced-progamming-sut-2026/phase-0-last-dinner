package model.zombie.behavior;

import model.mechanism.Board;
import model.mechanism.TerrainType;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GraveSummonerBehavior implements ZombieBehavior {
    private final int graveCount;
    private final long summonIntervalTicks;
    private long ticksSinceLastSummon;
    private Random random = new Random();

    public GraveSummonerBehavior(int graveCount, long summonIntervalTicks) {
        this.graveCount = Math.max(0, graveCount);
        this.summonIntervalTicks = Math.max(1, summonIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceLastSummon++;

        if (this.ticksSinceLastSummon >= this.summonIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceLastSummon = 0;
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || board == null) {
            return;
        }

        List<Tile> compatibleTiles = new ArrayList<>();

        for (Tile tile : board.getTiles()) {
            if (tile == null || tile.getPosition() == null
                    || tile.getTerrainType() != TerrainType.CLASSIC
                    || !tile.getPlants().isEmpty() || !tile.getZombies().isEmpty()) {
                continue;
            }

            compatibleTiles.add(tile);
        }

        Collections.shuffle(compatibleTiles, this.random);
        int gravesToPlace = Math.min(this.graveCount, compatibleTiles.size());

        for (int index = 0; index < gravesToPlace; index++) {
            Position position = compatibleTiles.get(index).getPosition();
            board.setTerrain(position, TerrainType.GRAVE);
        }
    }

    public void setRandom(Random random) {
        this.random = random == null ? new Random() : random;
    }
}
