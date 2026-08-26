package college.java.project.graphics.minigame.multiplayer;

import college.java.project.graphics.GameplayPlantCoverInspector;
import college.java.project.graphics.GameplaySeedBankDataSource;
import college.java.project.graphics.GameplayWorldDataSource;
import model.Plant;
import model.chapters.ChapterType;
import model.collection.PlantCollectionState;
import model.level.LevelType;
import model.mechanism.*;
import model.plant.Projectile;
import model.zombie.Zombie;
import network.izombie.client.IZombieClientGameData;

import java.util.Collections;
import java.util.List;

public final class IZombieMultiplayerWorldDataSource implements GameplaySeedBankDataSource, GameplayWorldDataSource {

    private final IZombieClientGameData data;

    public IZombieMultiplayerWorldDataSource(IZombieClientGameData data) {
        if (data == null) {
            throw new IllegalArgumentException("Multiplayer game data is required.");
        }

        this.data = data;
    }

    @Override
    public List<PlantCollectionState> getSelectedPlants() {
        return Collections.emptyList();
    }

    @Override
    public List<PlantStatus> getPlantStatuses() {
        return Collections.emptyList();
    }

    @Override
    public int getSunAmount() {
        return data.getSunAmount();
    }

    @Override
    public int getPlantFoodCount() {
        return 0;
    }

    @Override
    public boolean isBoosted(String plantName) {
        return false;
    }

    @Override
    public boolean plant(String plantName, int column, int row) {
        return false;
    }

    @Override
    public void setImitaterCopyTarget(String plantName) {
        GameplaySeedBankDataSource.super.setImitaterCopyTarget(plantName);
    }

    @Override
    public String getImitaterCopyTarget() {
        return GameplaySeedBankDataSource.super.getImitaterCopyTarget();
    }

    @Override
    public boolean canPlant(String plantName, int column, int row) {
        return GameplaySeedBankDataSource.super.canPlant(plantName, column, row);
    }

    @Override
    public String getPlantingFailureMessage(String plantName, int column, int row) {
        return GameplaySeedBankDataSource.super.getPlantingFailureMessage(plantName, column, row);
    }

    @Override
    public boolean hasPlantAt(int column, int row) {
        return GameplaySeedBankDataSource.super.hasPlantAt(column, row);
    }

    @Override
    public boolean canFeedPlantAt(int column, int row) {
        return GameplaySeedBankDataSource.super.canFeedPlantAt(column, row);
    }

    @Override
    public boolean pluckPlant(int column, int row) {
        return GameplaySeedBankDataSource.super.pluckPlant(column, row);
    }

    @Override
    public boolean feedPlant(int column, int row) {
        return GameplaySeedBankDataSource.super.feedPlant(column, row);
    }

    @Override
    public List<Zombie> getZombiesOnBoard() {
        return GameplayWorldDataSource.super.getZombiesOnBoard();
    }

    @Override
    public List<Plant> getPlantsOnBoard() {
        return GameplaySeedBankDataSource.super.getPlantsOnBoard();
    }

    @Override
    public List<Projectile> getProjectiles() {
        return GameplayWorldDataSource.super.getProjectiles();
    }

    @Override
    public List<Sun> getGroundSuns() {
        return GameplayWorldDataSource.super.getGroundSuns();
    }

    @Override
    public List<LawnMower> getLawnMowers() {
        return GameplayWorldDataSource.super.getLawnMowers();
    }

    @Override
    public List<Tile> getTiles() {
        return GameplayWorldDataSource.super.getTiles();
    }

    @Override
    public Plant getTopPlantAt(int column, int row) {
        return GameplaySeedBankDataSource.super.getTopPlantAt(column, row);
    }

    @Override
    public int getCoinCount() {
        return GameplaySeedBankDataSource.super.getCoinCount();
    }

    @Override
    public int getGemCount() {
        return GameplaySeedBankDataSource.super.getGemCount();
    }

    @Override
    public boolean isDebugModeEnabled() {
        return GameplaySeedBankDataSource.super.isDebugModeEnabled();
    }

