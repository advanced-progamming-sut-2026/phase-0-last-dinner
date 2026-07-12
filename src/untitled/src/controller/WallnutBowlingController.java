package controller;

import model.mechanism.Position;
import model.minigame.wallnutbowlingminigame.WallnutBowlingActionResult;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;
import model.minigame.wallnutbowlingminigame.WallnutBowlingStateResult;
import view.wallnutbowling.WallnutBowlingView;
import view.wallnutbowling.WallnutBowlingViewObserver;

public class WallnutBowlingController
        implements WallnutBowlingViewObserver {

    private static final int MAX_TICKS_PER_COMMAND = 10_000;

    private final WallnutBowlingMiniGame game;

    public WallnutBowlingController(
            WallnutBowlingView view
    ) {
        this(
                view,
                new WallnutBowlingMiniGame()
        );
    }

    public WallnutBowlingController(
            WallnutBowlingView view,
            WallnutBowlingMiniGame game
    ) {
        if (view == null) {
            throw new IllegalArgumentException(
                    "Wallnut Bowling view cannot be null."
            );
        }

        if (game == null) {
            this.game =
                    new WallnutBowlingMiniGame();
        } else {
            this.game = game;
        }

        view.setObserver(this);
    }

    @Override
    public WallnutBowlingActionResult
    onStartWallnutBowlingRequested(
            int stageNumber
    ) {
        return game.startStage(stageNumber);
    }

    @Override
    public WallnutBowlingActionResult
    onPlaceWallnutRequested(
            int conveyorIndex,
            Position position
    ) {
        return game.placeWallnutFromConveyor(
                conveyorIndex,
                position
        );
    }

    @Override
    public WallnutBowlingActionResult
    onAdvanceTicksRequested(
            int ticks
    ) {
        if (ticks <= 0
                || ticks > MAX_TICKS_PER_COMMAND) {

            return WallnutBowlingActionResult
                    .invalidAction();
        }

        if (!game.isStarted()) {
            return WallnutBowlingActionResult
                    .gameNotStarted();
        }

        if (game.isCompleted()
                || game.isLoseConditionMet()) {

            return WallnutBowlingActionResult
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

        return WallnutBowlingActionResult
                .timeAdvanced(
                        advancedTicks,
                        game.isCompleted(),
                        game.isLoseConditionMet()
                );
    }

    @Override
    public WallnutBowlingStateResult
    onShowWallnutBowlingRequested() {
        return game.getState();
    }

    public WallnutBowlingMiniGame getGame() {
        return game;
    }
}