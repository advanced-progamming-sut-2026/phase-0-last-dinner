package view.vasebreaker;

import model.mechanism.Position;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;

public interface VasebreakerViewObserver {
    VasebreakerActionResult onBreakVaseRequested(Position position);

    VasebreakerActionResult onCollectSeedPacketRequested(Position position);

    VasebreakerActionResult onAdvanceTicksRequested(int ticks);

    VasebreakerStateResult onShowVasebreakerRequested();

    VasebreakerActionResult onStartVasebreakerRequested();
}