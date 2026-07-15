package model.minigame.zombotanyminigame;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.GameEngine;
import model.mechanism.PlantCooldownManager;
import model.mechanism.PlantFoodSystem;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.SunSystem;
import model.mechanism.Wave;
import model.mechanism.WaveManager;
import model.mechanism.ZombieSpawner;
import model.minigame.zombotanyminigame.ZombotanyTrait;
import model.minigame.behavior.ZombotanyJalapenoBehavior;
import model.minigame.behavior.ZombotanyPeashooterBehavior;
import model.minigame.behavior.ZombotanySquashBehavior;
import model.minigame.behavior.ZombotanyWallnutBehavior;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieChapter;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.behavior.ZombieBehavior;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlantZombieZombotanyIntegration implements ZombotanyIntegration {

    private static final String PLANTS_RESOURCE =
            "data/plants.csv";

    private static final String ZOMBIES_RESOURCE =
            "data/zombies.json";

    private static final String ARMOR_RESOURCE =
            "data/ArmorTypeData.json";

    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;

    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;

    private final Map<Zombie, ZombotanyTrait>
            assignedZombieTraits;

    private Board board;
    private GameEngine engine;

    private SunSystem sunSystem;
    private PlantFoodSystem plantFoodSystem;
    private PlantCooldownManager cooldownManager;
    private PlantingSystem plantingSystem;
    private CombatSystem combatSystem;

    private ZombieSpawner zombieSpawner;
    private WaveManager waveManager;

    private ZombotanyStageConfig currentConfig;

    private List<PlantDefinition> availablePlants;
    private List<ZombieDefinition> availableZombies;

    private Map<ZombieDefinition, ZombotanyTrait>
            zombieTraits;

    public PlantZombieZombotanyIntegration() {
        this(loadBundledDefinitions());
    }

    private PlantZombieZombotanyIntegration(
            BundledDefinitions definitions
    ) {
        this(
                definitions.plants,
                definitions.zombies,
                new ZombieFactory(definitions.zombies)
        );
    }

    public PlantZombieZombotanyIntegration(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        if (plantDefinitions == null
                || zombieDefinitions == null) {
            throw new IllegalArgumentException(
                    "Zombotany requires plant and zombie definitions."
            );
        }

        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;

        this.plantFactory = new PlantFactory();

        this.zombieFactory = zombieFactory == null
                ? new ZombieFactory(zombieDefinitions)
                : zombieFactory;

        this.assignedZombieTraits =
                new IdentityHashMap<>();

        this.availablePlants = new ArrayList<>();
        this.availableZombies = new ArrayList<>();
        this.zombieTraits = new LinkedHashMap<>();
    }

    @Override
    public boolean isReady() {
        return board != null
                && engine != null
                && sunSystem != null
                && plantFoodSystem != null
                && plantingSystem != null
                && combatSystem != null
                && zombieSpawner != null
                && waveManager != null
                && currentConfig != null;
    }

    @Override
    public void prepareStage(
            ZombotanyStageConfig stageConfig
    ) {
        if (stageConfig == null) {
            throw new IllegalArgumentException(
                    "Zombotany stage configuration cannot be null."
            );
        }

        this.currentConfig = stageConfig;

        assignedZombieTraits.clear();

        configureAvailablePlants(stageConfig);
        configureZombieTraits(stageConfig);

        this.board = new Board();
        this.engine = new GameEngine(board);

        this.sunSystem = new SunSystem(
                board,
                engine.getClock()
        );

        this.plantFoodSystem =
                new PlantFoodSystem(board);

        this.cooldownManager =
                new PlantCooldownManager(
                        engine.getClock()
                );

        this.plantingSystem = new PlantingSystem(
                board,
                sunSystem,
                cooldownManager
        );

        this.combatSystem =
                new CombatSystem(board);

        ZombieDefinitionRepository filteredRepository =
                new FilteredZombieDefinitionRepository(
                        availableZombies
                );

        this.zombieSpawner = new ZombieSpawner(
                zombieFactory,
                filteredRepository,
                board
        );

        this.waveManager = new WaveManager(
                createWaves(stageConfig),
                zombieSpawner
        );

        engine.register(sunSystem);
        engine.register(cooldownManager);
        engine.register(combatSystem);
        engine.register(waveManager);

        if (stageConfig.getStageNumber() == 2) {
            sunSystem.addSun(25);
        } else if (stageConfig.getStageNumber() == 3) {
            sunSystem.addSun(50);
        }
    }

    @Override
    public boolean plant(
            String plantName,
            Position position
    ) {
        if (!isReady()
                || plantName == null
                || plantName.isBlank()) {
            return false;
        }

        Position boardPosition =
                toBoardPosition(position);

        if (!board.isInsideBoard(boardPosition)) {
            return false;
        }

        PlantDefinition definition =
                findAvailablePlant(plantName);

        if (definition == null) {
            return false;
        }

        Plant plant = plantFactory.create(definition);

        if (plant == null
                || !plantingSystem.canPlant(
                plant,
                boardPosition
        )) {
            return false;
        }

        plantingSystem.plant(
                plant,
                boardPosition
        );

        return plant.getBoard() == board;
    }

    @Override
    public int collectSun(Position position) {
        if (!isReady()) {
            return 0;
        }

        Position boardPosition =
                toBoardPosition(position);

        if (!board.isInsideBoard(boardPosition)) {
            return 0;
        }

        return sunSystem.collectSun(boardPosition);
    }

    @Override
    public boolean usePlantFood(Position position) {
        if (!isReady()) {
            return false;
        }

        Position boardPosition =
                toBoardPosition(position);

        if (!board.isInsideBoard(boardPosition)) {
            return false;
        }

        return plantFoodSystem.feedPlant(
                boardPosition
        );
    }

    @Override
    public void advanceOneTick() {
        if (!isReady()
                || !engine.isGameRunning()) {
            return;
        }

        engine.advanceTime(1);

        applyTraitsToNewZombies();
    }

    @Override
    public int getSunAmount() {
        return sunSystem == null
                ? 0
                : sunSystem.getSunAmount();
    }

    @Override
    public int getPlantFoodAmount() {
        return plantFoodSystem == null
                ? 0
                : plantFoodSystem.getPlantFoodAmount();
    }

    @Override
    public int getCurrentWaveNumber() {
        if (waveManager == null
                || waveManager.getCurrentWave() == null) {
            return 0;
        }

        return waveManager
                .getCurrentWave()
                .getNumber();
    }

    @Override
    public int getWaveCount() {
        return currentConfig == null
                ? 0
                : currentConfig.getWaveCount();
    }

    @Override
    public int getAliveZombieCount() {
        if (board == null) {
            return 0;
        }

        int count = 0;

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null
                    && !zombie.isDead()
                    && !zombie.isHypnotized()) {
                count++;
            }
        }

        return count;
    }

    @Override
    public boolean areAllWavesFinished() {
        if (!isReady()
                || waveManager.getCurrentWave() == null) {
            return false;
        }

        Wave currentWave =
                waveManager.getCurrentWave();

        return currentWave.isFinalWave()
                && !waveManager.hasNextWave()
                && getAliveZombieCount() == 0;
    }

    @Override
    public boolean isBrainEaten() {
        return board != null
                && board.isBrainEaten();
    }

    @Override
    public List<PlantDefinition> getAvailablePlants() {
        return Collections.unmodifiableList(
                new ArrayList<>(availablePlants)
        );
    }

    @Override
    public List<ZombieDefinition> getAvailableZombies() {
        return Collections.unmodifiableList(
                new ArrayList<>(availableZombies)
        );
    }

    @Override
    public Map<ZombieDefinition, ZombotanyTrait>
    getZombieTraits() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(zombieTraits)
        );
    }

    @Override
    public ZombotanyTrait getTrait(Zombie zombie) {
        if (zombie == null) {
            return null;
        }

        ZombotanyTrait assignedTrait =
                assignedZombieTraits.get(zombie);

        if (assignedTrait != null) {
            return assignedTrait;
        }

        return zombieTraits.get(
                zombie.getDefinition()
        );
    }

    @Override
    public Board getBoard() {
        return board;
    }

    private void configureAvailablePlants(
            ZombotanyStageConfig stageConfig
    ) {
        availablePlants = new ArrayList<>();

        for (String plantName
                : stageConfig.getAvailablePlantNames()) {
            PlantDefinition definition =
                    findPlantDefinition(plantName);

            if (definition != null
                    && !availablePlants.contains(definition)) {
                availablePlants.add(definition);
            }
        }

        if (availablePlants.isEmpty()) {
            for (PlantDefinition definition
                    : plantDefinitions.findAll()) {
                if (definition != null) {
                    availablePlants.add(definition);
                }

                if (availablePlants.size() >= 6) {
                    break;
                }
            }
        }

        if (availablePlants.isEmpty()) {
            throw new IllegalStateException(
                    "No plant definitions are available for Zombotany."
            );
        }
    }

    private void configureZombieTraits(
            ZombotanyStageConfig stageConfig
    ) {
        zombieTraits = new LinkedHashMap<>();
        availableZombies = new ArrayList<>();

        for (ZombotanyTrait trait
                : stageConfig.getAvailableTraits()) {

            ZombieDefinition definition =
                    findPreferredZombieForTrait(trait);

            if (definition == null
                    || availableZombies.contains(definition)) {
                definition =
                        findUnusedZombieDefinition();
            }

            if (definition == null) {
                continue;
            }

            availableZombies.add(definition);
            zombieTraits.put(definition, trait);
        }

        if (availableZombies.isEmpty()) {
            throw new IllegalStateException(
                    "No zombie definitions are available for Zombotany."
            );
        }
    }

    private ZombieDefinition findPreferredZombieForTrait(
            ZombotanyTrait trait
    ) {
        if (trait == null) {
            return null;
        }

        String[] preferredAliases;

        switch (trait) {
            case PEASHOOTER:
                preferredAliases = new String[]{
                        "ZombieDefault",
                        "ZombieTutorialDefault"
                };
                break;

            case WALLNUT:
                preferredAliases = new String[]{
                        "ZombieArmor2",
                        "ZombieTutorialArmor2"
                };
                break;

            case JALAPENO:
                preferredAliases = new String[]{
                        "ZombieArmor1",
                        "ZombieTutorialArmor1"
                };
                break;

            case SQUASH:
                preferredAliases = new String[]{
                        "ZombieArmor4",
                        "ZombieModernAllStar"
                };
                break;

            default:
                preferredAliases = new String[0];
        }

        for (String alias : preferredAliases) {
            ZombieDefinition definition =
                    zombieDefinitions.findByAlias(alias);

            if (isUsableZombieDefinition(definition)
                    && !availableZombies.contains(definition)) {
                return definition;
            }
        }

        return null;
    }

    private ZombieDefinition findUnusedZombieDefinition() {
        List<ZombieDefinition> definitions =
                zombieDefinitions.findAll();

        if (definitions == null) {
            return null;
        }

        for (ZombieDefinition definition : definitions) {
            if (isUsableZombieDefinition(definition)
                    && !availableZombies.contains(definition)) {
                return definition;
            }
        }

        return null;
    }

    private boolean isUsableZombieDefinition(
            ZombieDefinition definition
    ) {
        return definition != null
                && definition.getWavePointCost() > 0
                && definition.getHitpoints() > 0;
    }

    private List<Wave> createWaves(
            ZombotanyStageConfig stageConfig
    ) {
        List<Wave> waves = new ArrayList<>();

        List<Integer> difficulties =
                stageConfig.getWaveDifficulties();

        for (int index = 0;
             index < difficulties.size();
             index++) {

            boolean finalWave =
                    index == difficulties.size() - 1;

            waves.add(
                    new Wave(
                            index + 1,
                            difficulties.get(index),
                            finalWave
                    )
            );
        }

        return waves;
    }

    private void applyTraitsToNewZombies() {
        if (board == null) {
            return;
        }

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null
                    || zombie.isDead()
                    || zombie.getWave() == null
                    || assignedZombieTraits
                    .containsKey(zombie)) {
                continue;
            }

            ZombotanyTrait trait =
                    zombieTraits.get(
                            zombie.getDefinition()
                    );

            if (trait == null) {
                continue;
            }

            ZombieBehavior behavior =
                    createTraitBehavior(trait);

            if (behavior == null) {
                continue;
            }

            zombie.addBehavior(behavior);

            assignedZombieTraits.put(
                    zombie,
                    trait
            );
        }
    }

    private ZombieBehavior createTraitBehavior(
            ZombotanyTrait trait
    ) {
        if (trait == null) {
            return null;
        }

        return switch (trait) {
            case PEASHOOTER -> new ZombotanyPeashooterBehavior();
            case WALLNUT -> new ZombotanyWallnutBehavior();
            case JALAPENO -> new ZombotanyJalapenoBehavior();
            case SQUASH -> new ZombotanySquashBehavior();
            default -> null;
        };
    }

    private PlantDefinition findAvailablePlant(
            String name
    ) {
        if (name == null) {
            return null;
        }

        for (PlantDefinition definition
                : availablePlants) {
            if (definition != null
                    && definition.getName() != null
                    && definition.getName()
                    .equalsIgnoreCase(name.trim())) {
                return definition;
            }
        }

        return null;
    }

    private PlantDefinition findPlantDefinition(
            String name
    ) {
        if (name == null) {
            return null;
        }

        PlantDefinition exact =
                plantDefinitions.findByName(name);

        if (exact != null) {
            return exact;
        }

        for (PlantDefinition definition
                : plantDefinitions.findAll()) {
            if (definition != null
                    && definition.getName() != null
                    && definition.getName()
                    .equalsIgnoreCase(name.trim())) {
                return definition;
            }
        }

        return null;
    }

    private Position toBoardPosition(
            Position externalPosition
    ) {
        if (externalPosition == null) {
            return null;
        }

        return new Position(
                externalPosition.getX() - 1,
                externalPosition.getY() - 1
        );
    }

    private static BundledDefinitions loadBundledDefinitions() {
        try {
            PlantDefinitionRepository plants =
                    CsvPlantDefinitionRepository
                            .fromClasspath(
                                    PLANTS_RESOURCE
                            );

            ZombieDefinitionRepository zombies =
                    JsonZombieDefinitionRepository
                            .fromClasspath(
                                    ZOMBIES_RESOURCE,
                                    ARMOR_RESOURCE
                            );

            return new BundledDefinitions(
                    plants,
                    zombies
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load bundled Zombotany definitions.",
                    exception
            );
        }
    }

    private static class BundledDefinitions {

        private final PlantDefinitionRepository plants;
        private final ZombieDefinitionRepository zombies;

        private BundledDefinitions(
                PlantDefinitionRepository plants,
                ZombieDefinitionRepository zombies
        ) {
            this.plants = plants;
            this.zombies = zombies;
        }
    }

    private static class FilteredZombieDefinitionRepository implements ZombieDefinitionRepository {

        private final List<ZombieDefinition> definitions;

        private FilteredZombieDefinitionRepository(
                List<ZombieDefinition> definitions
        ) {
            this.definitions =
                    definitions == null
                            ? new ArrayList<>()
                            : new ArrayList<>(definitions);
        }

        @Override
        public ZombieDefinition findByAlias(
                String alias
        ) {
            if (alias == null) {
                return null;
            }

            for (ZombieDefinition definition
                    : definitions) {
                if (definition != null
                        && definition.getAlias() != null
                        && definition.getAlias()
                        .equalsIgnoreCase(alias)) {
                    return definition;
                }
            }

            return null;
        }

        @Override
        public List<ZombieDefinition> findByChapter(
                ZombieChapter chapter
        ) {
            List<ZombieDefinition> result =
                    new ArrayList<>();

            if (chapter == null) {
                return result;
            }

            for (ZombieDefinition definition
                    : definitions) {
                if (definition == null) {
                    continue;
                }

                ZombieChapter definitionChapter =
                        definition.getChapter();

                if (chapter
                        == ZombieChapter.ALL_CHAPTERS
                        || definitionChapter == null
                        || definitionChapter
                        == ZombieChapter.ALL_CHAPTERS
                        || definitionChapter == chapter) {
                    result.add(definition);
                }
            }

            return result;
        }

        @Override
        public List<ZombieDefinition> findAll() {
            return new ArrayList<>(definitions);
        }
    }
}