package controller;

import model.mechanism.Position;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;
import view.vasebreaker.VaseBreakerView;
import view.vasebreaker.VasebreakerViewObserver;

public class VasebreakerController
        implements VasebreakerViewObserver {

    private static final int MAX_TICKS_PER_COMMAND = 10_000;

    private final VasebreakerMiniGame game;

    public VasebreakerController(
            VaseBreakerView view
    ) {
        this(
                view,
                new VasebreakerMiniGame()
        );
    }

    public VasebreakerController(
            VaseBreakerView view,
            VasebreakerMiniGame game
    ) {
        if (view == null) {
            throw new IllegalArgumentException(
                    "Vasebreaker view cannot be null."
            );
        }

        if (game == null) {
            this.game = new VasebreakerMiniGame();
        } else {
            this.game = game;
        }

        view.setObserver(this);
    }

    @Override
    public VasebreakerActionResult
    onStartVasebreakerRequested(
            int stageNumber
    ) {
        return game.startStage(stageNumber);
    }

    @Override
    public VasebreakerActionResult
    onBreakVaseRequested(
            Position position
    ) {
        return game.breakVase(position);
    }

    @Override
    public VasebreakerActionResult
    onCollectSeedPacketRequested(
            Position position
    ) {
        return game.collectSeedPacket(position);
    }

    @Override
    public VasebreakerActionResult
    onPlantSeedPacketRequested(
            String plantName,
            Position targetPosition
    ) {
        return game.plantFromCollectedPacket(
                plantName,
                targetPosition
        );
    }

    @Override
    public VasebreakerActionResult
    onAdvanceTicksRequested(
            int ticks
    ) {
        if (ticks <= 0
                || ticks > MAX_TICKS_PER_COMMAND) {

            return VasebreakerActionResult
                    .invalidAction(null);
        }

        if (!game.isStarted()) {
            return VasebreakerActionResult
                    .gameNotStarted();
        }

        if (game.isCompleted()
                || game.isLoseConditionMet()) {

            return VasebreakerActionResult
                    .gameAlreadyFinished(
                            game.isCompleted(),
                            game.isLoseConditionMet()
                    );
        }

        int advancedTicks = 0;

        for (int i = 0; i < ticks; i++) {
            game.onTick();
            advancedTicks++;

            if (game.isCompleted()
                    || game.isLoseConditionMet()) {
                break;
            }
        }

        return VasebreakerActionResult.timeAdvanced(
                advancedTicks,
                game.isCompleted(),
                game.isLoseConditionMet()
        );
    }

    @Override
    public VasebreakerStateResult
    onShowVasebreakerRequested() {
        return game.getState();
    }

    public VasebreakerMiniGame getGame() {
        return game;
    }
}