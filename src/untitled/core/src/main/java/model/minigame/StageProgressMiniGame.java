package model.minigame;

public interface StageProgressMiniGame {
    int getHighestUnlockedStage();

    void restoreHighestUnlockedStage(int stageNumber);
}
