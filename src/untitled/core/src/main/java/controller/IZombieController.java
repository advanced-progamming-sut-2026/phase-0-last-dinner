package controller;

import model.mechanism.Position;
import model.minigame.izombieminigame.IZombieActionResult;
import model.minigame.izombieminigame.IZombieActionStatus;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.izombieminigame.IZombieStateResult;
import model.zombie.ZombieDefinition;
import view.izombie.IZombieView;
import view.izombie.IZombieViewObserver;

public class IZombieController implements IZombieViewObserver {

    private final IZombieMiniGame game;

    public IZombieController(IZombieView view) {
        this(
                view,
                new IZombieMiniGame()
        );
    }

    public IZombieController(
            IZombieView view,
            IZombieMiniGame game
    ) {
        if (view == null) {
            throw new IllegalArgumentException(
                    "I Zombie view cannot be null."
            );
        }

        if (game == null) {
            throw new IllegalArgumentException(
                    "I Zombie mini game cannot be null."
            );
        }

        this.game = game;
        view.setObserver(this);
    }

    @Override
    public IZombieActionResult onStartIZombieRequested() {
        return game.startGame();
    }

    @Override
    public IZombieActionResult onPlaceZombieRequested(
            String zombieAliasOrName,
            Position position
    ) {
        ZombieDefinition definition =
                game.findAvailableZombie(zombieAliasOrName);

        if (definition == null) {
            return IZombieActionResult.failure(
                    IZombieActionStatus.INVALID_ZOMBIE,
                    "No available zombie matches: "
                            + zombieAliasOrName,
                    game.getSunAmount()
            );
        }

        return game.placeZombie(
                definition,
                position
        );
    }

    @Override
    public IZombieActionResult onAdvanceTicksRequested(int ticks) {
        if (ticks <= 0) {
            return IZombieActionResult.success(
                    "No time was advanced.",
                    game.getSunAmount()
            );
        }

        IZombieActionResult lastResult = null;
        boolean stageChanged = false;

        for (int tick = 0; tick < ticks; tick++) {
            lastResult = game.advanceOneTick();

            if (lastResult.getStatus()
                    == IZombieActionStatus.STAGE_WON) {
                stageChanged = true;
            }

            if (lastResult.isTerminal()
                    || !lastResult.isSuccessful()) {
                return lastResult;
            }
        }

        if (lastResult != null
                && lastResult.getStatus()
                == IZombieActionStatus.STAGE_WON) {
            return lastResult;
        }

        String message = "Time advanced by "
                + ticks + " ticks.";

        if (stageChanged) {
            message += " A new stage was started during this time.";
        }

        return IZombieActionResult.success(
                message,
                game.getSunAmount()
        );
    }

    @Override
    public IZombieStateResult onShowIZombieRequested() {
        return game.getState();
    }

    public IZombieMiniGame getGame() {
        return game;
    }
}