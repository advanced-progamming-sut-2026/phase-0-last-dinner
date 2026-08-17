package college.java.project.graphics;

import controller.CollectionController;
import controller.GameController;
import model.collection.CollectionActionResult;
import model.collection.CollectionStateResult;
import model.collection.PlantCollectionState;

import java.util.Collections;
import java.util.List;


public final class ControllerPlantCollectionDataSource implements PlantCollectionDataSource {
    private final CollectionController controller;
    private final GameController gameController;
    private final Runnable saveAction;
    private int gold;
    private boolean debugModeEnabled;
    private String loadErrorMessage = "";

    public ControllerPlantCollectionDataSource(CollectionController controller) {
        this(controller, null, false, null);
    }

    public ControllerPlantCollectionDataSource(
            CollectionController controller,
            GameController gameController
    ) {
        this(controller, gameController, false, null);
    }

    public ControllerPlantCollectionDataSource(
            CollectionController controller,
            GameController gameController,
            boolean debugModeEnabled
    ) {
        this(controller, gameController, debugModeEnabled, null);
    }

    public ControllerPlantCollectionDataSource(
            CollectionController controller,
            GameController gameController,
            boolean debugModeEnabled,
            Runnable saveAction
    ) {
        if (controller == null) {
            throw new IllegalArgumentException("Collection controller is required");
        }
        this.controller = controller;
        this.gameController = gameController;
        this.debugModeEnabled = debugModeEnabled;
        this.saveAction = saveAction;
    }

    @Override
    public List<PlantCollectionState> getPlants() {
        CollectionStateResult result = this.controller.onShowAllPlantsRequested();
        if (result == null || !result.isSuccessful()) {
            this.loadErrorMessage = result == null
                    ? "Unable to load plant collection."
                    : result.getMessage();
            return Collections.emptyList();
        }
        this.loadErrorMessage = "";
        this.gold = result.getGold();
        return result.getPlants() == null ? Collections.emptyList() : result.getPlants();
    }

    @Override
    public int getGold() {
        return this.gameController == null ? this.gold : this.gameController.coinWallet();
    }

    @Override
    public int getGems() {
        return this.gameController == null ? 0 : this.gameController.gemWallet();
    }

    @Override
    public CollectionActionResult upgradePlant(String plantName) {
        CollectionActionResult result = this.controller.onUpgradePlantRequested(plantName);
        refreshGold();
        if (result != null && result.isSuccessful()) {
            save();
        }
        return result;
    }

    @Override
    public CollectionActionResult purchasePlant(String plantName) {
        CollectionActionResult result = this.controller.onPurchasePlantRequested(plantName);
        refreshGold();
        if (result != null && result.isSuccessful()) {
            save();
        }
        return result;
    }

    @Override
    public boolean isDebugModeEnabled() {
        return this.debugModeEnabled;
    }

    @Override
    public boolean supportsCurrencyCheats() {
        return this.gameController != null;
    }

    @Override
    public void setDebugModeEnabled(boolean enabled) {
        this.debugModeEnabled = enabled;
    }

    @Override
    public void cheatAddGold(int amount) {
        if (this.gameController != null && amount > 0) {
            this.gameController.onCheatAddRequested(amount, "coin");
            this.gold = this.gameController.coinWallet();
        }
    }

    @Override
    public void cheatAddGems(int amount) {
        if (this.gameController != null && amount > 0) {
            this.gameController.onCheatAddRequested(amount, "diamond");
        }
    }

    @Override
    public String getLoadErrorMessage() {
        return this.loadErrorMessage;
    }

    @Override
    public void save() {
        if (this.saveAction != null) {
            this.saveAction.run();
        }
    }

    private void refreshGold() {
        CollectionStateResult result = this.controller.onShowAllPlantsRequested();
        if (result != null && result.isSuccessful()) {
            this.gold = result.getGold();
        }
    }
}
