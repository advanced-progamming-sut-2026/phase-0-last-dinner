package model.level;

import lombok.Getter;import lombok.Setter;import model.Plant;
import model.chapters.Chapter;import model.mechanism.Board;
import model.mechanism.Tile;import model.mechanism.Wave;

import java.util.ArrayList;import java.util.List;
@Setter
@Getter
public class NormalLevel extends Level {
    private static final int WAVE_COUNT = 4;
    public NormalLevel(Chapter chapter, List<Plant> allowedPlants, double baseDifficulty) {
        super(LevelType.NORMAL, chapter, allowedPlants, baseDifficulty);
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

    }

    @Override
    public boolean isWinConditionMet() {
        List<Wave> waves = getWaves();
        if (waves == null) return false;
        Wave lastWave = waves.get(waves.size() - 1);
        return lastWave.isStarted()
                && lastWave.getRemainingHealthPercentage() == 0;
    }

    @Override
    public boolean isLoseConditionMet() {
        return false;
    }
}
