package college.java.project.graphics;

import controller.CollectionController;
import controller.GameController;
import controller.MidGameController;
import model.collection.CollectionStateResult;
import model.level.ConveyorBeltLevel;
import model.level.Level;
import model.level.LevelType;
import model.collection.PlantCollectionState;
import model.mechanism.PlantStatus;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.mechanism.LawnMower;
import model.mechanism.Loot;
import model.mechanism.Sun;
import model.mechanism.Wave;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.chapters.ChapterMedieval;
import model.chapters.ChapterType;
import model.Plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bridges the existing MidGameController and CollectionController to the
 * graphical in-game seed bank without changing Phase 1 Model/Controller code.
 */
public final class ControllerGameplaySeedBankDataSource implements GameplaySeedBankDataSource, GameplayWorldDataSource {
    private final MidGameController midGameController;
    private final CollectionController collectionController;
    private final GameController gameController;
    private final PlantZombieGame game;
    private boolean debugModeEnabled;
    private String imitaterCopyTarget;

    public ControllerGameplaySeedBankDataSource(
            MidGameController midGameController,
            CollectionController collectionController,
            PlantZombieGame game,
            boolean debugModeEnabled
    ) {
        this(midGameController, collectionController, game, null, debugModeEnabled);
    }

    public ControllerGameplaySeedBankDataSource(
            MidGameController midGameController,
            CollectionController collectionController,
            PlantZombieGame game,
            GameController gameController,
            boolean debugModeEnabled
    ) {
        if (midGameController == null) {
            throw new IllegalArgumentException("Mid-game controller is required");
        }
        if (collectionController == null) {
            throw new IllegalArgumentException("Collection controller is required");
        }
        if (game == null) {
            throw new IllegalArgumentException("PlantZombieGame is required");
        }
        this.midGameController = midGameController;
        this.collectionController = collectionController;
        this.gameController = gameController;
        this.game = game;
        this.debugModeEnabled = debugModeEnabled;
    }

    @Override
    public List<PlantCollectionState> getSelectedPlants() {
        List<PlantStatus> statuses = getPlantStatuses();
        if (statuses.isEmpty()) {
            return Collections.emptyList();
        }

        CollectionStateResult result = this.collectionController.onShowAllPlantsRequested();
        if (result == null || !result.isSuccessful() || result.getPlants() == null) {
            return Collections.emptyList();
        }

        Map<String, PlantCollectionState> statesByName = new HashMap<>();
        for (PlantCollectionState state : result.getPlants()) {
            if (state != null && state.getName() != null) {
                statesByName.put(normalize(state.getName()), state);
            }
        }

        List<PlantCollectionState> selected = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (PlantStatus status : statuses) {
            String name = statusPlantName(status);
            PlantCollectionState state = statesByName.get(normalize(name));
            if (state != null && added.add(normalize(state.getName()))) {
                selected.add(state);
            }
        }
        return selected;
    }

    @Override
    public List<PlantStatus> getPlantStatuses() {
        List<PlantStatus> statuses = this.midGameController.onShowPlantsStatusRequested();
        return statuses == null ? Collections.emptyList() : statuses;
    }

    @Override
    public int getSunAmount() {
        return Math.max(0, this.midGameController.onShowSunAmountRequested());
    }

    @Override
    public int getPlantFoodCount() {
        return Math.max(0, this.midGameController.onShowPlantFoodCountRequested());
    }

    @Override
    public boolean isBoosted(String plantName) {
        String normalizedName = normalize(plantName);
        for (String boosted : this.game.getBoostedPlantNames()) {
            if (normalize(boosted).equals(normalizedName)) {
                return true;
            }
        }
        return this.game.getUser() != null
                && this.game.getUser().getGreenhouse() != null
                && this.game.getUser().getGreenhouse().hasStoredBoost(plantName);
    }

    @Override
    public boolean plant(String plantName, int column, int row) {
        if ("imitater".equals(normalize(plantName))) {
            if (this.imitaterCopyTarget == null || this.imitaterCopyTarget.isBlank()) {
                return false;
            }
            return this.midGameController.onPlantImitaterRequested(this.imitaterCopyTarget, column, row);
        }
        return this.midGameController.onPlantPlantRequested(plantName, column, row);
    }

    @Override
    public void setImitaterCopyTarget(String plantName) {
        String normalized = normalize(plantName);
        this.imitaterCopyTarget = normalized.isEmpty() || "imitater".equals(normalized) ? null : plantName;
    }

