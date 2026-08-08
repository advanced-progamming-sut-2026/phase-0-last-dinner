package model.level;

import model.Plant;
import model.chapters.Chapter;
import model.mechanism.Board;
import model.mechanism.Wave;

import java.util.ArrayList;
import java.util.List;

public class LoveYourPlantsLevel extends Level {
    private static final int WAVE_COUNT = 4;
    private final int maximumDestroyedPlants = 5;
    private int destroyedPlantCount;

    public LoveYourPlantsLevel(Chapter chapter, List<Plant> allowedPlants, double baseDifficulty) {
        super(LevelType.LOVE_YOUR_PLANTS, chapter, allowedPlants, baseDifficulty);
        this.destroyedPlantCount = 0;
    }

    public void recordDestroyedPlant() {
        this.destroyedPlantCount++;
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
        this.destroyedPlantCount = 0;
        List<Wave> waves = getWaves();
        if (waves != null && !waves.isEmpty()) {
            waves.get(0).start();
        }
    }

    @Override
    public boolean isWinConditionMet() {
        return this.areAllWavesDefeated();
    }

    @Override
    public boolean isLoseConditionMet() {
        if (this.destroyedPlantCount >= this.maximumDestroyedPlants) {
            return true;
        }

        Board board = getBoard();

        if (board == null) {
            return false;
        }

        return board.isBrainEaten();
    }
}
