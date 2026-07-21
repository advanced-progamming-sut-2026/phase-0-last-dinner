package model.minigame.zombotanyminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.minigame.StageProgressMiniGame;
import model.minigame.zombotanyminigame.PlantZombieZombotanyIntegration;
import model.minigame.zombotanyminigame.ZombotanyIntegration;
import model.minigame.zombotanyminigame.ZombotanyStageConfig;
import model.plant.PlantDefinition;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class ZombotanyMiniGame extends MiniGame implements StageProgressMiniGame {
    private static final int PLANT_SLOT_COUNT = 8;

    private final ZombotanyIntegration integration;

    private Map<ZombieDefinition, ZombotanyTrait>
            zombieTraits;

    private List<ZombieDefinition> availableZombies;
    private List<PlantDefinition> availablePlants;
    private Set<String> selectedPlantNames;
    private boolean plantSelectionPrepared;

    private int currentStageNumber;
    private int highestUnlockedStage;

    private boolean lost;

    public ZombotanyMiniGame() {
        this(new PlantZombieZombotanyIntegration());
    }

    public ZombotanyMiniGame(
            ZombotanyIntegration integration
    ) {
        super(MiniGameType.ZOMBOTANY);

        if (integration == null) {
            throw new IllegalArgumentException(
                    "Zombotany integration cannot be null."
            );
        }

        this.integration = integration;

        this.zombieTraits = new LinkedHashMap<>();
        this.availableZombies = new ArrayList<>();
        this.availablePlants = new ArrayList<>();
        this.selectedPlantNames = new LinkedHashSet<>();

        this.currentStageNumber = 1;
        this.highestUnlockedStage = 1;
        this.lost = false;
    }

    @Override
    public void start() {
        startStage(1);
    }

    public boolean startStage(int stageNumber) {
        if (!this.preparePlantSelection(stageNumber)) {
            return false;
        }

        for (PlantDefinition definition : this.availablePlants) {
            if (definition != null) {
                this.addSelectedPlant(definition.getName());
            }
        }
        return this.startSelectedStage();
    }

    public boolean preparePlantSelection(int stageNumber) {
        if (stageNumber < 1
                || stageNumber > 3
                || stageNumber
                > highestUnlockedStage) {
            return false;
        }

        ZombotanyStageConfig config =
                ZombotanyStageConfig.forStage(
                        stageNumber
                );

        integration.prepareStage(config);

        if (!integration.isReady()) {
            return false;
        }

        this.currentStageNumber = stageNumber;
        this.lost = false;
        this.plantSelectionPrepared = true;
        this.selectedPlantNames.clear();

        this.zombieTraits =
                new LinkedHashMap<>(
                        integration.getZombieTraits()
                );

        this.availableZombies =
                new ArrayList<>(
                        integration.getAvailableZombies()
                );

        this.availablePlants =
                new ArrayList<>(
                        integration.getAvailablePlants()
                );

        setBoard(integration.getBoard());

        setCurrentStage(
                getStages().get(stageNumber - 1)
        );

        setCompleted(false);
        setStarted(false);

        return true;
    }

    public boolean addSelectedPlant(String plantName) {
        PlantDefinition definition = this.findAvailablePlantDefinition(plantName);
        if (!this.plantSelectionPrepared || isStarted() || definition == null
                || this.selectedPlantNames.size() >= PLANT_SLOT_COUNT) {
            return false;
        }

        return this.selectedPlantNames.add(this.normalizePlantName(definition.getName()));
    }

    public boolean removeSelectedPlant(String plantName) {
        if (!this.plantSelectionPrepared || isStarted()) {
            return false;
        }

        return this.selectedPlantNames.remove(this.normalizePlantName(plantName));
    }

    public boolean startSelectedStage() {
        if (!this.plantSelectionPrepared || isStarted() || this.selectedPlantNames.isEmpty()) {
            return false;
        }

        markStarted();
        return true;
    }

    public boolean plant(
            String plantName,
            Position position
    ) {
        if (!canPerformAction()) {
            return false;
        }

        if (!this.selectedPlantNames.contains(this.normalizePlantName(plantName))) {
            return false;
        }

        return integration.plant(
                plantName,
                position
        );
    }

    public int collectSun(Position position) {
        if (!canPerformAction()) {
            return 0;
        }

        return integration.collectSun(position);
    }

    public boolean usePlantFood(
            Position position
    ) {
        if (!canPerformAction()) {
            return false;
        }

        return integration.usePlantFood(position);
    }

    @Override
    public void onTick() {
        if (!canPerformAction()) {
            return;
        }

        integration.advanceOneTick();

        if (integration.isBrainEaten()) {
            lost = true;
            markCompleted();
            return;
        }

        if (integration.areAllWavesFinished()) {
            lost = false;

            if (currentStageNumber < 3) {
                highestUnlockedStage = Math.max(
                        highestUnlockedStage,
                        currentStageNumber + 1
                );
            }

            if (currentStageNumber >= 3) {
                markAllStagesCompleted();
            } else {
                markCompleted();
            }
        }
    }

    @Override
    public boolean isWinConditionMet() {
        return isStarted()
                && isCompleted()
                && !lost
                && integration.areAllWavesFinished();
    }

    @Override
    public boolean isLoseConditionMet() {
        return lost
                || integration.isBrainEaten();
    }

    public ZombotanyTrait getTrait(
            ZombieDefinition definition
    ) {
        if (definition == null) {
            return null;
        }

        return zombieTraits.get(definition);
    }

    public ZombotanyTrait getTrait(Zombie zombie) {
        return integration.getTrait(zombie);
    }

    public boolean findAvailablePlant(
            String plantName
    ) {
        return this.findAvailablePlantDefinition(plantName) != null;
    }

    public boolean isPlantSelected(String plantName) {
        return this.selectedPlantNames.contains(this.normalizePlantName(plantName));
    }

    public List<PlantDefinition> getSelectedPlants() {
        List<PlantDefinition> selected = new ArrayList<>();
        for (PlantDefinition definition : this.availablePlants) {
            if (definition != null && this.isPlantSelected(definition.getName())) {
                selected.add(definition);
            }
        }
        return selected;
    }

    private PlantDefinition findAvailablePlantDefinition(
            String plantName
    ) {
        if (plantName == null
                || plantName.trim().isEmpty()) {
            return null;
        }

        for (PlantDefinition definition
                : availablePlants) {
            if (definition != null
                    && definition.getName() != null
                    && definition.getName()
                    .equalsIgnoreCase(
                            plantName.trim()
                    )) {
                return definition;
            }
        }

        return null;
    }

    private String normalizePlantName(String plantName) {
        return plantName == null ? "" : plantName.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean canPerformAction() {
        return isStarted()
                && !isCompleted()
                && !lost
                && integration.isReady();
    }

    public Map<ZombieDefinition, ZombotanyTrait>
    getZombieTraits() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(zombieTraits)
        );
    }

    public List<ZombieDefinition>
    getAvailableZombies() {
        return Collections.unmodifiableList(
                new ArrayList<>(availableZombies)
        );
    }

    public List<PlantDefinition>
    getAvailablePlants() {
        return Collections.unmodifiableList(
                new ArrayList<>(availablePlants)
        );
    }

    public int getSunAmount() {
        return integration.getSunAmount();
    }

    public int getPlantFoodAmount() {
        return integration.getPlantFoodAmount();
    }

    public int getCurrentWaveNumber() {
        return integration.getCurrentWaveNumber();
    }

    public int getWaveCount() {
        return integration.getWaveCount();
    }

    public int getAliveZombieCount() {
        return integration.getAliveZombieCount();
    }

    @Override
    public void restoreHighestUnlockedStage(int stageNumber) {
        this.highestUnlockedStage = Math.max(1, Math.min(3, stageNumber));
    }
}
