package college.java.project.graphics;

import controller.ApplicationController;
import controller.CollectionController;
import controller.GameController;
import controller.PlantPickController;
import model.User.User;
import model.collection.CollectionActionResult;
import model.collection.CollectionStateResult;
import model.collection.PlantCollectionState;
import model.plant.PlantDefinition;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapts the existing Phase 1 controllers to the graphical plant picker without
 * changing Model or Controller code.
 */
public final class ControllerPlantPickDataSource implements PlantPickDataSource {
    private final PlantPickController plantPickController;
    private final CollectionController collectionController;
    private final User user;
    private final GameController gameController;
    private final Runnable saveAction;
    private final int slotCount;
    private boolean debugModeEnabled;

    public ControllerPlantPickDataSource(
            PlantPickController plantPickController,
            CollectionController collectionController,
            User user
    ) {
        this(
                plantPickController,
                collectionController,
                user,
                null,
                PlantPickController.DEFAULT_SLOT_COUNT,
                false,
                null
        );
    }

    public ControllerPlantPickDataSource(
            PlantPickController plantPickController,
            CollectionController collectionController,
            User user,
            ApplicationController applicationController
    ) {
        this(
                plantPickController,
                collectionController,
                user,
                null,
                PlantPickController.DEFAULT_SLOT_COUNT,
                false,
                applicationController == null ? null : applicationController::save
        );
    }

    public ControllerPlantPickDataSource(
            PlantPickController plantPickController,
            CollectionController collectionController,
            User user,
            GameController gameController,
            int slotCount,
            boolean debugModeEnabled
    ) {
        this(
                plantPickController,
                collectionController,
                user,
                gameController,
                slotCount,
                debugModeEnabled,
                null
        );
    }

    private ControllerPlantPickDataSource(
            PlantPickController plantPickController,
            CollectionController collectionController,
            User user,
            GameController gameController,
            int slotCount,
            boolean debugModeEnabled,
            Runnable saveAction
    ) {
        if (plantPickController == null) {
            throw new IllegalArgumentException("Plant pick controller is required");
        }
        if (collectionController == null) {
            throw new IllegalArgumentException("Collection controller is required");
        }
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        if (slotCount <= 0) {
            throw new IllegalArgumentException("Plant slot count must be positive");
        }
        user.initializeMissingFields();
        this.plantPickController = plantPickController;
        this.collectionController = collectionController;
        this.user = user;
        this.gameController = gameController;
        this.saveAction = saveAction;
        this.slotCount = slotCount;
        this.debugModeEnabled = debugModeEnabled;
    }

    @Override
    public List<PlantCollectionState> getPlants() {
        CollectionStateResult result = this.collectionController.onShowAllPlantsRequested();
        if (result == null || !result.isSuccessful() || result.getPlants() == null) {
            return Collections.emptyList();
        }
        return result.getPlants();
    }

    @Override
    public boolean isAvailable(String plantName) {
        return normalizedSet(this.plantPickController.showAvailablePlants())
                .contains(normalize(plantName));
    }

    @Override
    public boolean isSelected(String plantName) {
        String normalizedName = normalize(plantName);
        for (PlantDefinition definition : this.plantPickController.getSelectedPlants()) {
            if (definition != null && normalize(definition.getName()).equals(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBoosted(String plantName) {
        String normalizedName = normalize(plantName);
        for (String boosted : this.plantPickController.getBoostedPlantNames()) {
            if (normalize(boosted).equals(normalizedName)) {
                return true;
            }
        }
        return isGreenhouseBoosted(plantName);
    }

    @Override
    public boolean isGreenhouseBoosted(String plantName) {
        return this.user.getGreenhouse() != null
                && this.user.getGreenhouse().hasStoredBoost(plantName);
    }

    @Override
    public int getSelectedCount() {
        return this.plantPickController.getSelectedPlants().size();
    }

    @Override
    public int getSlotCount() {
        return this.slotCount;
    }

    @Override
    public int getCoins() {
        return this.gameController == null
                ? Math.max(0, this.user.getGold())
                : Math.max(0, this.gameController.coinWallet());
    }

    @Override
    public int getGems() {
        return this.gameController == null
                ? Math.max(0, this.user.getDiamond())
                : Math.max(0, this.gameController.gemWallet());
    }

    @Override
    public String togglePlant(String plantName) {
        String result = isSelected(plantName)
                ? this.plantPickController.removePlant(plantName)
                : this.plantPickController.addPlant(plantName);
        if (actionSucceeded(result)) {
            save();
        }
        return result;
    }

    @Override
    public String boostPlant(String plantName) {
        if (isGreenhouseBoosted(plantName)) {
            return "Boost already available from Greenhouse.";
        }
        String result = this.plantPickController.boostPlant(plantName);
        if (actionSucceeded(result)) {
            save();
        }
        return result;
    }

    @Override
    public CollectionActionResult upgradePlant(String plantName) {
        CollectionActionResult result = this.collectionController.onUpgradePlantRequested(plantName);
        if (result != null && result.isSuccessful()) {
            save();
        }
        return result;
    }

    @Override
    public String startGame() {
        return this.plantPickController.startGame();
    }

    @Override
    public boolean isStarted() {
        return this.plantPickController.isStarted();
    }

    @Override
    public boolean isDebugModeEnabled() {
        return this.debugModeEnabled;
    }

    @Override
    public boolean supportsCurrencyCheats() {
        return true;
    }

    @Override
    public void setDebugModeEnabled(boolean enabled) {
        this.debugModeEnabled = enabled;
    }

    @Override
    public void cheatAddCoins(int amount) {
        if (amount <= 0) {
            return;
        }
        if (this.gameController != null) {
            this.gameController.onCheatAddRequested(amount, "coin");
            return;
        }
        this.user.setGold(safeAdd(this.user.getGold(), amount));
    }

    @Override
    public void cheatAddGems(int amount) {
        if (amount <= 0) {
            return;
        }
        if (this.gameController != null) {
            this.gameController.onCheatAddRequested(amount, "diamond");
            return;
        }
        this.user.setDiamond(safeAdd(this.user.getDiamond(), amount));
    }

    @Override
    public void save() {
        if (this.saveAction != null) {
            this.saveAction.run();
        }
    }

    private boolean actionSucceeded(String message) {
        return message != null
                && (message.endsWith(" was added.")
                || message.endsWith(" was removed.")
                || message.endsWith(" was boosted."));
    }

    private int safeAdd(int current, int amount) {
        long value = (long) Math.max(0, current) + amount;
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private Set<String> normalizedSet(List<String> names) {
        Set<String> normalized = new HashSet<>();
        if (names == null) {
            return normalized;
        }
        for (String name : names) {
            normalized.add(normalize(name));
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
