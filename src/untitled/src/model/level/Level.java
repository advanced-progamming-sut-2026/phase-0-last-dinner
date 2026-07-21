package model.level;

import lombok.Getter;import lombok.Setter;
import model.Plant;
import model.chapters.Chapter;
import model.mechanism.Board;
import model.mechanism.Wave;

import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
public abstract class Level {
    private LevelType levelType;
    private List<Plant> allowedPlants;
    private List<Wave> waves;
    private Board board;
    private boolean started;
    private boolean completed;

    protected Level(LevelType levelType) {
        this.levelType = levelType;
    }

    protected Level(LevelType levelType, Chapter chapter,
                    List<Plant> allowedPlants, double baseDifficulty) {
        this.levelType = levelType;
        this.board = chapter == null ? new Board() : chapter.buildBoard();
        this.allowedPlants = allowedPlants;
        this.waves = this.buildWaves(baseDifficulty);
    }

    protected List<Wave> buildWaves(double baseDifficulty) {
        return new ArrayList<>();
    }

    protected boolean areAllWavesDefeated() {
        if (this.waves == null || this.waves.isEmpty()) {
            return false;
        }

        Wave lastWave = this.waves.get(this.waves.size() - 1);
        if (lastWave == null || !lastWave.isFinalWave() || !lastWave.isStarted()) {
            return false;
        }

        for (Wave wave : this.waves) {
            if (wave != null && wave.getRemainingHealthPercentage() > 0) {
                return false;
            }
        }

        return this.board == null || !this.board.hasLivingZombies();
    }

    public abstract void start();

    public abstract boolean isWinConditionMet();

    public abstract boolean isLoseConditionMet();
}
