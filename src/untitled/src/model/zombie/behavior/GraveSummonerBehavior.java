package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.TerrainType;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.zombie.Zombie;

import java.util.Random;

public class GraveSummonerBehavior implements ZombieBehavior {
    private int graveCount;
    private long summonIntervalTicks;
    private long ticksSinceLastSummon;
    private Random random = new Random();

    public GraveSummonerBehavior(int graveCount, long summonIntervalTicks) {
        this.graveCount = graveCount;
        this.summonIntervalTicks = summonIntervalTicks;
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
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || board == null) {
            return;
        }

        int placed = 0;
        int attempts = 0;

        while (placed < this.graveCount && attempts < 90) {
            attempts++;
            Position candidate = new Position(this.random.nextInt(9), this.random.nextInt(5));
            Tile tile = board.getTile(candidate);

            if (tile == null || tile.getTerrainType() == TerrainType.GRAVE
                    || !tile.getPlants().isEmpty() || !tile.getZombies().isEmpty()) {
                continue;
            }

            if (board.setTerrain(candidate, TerrainType.GRAVE)) {
                placed++;
            }
        }
    }
}
