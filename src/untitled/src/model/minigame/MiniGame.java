package model.minigame;

import model.mechanism.Board;

import java.util.Arrays;
import java.util.List;

public abstract class MiniGame {
    private MiniGameType type;
    private List<MiniGameStage> stages;
    private MiniGameStage currentStage;
    private Board board;
    private boolean started;
    private boolean completed;

    protected MiniGame(MiniGameType type) {
        this.type = type;
        this.stages = Arrays.asList(
                new MiniGameStage(1, 1.0, true),
                new MiniGameStage(2, 1.5, false),
                new MiniGameStage(3, 2.0, false)
        );
    }

    public abstract void start();

    public abstract boolean isWinConditionMet();

    public abstract boolean isLoseConditionMet();
}
