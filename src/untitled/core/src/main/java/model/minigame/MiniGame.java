package model.minigame;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Board;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public abstract class MiniGame {
    private MiniGameType type;
    private List<MiniGameStage> stages;
    private MiniGameStage currentStage;
    private Board board;
    private boolean started;
    private boolean completed;
    private boolean allStagesCompleted;

    protected MiniGame(MiniGameType type) {
        this.type = type;
        this.stages = Arrays.asList(
                new MiniGameStage(1, 1.0, true),
                new MiniGameStage(2, 1.5, false),
                new MiniGameStage(3, 2.0, false)
        );
        this.currentStage = this.stages.get(0);
        this.started = false;
        this.completed = false;
        this.allStagesCompleted = false;
    }

    protected void markStarted() {
        this.started = true;
    }

    protected void markCompleted() {
        this.completed = true;
    }

    protected void markAllStagesCompleted() {
        this.completed = true;
        this.allStagesCompleted = true;
    }

    public abstract void start();

    public abstract void onTick();

    public abstract boolean isWinConditionMet();

    public abstract boolean isLoseConditionMet();
}
