package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.SunSystem;
import model.zombie.Zombie;

public class SunStealerBehavior implements ZombieBehavior {
    private int stealAmount;
    private SunSystem sunSystem;
    private long ticksSinceLastSteal;
    private long stealIntervalTicks = 30;
    private int maximumStolenSun = Integer.MAX_VALUE;
    private int stolenSun;

    public SunStealerBehavior(int stealAmount, SunSystem sunSystem) {
        this.stealAmount = stealAmount;
        this.sunSystem = sunSystem;
    }

    public SunStealerBehavior(
            int stealAmount,
            long stealIntervalTicks,
            int maximumStolenSun,
            double deathRefundFraction,
            SunSystem sunSystem
    ) {
        this(stealAmount, sunSystem);
        this.stealIntervalTicks = Math.max(1, stealIntervalTicks);
        this.maximumStolenSun = Math.max(0, maximumStolenSun);
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
    public void attack(Zombie zombie, Plant plant, Board board) {
        this.activate(zombie, board);
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

        int remainingCapacity = Math.max(0, this.maximumStolenSun - this.stolenSun);
        int amount = Math.min(Math.max(0, this.stealAmount), remainingCapacity);
        this.stolenSun += system.stealGroundSun(amount);
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
