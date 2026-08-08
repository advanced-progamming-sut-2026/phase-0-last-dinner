package model.level;

import model.Plant;
import model.chapters.Chapter;
import model.mechanism.Board;
import model.mechanism.Wave;

import java.util.ArrayList;
import java.util.List;

public class NightOpsLevel extends Level {
    private static final int WAVE_COUNT = 4;
    private final boolean skySunEnabled = false;

    public NightOpsLevel(Chapter chapter, List<Plant> allowedPlants, double baseDifficulty) {
        super(LevelType.NIGHT_OPS, chapter, allowedPlants, baseDifficulty);
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

        Board board = getBoard();
        if (board != null && board.getSunSystem() != null) {
            board.getSunSystem().setSkySunEnabled(this.skySunEnabled);
        }

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
        Board board = getBoard();

        if (board == null) {
            return false;
        }

        return board.isBrainEaten();
    }
}
