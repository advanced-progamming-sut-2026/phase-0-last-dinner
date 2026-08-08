package view.beghouled;

import model.mechanism.Position;
import model.minigame.beghouledminigame.BeghouledActionResult;
import model.minigame.beghouledminigame.BeghouledStateResult;

public interface BeghouledViewObserver {

    BeghouledActionResult onStartBeghouledRequested(
            int stageNumber
    );

    BeghouledActionResult onSwapRequested(
            Position first,
            Position second
    );

    BeghouledActionResult onUpgradeRequested(
            String sourcePlantName
    );

    BeghouledActionResult onAdvanceTicksRequested(
            int ticks
    );

    BeghouledStateResult onShowBeghouledRequested();
}