    @Override
    public void setDebugModeEnabled(boolean enabled) {
        GameplaySeedBankDataSource.super.setDebugModeEnabled(enabled);
    }

    @Override
    public boolean supportsCurrencyCheats() {
        return GameplaySeedBankDataSource.super.supportsCurrencyCheats();
    }

    @Override
    public void cheatAddCoins(int amount) {
        GameplaySeedBankDataSource.super.cheatAddCoins(amount);
    }

    @Override
    public void cheatAddGems(int amount) {
        GameplaySeedBankDataSource.super.cheatAddGems(amount);
    }

    @Override
    public void cheatAddSun(int amount) {
        GameplaySeedBankDataSource.super.cheatAddSun(amount);
    }

    @Override
    public void cheatAddPlantFood() {
        GameplaySeedBankDataSource.super.cheatAddPlantFood();
    }

    @Override
    public void cheatRemoveCooldowns() {
        GameplaySeedBankDataSource.super.cheatRemoveCooldowns();
    }

    @Override
    public long getCurrentTick() {
        return data.getServerTick();
    }

    @Override
    public Wave getCurrentWave() {
        return GameplayWorldDataSource.super.getCurrentWave();
    }

    @Override
    public int getWaveIndex() {
        return GameplayWorldDataSource.super.getWaveIndex();
    }

    @Override
    public int getWaveCount() {
        return GameplayWorldDataSource.super.getWaveCount();
    }

    @Override
    public boolean collectSun(Sun sun) {
        return GameplayWorldDataSource.super.collectSun(sun);
    }

    @Override
    public GameplayPlantCoverInspector.State getPlantCoverState(Plant plant) {
        return GameplayWorldDataSource.super.getPlantCoverState(plant);
    }

    @Override
    public int getPlantFoodAmount() {
        return GameplayWorldDataSource.super.getPlantFoodAmount();
    }

    @Override
    public List<Loot> getLootHistory() {
        return GameplayWorldDataSource.super.getLootHistory();
    }

    @Override
    public ChapterType getChapterType() {
        return switch (data.getStageNumber()) {
            case 2 -> ChapterType.ICE_CAVES;
            case 3 -> ChapterType.MEDIEVAL;
            default -> ChapterType.ANCIENT_EGYPT;
        };
    }

    @Override
    public LevelType getLevelType() {
        return GameplayWorldDataSource.super.getLevelType();
    }

    @Override
    public boolean isLevelWon() {
        return data.isFinished() && data.didCurrentPlayerWin();
    }

    @Override
    public boolean isLevelLost() {
        return data.isFinished() && !data.didCurrentPlayerWin();
    }

    @Override
    public List<String> getConveyorPlantNames() {
        return GameplayWorldDataSource.super.getConveyorPlantNames();
    }

    @Override
    public List<Position> getProtectedSeedCells() {
        return GameplayWorldDataSource.super.getProtectedSeedCells();
    }

    @Override
    public int getDeadlineColumn() {
        return GameplayWorldDataSource.super.getDeadlineColumn();
    }

    @Override
    public List<String> getTimedMissionStatusLines() {
        return GameplayWorldDataSource.super.getTimedMissionStatusLines();
    }

    @Override
    public int getRemainingPlantCount() {
        return GameplayWorldDataSource.super.getRemainingPlantCount();
    }

    @Override
    public boolean isPreWavePlanting() {
        return GameplayWorldDataSource.super.isPreWavePlanting();
    }

    @Override
    public void startPreparedWave() {
        GameplayWorldDataSource.super.startPreparedWave();
    }

    @Override
    public List<Position> getNecromancyCells() {
        return GameplayWorldDataSource.super.getNecromancyCells();
    }

    @Override
    public int getMaximumWaterColumn() {
        return GameplayWorldDataSource.super.getMaximumWaterColumn();
    }

    @Override
    public String getMissionTitle() {
        return GameplayWorldDataSource.super.getMissionTitle();
    }

    @Override
    public String getMissionDescription() {
        return GameplayWorldDataSource.super.getMissionDescription();
    }
}
