package model.level;

import model.Plant;
import model.chapters.Chapter;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Wave;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class DeadlineLevel extends Level {
    private final int deadlineX = 3;
    private static final int WAVE_COUNT = 4;

    public DeadlineLevel(Chapter chapter, List<Plant> allowedPlants, double baseDifficulty) {
        super(LevelType.DEADLINE, chapter, allowedPlants, baseDifficulty);
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

    public boolean hasZombieCrossedDeadline() {
        Board board = getBoard();

        if (board == null) {
            return false;
        }

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }
            Position position = zombie.getPosition();
            if (position != null && zombie.getExactX() <= this.deadlineX) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        setStarted(true);

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
        return hasZombieCrossedDeadline();
    }
}
