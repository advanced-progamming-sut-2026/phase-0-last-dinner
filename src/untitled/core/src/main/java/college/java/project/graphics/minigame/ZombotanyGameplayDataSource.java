package college.java.project.graphics.minigame;

import model.Plant;
import model.User.User;
import model.chapters.ChapterType;
import model.collection.PlantCollectionState;
import model.mechanism.PlantStatus;
import model.mechanism.Position;
import model.mechanism.Wave;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

public final class ZombotanyGameplayDataSource extends MiniGameGameplayDataSource {
    private final ZombotanyMiniGame game;
    private final PlantFactory plantFactory = new PlantFactory();
    private final Map<String, Plant> statusPlants = new HashMap<>();

    public ZombotanyGameplayDataSource(ZombotanyMiniGame game, User user, LongSupplier currentTickSupplier) {
        this(game, user, currentTickSupplier, null);
    }

    public ZombotanyGameplayDataSource(ZombotanyMiniGame game, User user, LongSupplier currentTickSupplier,
                                       ChapterType chapterType) {
        super(game, user, currentTickSupplier, chapterType);

        if (game == null)
            throw new IllegalArgumentException("Zombotany game is required.");

        this.game = game;
    }


    @Override
    public List<PlantCollectionState> getSelectedPlants() {
        List<PlantCollectionState> states = new ArrayList<>();

        for (PlantDefinition definition : this.game.getSelectedPlants()) {
            PlantCollectionState state = createCollectionState(definition);

            if (state != null) states.add(state);
        }

        return states;
    }

    @Override
    public List<PlantStatus> getPlantStatuses() {
        List<PlantStatus> statuses = new ArrayList<>();
        int sunAmount = this.game.getSunAmount();

        for (PlantDefinition definition : this.game.getSelectedPlants()) {
            if (definition == null || definition.getName() == null) continue;

            Plant plant = statusPlant(definition);

            if (plant == null) continue;

            long cooldown = this.game.getRemainingCooldownTicks(definition.getName());
            boolean available = cooldown <= 0L && sunAmount >= definition.getCost();

            statuses.add(new PlantStatus(plant, available, cooldown));
        }

        return statuses;
    }

    @Override
    public boolean plant(String plantName, int column, int row) {
        ZombotanyActionResult result = new controller.ZombotanyController(this.game).onPlantRequested(plantName,
            new Position(column + 1, row + 1));

        return result.isSuccessful();
    }

    @Override
    public boolean canPlant(String plantName, int column, int row) {
        PlantDefinition definition = findSelectedPlant(plantName);

        if (definition == null) return false;

        if (this.game.getSunAmount() < definition.getCost()) return false;

        if (this.game.getRemainingCooldownTicks(definition.getName()) > 0L) return false;

        return !hasPlantAt(column, row);
    }

    private PlantStatus findPlantStatus(String plantName) {
        if (plantName == null) return null;

        for (PlantStatus status : getPlantStatuses()) {
            if (status == null || status.getPlant() == null || status.getPlant().getName() == null) {
                continue;
            }

            if (status.getPlant().getName().equalsIgnoreCase(plantName)) {
                return status;
            }
        }

        return null;
    }

    @Override
    public boolean canFeedPlantAt(int column, int row) {
        Plant plant = getTopPlantAt(column, row);

        return this.game.getPlantFoodAmount() > 0 && plant != null && plant.canReceivePlantFood();
    }

    @Override
    public boolean feedPlant(int column, int row) {
        ZombotanyActionResult result = new controller.ZombotanyController(this.game).onUsePlantFoodRequested(
            new Position(column + 1, row + 1));

        return result.isSuccessful();
    }

    @Override
    public boolean isBoosted(String plantName) {
        return false;
    }

    private Plant statusPlant(PlantDefinition definition) {
        String key = normalize(definition.getName());
        Plant existing = this.statusPlants.get(key);

        if (existing != null) return existing;

        Plant created = this.plantFactory.create(definition);

        if (created != null) this.statusPlants.put(key, created);

        return created;
    }

    private PlantDefinition findSelectedPlant(String plantName) {
        String expectedName = normalize(plantName);

        for (PlantDefinition definition : this.game.getSelectedPlants()) {
            if (definition != null && normalize(definition.getName()).equals(expectedName)) return definition;
        }

        return null;
    }

    private PlantCollectionState createCollectionState(PlantDefinition definition) {
        if (definition == null) return null;

        return new PlantCollectionState(definition.getName(), true, 1,
            1, 0, 0, 0, definition.getCost(), definition.getBaseHealth(),
            definition.getDamageExpression(), definition.getActionIntervalSeconds(), definition.getRechargeSeconds(),
            definition.getCategories(), definition.getTags(), definition.getBaseAbilityDescription(),
            definition.getPlantFoodEffectDescription(), definition.getLevelUpEffects());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public Wave getCurrentWave() {
        return this.game.getCurrentWave();
    }

    @Override
    public int getWaveIndex() {
        return Math.max(0, this.game.getCurrentWaveNumber() - 1);
    }

    @Override
    public int getWaveCount() {
        return Math.max(0, this.game.getWaveCount());
    }

    @Override
    public boolean shouldShowMissionAtStart() {
        return true;
    }

    @Override
    public String getMissionTitle() {
        return "ZOMBOTANY - STAGE " + this.game.getCurrentStageNumber();
    }

    @Override
    public String getMissionDescription() {
        return switch (this.game.getCurrentStageNumber()) {
            case 1 -> "Defeat Peashooter and Wall-nut zombies.";
            case 2 -> "Watch out for explosive Jalapeno zombies.";
            case 3 -> "Survive every plant-powered zombie trait.";
            default -> "Defeat the plant-powered zombies.";
        };
    }

    @Override
    public String getPlantingFailureMessage(String plantName, int column, int row) {
        if (plantName == null || plantName.trim().isEmpty()) return "Select a plant first.";

        if (hasPlantAt(column, row)) return "That tile is already occupied.";

        PlantStatus status = findPlantStatus(plantName);

        if (status != null && status.getRemainingCooldownTicks() > 0) return "This plant is still recharging.";

        if (status != null && getSunAmount() < status.getSunCost()) return "You do not have enough sun.";

        return "This plant cannot be placed there.";
    }


}
