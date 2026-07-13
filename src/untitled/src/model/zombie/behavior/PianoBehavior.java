package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.zombie.Zombie;

import java.util.Random;

public class PianoBehavior implements ZombieBehavior {
    private long danceIntervalTicks;
    private long ticksSinceDance;
    private Random random = new Random();

    public PianoBehavior(long danceIntervalTicks) {
        this.danceIntervalTicks = Math.max(1, danceIntervalTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceDance++;
        if (this.ticksSinceDance >= this.danceIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceDance = 0;
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return;
        }

        for (Zombie dancer : board.getZombiesInRadius(zombie.getPosition(), 3)) {
            if (dancer == null || dancer == zombie || dancer.isDead() || dancer.getPosition() == null) {
                continue;
            }

            int direction = this.random.nextBoolean() ? 1 : -1;
            Position destination = new Position(
                    dancer.getPosition().getX(),
                    dancer.getPosition().getY() + direction
            );
            if (board.getTile(destination) != null) {
                board.moveZombie(dancer, destination);
            }
        }
    }
}
