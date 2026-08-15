package college.java.project.graphics;

import model.collection.ZombieCollectionState;

import java.util.List;


public interface ZombieCollectionDataSource {
    List<ZombieCollectionState> loadZombies();

    default int getMintCount() { return 0; }
    default int getGemCount() { return 0; }
    default int getCoinCount() { return 0; }
    default boolean isDebugModeEnabled() { return false; }
    default boolean supportsCurrencyCheats() { return false; }
    default void setDebugModeEnabled(boolean enabled) { }
    default void cheatAddCoins(int amount) { }
    default void cheatAddGems(int amount) { }
    default String getLoadErrorMessage() { return ""; }
    default void save() { }
}
