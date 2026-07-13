package model.mechanism;

import model.Plant;
import model.minigame.vasebreakerminigame.PlantZombieVasebreakerIntegration;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.plant.PlantUpgradeService;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

import java.util.List;

// system haye plant va zombie ro baraye yek bazi be ham vasl mikone
public class PlantZombieGame {
    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final PlantFactory plantFactory;
    // upgrade haye daemi ro az birun migire ta beyn game ha moshtarak bemune
    private final PlantUpgradeService plantUpgradeService;
    private final ZombieFactory zombieFactory;
    private final Board board;
    private final GameEngine engine;
    private final SunSystem sunSystem;
    private final PlantFoodSystem plantFoodSystem;
    private final PlantCooldownManager cooldownManager;
    private final PlantingSystem plantingSystem;
    private final CombatSystem combatSystem;
    private final ZombieSpawner zombieSpawner;
    private final WaveManager waveManager;
    private final GameStatusService gameStatusService;

    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        this(plantDefinitions, zombieDefinitions, zombieFactory, new PlantUpgradeService());
    }

    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            PlantUpgradeService plantUpgradeService
    ) {
        if (plantDefinitions == null || zombieDefinitions == null || zombieFactory == null
                || plantUpgradeService == null) {
            throw new IllegalArgumentException("Plant and zombie definitions are required");
        }

        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.zombieFactory = zombieFactory;
        this.plantUpgradeService = plantUpgradeService;
        this.plantFactory = new PlantFactory(this.plantUpgradeService);
        this.board = new Board();
        this.engine = new GameEngine(this.board);
        this.sunSystem = new SunSystem(this.board, this.engine.getClock());
        this.plantFoodSystem = new PlantFoodSystem(this.board);
        this.cooldownManager = new PlantCooldownManager(this.engine.getClock());
        this.plantingSystem = new PlantingSystem(this.board, this.sunSystem, this.cooldownManager);
        this.combatSystem = new CombatSystem(this.board);
        this.zombieSpawner = new ZombieSpawner(this.zombieFactory, this.zombieDefinitions, this.board);
        this.waveManager = new WaveManager(null, this.zombieSpawner);
        this.gameStatusService = new GameStatusService(
                this.board,
                this.waveManager,
                this.sunSystem,
                this.plantFoodSystem,
                this.cooldownManager
        );

        this.engine.register(this.sunSystem);
        this.engine.register(this.cooldownManager);
        this.engine.register(this.combatSystem);
        this.engine.register(this.waveManager);
    }

    public boolean plant(String plantName, Position position) {
        PlantDefinition definition = this.plantDefinitions.findByName(plantName);

        if (definition == null) {
            return false;
        }

        return this.placePlant(this.plantFactory.create(definition), position);
    }

    public boolean plantImitater(String copiedPlantName, Position position) {
        PlantDefinition imitater = this.plantDefinitions.findByName("Imitater");
        PlantDefinition copiedDefinition = this.plantDefinitions.findByName(copiedPlantName);

        if (imitater == null || copiedDefinition == null || copiedDefinition == imitater) {
            return false;
        }

        return this.placePlant(this.plantFactory.createImitater(imitater, copiedDefinition), position);
    }

    public Zombie spawnZombie(String alias, int row) {
        if (row < 0 || row >= 5) {
            return null;
        }

        ZombieDefinition definition = this.zombieDefinitions.findByAlias(alias);

        if (definition == null) {
            return null;
        }

        Position position = new Position(8, row);
        Zombie zombie = this.zombieFactory.create(definition, position);
        this.board.addZombie(zombie, position);
        return zombie;
    }

    public void advanceTime(int tickCount) {
        this.engine.advanceTime(tickCount);
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

    public PlantingSystem getPlantingSystem() {
        return this.plantingSystem;
    }

    public PlantUpgradeService getPlantUpgradeService() {
        return this.plantUpgradeService;
    }

    public CombatSystem getCombatSystem() {
        return this.combatSystem;
    }

    public ZombieSpawner getZombieSpawner() {
        return this.zombieSpawner;
    }

    public WaveManager getWaveManager() {
        return this.waveManager;
    }

    public GameStatusService getGameStatusService() {
        return this.gameStatusService;
    }

    public void configureWaves(List<Wave> waves) {
        this.waveManager.configureWaves(waves);
    }

    // minigame ro ba hamin data va factory haye bazi misaze
    public VasebreakerMiniGame createVasebreakerMiniGame() {
        return new VasebreakerMiniGame(new PlantZombieVasebreakerIntegration(
                this.plantDefinitions,
                this.zombieDefinitions,
                this.zombieFactory
        ));
    }

    private boolean placePlant(Plant plant, Position position) {
        if (!this.plantingSystem.canPlant(plant, position)) {
            return false;
        }

        this.plantingSystem.plant(plant, position);
        return true;
    }
}
