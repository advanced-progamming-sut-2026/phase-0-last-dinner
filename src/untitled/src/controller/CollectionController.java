package controller;

import model.Menu.MenuType;
import model.Plant;
import model.User.User;
import model.collection.CollectionActionResult;
import model.collection.CollectionActionStatus;
import model.collection.CollectionStateResult;
import model.collection.PlantCollectionState;
import model.collection.ZombieCollectionState;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.plant.PlantUpgradeResult;
import model.plant.PlantUpgradeService;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import view.collection.CollectionView;
import view.collection.CollectionViewObserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CollectionController implements MenuController, CollectionViewObserver {
    private static final int PLANT_PURCHASE_COST = 2000;

    private final PlantDefinitionRepository plantDefinitions;
    private final PlantUpgradeService plantUpgrades;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final User user;

    public CollectionController() {
        this(null, new PlantUpgradeService());
    }

    public CollectionController(
            PlantDefinitionRepository plantDefinitions,
            PlantUpgradeService plantUpgrades
    ) {
        if (plantUpgrades == null)
            throw new IllegalArgumentException("Plant upgrade service is required");
        this.plantDefinitions = plantDefinitions;
        this.plantUpgrades = plantUpgrades;
        this.zombieDefinitions = null;
        this.user = null;
    }

    public CollectionController(
            CollectionView view,
            User user,
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions
    ) {
        if (view == null)
            throw new IllegalArgumentException("Collection view is required");
        if (user == null)
            throw new IllegalArgumentException("User is required");
        if (plantDefinitions == null)
            throw new IllegalArgumentException("Plant definitions are required");
        if (zombieDefinitions == null)
            throw new IllegalArgumentException("Zombie definitions are required");
        user.initializeMissingFields();
        this.user = user;
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.plantUpgrades = user.getPlantUpgradeService();
        view.setObserver(this);
    }

    @Override
    public MenuType getCurrentMenu() {
        return MenuType.COLLECTION_MENU;
    }

    @Override
    public void changeMenu() {
    }

    public PlantUpgradeResult upgradePlant(String plantName) {
        if (this.plantDefinitions == null) {
            return PlantUpgradeResult.PLANT_NOT_FOUND;
        }
        PlantDefinition definition = this.findPlantDefinition(plantName);
        if (definition == null) {
            return PlantUpgradeResult.PLANT_NOT_FOUND;
        }
        if (this.user == null) {
            return this.plantUpgrades.upgrade(definition);
        }
        if (!this.isPlantUnlocked(definition.getName())) {
            return PlantUpgradeResult.PLANT_NOT_FOUND;
        }
        PlantUpgradeResult result = this.plantUpgrades.upgrade(
                definition,
                this.user.getGold()
        );
        if (result == PlantUpgradeResult.SUCCESS) {
            this.user.setGold(this.plantUpgrades.getCoins());
        }
        return result;
    }

    public PlantUpgradeService getPlantUpgradeService() {
        return this.plantUpgrades;
    }

    @Override
    public CollectionStateResult onShowUnlockedPlantsRequested() {
        if (this.user == null) {
            return CollectionStateResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    0
            );
        }
        List<PlantCollectionState> states = new ArrayList<>();
        Set<String> addedNames = new HashSet<>();
        for (Plant plant : this.user.getUnlockedPlants()) {
            if (plant == null || plant.getName() == null)
                continue;
            PlantDefinition definition = this.findPlantDefinition(plant.getName());
            if (definition == null)
                continue;
            String normalizedName = this.normalize(definition.getName());
            if (!addedNames.add(normalizedName))
                continue;
            PlantCollectionState state = PlantCollectionState.from(definition,
                    this.plantUpgrades,
                    true
            );
            if (state != null)
                states.add(state);
        }
        this.sortPlantStates(states);
        return CollectionStateResult.plants(
                states,
                this.user.getGold()
        );
    }

    @Override
    public CollectionStateResult onShowAllPlantsRequested() {
        if (this.user == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    0);
        List<PlantCollectionState> states = new ArrayList<>();
        List<PlantDefinition> definitions = this.plantDefinitions.findAll();
        if (definitions != null) {
            for (PlantDefinition definition : definitions) {
                if (definition == null)
                    continue;
                PlantCollectionState state = PlantCollectionState.from(definition,
                        this.plantUpgrades,
                        this.isPlantUnlocked(definition.getName()));
                if (state != null)
                    states.add(state);
            }
        }
        this.sortPlantStates(states);
        return CollectionStateResult.plants(
                states,
                this.user.getGold()
        );
    }

    @Override
    public CollectionStateResult onShowEncounteredZombiesRequested() {
        if (this.user == null)
            return CollectionStateResult.failure(CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.", 0);
        if (this.zombieDefinitions == null)
            return CollectionStateResult.failure(CollectionActionStatus.INVALID,
                    "Zombie definitions are not available.", this.user.getGold());
        List<ZombieCollectionState> states = new ArrayList<>();
        Set<String> addedAliases = new HashSet<>();
        for (String alias : this.user.getEncounteredZombieAliases()) {
            ZombieDefinition definition = this.findZombieDefinition(alias);
            if (definition == null || !addedAliases.add(this.normalize(definition.getAlias())))
                continue;
            ZombieCollectionState state = ZombieCollectionState.from(definition, true);
            states.add(state);
        }
        this.sortZombieStates(states);
        return CollectionStateResult.zombies(states, this.user.getGold());
    }

    @Override
    public CollectionStateResult onShowAllZombiesRequested() {
        if (this.user == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    0);
        if (this.zombieDefinitions == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.INVALID,
                    "Zombie definitions are not available.",
                    this.user.getGold());
        List<ZombieCollectionState> states = new ArrayList<>();
        List<ZombieDefinition> definitions = this.zombieDefinitions.findAll();
        if (definitions != null) {
            for (ZombieDefinition definition : definitions) {
                if (definition == null)
                    continue;
                ZombieCollectionState state = ZombieCollectionState.from(
                        definition,
                        this.isZombieEncountered(definition));
                states.add(state);
            }
        }
        this.sortZombieStates(states);
        return CollectionStateResult.zombies(
                states,
                this.user.getGold()
        );
    }

    @Override
    public CollectionStateResult onShowPlantRequested(String plantName) {
        if (this.user == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    0);
        PlantDefinition definition = this.findPlantDefinition(plantName);
        if (definition == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.PLANT_NOT_FOUND,
                    "Plant was not found.",
                    this.user.getGold());
        PlantCollectionState state = PlantCollectionState.from(
                definition,
                this.plantUpgrades,
                this.isPlantUnlocked(definition.getName())
        );
        List<PlantCollectionState> states = new ArrayList<>();
        states.add(state);
        return CollectionStateResult.plants(
                states,
                this.user.getGold()
        );
    }

    @Override
    public CollectionStateResult onShowZombieRequested(String zombieName) {
        if (this.user == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    0);
        ZombieDefinition definition = this.findZombieDefinition(zombieName);
        if (definition == null)
            return CollectionStateResult.failure(
                    CollectionActionStatus.ZOMBIE_NOT_FOUND,
                    "Zombie was not found.",
                    this.user.getGold());
        ZombieCollectionState state = ZombieCollectionState.from(
                definition,
                this.isZombieEncountered(definition)
        );
        List<ZombieCollectionState> states = new ArrayList<>();
        states.add(state);
        return CollectionStateResult.zombies(
                states,
                this.user.getGold()
        );
    }

    @Override
    public CollectionActionResult onUpgradePlantRequested(String plantName) {
        if (this.user == null) {
            return CollectionActionResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    plantName,
                    0,
                    0,
                    0);
        }
        PlantDefinition definition = this.findPlantDefinition(plantName);
        if (definition == null) {
            return this.actionFailure(
                    CollectionActionStatus.PLANT_NOT_FOUND,
                    "Plant was not found.",
                    plantName);
        }
        if (!this.isPlantUnlocked(definition.getName())) {
            return this.actionFailure(
                    CollectionActionStatus.PLANT_NOT_UNLOCKED,
                    "Plant is not unlocked.",
                    definition.getName());
        }
        int previousLevel = this.plantUpgrades.getLevel(definition.getName());
        int spentCoins = this.plantUpgrades.requiredCoins(previousLevel);
        PlantUpgradeResult upgradeResult = this.plantUpgrades.upgrade(
                definition,
                this.user.getGold()
        );
        if (upgradeResult != PlantUpgradeResult.SUCCESS) {
            return this.upgradeFailure(
                    upgradeResult,
                    definition.getName());
        }
        this.user.setGold(this.plantUpgrades.getCoins());
        int currentLevel = this.plantUpgrades.getLevel(definition.getName());
        int remainingSeedPackets = this.plantUpgrades.getSeedPackets(definition.getName());
        return CollectionActionResult.plantUpgraded(
                definition.getName(),
                previousLevel,
                currentLevel,
                remainingSeedPackets,
                this.user.getGold(),
                spentCoins);
    }

    @Override
    public CollectionActionResult onPurchasePlantRequested(String plantName) {
        if (this.user == null) {
            return CollectionActionResult.failure(
                    CollectionActionStatus.USER_NOT_AVAILABLE,
                    "User is not available.",
                    plantName,
                    0,
                    0,
                    0);
        }
        PlantDefinition definition = this.findPlantDefinition(plantName);
        if (definition == null) {
            return this.actionFailure(
                    CollectionActionStatus.PLANT_NOT_FOUND,
                    "Plant was not found.",
                    plantName);
        }
        if (this.isPlantUnlocked(definition.getName())) {
            return this.actionFailure(
                    CollectionActionStatus.PLANT_ALREADY_UNLOCKED,
                    "Plant is already unlocked.",
                    definition.getName());
        }
        if (this.user.getGold() < PLANT_PURCHASE_COST) {
            return this.actionFailure(
                    CollectionActionStatus.NOT_ENOUGH_COINS,
                    "Not enough coins. Required: "
                            + PLANT_PURCHASE_COST,
                    definition.getName());
        }
        PlantFactory plantFactory = new PlantFactory(this.plantUpgrades);
        Plant plant = plantFactory.create(definition);
        if (plant == null) {
            return this.actionFailure(
                    CollectionActionStatus.INVALID,
                    "Could not create the selected plant.",
                    definition.getName());
        }
        this.user.getUnlockedPlants().add(plant);
        this.user.addNews("New plant unlocked: " + definition.getName());
        this.user.setGold(this.user.getGold() - PLANT_PURCHASE_COST);
        return CollectionActionResult.plantPurchased(
                definition.getName(),
                this.user.getGold()
        );
    }

    private PlantDefinition findPlantDefinition(String plantName) {
        String normalizedName = this.normalize(plantName);
        if (normalizedName.isEmpty()
                || this.plantDefinitions == null
                || this.plantDefinitions.findAll() == null)
            return null;
        for (PlantDefinition definition : this.plantDefinitions.findAll()) {
            if (definition != null
                    && this.normalize(definition.getName())
                    .equals(normalizedName))
                return definition;
        }
        return null;
    }

    private ZombieDefinition findZombieDefinition(String zombieName) {
        String normalizedName = this.normalize(zombieName);
        if (normalizedName.isEmpty()
                || this.zombieDefinitions == null
                || this.zombieDefinitions.findAll() == null)
            return null;
        for (ZombieDefinition definition : this.zombieDefinitions.findAll()) {
            if (definition == null)
                continue;
            boolean aliasMatches = this.normalize(definition.getAlias()).equals(normalizedName);
            boolean displayNameMatches = this.normalize(definition.getDisplayName()).equals(normalizedName);
            if (aliasMatches || displayNameMatches)
                return definition;
        }
        return null;
    }

    private boolean isPlantUnlocked(String plantName) {
        if (this.user == null)
            return false;
        String normalizedName = this.normalize(plantName);
        if (normalizedName.isEmpty())
            return false;
        for (Plant plant : this.user.getUnlockedPlants()) {
            if (plant != null && this.normalize(plant.getName()).equals(normalizedName))
                return true;
        }
        return false;
    }

    private boolean isZombieEncountered(ZombieDefinition definition) {
        return this.user != null
                && definition != null
                && this.user.hasEncounteredZombie(definition.getAlias());
    }

    private CollectionActionResult upgradeFailure(
            PlantUpgradeResult upgradeResult,
            String plantName
    ) {
        CollectionActionStatus status;
        String message;
        switch (upgradeResult) {
            case PLANT_NOT_FOUND:
                status = CollectionActionStatus.PLANT_NOT_FOUND;
                message = "Plant was not found.";
                break;
            case MAXIMUM_LEVEL_REACHED:
                status = CollectionActionStatus.MAXIMUM_LEVEL_REACHED;
                message = "Plant has reached its maximum level.";
                break;
            case NOT_ENOUGH_SEED_PACKETS:
                status = CollectionActionStatus.NOT_ENOUGH_SEED_PACKETS;
                message = "Not enough seed packets.";
                break;
            case NOT_ENOUGH_COINS:
                status = CollectionActionStatus.NOT_ENOUGH_COINS;
                message = "Not enough coins.";
                break;
            default:
                status = CollectionActionStatus.INVALID;
                message = "Plant upgrade failed.";
                break;
        }
        return this.actionFailure(
                status,
                message,
                plantName
        );
    }

    private CollectionActionResult actionFailure(
            CollectionActionStatus status,
            String message,
            String plantName
    ) {
        int currentLevel = this.plantUpgrades.getLevel(plantName);
        int seedPackets = this.plantUpgrades.getSeedPackets(plantName);
        return CollectionActionResult.failure(
                status,
                message,
                plantName,
                currentLevel,
                seedPackets,
                this.user == null ? 0 : this.user.getGold()
        );
    }

    private void sortPlantStates(
            List<PlantCollectionState> states
    ) {
        states.sort(Comparator.comparing(PlantCollectionState::getName,
                String.CASE_INSENSITIVE_ORDER));
    }

    private void sortZombieStates(List<ZombieCollectionState> states) {
        states.sort(Comparator.comparing(ZombieCollectionState::getDisplayName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(Locale.ROOT);
    }
}
