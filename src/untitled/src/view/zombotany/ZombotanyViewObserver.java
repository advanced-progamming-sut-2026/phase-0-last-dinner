package view.zombotany;

import model.mechanism.Position;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyStateResult;

public interface ZombotanyViewObserver {

    ZombotanyActionResult onStartRequested(
            int stageNumber
    );

    ZombotanyActionResult onPlantRequested(
            String plantName,
            Position position
    );

    ZombotanyActionResult onCollectSunRequested(
            Position position
    );

    ZombotanyActionResult onUsePlantFoodRequested(
            Position position
    );

    ZombotanyActionResult onAdvanceTicksRequested(
            int ticks
    );

    ZombotanyStateResult onShowRequested();
}