    @Override
    public String getImitaterCopyTarget() {
        return this.imitaterCopyTarget;
    }

    @Override
    public boolean canPlant(String plantName, int column, int row) {
        Position position = new Position(column, row);
        if (!this.game.getBoard().isInsideBoard(position)) {
            return false;
        }
        if (this.game.getActiveLevel() instanceof ConveyorBeltLevel conveyor) {
            Plant conveyorPlant = findConveyorPlant(conveyor, plantName);
            Tile tile = this.game.getBoard().getTile(position);
            return conveyorPlant != null && tile != null && tile.canPlacePlant(conveyorPlant);
        }
        PlantStatus status = findPlantStatus(plantName);
        if ("imitater".equals(normalize(plantName))) {
            return this.canPlantImitater(status, position);
        }
        return status != null
                && status.getPlant() != null
                && status.isAvailable()
                && this.game.getPlantingSystem().canPlant(status.getPlant(), position);
    }

    @Override
    public String getPlantingFailureMessage(String plantName, int column, int row) {
        Position position = new Position(column, row);
        if (!this.game.getBoard().isInsideBoard(position)) {
            return "That tile is outside the lawn.";
        }
        if (this.game.getActiveLevel() instanceof ConveyorBeltLevel conveyor) {
            Plant conveyorPlant = findConveyorPlant(conveyor, plantName);
            if (conveyorPlant == null) {
                return "That seed packet is no longer on the conveyor.";
            }
            return "That plant cannot be placed on this tile.";
        }
        PlantStatus status = findPlantStatus(plantName);
        if (status == null || status.getPlant() == null) {
            return "That plant is not available in this level.";
        }
        if (getSunAmount() < status.getSunCost()) {
            return "Not enough sun.";
        }
        if (status.getRemainingCooldownTicks() > 0) {
            return String.format(
                    Locale.ROOT,
                    "Plant is recharging: %.1fs remaining.",
                    Math.max(0d, status.getRemainingSeconds())
            );
        }
        if ("imitater".equals(normalize(plantName)) && this.imitaterCopyTarget == null) {
            return "Choose a seed packet for Imitater to copy first.";
        }
        return "That plant cannot be placed on this tile.";
    }

    @Override
    public boolean hasPlantAt(int column, int row) {
        Position position = new Position(column, row);
        if (!this.game.getBoard().isInsideBoard(position)) {
            return false;
        }
        return !this.game.getBoard().getPlantsAt(position).isEmpty();
    }

    @Override
    public boolean canFeedPlantAt(int column, int row) {
        if (getPlantFoodCount() <= 0) {
            return false;
        }
        Position position = new Position(column, row);
        if (!this.game.getBoard().isInsideBoard(position)) {
            return false;
        }
        Tile tile = this.game.getBoard().getTile(position);
        if (tile == null) {
            return false;
        }
        if (tile.getPlants() == null || tile.getPlants().isEmpty()) {
            return false;
        }
        Plant topPlant = tile.getPlants().get(tile.getPlants().size() - 1);
        return topPlant != null && topPlant.canReceivePlantFood();
    }

    @Override
    public boolean pluckPlant(int column, int row) {
        return this.midGameController.onPluckPlantRequested(column, row);
    }

    @Override
    public boolean feedPlant(int column, int row) {
        return this.midGameController.onFeedPlantRequested(column, row);
    }

    @Override
    public List<Plant> getPlantsOnBoard() {
        return new ArrayList<>(this.game.getBoard().getAllPlants());
    }

    @Override
    public Plant getTopPlantAt(int column, int row) {
        Position position = new Position(column, row);
        if (!this.game.getBoard().isInsideBoard(position)) {
            return null;
        }
        Tile tile = this.game.getBoard().getTile(position);
        if (tile == null || tile.getPlants() == null || tile.getPlants().isEmpty()) {
            return null;
        }
        return tile.getPlants().get(tile.getPlants().size() - 1);
    }

    @Override
    public List<Zombie> getZombiesOnBoard() {
        return new ArrayList<>(this.game.getBoard().getAllZombies());
    }

    @Override
    public List<Projectile> getProjectiles() {
        return new ArrayList<>(this.game.getBoard().getProjectiles());
    }

    @Override
    public List<Sun> getGroundSuns() {
        return new ArrayList<>(this.game.getSunSystem().getSuns());
    }

    @Override
    public List<LawnMower> getLawnMowers() {
        return new ArrayList<>(this.game.getBoard().getLawnMowers());
    }

