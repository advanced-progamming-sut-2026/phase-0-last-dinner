package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.TerrainType;
import model.zombie.Zombie;

public class GraveSummonerBehavior implements ZombieBehavior {
    private int graveCount;
    private long summonIntervalTicks;
    private long ticksSinceLastSummon;

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

        board.placeTerrainNear(zombie.getPosition(), TerrainType.GRAVE, this.graveCount);
    }
}
