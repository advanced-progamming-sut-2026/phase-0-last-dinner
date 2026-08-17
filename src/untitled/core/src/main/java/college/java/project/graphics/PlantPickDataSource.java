package college.java.project.graphics;

import model.collection.CollectionActionResult;
import model.collection.PlantCollectionState;

import java.util.List;

/** Graphics-only bridge for the Phase 2 plant selection menu. */
public interface PlantPickDataSource {
    List<PlantCollectionState> getPlants();

    boolean isAvailable(String plantName);

    boolean isSelected(String plantName);

    boolean isBoosted(String plantName);

    boolean isGreenhouseBoosted(String plantName);

    int getSelectedCount();

    int getSlotCount();

    int getCoins();

    int getGems();

    String togglePlant(String plantName);

    String boostPlant(String plantName);

    CollectionActionResult upgradePlant(String plantName);

    String startGame();

    boolean isStarted();

    default boolean isDebugModeEnabled() {
        return false;
    }

    default boolean supportsCurrencyCheats() {
        return false;
    }

    default void setDebugModeEnabled(boolean enabled) {
    }

    default void cheatAddCoins(int amount) {
    }

    default void cheatAddGems(int amount) {
    }

    default void save() {
    }
}
