package model.level;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.chapters.Chapter;
import model.mechanism.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
@Getter
@Setter
public class ConveyorBeltLevel extends Level implements Tickable {
    private List<Plant> conveyorPlants;
    private List<Plant> availablePlants;
    private long plantGenerationIntervalTicks;
    private static final int GENERATION_INTERVAL_SECONDS = 12;
    private GameClock gameClock;
    private Random random;
    private long nextGenerationTick;
    public ConveyorBeltLevel(Chapter chapter, List<Plant> availablePlants,
            double baseDifficulty, GameClock gameClock) {
        super(LevelType.CONVEYOR_BELT, chapter, availablePlants, baseDifficulty);
        this.availablePlants = availablePlants;
        this.conveyorPlants = new ArrayList<>();
        this.gameClock = gameClock;
        this.random = new Random();
        this.plantGenerationIntervalTicks = (long) GENERATION_INTERVAL_SECONDS * gameClock.getTicksPerSecond();
    }
    private static final int WAVE_COUNT = 4;
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
    public Plant generateRandomPlant() {
        if (this.availablePlants == null || this.availablePlants.isEmpty()) {
            return null;
        }

        Plant template = this.availablePlants.get(this.random.nextInt(this.availablePlants.size()));
        Plant fresh = template.copyForPlantFood(null);
        fresh.healToFull();

        this.conveyorPlants.add(fresh);
        return fresh;
    }

    @Override
    public void start() {
        setStarted(true);
        this.conveyorPlants.clear();
        this.nextGenerationTick = this.gameClock.getCurrentTick();
        this.generateRandomPlant();
        this.nextGenerationTick += this.plantGenerationIntervalTicks;
        List<Wave> waves = getWaves();
        if (waves != null && !waves.isEmpty()) {
            waves.get(0).start();
        }
    }
    @Override
    public void onTick() {
        if (!isStarted() || isCompleted()) {
            return;
        }
        long currentTick = this.gameClock.getCurrentTick();
        while (currentTick >= this.nextGenerationTick) {
            this.generateRandomPlant();
            this.nextGenerationTick += this.plantGenerationIntervalTicks;
        }
    }
    public Plant takePlantFromConveyor(Plant plant) {
        if (plant == null) {
            return null;
        }

        boolean removed = this.conveyorPlants.remove(plant);
        return removed ? plant : null;
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
}