    @Override
    public List<Tile> getTiles() {
        return new ArrayList<>(this.game.getBoard().getTiles());
    }

    @Override
    public ChapterType getChapterType() {
        return this.game.getActiveChapter() == null
                ? ChapterType.ANCIENT_EGYPT
                : this.game.getActiveChapter().getChapter();
    }

    @Override
    public long getCurrentTick() {
        return this.game.getEngine().getClock().getCurrentTick();
    }

    @Override
    public Wave getCurrentWave() {
        return this.game.getWaveManager().getCurrentWave();
    }

    @Override
    public int getWaveIndex() {
        return Math.max(0, this.game.getWaveManager().getCurrentWaveIndex());
    }

    @Override
    public int getWaveCount() {
        List<Wave> waves = this.game.getWaveManager().getWaves();
        return waves == null ? 0 : waves.size();
    }

    @Override
    public boolean collectSun(Sun sun) {
        if (sun == null || sun.isCollected() || sun.getPosition() == null) {
            return false;
        }
        this.game.getSunSystem().collectSun(sun);
        return sun.isCollected();
    }

    @Override
    public GameplayPlantCoverInspector.State getPlantCoverState(Plant plant) {
        return GameplayPlantCoverInspector.inspect(
                plant,
                this.game.getBoard().getPlantCoverSystem()
        );
    }

    @Override
    public int getPlantFoodAmount() {
        return getPlantFoodCount();
    }

