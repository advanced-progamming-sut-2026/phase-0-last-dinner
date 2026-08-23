package model.zombie.behavior;

import model.mechanism.Board;
import model.mechanism.SunSystem;
import model.zombie.Zombie;

public class SunStealerBehavior implements ZombieBehavior {
    private final int stealAmount;
    private final SunSystem sunSystem;
    private long ticksSinceLastSteal;
    private final long stealIntervalTicks;
    private int stolenSun;

    public SunStealerBehavior(int stealAmount, SunSystem sunSystem) {
        this(stealAmount, 30, sunSystem);
    }

    public SunStealerBehavior(
            int stealAmount,
            long stealIntervalTicks,
            SunSystem sunSystem
    ) {
        this.stealAmount = Math.max(0, stealAmount);
        this.stealIntervalTicks = Math.max(1, stealIntervalTicks);
        this.sunSystem = sunSystem;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        this.ticksSinceLastSteal++;

        if (this.ticksSinceLastSteal >= this.stealIntervalTicks) {
            this.activate(zombie, board);
            this.ticksSinceLastSteal = 0;
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        SunSystem system = this.sunSystem;

        if (system == null && board != null) {
            system = board.getSunSystem();
        }

        if (system == null) {
            return;
        }

        this.stolenSun += system.stealGroundSun(this.stealAmount, Integer.MAX_VALUE);
    }

    /** Returns the amount of ground sun currently held by this zombie for read-only visual sync. */
    public int getStolenSun() {
        return this.stolenSun;
    }

    @Override
    public void onDeath(Zombie zombie, Board board) {
        SunSystem system = this.sunSystem;
        if (system == null && board != null) {
            system = board.getSunSystem();
        }

        if (system != null && this.stolenSun > 0) {
            system.addSun(this.stolenSun);
        }
        this.stolenSun = 0;
    }
}
