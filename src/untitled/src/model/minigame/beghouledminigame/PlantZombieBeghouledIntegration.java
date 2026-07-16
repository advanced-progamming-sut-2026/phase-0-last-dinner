package model.minigame.beghouledminigame;

import lombok.Getter;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.GameEngine;
import model.mechanism.PlantFoodSystem;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.mechanism.Wave;
import model.mechanism.WaveManager;
import model.mechanism.ZombieSpawner;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

import java.io.IOException;
import java.util.*;

@Getter
public class PlantZombieBeghouledIntegration
        implements BeghouledIntegration {

    private static final String PLANTS_RESOURCE =
            "data/plants.csv";

    private static final String ZOMBIES_RESOURCE =
            "data/zombies.json";

    private static final String ARMOR_RESOURCE =
            "data/ArmorTypeData.json";

    private static final int COLUMN_COUNT = 9;

    private static final int ROW_COUNT = 5;

    private static final int ENDLESS_WAVE_COUNT = 300;

    private static final String[] UPGRADE_SOURCES = {
            "Peashooter",
            "Repeater",
            "Wall-nut",
            "Puff-shroom",
            "Cabbage-pult",
            "Melon-pult"
    };

    private static final String[] UPGRADE_TARGETS = {
            "Repeater",
            "Mega Gatling Pea",
            "Tall-nut",
            "Fume-shroom",
            "Melon-pult",
            "Winter Melon"
    };

    private static final int[] UPGRADE_COSTS = {
            500,
            1500,
            500,
            250,
            1000,
            750
    };

    private static final int[][] STAGE_UPGRADE_INDEXES = {
            {0, 2, 3, 4, 5},
            {0, 1, 2, 3, 4},
            {1, 2, 4, 5}
    };

    private final PlantDefinitionRepository plantDefinitions;

    private final ZombieDefinitionRepository zombieDefinitions;

    private final PlantFactory plantFactory;

    private final ZombieFactory zombieFactory;

    private final Random random;

    private Board board;

    private GameEngine engine;

    private CombatSystem combatSystem;

    private PlantFoodSystem plantFoodSystem;

    private PlantingSystem plantingSystem;

    private ZombieSpawner zombieSpawner;

    private WaveManager waveManager;

    private List<PlantDefinition> currentPlantTypes;

    private int currentStageNumber;

    public PlantZombieBeghouledIntegration() {
        this(loadBundledDefinitions());
    }

    private PlantZombieBeghouledIntegration(
            BundledDefinitions definitions
    ) {
        this(
                definitions.plants,
                definitions.zombies,
                new ZombieFactory(definitions.zombies),
                new Random()
        );
    }

    public PlantZombieBeghouledIntegration(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        this(
                plantDefinitions,
                zombieDefinitions,
                zombieFactory,
                new Random()
        );
    }

    public PlantZombieBeghouledIntegration(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            Random random
    ) {
        if (plantDefinitions == null
                || zombieDefinitions == null) {

            throw new IllegalArgumentException(
                    "Beghouled requires plant "
                            + "and zombie definitions."
            );
        }

        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.plantFactory = new PlantFactory();

        this.zombieFactory = zombieFactory == null
                ? new ZombieFactory(zombieDefinitions)
                : zombieFactory;

        this.random = random == null ? new Random() : random;

        currentPlantTypes = new ArrayList<>();
    }

    @Override
    public boolean isReady() {
        return board != null
                && engine != null
                && combatSystem != null
                && plantingSystem != null
                && zombieSpawner != null
                && waveManager != null;
    }

    @Override
    public void prepareStage(
            int stageNumber,
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    ) {
        requireValidStage(stageNumber);
        requireFivePlantTypes(plantTypes);

        currentStageNumber = stageNumber;
        currentPlantTypes =
                new ArrayList<>(plantTypes);

        board = new Board();
        engine = new GameEngine(board);
        combatSystem = new CombatSystem(board);
        plantFoodSystem = new PlantFoodSystem(board);

        plantingSystem = new PlantingSystem(
                board,
                null,
                null
        );

        zombieSpawner = new ZombieSpawner(
                zombieFactory,
                zombieDefinitions,
                board
        );

        waveManager = new WaveManager(
                createEndlessWaves(stageNumber),
                zombieSpawner
        );

        engine.register(combatSystem);
        engine.register(waveManager);

        resetBoard(
                currentPlantTypes,
                craters
        );
    }

    @Override
    public PlantDefinition findPlantDefinition(
            String plantName
    ) {
        if (plantName == null
                || plantName.trim().isEmpty()) {

            return null;
        }

        for (PlantDefinition definition
                : plantDefinitions.findAll()) {

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

    @Override
    public List<PlantUpgradeOption>
    createUpgradeOptions(int stageNumber) {
        requireValidStage(stageNumber);

        List<PlantUpgradeOption> options =
                new ArrayList<>();

        for (int upgradeIndex
                : STAGE_UPGRADE_INDEXES[
                stageNumber - 1
                ]) {

            PlantDefinition source =
                    findPlantDefinition(
                            UPGRADE_SOURCES[
                                    upgradeIndex
                                    ]
                    );

            PlantDefinition target =
                    findPlantDefinition(
                            UPGRADE_TARGETS[
                                    upgradeIndex
                                    ]
                    );

            if (source == null || target == null) {
                continue;
            }

            options.add(
                    new PlantUpgradeOption(
                            source,
                            target,
                            UPGRADE_COSTS[
                                    upgradeIndex
                                    ]
                    )
            );
        }

        return options;
    }

    @Override
    public PlantDefinition getPlantAt(
            Position userPosition
    ) {
        if (!isReady()) {
            return null;
        }

        Plant plant = getRuntimePlantAt(
                userPosition
        );

        if (plant == null) {
            return null;
        }

        return findPlantDefinition(
                plant.getName()
        );
    }

    @Override
    public boolean swapPlants(
            Position first,
            Position second
    ) {
        if (!isReady()
                || first == null
                || second == null) {

            return false;
        }

        Position firstBoardPosition =
                toBoardPosition(first);

        Position secondBoardPosition =
                toBoardPosition(second);

        if (!board.isInsideBoard(firstBoardPosition)
                || !board.isInsideBoard(
                secondBoardPosition
        )) {

            return false;
        }

        Tile firstTile =
                board.getTile(firstBoardPosition);

        Tile secondTile =
                board.getTile(secondBoardPosition);

        if (firstTile == null
                || secondTile == null
                || firstTile.getTerrainType()
                == TerrainType.CRATER
                || secondTile.getTerrainType()
                == TerrainType.CRATER) {

            return false;
        }

        Plant firstPlant =
                getTopPlant(firstTile);

        Plant secondPlant =
                getTopPlant(secondTile);

        if (firstPlant == null
                || secondPlant == null) {

            return false;
        }

        board.removePlant(firstPlant);
        board.removePlant(secondPlant);

        placeExistingPlant(
                firstPlant,
                secondBoardPosition
        );

        placeExistingPlant(
                secondPlant,
                firstBoardPosition
        );

        return true;
    }

    @Override
    public void removePlants(
            Set<Position> positions
    ) {
        if (!isReady() || positions == null) {
            return;
        }

        for (Position position : positions) {
            Plant plant =
                    getRuntimePlantAt(position);

            if (plant != null) {
                board.removePlant(plant);
            }
        }
    }

    @Override
    public void collapseAndRefill(
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    ) {
        if (!isReady()) {
            return;
        }

        requireFivePlantTypes(plantTypes);

        for (int column = 0;
             column < COLUMN_COUNT;
             column++) {

            collapseColumn(
                    column,
                    plantTypes,
                    craters
            );
        }
    }

    @Override
    public void resetBoard(
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    ) {
        if (!isReady()) {
            return;
        }

        requireFivePlantTypes(plantTypes);

        List<Plant> existingPlants =
                new ArrayList<>(
                        board.getAllPlants()
                );

        for (Plant plant : existingPlants) {
            board.removePlant(plant);
        }

        for (int y = 0;
             y < ROW_COUNT;
             y++) {

            for (int x = 0;
                 x < COLUMN_COUNT;
                 x++) {

                Position boardPosition =
                        new Position(x, y);

                Position userPosition =
                        toUserPosition(
                                boardPosition
                        );

                boolean crater =
                        craters != null
                                && craters.contains(
                                userPosition
                        );

                board.setTerrain(
                        boardPosition,
                        crater
                                ? TerrainType.CRATER
                                : TerrainType.CLASSIC
                );
            }
        }

        fillBoardWithoutImmediateMatches(
                plantTypes,
                craters
        );
    }

    @Override
    public Set<Position>
    findDestroyedPlantPositions(
            Set<Position> craters
    ) {
        Set<Position> destroyedPositions =
                new HashSet<>();

        if (!isReady()) {
            return destroyedPositions;
        }

        for (int y = 0;
             y < ROW_COUNT;
             y++) {

            for (int x = 0;
                 x < COLUMN_COUNT;
                 x++) {

                Position boardPosition =
                        new Position(x, y);

                Position userPosition =
                        toUserPosition(
                                boardPosition
                        );

                if (craters != null
                        && craters.contains(
                        userPosition
                )) {
                    continue;
                }

                Tile tile =
                        board.getTile(boardPosition);

                if (tile == null
                        || tile.getTerrainType()
                        == TerrainType.CRATER) {

                    continue;
                }

                if (tile.getPlants() == null
                        || tile.getPlants().isEmpty()) {

                    destroyedPositions.add(
                            userPosition
                    );
                }
            }
        }

        return destroyedPositions;
    }

    @Override
    public boolean createCrater(
            Position userPosition
    ) {
        if (!isReady() || userPosition == null) {
            return false;
        }

        Position boardPosition =
                toBoardPosition(userPosition);

        if (!board.isInsideBoard(boardPosition)) {
            return false;
        }

        Plant plant =
                getRuntimePlantAt(userPosition);

        if (plant != null) {
            board.removePlant(plant);
        }

        return board.setTerrain(
                boardPosition,
                TerrainType.CRATER
        );
    }

    @Override
    public int upgradePlants(
            PlantUpgradeOption upgradeOption
    ) {
        if (!isReady() || upgradeOption == null) {
            return 0;
        }

        String sourceName =
                upgradeOption
                        .getSourcePlant()
                        .getName();

        List<Plant> sourcePlants =
                new ArrayList<>();

        for (Plant plant : board.getAllPlants()) {
            if (plant != null
                    && plant.getName() != null
                    && plant.getName()
                    .equalsIgnoreCase(sourceName)) {

                sourcePlants.add(plant);
            }
        }

        int upgradedCount = 0;

        for (Plant sourcePlant : sourcePlants) {
            Position position =
                    sourcePlant.getPosition();

            if (position == null) {
                continue;
            }

            board.removePlant(sourcePlant);

            Plant upgradedPlant =
                    createPlant(
                            upgradeOption
                                    .getTargetPlant()
                    );

            if (upgradedPlant == null) {
                placeExistingPlant(
                        sourcePlant,
                        position
                );
                continue;
            }

            placeExistingPlant(
                    upgradedPlant,
                    position
            );

            upgradedCount++;
        }

        return upgradedCount;
    }

    @Override
    public void advanceOneTick() {
        if (isReady()) {
            engine.advanceTime(1);
        }
    }

    @Override
    public void destroyAllZombies() {
        if (!isReady()) {
            return;
        }

        List<Zombie> zombies =
                new ArrayList<>(
                        board.getAllZombies()
                );

        for (Zombie zombie : zombies) {
            if (zombie != null) {
                combatSystem
                        .killZombieIgnoringAllegiance(
                                zombie
                        );
            }
        }
    }

    @Override
    public boolean isBrainEaten() {
        return board != null
                && board.isBrainEaten();
    }

    private void collapseColumn(
            int column,
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    ) {
        List<Plant> survivingPlants =
                new ArrayList<>();

        for (int row = ROW_COUNT - 1;
             row >= 0;
             row--) {

            Position userPosition =
                    new Position(
                            column + 1,
                            row + 1
                    );

            if (craters != null
                    && craters.contains(userPosition)) {

                continue;
            }

            Plant plant =
                    getRuntimePlantAt(userPosition);

            if (plant != null) {
                survivingPlants.add(plant);
            }
        }

        for (Plant plant
                : new ArrayList<>(
                survivingPlants
        )) {

            board.removePlant(plant);
        }

        int survivingIndex = 0;

        for (int row = ROW_COUNT - 1;
             row >= 0;
             row--) {

            Position userPosition =
                    new Position(
                            column + 1,
                            row + 1
                    );

            Position boardPosition =
                    new Position(column, row);

            if (craters != null
                    && craters.contains(userPosition)) {

                board.setTerrain(
                        boardPosition,
                        TerrainType.CRATER
                );

                continue;
            }

            board.setTerrain(
                    boardPosition,
                    TerrainType.CLASSIC
            );

            Plant plant;

            if (survivingIndex
                    < survivingPlants.size()) {

                plant = survivingPlants.get(
                        survivingIndex
                );

                survivingIndex++;
            } else {
                plant = createRandomPlant(
                        plantTypes
                );
            }

            placeExistingPlant(
                    plant,
                    boardPosition
            );
        }
    }

    private void fillBoardWithoutImmediateMatches(
            List<PlantDefinition> plantTypes,
            Set<Position> craters
    ) {
        for (int y = 0;
             y < ROW_COUNT;
             y++) {

            for (int x = 0;
                 x < COLUMN_COUNT;
                 x++) {

                Position boardPosition =
                        new Position(x, y);

                Position userPosition =
                        toUserPosition(
                                boardPosition
                        );

                if (craters != null
                        && craters.contains(
                        userPosition
                )) {
                    continue;
                }

                PlantDefinition definition =
                        chooseDefinitionWithoutMatch(
                                boardPosition,
                                plantTypes
                        );

                Plant plant =
                        createPlant(definition);

                placeExistingPlant(
                        plant,
                        boardPosition
                );
            }
        }
    }

    private PlantDefinition chooseDefinitionWithoutMatch(
            Position boardPosition,
            List<PlantDefinition> plantTypes
    ) {
        List<PlantDefinition> shuffled =
                new ArrayList<>(plantTypes);

        Collections.shuffle(
                shuffled,
                random
        );

        for (PlantDefinition definition
                : shuffled) {

            if (!wouldCreateImmediateMatch(
                    boardPosition,
                    definition
            )) {
                return definition;
            }
        }

        return shuffled.get(0);
    }

    private boolean wouldCreateImmediateMatch(
            Position position,
            PlantDefinition definition
    ) {
        if (position == null
                || definition == null
                || definition.getName() == null) {

            return false;
        }

        String name = definition.getName();

        boolean horizontalMatch =
                hasPlantNameAt(
                        position.getX() - 1,
                        position.getY(),
                        name
                )
                        && hasPlantNameAt(
                        position.getX() - 2,
                        position.getY(),
                        name
                );

        boolean verticalMatch =
                hasPlantNameAt(
                        position.getX(),
                        position.getY() - 1,
                        name
                )
                        && hasPlantNameAt(
                        position.getX(),
                        position.getY() - 2,
                        name
                );

        return horizontalMatch
                || verticalMatch;
    }

    private boolean hasPlantNameAt(
            int x,
            int y,
            String plantName
    ) {
        Position position =
                new Position(x, y);

        if (!board.isInsideBoard(position)) {
            return false;
        }

        Tile tile = board.getTile(position);
        Plant plant = getTopPlant(tile);

        return plant != null
                && plant.getName() != null
                && plant.getName()
                .equalsIgnoreCase(plantName);
    }

    private Plant getRuntimePlantAt(
            Position userPosition
    ) {
        if (userPosition == null || board == null) {
            return null;
        }

        Position boardPosition =
                toBoardPosition(userPosition);

        if (!board.isInsideBoard(boardPosition)) {
            return null;
        }

        Tile tile = board.getTile(boardPosition);

        return getTopPlant(tile);
    }

    private Plant getTopPlant(Tile tile) {
        if (tile == null
                || tile.getPlants() == null
                || tile.getPlants().isEmpty()) {

            return null;
        }

        return tile.getPlants().get(
                tile.getPlants().size() - 1
        );
    }

    private Plant createRandomPlant(
            List<PlantDefinition> plantTypes
    ) {
        PlantDefinition definition =
                plantTypes.get(
                        random.nextInt(
                                plantTypes.size()
                        )
                );

        return createPlant(definition);
    }

    private Plant createPlant(
            PlantDefinition definition
    ) {
        if (definition == null) {
            return null;
        }

        Plant plant =
                plantFactory.create(definition);

        if (plant != null) {
            plant.makePermanent();
        }

        return plant;
    }

    private void placeExistingPlant(
            Plant plant,
            Position boardPosition
    ) {
        if (plant == null
                || boardPosition == null
                || !board.isInsideBoard(boardPosition)) {

            return;
        }

        Tile tile = board.getTile(boardPosition);

        if (tile == null
                || tile.getTerrainType()
                == TerrainType.CRATER) {

            return;
        }

        plant.setPosition(boardPosition);
        plant.setBoard(board);
        tile.addPlant(plant);
    }

    private List<Wave> createEndlessWaves(
            int stageNumber
    ) {
        List<Wave> waves =
                new ArrayList<>();

        int startingCost =
                200 + stageNumber * 100;

        for (int index = 0;
             index < ENDLESS_WAVE_COUNT;
             index++) {

            int costIncrease =
                    (index / 5) * 50;

            int waveCost = Math.min(
                    2000,
                    startingCost + costIncrease
            );

            waves.add(
                    new Wave(
                            index + 1,
                            waveCost,
                            false
                    )
            );
        }

        return waves;
    }

    private Position toBoardPosition(
            Position userPosition
    ) {
        if (userPosition == null) {
            return null;
        }

        return new Position(
                userPosition.getX() - 1,
                userPosition.getY() - 1
        );
    }

    private Position toUserPosition(
            Position boardPosition
    ) {
        if (boardPosition == null) {
            return null;
        }

        return new Position(
                boardPosition.getX() + 1,
                boardPosition.getY() + 1
        );
    }

    private void requireFivePlantTypes(
            List<PlantDefinition> plantTypes
    ) {
        if (plantTypes == null
                || plantTypes.size() != 5) {

            throw new IllegalArgumentException(
                    "Beghouled requires exactly "
                            + "five plant types."
            );
        }

        for (PlantDefinition definition
                : plantTypes) {

            if (definition == null) {
                throw new IllegalArgumentException(
                        "Beghouled plant definitions "
                                + "cannot be null."
                );
            }
        }
    }

    private void requireValidStage(
            int stageNumber
    ) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException(
                    "Beghouled stage must be "
                            + "between 1 and 3."
            );
        }
    }

    private static BundledDefinitions
    loadBundledDefinitions() {
        try {
            PlantDefinitionRepository plants =
                    CsvPlantDefinitionRepository
                            .fromClasspath(
                                    PLANTS_RESOURCE
                            );

            JsonZombieDefinitionRepository zombies =
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
                    "Could not load Beghouled data.",
                    exception
            );
        }
    }

    private static final class BundledDefinitions {

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
}