    @Override
    public List<Loot> getLootHistory() {
        if (this.game.getLootSystem() == null
                || this.game.getLootSystem().getDroppedLoot() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(this.game.getLootSystem().getDroppedLoot());
    }


    @Override
    public int getCoinCount() {
        if (this.gameController != null) {
            return Math.max(0, this.gameController.coinWallet());
        }
        return this.game.getUser() == null ? 0 : Math.max(0, this.game.getUser().getGold());
    }

    @Override
    public int getGemCount() {
        if (this.gameController != null) {
            return Math.max(0, this.gameController.gemWallet());
        }
        return this.game.getUser() == null ? 0 : Math.max(0, this.game.getUser().getDiamond());
    }

    @Override
    public boolean supportsCurrencyCheats() {
        return this.gameController != null || this.game.getUser() != null;
    }

    @Override
    public void cheatAddCoins(int amount) {
        if (amount <= 0) {
            return;
        }
        if (this.gameController != null) {
            this.gameController.onCheatAddRequested(amount, "coin");
        } else if (this.game.getUser() != null) {
            this.game.getUser().setGold(safeAdd(this.game.getUser().getGold(), amount));
        }
    }

    @Override
    public void cheatAddGems(int amount) {
        if (amount <= 0) {
            return;
        }
        if (this.gameController != null) {
            this.gameController.onCheatAddRequested(amount, "diamond");
        } else if (this.game.getUser() != null) {
            this.game.getUser().setDiamond(safeAdd(this.game.getUser().getDiamond(), amount));
        }
    }

    private int safeAdd(int current, int amount) {
        long value = (long) Math.max(0, current) + amount;
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    @Override
    public LevelType getLevelType() {
        Level level = this.game.getActiveLevel();
        return level == null || level.getLevelType() == null
                ? LevelType.NORMAL
                : level.getLevelType();
    }

    @Override
    public boolean shouldShowMissionAtStart() {
        return this.game.getActiveLevel() != null;
    }

    @Override
    public String getMissionTitle() {
        return missionTitle(getLevelType());
    }

    @Override
    public String getMissionDescription() {
        return missionDescription(getLevelType());
    }

    @Override
    public boolean isLevelWon() {
        Level level = this.game.getActiveLevel();
        return level != null && level.isWinConditionMet();
    }

    @Override
    public boolean isLevelLost() {
        Level level = this.game.getActiveLevel();
        return level != null && level.isLoseConditionMet();
    }

    @Override
    public List<String> getConveyorPlantNames() {
        List<String> names = this.midGameController.onShowConveyorPlantsRequested();
        return names == null ? Collections.emptyList() : new ArrayList<>(names);
    }

    @Override
    public int getDeadlineColumn() {
        return getLevelType() == LevelType.DEADLINE ? 3 : -1;
    }

    @Override
    public int getRemainingPlantCount() {
        if (getLevelType() != LevelType.LOVE_YOUR_PLANTS || this.game.getBoard() == null) {
            return -1;
        }
        return this.game.getBoard().getAllPlants().size();
    }

    @Override
    public List<Position> getNecromancyCells() {
        if (!(this.game.getActiveChapter() instanceof ChapterMedieval medieval)) {
            return Collections.emptyList();
        }
        List<Position> result = new ArrayList<>();
        for (Tile tile : medieval.getNecromancyGraves()) {
            if (tile != null && tile.getPosition() != null) {
                result.add(tile.getPosition());
            }
        }
        return result;
    }

    @Override
    public int getMaximumWaterColumn() {
        return getChapterType() == ChapterType.BIG_WAVE_BEACH ? 4 : -1;
    }

    @Override
    public boolean isDebugModeEnabled() {
        return this.debugModeEnabled;
    }

    @Override
    public void setDebugModeEnabled(boolean enabled) {
        this.debugModeEnabled = enabled;
    }

    @Override
    public void cheatAddSun(int amount) {
        if (amount > 0) {
            this.midGameController.onCheatAddSunsRequested(amount);
        }
    }

    @Override
    public void cheatAddPlantFood() {
        this.midGameController.onCheatAddPlantFoodRequested();
    }

    @Override
    public void cheatRemoveCooldowns() {
        this.midGameController.onCheatRemoveCooldownRequested();
    }


    private String missionTitle(LevelType type) {
        if (type == null) {
            return "LEVEL OBJECTIVE";
        }
        return switch (type) {
            case DEADLINE -> "DEADLINE";
            case CONVEYOR_BELT -> "CONVEYOR BELT";
            case SAVE_OUR_SEEDS -> "SAVE OUR SEEDS";
            case TIMED_WAR -> "TIMED WAR";
            case LOVE_YOUR_PLANTS -> "LOVE YOUR PLANTS";
            case PLANT_WHAT_YOU_GET -> "PLANT WHAT YOU GET";
            case BOSS, MEOW_POINT -> "BOSS BATTLE";
            default -> "LEVEL OBJECTIVE";
        };
    }

    private String missionDescription(LevelType type) {
        if (type == null) {
            return "Don't let the zombies reach your house!";
        }
        return switch (type) {
            case DEADLINE -> "Don't let any zombie cross the red deadline.";
            case CONVEYOR_BELT -> "Plant only the seed packets delivered by the conveyor.";
            case SAVE_OUR_SEEDS -> "Protect every marked plant until the final wave is defeated.";
            case TIMED_WAR -> "Complete the mission goals before their timers expire.";
            case LOVE_YOUR_PLANTS -> "Keep enough of your plants alive until the final wave.";
            case PLANT_WHAT_YOU_GET -> "Build your defense first, then start the zombie waves.";
            case NIGHT_OPS -> "Survive the night without falling sun from the sky.";
            case BOSS, MEOW_POINT -> "Defeat the boss and keep the zombies away from the house.";
            default -> "Don't let the zombies reach your house!";
        };
    }


    private boolean canPlantImitater(PlantStatus status, Position position) {
        if (status == null || !status.isAvailable() || this.imitaterCopyTarget == null) {
            return false;
        }
        model.plant.PlantDefinition imitater = this.game.getPlantDefinitions().findByName("Imitater");
        model.plant.PlantDefinition copied = this.game.getPlantDefinitions().findByName(this.imitaterCopyTarget);
        if (imitater == null || copied == null || "imitater".equals(normalize(copied.getName()))) {
            return false;
        }
        Plant copy = this.game.getPlantFactory().createImitater(imitater, copied);
        return this.game.getPlantingSystem().canPlant(copy, position);
    }

    private Plant findConveyorPlant(ConveyorBeltLevel conveyor, String plantName) {
        if (conveyor == null || conveyor.getConveyorPlants() == null) {
            return null;
        }
        String wanted = normalize(plantName);
        for (Plant plant : conveyor.getConveyorPlants()) {
            if (plant != null && normalize(plant.getName()).equals(wanted)) {
                return plant;
            }
        }
        return null;
    }

    private PlantStatus findPlantStatus(String plantName) {
        String normalizedName = normalize(plantName);
        for (PlantStatus status : getPlantStatuses()) {
            if (status == null || status.getPlant() == null) {
                continue;
            }
            if (normalize(status.getPlant().getName()).equals(normalizedName)) {
                return status;
            }
        }
        return null;
    }

    private String statusPlantName(PlantStatus status) {
        if (status == null || status.getPlant() == null) {
            return "";
        }
        return status.getPlant().getName();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
