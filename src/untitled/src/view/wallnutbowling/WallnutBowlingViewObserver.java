package view.wallnutbowling;

import model.mechanism.Position;
import model.minigame.wallnutbowlingminigame.WallnutBowlingActionResult;
import model.minigame.wallnutbowlingminigame.WallnutBowlingStateResult;

public interface WallnutBowlingViewObserver {
    WallnutBowlingActionResult
    onStartWallnutBowlingRequested(
            int stageNumber
    );

    default WallnutBowlingActionResult
    onStartWallnutBowlingRequested() {
        return onStartWallnutBowlingRequested(1);
    }

    WallnutBowlingActionResult
    onPlaceWallnutRequested(
            int conveyorIndex,
            Position position
    );

    WallnutBowlingActionResult
    onAdvanceTicksRequested(
            int ticks
    );

    WallnutBowlingStateResult
    onShowWallnutBowlingRequested();
}