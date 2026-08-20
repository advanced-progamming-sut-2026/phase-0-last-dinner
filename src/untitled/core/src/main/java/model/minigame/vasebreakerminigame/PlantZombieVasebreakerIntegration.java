package model.minigame.vasebreakerminigame;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.GameEngine;
import model.mechanism.PlantFoodSystem;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.SunSystem;
import model.mechanism.Tile;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantCategory;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.ZombieType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import model.zombie.ZombieCondition;

// vasebreaker ro be runtime asli plant va zombie vasl mikone
public class PlantZombieVasebreakerIntegration implements VasebreakerIntegration {
    private static final String PLANTS_RESOURCE = "data/plants.csv";
    private static final String ZOMBIES_RESOURCE = "data/zombies.json";
    private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";

    private static final String[][] PLANT_NAMES_BY_STAGE = {
        {
            "Repeater",
            "Snow Pea",
            "Potato Mine",
            "Bonk Choy",
            "Peashooter",
            "Wall-nut"
        },
        {
            "Repeater",
            "Snow Pea",
            "Threepeater",
            "Kernel-pult",
            "Cherry Bomb",
            "Chomper",
            "Tall-nut"
        },
        {
            "Mega Gatling Pea",
            "Winter Melon",
            "Fire Peashooter",
            "Bowling Bulb",
            "Cherry Bomb",
            "Grapeshot",
            "Jalapeno",
            "Primal Potato Mine",
            "Endurian",
            "Magnet-shroom"
        }
    };

    private static final String[][] REGULAR_ZOMBIE_ALIASES_BY_STAGE = {
        {
            "ZombieDefault",
            "ZombieDefault",
            "ZombieDefault",
            "ZombieArmor1",
            "ZombieArmor1"
        },
        {
            "ZombieDefault",
            "ZombieArmor1",
            "ZombieDefault",
            "ZombieArmor1",
            "ZombieImp",
            "ZombieNewspaper",
            "ZombieArmor2",
            "ZombieExplorer"
        },
        {
            "ZombieDefault",
            "ZombieDefault",
            "ZombieDefault",
            "ZombieArmor1",
            "ZombieArmor1",
            "ZombieArmor1",
            "ZombieArmor2",
            "ZombieArmor2",
            "ZombieImp",
            "ZombieImp",
            "ZombieNewspaper",
            "ZombieNewspaper",
            "ZombieExplorer",
            "ZombieArmor4"
        }
    };

    private static final double[] ZOMBIE_SPEED_MULTIPLIER_BY_STAGE = {
        0.72d,
        0.82d,
        0.90d
    };

    private static final long RELEASE_STUN_TICKS = 12;

    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Random random;
    // faghat zombie haye azad shode az vase baraye check payan stage
    private final List<Zombie> releasedZombies;

    private Board board;
    private GameEngine engine;
    private SunSystem sunSystem;
    private PlantFoodSystem plantFoodSystem;
    private PlantingSystem seedPacketPlantingSystem;
    private CombatSystem combatSystem;
    private int stageNumber;

    private int nextPlantIndex;
    private int nextRegularZombieIndex;

    public PlantZombieVasebreakerIntegration() {
        this(loadBundledDefinitions(), new Random());
    }

    public PlantZombieVasebreakerIntegration(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        this(plantDefinitions, zombieDefinitions, zombieFactory, new Random());
    }

    public PlantZombieVasebreakerIntegration(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            Random random
    ) {
        if (plantDefinitions == null || zombieDefinitions == null) {
            throw new IllegalArgumentException("Vasebreaker requires plant and zombie definitions");
        }

        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.plantFactory = new PlantFactory();
        this.zombieFactory = zombieFactory == null ? new ZombieFactory(zombieDefinitions) : zombieFactory;
        this.random = random == null ? new Random() : random;
        this.releasedZombies = new ArrayList<>();
        this.prepareStage(1);
    }

    private PlantZombieVasebreakerIntegration(BundledDefinitions definitions, Random random) {
        this(
                definitions.plants,
                definitions.zombies,
                new ZombieFactory(definitions.zombies),
                random
        );
    }

    @Override
    public boolean isReady() {
        return this.board != null
                && this.engine != null
                && this.sunSystem != null
                && this.seedPacketPlantingSystem != null
                && this.combatSystem != null;
    }

    @Override
    public void prepareStage(int stageNumber) {
        this.requireValidStage(stageNumber);
        this.stageNumber = stageNumber;
        this.nextPlantIndex = 0;
        this.nextRegularZombieIndex = 0;
        this.releasedZombies.clear();

        this.board = new Board();
        this.engine = new GameEngine(this.board);
        this.sunSystem = new SunSystem(this.board, this.engine.getClock());
        this.sunSystem.setAutomaticSunEnabled(false);
        this.plantFoodSystem = new PlantFoodSystem(this.board);
        // seed packet baraye planting be sun va cooldown vabaste nist
        this.seedPacketPlantingSystem = new PlantingSystem(this.board, null, null);
        this.combatSystem = new CombatSystem(this.board);

        this.engine.register(this.sunSystem);
        this.engine.register(this.combatSystem);
    }

    @Override
    public PlantDefinition choosePlantDefinition(int stageNumber) {
        this.requireValidStage(stageNumber);
        String[] deck = PLANT_NAMES_BY_STAGE[stageNumber - 1];

        for (int attempt = 0; attempt < deck.length; attempt++) {
            String name = deck[this.nextPlantIndex % deck.length];
            this.nextPlantIndex++;

            PlantDefinition definition = this.plantDefinitions.findByName(name);
            if (definition != null)
                return definition;
        }

        for (PlantDefinition definition : this.plantDefinitions.findAll()) {
            if (this.isUsefulVasebreakerPlant(definition))
                return definition;
        }

        return null;
    }

