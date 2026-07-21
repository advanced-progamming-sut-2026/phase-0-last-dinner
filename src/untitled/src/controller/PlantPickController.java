package controller;

import model.Menu.MenuType;
import model.Plant;
import model.User.User;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import view.PlantPickView;
import view.PlantPickViewObserver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlantPickController implements MenuController, PlantPickViewObserver {
    public static final int DEFAULT_SLOT_COUNT = 8;
    public static final int BOOST_COST = 2;

    private final User user;
    private final PlantDefinitionRepository plantDefinitions;
    // null yani marhale mahdudiate giah nadare
    private final List<Plant> levelAvailablePlants;
    private final int slotCount;
    private final List<PlantDefinition> selectedPlants;
    private final Set<String> boostedPlantNames;
    private boolean started;

    public PlantPickController(
            User user,
            PlantDefinitionRepository plantDefinitions
    ) {
        this(null, user, plantDefinitions, null, DEFAULT_SLOT_COUNT);
    }

    public PlantPickController(
            User user,
            PlantDefinitionRepository plantDefinitions,
            List<Plant> levelAvailablePlants,
            int slotCount
    ) {
        this(null, user, plantDefinitions, levelAvailablePlants, slotCount);
    }

    public PlantPickController(
            PlantPickView view,
            User user,
            PlantDefinitionRepository plantDefinitions,
            List<Plant> levelAvailablePlants,
            int slotCount
    ) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        if (plantDefinitions == null) {
            throw new IllegalArgumentException("Plant definitions are required");
        }
        if (slotCount <= 0) {
            throw new IllegalArgumentException("Plant slot count must be positive");
        }

        user.initializeMissingFields();
        this.user = user;
        this.plantDefinitions = plantDefinitions;
        this.levelAvailablePlants = levelAvailablePlants;
        this.slotCount = slotCount;
        this.selectedPlants = new ArrayList<>();
        this.boostedPlantNames = new LinkedHashSet<>();

        if (view != null) {
            view.setObserver(this);
        }
    }

    @Override
    public MenuType getCurrentMenu() {
        return MenuType.PLANT_PICK_MENU;
    }

    @Override
    public void changeMenu() {
    }

    @Override
    public List<String> onShowAllPlantsRequested() {
        return this.showAllPlants();
    }

    @Override
    public List<String> onShowAvailablePlantsRequested() {
        return this.showAvailablePlants();
    }

    @Override
    public String onAddPlantRequested(String plantName) {
        return this.addPlant(plantName);
    }

    @Override
    public String onRemovePlantRequested(String plantName) {
        return this.removePlant(plantName);
    }

    @Override
    public String onBoostPlantRequested(String plantName) {
        return this.boostPlant(plantName);
    }

    @Override
    public String onStartGameRequested() {
        return this.startGame();
    }

    public List<String> showAllPlants() {
        List<String> names = new ArrayList<>();

        for (PlantDefinition definition : this.plantDefinitions.findAll()) {
            if (definition != null && definition.getName() != null) {
                names.add(definition.getName());
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<String> showAvailablePlants() {
        List<String> names = new ArrayList<>();

        for (PlantDefinition definition : this.plantDefinitions.findAll()) {
            if (definition != null
                    && this.isPlantUnlocked(definition.getName())
                    && this.isAvailableInLevel(definition.getName())) {
                names.add(definition.getName());
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String addPlant(String plantName) {
        if (this.started) {
            return "Game has already started.";
        }

        PlantDefinition definition = this.findPlantDefinition(plantName);

        if (definition == null) {
            return "Plant was not found.";
        }
        if (!this.isPlantUnlocked(definition.getName())) {
            return "Plant is locked.";
        }
        if (!this.isAvailableInLevel(definition.getName())) {
            return "Plant is not available in this level.";
        }
        if (this.findSelectedPlant(definition.getName()) != null) {
            return "Plant is already selected.";
        }
        if (this.selectedPlants.size() >= this.slotCount) {
            return "Plant selection is full.";
        }

        this.selectedPlants.add(definition);
        return definition.getName() + " was added.";
    }

    public String removePlant(String plantName) {
        if (this.started) {
            return "Game has already started.";
        }

        PlantDefinition definition = this.findPlantDefinition(plantName);

        if (definition == null) {
            return "Plant was not found.";
        }

        PlantDefinition selected = this.findSelectedPlant(definition.getName());

        if (selected == null) {
            return "Plant is not selected.";
        }

        this.selectedPlants.remove(selected);
        return definition.getName() + " was removed.";
    }

    public String boostPlant(String plantName) {
        if (this.started) {
            return "Game has already started.";
        }

        PlantDefinition definition = this.findPlantDefinition(plantName);

        if (definition == null) {
            return "Plant was not found.";
        }
        if (this.findSelectedPlant(definition.getName()) == null) {
            return "Plant is not selected.";
        }
        if (this.isBoosted(definition.getName())) {
            return "Plant is already boosted.";
        }
        if (this.user.getDiamond() < BOOST_COST) {
            return "Not enough diamonds. Required: " + BOOST_COST;
        }

        this.user.setDiamond(this.user.getDiamond() - BOOST_COST);
        this.boostedPlantNames.add(definition.getName());
        return definition.getName() + " was boosted.";
    }

    public String startGame() {
        if (this.started) {
            return "Game has already started.";
        }
        if (this.selectedPlants.isEmpty()) {
            return "Select at least one plant before starting the game.";
        }

        this.started = true;
        return "Game started.";
    }

    public List<PlantDefinition> getSelectedPlants() {
        return this.selectedPlants;
    }

    public Set<String> getBoostedPlantNames() {
        return this.boostedPlantNames;
    }

    public int getSlotCount() {
        return this.slotCount;
    }

    public boolean isStarted() {
        return this.started;
    }

    private PlantDefinition findPlantDefinition(String plantName) {
        String normalizedName = this.normalize(plantName);

        if (normalizedName.isEmpty()) {
            return null;
        }

        for (PlantDefinition definition : this.plantDefinitions.findAll()) {
            if (definition != null
                    && this.normalize(definition.getName()).equals(normalizedName)) {
                return definition;
            }
        }

        return null;
    }

    private PlantDefinition findSelectedPlant(String plantName) {
        String normalizedName = this.normalize(plantName);

        for (PlantDefinition definition : this.selectedPlants) {
            if (definition != null
                    && this.normalize(definition.getName()).equals(normalizedName)) {
                return definition;
            }
        }

        return null;
    }

    private boolean isPlantUnlocked(String plantName) {
        String normalizedName = this.normalize(plantName);

        for (Plant plant : this.user.getUnlockedPlants()) {
            if (plant != null
                    && this.normalize(plant.getName()).equals(normalizedName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAvailableInLevel(String plantName) {
        if (this.levelAvailablePlants == null) {
            return true;
        }

        String normalizedName = this.normalize(plantName);

        for (Plant plant : this.levelAvailablePlants) {
            if (plant != null
                    && this.normalize(plant.getName()).equals(normalizedName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBoosted(String plantName) {
        String normalizedName = this.normalize(plantName);

        for (String boostedPlantName : this.boostedPlantNames) {
            if (this.normalize(boostedPlantName).equals(normalizedName)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
