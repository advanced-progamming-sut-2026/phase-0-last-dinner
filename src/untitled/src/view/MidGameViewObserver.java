package view;

import model.mechanism.PlantStatus;
import model.mechanism.Tile;
import model.mechanism.ZombieStatus;
import model.mechanism.Board;

import java.util.List;

public interface MidGameViewObserver {
    void onAdvanceTimeRequested(int ticks);

    Board onShowMapRequested();

    int onShowSunAmountRequested();

    List<PlantStatus> onShowPlantsStatusRequested();

    default List<String> onShowConveyorPlantsRequested() {
        return java.util.Collections.emptyList();
    }

    Tile onShowTileStatusRequested(int x, int y);

    boolean onCollectSunRequested(int x, int y);

    boolean onPlantPlantRequested(String type, int x, int y);

    default boolean onPlantImitaterRequested(String type, int x, int y) {
        return false;
    }

    boolean onPluckPlantRequested(int x, int y);

    boolean onFeedPlantRequested(int x, int y);

    void onCheatAddSunsRequested(int count);

    void onCheatRemoveCooldownRequested();

    void onCheatAddPlantFoodRequested();

    void onReleaseTheNukeRequested();

    List<ZombieStatus> onZombiesInfoRequested();

    boolean onSpawnZombieRequested(String type, int x, int y);

    boolean isGameOver();

    int getCurrentWaveNumber();

    int onShowPlantFoodCountRequested();
}
