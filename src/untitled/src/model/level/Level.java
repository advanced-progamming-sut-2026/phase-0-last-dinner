package model.level;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Wave;

import java.util.List;

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

    public abstract void start();

    public abstract boolean isWinConditionMet();

    public abstract boolean isLoseConditionMet();
}
