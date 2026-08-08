package model.minigame;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiniGameStage {
    private int stageNumber;
    private double difficultyMultiplier;
    private boolean unlocked;
    private boolean completed;

    public MiniGameStage(
            int stageNumber,
            double difficultyMultiplier,
            boolean unlocked
    ) {
        this.stageNumber = stageNumber;
        this.difficultyMultiplier = difficultyMultiplier;
        this.unlocked = unlocked;
    }
}
