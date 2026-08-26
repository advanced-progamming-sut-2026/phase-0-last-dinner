package model.minigame.izombieminigame.multiplayer;

import network.izombie.server.IZombieMatchFactory;
import network.izombie.server.IZombieMatchLifecycle;
import network.izombie.server.IZombieMatchSession;
import network.izombie.server.IZombieServerTransport;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class IZombieAuthoritativeMatchFactory implements IZombieMatchFactory, AutoCloseable {

    private static final long TICK_INTERVAL_MILLIS = 1000L / IZombieMatchRules.TICKS_PER_SECOND;

    private final ScheduledExecutorService scheduler;
    private final Supplier<IZombieMultiplayerIntegration> integrationFactory;
    private final Map<String, ScheduledFuture<?>> scheduledTicks;

    public IZombieAuthoritativeMatchFactory() {
        this(PlantZombieIZombieMultiplayerIntegration::new, createDefaultScheduler());
    }

    public IZombieAuthoritativeMatchFactory(Supplier<IZombieMultiplayerIntegration> integrationFactory) {
        this(integrationFactory, createDefaultScheduler());
    }

    public IZombieAuthoritativeMatchFactory(Supplier<IZombieMultiplayerIntegration> integrationFactory,
                                            ScheduledExecutorService scheduler) {
        this.integrationFactory = Objects.requireNonNull(integrationFactory);

        this.scheduler = Objects.requireNonNull(scheduler);
        this.scheduledTicks = new ConcurrentHashMap<>();
    }

    @Override
    public IZombieMatchSession create(String matchId, String plantUsername, String zombieUsername, int stageNumber,
                                      IZombieServerTransport transport, IZombieMatchLifecycle lifecycle) {
        requireUniqueMatchId(matchId);

        IZombieMultiplayerIntegration integration = Objects.requireNonNull(integrationFactory.get(),
            "Integration factory returned null.");

        IZombieMatchLifecycle wrappedLifecycle = createWrappedLifecycle(lifecycle);

        IZombieAuthoritativeMatchSession session = new IZombieAuthoritativeMatchSession(matchId, plantUsername,
            zombieUsername, stageNumber, integration, transport, wrappedLifecycle);

        scheduleTicks(session);

        return session;
    }

    private IZombieMatchLifecycle createWrappedLifecycle(IZombieMatchLifecycle originalLifecycle) {
        Objects.requireNonNull(originalLifecycle);

        return (matchId, plantUsername, zombieUsername) -> {
            cancelScheduledTicks(matchId);

            originalLifecycle.onMatchClosed(matchId, plantUsername, zombieUsername);
        };
    }

    private void scheduleTicks(IZombieAuthoritativeMatchSession session) {
        Runnable tickTask = () -> runTickSafely(session);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(tickTask, TICK_INTERVAL_MILLIS, TICK_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS);

        ScheduledFuture<?> previous = scheduledTicks.putIfAbsent(session.getMatchId(), future);

        if (previous != null) {
            future.cancel(false);

            throw new IllegalStateException("A scheduled match already exists with ID: " + session.getMatchId());
        }
    }

    private void runTickSafely(IZombieAuthoritativeMatchSession session) {
        try {
            session.advanceOneTick();

            if (session.isClosed()) {
                cancelScheduledTicks(session.getMatchId());
            }
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            session.cancelBecauseOfServerError();
            cancelScheduledTicks(session.getMatchId());
        }
    }

    private void requireUniqueMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("Match ID cannot be empty.");
        }

        if (scheduledTicks.containsKey(matchId)) {
            throw new IllegalStateException("A match already exists with ID: " + matchId);
        }
    }

    private void cancelScheduledTicks(String matchId) {
        if (matchId == null) {
            return;
        }

        ScheduledFuture<?> future = scheduledTicks.remove(matchId);

        if (future != null) {
            future.cancel(false);
        }
    }

    public int getRunningMatchCount() {
        return scheduledTicks.size();
    }

    @Override
    public void close() {
        for (ScheduledFuture<?> future : scheduledTicks.values()) {

            if (future != null) {
                future.cancel(false);
            }
        }

        scheduledTicks.clear();
        scheduler.shutdownNow();
    }

    private static ScheduledExecutorService createDefaultScheduler() {
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        return Executors.newScheduledThreadPool(threadCount, new IZombieThreadFactory());
    }

    private static final class IZombieThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "izombie-server-tick-" + threadNumber.incrementAndGet());

            thread.setDaemon(true);
            return thread;
        }
    }
}
