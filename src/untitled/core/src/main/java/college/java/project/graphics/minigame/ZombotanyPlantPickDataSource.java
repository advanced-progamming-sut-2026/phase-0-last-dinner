package college.java.project.graphics.minigame;

import college.java.project.graphics.PlantPickDataSource;
import controller.ZombotanyController;
import model.User.User;
import model.collection.CollectionActionResult;
import model.collection.CollectionActionStatus;
import model.collection.PlantCollectionState;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.plant.PlantDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ZombotanyPlantPickDataSource implements PlantPickDataSource {

    private static final int SLOT_COUNT = 8;

    private final ZombotanyMiniGame game;
    private final ZombotanyController controller;
    private final User user;
    private final Runnable saveAction;

    public ZombotanyPlantPickDataSource(ZombotanyMiniGame game, ZombotanyController controller, User user, Runnable saveAction) {
        if (game == null || controller == null)
            throw new IllegalArgumentException("Zombotany game and controller are required.");

        this.game = game;
        this.controller = controller;
        this.user = user;
        this.saveAction = saveAction;

        if (this.user != null) this.user.initializeMissingFields();
    }

    @Override
    public List<PlantCollectionState> getPlants() {
        if (this.user == null) return Collections.emptyList();

        List<PlantCollectionState> states = new ArrayList<>();

        for (PlantDefinition definition : this.game.getAvailablePlants()) {

            if (definition == null) continue;

            PlantCollectionState state = PlantCollectionState.from(definition, this.user.getPlantUpgradeService(), true);

            if (state != null) states.add(state);
        }

        return states;
    }

    @Override
    public boolean isAvailable(String plantName) {
        return this.game.findAvailablePlant(plantName);
    }

    @Override
    public boolean isSelected(String plantName) {
        return this.game.isPlantSelected(plantName);
    }

    @Override
    public boolean isBoosted(String plantName) {
        return false;
    }

    @Override
    public boolean isGreenhouseBoosted(String plantName) {
        return false;
    }

    @Override
    public int getSelectedCount() {
        return this.game.getSelectedPlants().size();
    }

    @Override
    public int getSlotCount() {
        return SLOT_COUNT;
    }

    @Override
    public int getCoins() {
        return this.user == null ? 0 : Math.max(0, this.user.getGold());
    }

    @Override
    public int getGems() {
        return this.user == null ? 0 : Math.max(0, this.user.getDiamond());
    }

    @Override
    public String togglePlant(String plantName) {
        if (plantName == null || plantName.trim().isEmpty()) return "Select a valid plant.";

        boolean removing = isSelected(plantName);

        ZombotanyActionResult result = removing ? this.controller.onRemovePlantRequested(plantName) :
            this.controller.onAddPlantRequested(plantName);

        if (result == null) return "Plant selection failed.";

        if (!result.isSuccessful()) return result.getMessage();

        save();

        return plantName.trim() + (removing ? " was removed." : " was added.");
    }

    @Override
    public String boostPlant(String plantName) {
        return "Boosts are unavailable in Zombotany.";
    }

    @Override
    public CollectionActionResult upgradePlant(String plantName) {
        return CollectionActionResult.failure(CollectionActionStatus.INVALID,
            "Upgrades are unavailable in Zombotany.", plantName, 0, 0, getCoins());
    }

    @Override
    public String startGame() {
        ZombotanyActionResult result = this.controller.onStartGameRequested();

        if (result == null) return "Zombotany could not be started.";

        if (result.isSuccessful()) save();

        return result.getMessage();
    }

    @Override
    public boolean isStarted() {
        return this.game.isStarted();
    }

    @Override
    public boolean supportsUpgrades() {
        return false;
    }

    @Override
    public boolean supportsBoosts() {
        return false;
    }

    @Override
    public void save() {
        if (this.saveAction != null) this.saveAction.run();
    }
}
