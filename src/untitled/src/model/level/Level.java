package model.level;

import lombok.Getter;import lombok.Setter;
import model.Plant;
import model.chapters.Chapter;
import model.mechanism.Board;
import model.mechanism.Wave;

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

    protected Level(LevelType levelType, Chapter chapter,
                    List<Plant> allowedPlants, double baseDifficulty) {
        this.levelType = levelType;
        this.board = chapter.buildBoard();
        this.allowedPlants = allowedPlants;
        this.waves = buildWaves(baseDifficulty);
    }
    protected abstract List<Wave> buildWaves(double baseDifficulty);
    public abstract void start();

    public abstract boolean isWinConditionMet();

    public abstract boolean isLoseConditionMet();
}
