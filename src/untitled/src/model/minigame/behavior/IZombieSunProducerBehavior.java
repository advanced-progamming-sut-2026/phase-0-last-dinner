package model.minigame.behavior;

import lombok.Getter;
import model.mechanism.Board;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.zombie.Zombie;
import model.zombie.behavior.ZombieBehavior;

@Getter
public class IZombieSunProducerBehavior
        implements ZombieBehavior {

    private static final long
            DEFAULT_INTERVAL_DECREASE_TICKS = 5;

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
        validateSettings(
                miniGame,
                sunAmount,
                initialProductionIntervalTicks,
                minimumProductionIntervalTicks,
                intervalDecreaseTicks
        );
        this.miniGame = miniGame;
        this.sunAmount = sunAmount;
        this.initialProductionIntervalTicks = initialProductionIntervalTicks;
        this.minimumProductionIntervalTicks = minimumProductionIntervalTicks;
        this.intervalDecreaseTicks = intervalDecreaseTicks;
        resetProduction();
    }

    private static void validateSettings(
            IZombieMiniGame game,
            int amount,
            long initialInterval,
            long minimumInterval,
            long decrease
    ) {
        if (game == null) {
            throw new IllegalArgumentException("Mini game cannot be null.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Sun amount must be positive.");
        }
        if (initialInterval <= 0) {
            throw new IllegalArgumentException("Initial production interval must be positive.");
        }
        if (minimumInterval <= 0) {
            throw new IllegalArgumentException("Minimum production interval must be positive.");
        }
        if (minimumInterval > initialInterval) {
            throw new IllegalArgumentException(
                    "Minimum production interval cannot be greater than initial interval."
            );
        }
        if (decrease < 0) {
            throw new IllegalArgumentException("Interval decrease cannot be negative.");
        }
    }

    @Override
    public void onTick(
            Zombie zombie,
            Board board
    ) {
        if (!canProduceSun(zombie, board)) {
            return;
        }

        ticksSinceLastProduction++;

        long currentInterval =
                getCurrentProductionIntervalTicks();

        if (ticksSinceLastProduction
                < currentInterval) {
            return;
        }

        produceSun();

        ticksSinceLastProduction = 0;
        productionCount++;
    }

    @Override
    public void activate(
            Zombie zombie,
            Board board
    ) {
        resetProduction();
    }

    private boolean canProduceSun(
            Zombie zombie,
            Board board
    ) {
        if (zombie == null
                || zombie.isDead()
                || board == null) {
            return false;
        }

        if (zombie.getBoard() != board) {
            return false;
        }

        return miniGame.isStarted()
                && !miniGame.isCompleted();
    }

    private void produceSun() {
        miniGame.addSun(sunAmount);
    }

    public void resetProduction() {
        ticksSinceLastProduction = 0;
        productionCount = 0;
    }

    public long getCurrentProductionIntervalTicks() {
        long decreasedTicks =
                (long) productionCount
                        * intervalDecreaseTicks;

        long currentInterval =
                initialProductionIntervalTicks
                        - decreasedTicks;

        return Math.max(
                minimumProductionIntervalTicks,
                currentInterval
        );
    }

    public long getTicksUntilNextProduction() {
        long currentInterval =
                getCurrentProductionIntervalTicks();

        return Math.max(
                0,
                currentInterval
                        - ticksSinceLastProduction
        );
    }
}
