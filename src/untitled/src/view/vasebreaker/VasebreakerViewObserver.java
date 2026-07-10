package view.vasebreaker;

import model.mechanism.Position;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;

public interface VasebreakerViewObserver {
    VasebreakerActionResult onStartVasebreakerRequested(
            int stageNumber
    );

    default VasebreakerActionResult
    onStartVasebreakerRequested() {
        return onStartVasebreakerRequested(1);
    }

    VasebreakerActionResult onBreakVaseRequested(
            Position position
    );

    VasebreakerActionResult onCollectSeedPacketRequested(
            Position position
    );

    VasebreakerActionResult onPlantSeedPacketRequested(
            String plantName,
            Position targetPosition
    );

    VasebreakerActionResult onAdvanceTicksRequested(
            int ticks
    );

    VasebreakerStateResult onShowVasebreakerRequested();
}