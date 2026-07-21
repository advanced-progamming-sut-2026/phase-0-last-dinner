package model.level;

import lombok.Getter;
import model.Plant;
import model.chapters.Chapter;
import model.mechanism.Board;
import model.mechanism.GameClock;
import model.mechanism.LawnMower;
import model.mechanism.Tickable;
import model.mechanism.Wave;
import model.mechanism.ZombieKillEvent;
import model.mechanism.ZombieKillObserver;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


@Getter
public class MeowPointLevel extends Level implements Tickable, ZombieKillObserver {
    private static final int WAVE_COUNT = 4;
    private static final int MULTI_KILL_BONUS_PER_EXTRA_ZOMBIE = 10;
    private static final int QUICK_KILL_BONUS = 15;
    private static final int QUICK_KILL_SECONDS_THRESHOLD = 2;
    private static final int SIMULTANEOUS_KILL_BONUS_PER_ZOMBIE = 5;
    private static final int KILL_STREAK_BONUS = 5;
    private static final int KILL_STREAK_GAP_SECONDS = 3;
    private static final int NO_PLANT_LOST_BONUS = 100;

    private final GameClock gameClock;
    private final long quickKillTicksThreshold;
    private final long killStreakGapTicks;
    private final LocalDate challengeDate;
    private final long dailySeed;

    private int point;
    private int destroyedPlantCount;
    private boolean finalScoreCalculated;

    private final Map<Zombie, Long> zombieEntryTicks = new HashMap<>();
    private long lastKillTick = -1;

    private long currentBatchTick = Long.MIN_VALUE;
    private final List<ZombieKillEvent> currentBatch = new ArrayList<>();

    public MeowPointLevel(Chapter chapter, List<Plant> allowedPlants, double baseDifficulty, GameClock gameClock) {
        this(chapter, allowedPlants, baseDifficulty, gameClock, LocalDate.now());
    }

    public MeowPointLevel(
            Chapter chapter,
            List<Plant> allowedPlants,
            double baseDifficulty,
            GameClock gameClock,
            LocalDate challengeDate
    ) {
        super(LevelType.MEOW_POINT, chapter, allowedPlants, baseDifficulty);
        this.gameClock = gameClock;
        this.challengeDate = challengeDate == null ? LocalDate.now() : challengeDate;
        this.dailySeed = this.challengeDate.toEpochDay();
        int ticksPerSecond = 10;
        this.quickKillTicksThreshold = (long) QUICK_KILL_SECONDS_THRESHOLD * ticksPerSecond;
        this.killStreakGapTicks = (long) KILL_STREAK_GAP_SECONDS * ticksPerSecond;
    }

    public List<ZombieDefinition> createDailyZombieOrder(
            List<ZombieDefinition> definitions
    ) {
        List<ZombieDefinition> ordered = new ArrayList<>();
        if (definitions == null) {
            return ordered;
        }

        for (ZombieDefinition definition : definitions) {
            if (definition != null) {
                ordered.add(definition);
            }
        }

        Collections.shuffle(ordered, new Random(this.dailySeed));
        return ordered;
    }

    @Override
    protected List<Wave> buildWaves(double baseDifficulty) {
        List<Wave> waves = new ArrayList<>();

        for (int i = 1; i <= WAVE_COUNT; i++) {
            double difficulty;

            if (i == WAVE_COUNT) {
                difficulty = baseDifficulty * Math.pow(1.25, i - 2) * 2;
            } else {
                difficulty = baseDifficulty * Math.pow(1.25, i - 1);
            }

            waves.add(new Wave(i, difficulty, i == WAVE_COUNT));
        }

        return waves;
    }

    @Override
    public void start() {
        setStarted(true);
        this.point = 0;
        this.destroyedPlantCount = 0;
        this.finalScoreCalculated = false;
        this.zombieEntryTicks.clear();
        this.lastKillTick = -1;
        this.currentBatchTick = Long.MIN_VALUE;
        this.currentBatch.clear();

        List<Wave> waves = getWaves();
        if (waves != null && !waves.isEmpty()) {
            waves.get(0).start();
        }
    }

    public void recordDestroyedPlant() {
        this.destroyedPlantCount++;
    }


    @Override
    public void onTick() {
        Board board = getBoard();

        if (board == null) {
            return;
        }

        long currentTick = this.gameClock != null ? this.gameClock.getCurrentTick() : 0;

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && !this.zombieEntryTicks.containsKey(zombie)) {
                this.zombieEntryTicks.put(zombie, currentTick);
            }
        }
    }

    @Override
    public void onZombieKilled(ZombieKillEvent event) {
        if (event == null || event.getZombie() == null) {
            return;
        }

        if (event.getTick() != this.currentBatchTick) {
            this.flushBatch();
            this.currentBatchTick = event.getTick();
        }

        this.currentBatch.add(event);

        this.scoreQuickKill(event);
        this.scoreKillStreak(event);
    }

    public void calculatePoint() {
        if (this.finalScoreCalculated) {
            return;
        }

        this.flushBatch();

        if (this.destroyedPlantCount == 0) {
            this.addPoints(NO_PLANT_LOST_BONUS);
        }

        this.finalScoreCalculated = true;
    }

    @Override
    public boolean isWinConditionMet() {
        return this.areAllWavesDefeated();
    }

    @Override
    public boolean isLoseConditionMet() {
        Board board = getBoard();

        if (board == null) {
            return false;
        }

        return board.isBrainEaten();

    }

    private void scoreQuickKill(ZombieKillEvent event) {
        Long entryTick = this.zombieEntryTicks.remove(event.getZombie());

        if (entryTick == null) {
            return;
        }

        if (event.getTick() - entryTick <= this.quickKillTicksThreshold) {
            this.addPoints(QUICK_KILL_BONUS);
        }
    }

    private void scoreKillStreak(ZombieKillEvent event) {
        if (this.lastKillTick >= 0 && event.getTick() - this.lastKillTick <= this.killStreakGapTicks) {
            this.addPoints(KILL_STREAK_BONUS);
        }

        this.lastKillTick = event.getTick();
    }
    private void flushBatch() {
        if (this.currentBatch.isEmpty()) {
            return;
        }

        Map<Projectile, List<ZombieKillEvent>> killsByProjectile = new HashMap<>();

        for (ZombieKillEvent event : this.currentBatch) {
            if (event.getProjectile() == null) {
                continue;
            }

            killsByProjectile
                    .computeIfAbsent(event.getProjectile(), key -> new ArrayList<>())
                    .add(event);
        }

        for (List<ZombieKillEvent> group : killsByProjectile.values()) {
            if (group.size() >= 2) {
                this.addPoints(MULTI_KILL_BONUS_PER_EXTRA_ZOMBIE * (group.size() - 1));
            }
        }

        if (this.currentBatch.size() >= 2) {
            this.addPoints(SIMULTANEOUS_KILL_BONUS_PER_ZOMBIE * this.currentBatch.size());
        }

        this.currentBatch.clear();
    }

    private void addPoints(int amount) {
        this.point += amount;
    }
}
