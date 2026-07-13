package model.mechanism;

import model.Plant;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

public class PlantZombieGame {
    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Board board;
    private final GameEngine engine;
    private final SunSystem sunSystem;
    private final PlantFoodSystem plantFoodSystem;
    private final PlantCooldownManager cooldownManager;
    private final PlantingSystem plantingSystem;
    private final CombatSystem combatSystem;

    public PlantZombieGame(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        if (plantDefinitions == null || zombieDefinitions == null || zombieFactory == null) {
            throw new IllegalArgumentException("Plant and zombie definitions are required");
        }

        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.zombieFactory = zombieFactory;
        this.plantFactory = new PlantFactory();
        this.board = new Board();
        this.engine = new GameEngine(this.board);
        this.sunSystem = new SunSystem(this.board, this.engine.getClock());
        this.plantFoodSystem = new PlantFoodSystem(this.board);
        this.cooldownManager = new PlantCooldownManager(this.engine.getClock());
        this.plantingSystem = new PlantingSystem(this.board, this.sunSystem, this.cooldownManager);
        this.combatSystem = new CombatSystem(this.board);

        this.engine.register(this.sunSystem);
        this.engine.register(this.cooldownManager);
        this.engine.register(this.combatSystem);
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

    public CombatSystem getCombatSystem() {
        return this.combatSystem;
    }

    private boolean placePlant(Plant plant, Position position) {
        if (!this.plantingSystem.canPlant(plant, position)) {
            return false;
        }

        this.plantingSystem.plant(plant, position);
        return plant.getBoard() == this.board;
    }
}
