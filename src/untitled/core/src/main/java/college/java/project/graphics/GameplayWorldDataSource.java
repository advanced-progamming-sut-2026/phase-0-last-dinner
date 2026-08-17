package college.java.project.graphics;

import model.Plant;
import model.chapters.ChapterType;
import model.level.LevelType;
import model.mechanism.LawnMower;
import model.mechanism.Loot;
import model.mechanism.Position;
import model.mechanism.Sun;
import model.mechanism.Tile;
import model.mechanism.Wave;
import model.plant.Projectile;
import model.zombie.Zombie;

import java.util.Collections;
import java.util.List;

/** Supplies live Phase 1 world entities and mandatory level state to the graphical gameplay layers. */
public interface GameplayWorldDataSource {
    default List<Zombie> getZombiesOnBoard() {
        return Collections.emptyList();
    }

    default List<Plant> getPlantsOnBoard() {
        return Collections.emptyList();
    }

    default List<Projectile> getProjectiles() {
        return Collections.emptyList();
    }

    default List<Sun> getGroundSuns() {
        return Collections.emptyList();
    }

    default List<LawnMower> getLawnMowers() {
        return Collections.emptyList();
    }

    default List<Tile> getTiles() {
        return Collections.emptyList();
    }

    default ChapterType getChapterType() {
        return ChapterType.ANCIENT_EGYPT;
    }

    default LevelType getLevelType() {
        return LevelType.NORMAL;
    }

    default long getCurrentTick() {
        return 0L;
    }

    default Wave getCurrentWave() {
        return null;
    }

    default int getWaveIndex() {
        return 0;
    }

    default int getWaveCount() {
        return 0;
    }

    default boolean collectSun(Sun sun) {
        return false;
    }

    default GameplayPlantCoverInspector.State getPlantCoverState(Plant plant) {
        if (plant == null || plant.getBoard() == null) {
            return GameplayPlantCoverInspector.inspect(plant, null);
        }
        return GameplayPlantCoverInspector.inspect(
                plant,
                plant.getBoard().getPlantCoverSystem()
        );
    }

    default int getPlantFoodAmount() {
        return 0;
    }

    default List<Loot> getLootHistory() {
        return Collections.emptyList();
    }

    default boolean shouldShowMissionAtStart() {
        return false;
    }

    default String getMissionTitle() {
        return "LEVEL OBJECTIVE";
    }

    default String getMissionDescription() {
        return "Don't let the zombies reach your house!";
    }

    default boolean isLevelWon() {
        return false;
    }

    default boolean isLevelLost() {
        return false;
    }

    /** Current Phase 1 conveyor packet names, ordered as delivered by the level. */
    default List<String> getConveyorPlantNames() {
        return Collections.emptyList();
    }

    /** Protected tiles used by Save Our Seeds when that Phase 1 level supplies them. */
    default List<Position> getProtectedSeedCells() {
        return Collections.emptyList();
    }

    /** Zero-based deadline column; -1 means the active level has no deadline. */
    default int getDeadlineColumn() {
        return -1;
    }

    /** Text lines for timer/sun/zombie goals in Timed War. */
    default List<String> getTimedMissionStatusLines() {
        return Collections.emptyList();
    }

    /** Number of plants currently alive for Love Your Plants; -1 hides the counter. */
    default int getRemainingPlantCount() {
        return -1;
    }

    /** True while Plant What You Get is still in its unlimited preparation phase. */
    default boolean isPreWavePlanting() {
        return false;
    }

    /** Starts waves after Plant What You Get preparation. */
    default void startPreparedWave() {
    }

    /** Stable Medieval necromancy cells, even after a marked tile becomes a grave. */
    default List<Position> getNecromancyCells() {
        return Collections.emptyList();
    }

    /** Furthest inland Big Wave Beach water boundary from Phase 1; -1 hides it. */
    default int getMaximumWaterColumn() {
        return -1;
    }
}
