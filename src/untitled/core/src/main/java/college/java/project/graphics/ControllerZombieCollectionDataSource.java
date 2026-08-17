package college.java.project.graphics;

import controller.CollectionController;
import controller.GameController;
import model.collection.CollectionStateResult;
import model.collection.ZombieCollectionState;

import java.util.Collections;
import java.util.List;


public final class ControllerZombieCollectionDataSource implements ZombieCollectionDataSource {
    private final CollectionController controller;
    private final GameController gameController;
    private final Runnable saveAction;
    private int gold;
    private boolean debugModeEnabled;
    private String loadErrorMessage = "";

    public ControllerZombieCollectionDataSource(CollectionController controller) {
        this(controller, null, false, null);
    }

    public ControllerZombieCollectionDataSource(
            CollectionController controller,
            GameController gameController
    ) {
        this(controller, gameController, false, null);
    }

    public ControllerZombieCollectionDataSource(
            CollectionController controller,
            GameController gameController,
            boolean debugModeEnabled
    ) {
        this(controller, gameController, debugModeEnabled, null);
    }

    public ControllerZombieCollectionDataSource(
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
    public List<ZombieCollectionState> loadZombies() {
        CollectionStateResult result = this.controller.onShowAllZombiesRequested();
        if (result == null || !result.isSuccessful()) {
            this.loadErrorMessage = result == null
                    ? "Unable to load zombie collection."
                    : result.getMessage();
            return Collections.emptyList();
        }
        this.loadErrorMessage = "";
        this.gold = result.getGold();
        return result.getZombies() == null ? Collections.emptyList() : result.getZombies();
    }

    @Override
    public int getGemCount() {
        return this.gameController == null ? 0 : this.gameController.gemWallet();
    }

    @Override
    public int getCoinCount() {
        return this.gameController == null ? this.gold : this.gameController.coinWallet();
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
    public void cheatAddCoins(int amount) {
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
}
