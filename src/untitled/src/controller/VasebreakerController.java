package controller;

import model.mechanism.Position;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;
import view.vasebreaker.VaseBreakerView;
import view.vasebreaker.VasebreakerViewObserver;

public class VasebreakerController implements VasebreakerViewObserver {
    private final VasebreakerMiniGame game;

    public VasebreakerController(VaseBreakerView view) {
        this.game = new VasebreakerMiniGame();
        view.setObserver(this);
    }

    @Override
    public VasebreakerActionResult onStartVasebreakerRequested() {
        game.start();
        return VasebreakerActionResult.started(
                game.isCompleted(),
                game.isLoseConditionMet()
        );
    }

    @Override
    public VasebreakerActionResult onBreakVaseRequested(Position position) {
        return game.breakVase(position);
    }

    @Override
    public VasebreakerActionResult onCollectSeedPacketRequested(Position position) {
        return game.collectSeedPacket(position);
    }

    @Override
    public VasebreakerActionResult onAdvanceTicksRequested(int ticks) {
        for (int i = 0; i < ticks; i++) {
            game.onTick();
        }

        return VasebreakerActionResult.timeAdvanced(
                ticks,
                game.isCompleted(),
                game.isLoseConditionMet()
        );
    }

    @Override
    public VasebreakerStateResult onShowVasebreakerRequested() {
        return game.getState();
    }
}