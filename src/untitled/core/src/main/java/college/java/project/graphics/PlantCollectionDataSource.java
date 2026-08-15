package college.java.project.graphics;

import model.collection.CollectionActionResult;
import model.collection.PlantCollectionState;

import java.util.List;


public interface PlantCollectionDataSource {
    List<PlantCollectionState> getPlants();
    int getGold();
    default int getGems() { return 0; }
    default int getMints() { return 0; }
    CollectionActionResult upgradePlant(String plantName);
    CollectionActionResult purchasePlant(String plantName);
    default boolean isDebugModeEnabled() { return false; }
    default boolean supportsCurrencyCheats() { return false; }
    default void setDebugModeEnabled(boolean enabled) { }
    default void cheatAddGold(int amount) { }
    default void cheatAddGems(int amount) { }
    default String getLoadErrorMessage() { return ""; }
    default void save() { }
}
