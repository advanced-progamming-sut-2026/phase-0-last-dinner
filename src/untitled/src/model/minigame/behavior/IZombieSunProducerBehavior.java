package model.minigame.behavior;

import lombok.Getter;
import model.Plant;
import model.mechanism.Board;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

@Getter

public class IZombieSunProducerBehavior implements ZombieBehavior {

    private static final long DEFAULT_INTERVAL_DECREASE_TICKS = 5;

    private final IZombieMiniGame miniGame;
    private final int sunAmount;
    private final long initialProductionIntervalTicks;
    private final long minimumProductionIntervalTicks;
    private final long intervalDecreaseTicks;

    private long ticksSinceLastProduction;
    private int productionCount;

    public IZombieSunProducerBehavior(
            IZombieMiniGame miniGame,
            int sunAmount,
            long initialProductionIntervalTicks,
            long minimumProductionIntervalTicks
    ) {
        this(
                miniGame,
                sunAmount,
                initialProductionIntervalTicks,
                minimumProductionIntervalTicks,
                DEFAULT_INTERVAL_DECREASE_TICKS
        );
    }

    public IZombieSunProducerBehavior(
            IZombieMiniGame miniGame,
            int sunAmount,
            long initialProductionIntervalTicks,
            long minimumProductionIntervalTicks,
            long intervalDecreaseTicks
    ) {
        if (miniGame == null) {
            throw new IllegalArgumentException("Mini game cannot be null.");
        }

        if (sunAmount <= 0) {
            throw new IllegalArgumentException("Sun amount must be positive.");
        }

        if (initialProductionIntervalTicks <= 0) {
            throw new IllegalArgumentException(
                    "Initial production interval must be positive."
            );
        }

        if (minimumProductionIntervalTicks <= 0) {
            throw new IllegalArgumentException(
                    "Minimum production interval must be positive."
            );
        }

        if (minimumProductionIntervalTicks > initialProductionIntervalTicks) {
            throw new IllegalArgumentException(
                    "Minimum production interval cannot be greater than initial interval."
            );
        }

        if (intervalDecreaseTicks < 0) {
            throw new IllegalArgumentException(
                    "Interval decrease cannot be negative."
            );
        }

        this.miniGame = miniGame;
        this.sunAmount = sunAmount;
        this.initialProductionIntervalTicks = initialProductionIntervalTicks;
        this.minimumProductionIntervalTicks = minimumProductionIntervalTicks;
        this.intervalDecreaseTicks = intervalDecreaseTicks;

        resetProduction();
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (!miniGame.isStarted() || miniGame.isCompleted()) {
            return;
        }

        ticksSinceLastProduction++;

        if (ticksSinceLastProduction < getCurrentProductionIntervalTicks()) {
            return;
        }

        miniGame.addSun(sunAmount);

        ticksSinceLastProduction = 0;
        productionCount++;
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        // TODO Zombie module:
        // Sun producer behavior does not define attack logic.
        // BasicZombieBehavior must handle normal zombie attacks.
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        resetProduction();
    }

    public void resetProduction() {
        ticksSinceLastProduction = 0;
        productionCount = 0;
    }

    public long getCurrentProductionIntervalTicks() {
        long decreasedTicks = (long) productionCount * intervalDecreaseTicks;
        long currentInterval =
                initialProductionIntervalTicks - decreasedTicks;

        return Math.max(
                minimumProductionIntervalTicks,
                currentInterval
        );
    }

}