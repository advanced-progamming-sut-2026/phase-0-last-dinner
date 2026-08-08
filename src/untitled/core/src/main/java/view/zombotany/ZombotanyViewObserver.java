package view.zombotany;

import model.mechanism.Position;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyStateResult;
import model.plant.PlantDefinition;

import java.util.List;

public interface ZombotanyViewObserver {

    ZombotanyActionResult onStartRequested(
            int stageNumber
    );

    default List<PlantDefinition> onShowAvailablePlantsRequested() {
        return java.util.Collections.emptyList();
    }

    default List<PlantDefinition> onShowSelectedPlantsRequested() {
        return java.util.Collections.emptyList();
    }

    default ZombotanyActionResult onAddPlantRequested(String plantName) {
        return null;
    }

    default ZombotanyActionResult onRemovePlantRequested(String plantName) {
        return null;
    }

    default ZombotanyActionResult onStartGameRequested() {
        return null;
    }

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
