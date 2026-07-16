package view.izombie;

import model.mechanism.Position;
import model.minigame.izombieminigame.IZombieActionResult;
import model.minigame.izombieminigame.IZombieStateResult;

public interface IZombieViewObserver {

    IZombieActionResult onStartIZombieRequested();

    IZombieActionResult onPlaceZombieRequested(
            String zombieAliasOrName,
            Position position
    );

    IZombieActionResult onAdvanceTicksRequested(int ticks);

    IZombieStateResult onShowIZombieRequested();
}