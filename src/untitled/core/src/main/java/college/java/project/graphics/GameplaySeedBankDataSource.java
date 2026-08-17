package college.java.project.graphics;

import model.Plant;
import model.collection.PlantCollectionState;
import model.mechanism.PlantStatus;

import java.util.List;

/** Supplies live Phase 1 gameplay state to the reusable graphical gameplay HUD. */
public interface GameplaySeedBankDataSource {
    List<PlantCollectionState> getSelectedPlants();

    List<PlantStatus> getPlantStatuses();

    int getSunAmount();

    int getPlantFoodCount();

    boolean isBoosted(String plantName);

    boolean plant(String plantName, int column, int row);

    default void setImitaterCopyTarget(String plantName) {
    }

    default String getImitaterCopyTarget() {
        return null;
    }

    default boolean canPlant(String plantName, int column, int row) {
        return true;
    }

    default String getPlantingFailureMessage(String plantName, int column, int row) {
        return "Cannot plant there.";
    }

    default boolean hasPlantAt(int column, int row) {
        return false;
    }

    default boolean canFeedPlantAt(int column, int row) {
        return getPlantFoodCount() > 0 && hasPlantAt(column, row);
    }

    default boolean pluckPlant(int column, int row) {
        return false;
    }

    default boolean feedPlant(int column, int row) {
        return false;
    }

    default List<Plant> getPlantsOnBoard() {
        return java.util.Collections.emptyList();
    }

    default Plant getTopPlantAt(int column, int row) {
        return null;
    }

    default int getCoinCount() {
        return 0;
    }

    default int getGemCount() {
        return 0;
    }

    default boolean isDebugModeEnabled() {
        return false;
    }

    default void setDebugModeEnabled(boolean enabled) {
    }

    default boolean supportsCurrencyCheats() {
        return false;
    }

    default void cheatAddCoins(int amount) {
    }

    default void cheatAddGems(int amount) {
    }

    default void cheatAddSun(int amount) {
    }

    default void cheatAddPlantFood() {
    }

    default void cheatRemoveCooldowns() {
    }
}