    @Override
    public ZombieDefinition chooseRegularZombieDefinition(int stageNumber) {
        this.requireValidStage(stageNumber);
        String[] deck = REGULAR_ZOMBIE_ALIASES_BY_STAGE[stageNumber - 1];

        for (int attempt = 0; attempt < deck.length; attempt++) {
            String alias = deck[this.nextRegularZombieIndex % deck.length];
            this.nextRegularZombieIndex++;

            ZombieDefinition definition = this.zombieDefinitions.findByAlias(alias);
            if (this.isRegularZombie(definition))
                return definition;
        }

        for (ZombieDefinition definition : this.zombieDefinitions.findAll()) {
            if (this.isRegularZombie(definition))
                return definition;
        }

        return null;
    }

    @Override
    public ZombieDefinition chooseGargantuarDefinition(int stageNumber) {
        this.requireValidStage(stageNumber);
        ZombieDefinition gargantuar = this.zombieDefinitions.findByAlias("ZombieGargantuar");

        if (gargantuar != null) {
            return gargantuar;
        }

        for (ZombieDefinition definition : this.zombieDefinitions.findAll()) {
            if (definition != null && definition.getType() == ZombieType.GARGANTUAR) {
                return definition;
            }
        }

        return null;
    }

    @Override
    public boolean releaseZombie(ZombieDefinition zombieDefinition, Position vasePosition) {
        Position boardPosition = this.toBoardPosition(vasePosition);

        if (!this.isReady() || zombieDefinition == null || !this.board.isInsideBoard(boardPosition)) {
            return false;
        }

        Zombie zombie = this.zombieFactory.create(zombieDefinition, boardPosition);

        if (zombie == null) {
            return false;
        }

        double speedMultiplier = ZOMBIE_SPEED_MULTIPLIER_BY_STAGE[this.stageNumber - 1];
        zombie.setCurrentSpeed(zombie.getCurrentSpeed() * speedMultiplier);
        zombie.addCondition(ZombieCondition.STUNNED, RELEASE_STUN_TICKS);

        this.board.addZombie(zombie, boardPosition);

        if (zombie.getBoard() != this.board) {
            return false;
        }

        this.releasedZombies.add(zombie);
        return true;
    }

    @Override
    public boolean plantFromSeedPacket(PlantDefinition plantDefinition, Position vasePosition) {
        Position boardPosition = this.toBoardPosition(vasePosition);

        if (!this.isReady() || plantDefinition == null || !this.board.isInsideBoard(boardPosition)) {
            return false;
        }

        Plant plant = this.plantFactory.create(plantDefinition);

        if (!this.seedPacketPlantingSystem.canPlant(plant, boardPosition)) {
            return false;
        }

        this.seedPacketPlantingSystem.plant(plant, boardPosition);
        return true;
    }

    @Override
    public boolean isPlantingPositionOccupied(Position vasePosition) {
        Position boardPosition = this.toBoardPosition(vasePosition);

        if (!this.isReady() || !this.board.isInsideBoard(boardPosition)) {
            return true;
        }

        Tile tile = this.board.getTile(boardPosition);
        return tile == null || !tile.getPlants().isEmpty();
    }

    @Override
    public boolean hasAliveVasebreakerZombies() {
        for (Zombie zombie : this.releasedZombies) {
            if (zombie != null && !zombie.isDead() && zombie.getBoard() == this.board) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void advanceOneTick() {
        if (this.isReady()) {
            this.engine.advanceTime(1);
        }
    }

    @Override
    public boolean isBrainEaten() {
        return this.board != null && this.board.isBrainEaten();
    }

    public Board getBoard() {
        return this.board;
    }

    public GameEngine getEngine() {
        return this.engine;
    }

    public SunSystem getSunSystem() {
        return this.sunSystem;
    }

    public PlantFoodSystem getPlantFoodSystem() {
        return this.plantFoodSystem;
    }

    public int getStageNumber() {
        return this.stageNumber;
    }

    // mokhtasat vase ro az mabnaye yek be mabnaye sefr board tabdil mikone
    private Position toBoardPosition(Position vasePosition) {
        if (vasePosition == null) {
            return null;
        }

        return new Position(vasePosition.getX() - 1, vasePosition.getY() - 1);
    }

    private boolean isUsefulVasebreakerPlant(PlantDefinition definition) {
        if (definition == null || definition.getCategories() == null) {
            return false;
        }

        String name = definition.getName() == null
                ? ""
                : definition.getName().toLowerCase(Locale.ROOT);

        return !name.contains("imitater")
                && !definition.getCategories().contains(PlantCategory.SUN_PRODUCER)
                && !definition.getCategories().contains(PlantCategory.MINT);
    }

    private boolean isRegularZombie(ZombieDefinition definition) {
        return definition != null
                && definition.getType() != ZombieType.GARGANTUAR
                && definition.getType() != ZombieType.BOSS;
    }

    private <T> T chooseRandom(List<T> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.get(this.random.nextInt(candidates.size()));
    }

    private void requireValidStage(int stageNumber) {
        if (stageNumber < 1 || stageNumber > 3) {
            throw new IllegalArgumentException("Vasebreaker stage must be between 1 and 3");
        }
    }

    private static BundledDefinitions loadBundledDefinitions() {
        try {
            PlantDefinitionRepository plants = CsvPlantDefinitionRepository.fromClasspath(PLANTS_RESOURCE);
            JsonZombieDefinitionRepository zombies = JsonZombieDefinitionRepository.fromClasspath(
                    ZOMBIES_RESOURCE,
                    ARMOR_RESOURCE
            );
            return new BundledDefinitions(plants, zombies);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled Vasebreaker definitions", e);
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
