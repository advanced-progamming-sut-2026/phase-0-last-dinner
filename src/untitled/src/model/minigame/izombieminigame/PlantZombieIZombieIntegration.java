package model.minigame.izombieminigame;

import java.io.IOException;
import java.util.*;
import lombok.Getter;
import model.Plant;
import model.mechanism.CombatSystem;
import model.mechanism.GameEngine;
import model.mechanism.PlantFoodSystem;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.Tile;
import model.minigame.behavior.IZombieSunProducerBehavior;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.ZombieType;

@Getter
public class PlantZombieIZombieIntegration implements IZombieIntegration {
  private static final String PLANTS_RESOURCE = "data/plants.csv";
  private static final String ZOMBIES_RESOURCE = "data/zombies.json";
  private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";
  private static final int FIRST_ZOMBIE_COLUMN = 6;
  private static final int BOARD_ROW_COUNT = 5;
  private static final String[][] PLANTS_BY_STAGE = {
    {"Peashooter", "Wall-nut", "Snow Pea", "Repeater", "Bonk Choy"},
    {
      "Peashooter",
      "Repeater",
      "Snow Pea",
      "Wall-nut",
      "Tall-nut",
      "Fume-shroom",
      "Cabbage-pult",
      "Bonk Choy"
    },
    {
      "Repeater",
      "Threepeater",
      "Snow Pea",
      "Tall-nut",
      "Fume-shroom",
      "Cabbage-pult",
      "Kernel-pult",
      "Melon-pult",
      "Chomper"
    }
  };
  private static final String[][] ZOMBIES_BY_STAGE = {
    {
      "ZombieTutorialDefault",
      "ZombieTutorialArmor1Default",
      "ZombieTutorialArmor2Default",
      "ZombieTutorialImpDefault",
      "ZombieMummyDefault"
    },
    {
      "ZombieMummyArmor1Default",
      "ZombieMummyArmor4Default",
      "ZombieRaDefault",
      "ZombieExplorerDefault",
      "ZombieIceAgeHunter"
    },
    {
      "ZombieBeachDefault",
      "ZombieBeachSnorkel",
      "ZombieBeachSurfer",
      "ZombieDarkDefault",
      "ZombieWizardDefault"
    }
  };
  private final PlantDefinitionRepository plantDefinitions;
  private final ZombieDefinitionRepository zombieDefinitions;
  private final PlantFactory plantFactory;
  private final ZombieFactory zombieFactory;
  private final IZombieStageGenerator stageGenerator;
  private final Random random;
  private final List<Zombie> playerZombies;
  private IZombieBoard board;
  private GameEngine engine;
  private CombatSystem combatSystem;
  private PlantFoodSystem plantFoodSystem;
  private PlantingSystem plantingSystem;
  private int currentStageNumber;
  public PlantZombieIZombieIntegration() {
    this(loadBundledDefinitions());
  }
  private PlantZombieIZombieIntegration(BundledDefinitions definitions) {
    this(
        definitions.plants,
        definitions.zombies,
        new ZombieFactory(definitions.zombies),
        new Random());
  }

  public PlantZombieIZombieIntegration(
      PlantDefinitionRepository plantDefinitions,
      ZombieDefinitionRepository zombieDefinitions,
      ZombieFactory zombieFactory) {
    this(plantDefinitions, zombieDefinitions, zombieFactory, new Random());
  }

  public PlantZombieIZombieIntegration(
      PlantDefinitionRepository plantDefinitions,
      ZombieDefinitionRepository zombieDefinitions,
      ZombieFactory zombieFactory,
      Random random) {
    if (plantDefinitions == null || zombieDefinitions == null) {
      throw new IllegalArgumentException("I, Zombie requires plant " + "and zombie definitions.");
    }
    this.plantDefinitions = plantDefinitions;
    this.zombieDefinitions = zombieDefinitions;
    this.plantFactory = new PlantFactory();
    this.zombieFactory =
        zombieFactory == null ? new ZombieFactory(zombieDefinitions) : zombieFactory;
    this.random = random == null ? new Random() : random;
    stageGenerator = new IZombieStageGenerator();
    playerZombies = new ArrayList<>();
    prepareStage(1);
  }

  @Override
  public boolean isReady() {
    return board != null && engine != null && combatSystem != null && plantingSystem != null;
  }

  @Override
  public void prepareStage(int stageNumber) {
    requireValidStage(stageNumber);
    currentStageNumber = stageNumber;
    playerZombies.clear();
    board = new IZombieBoard();
    engine = new GameEngine(board);
    combatSystem = new CombatSystem(board);
    plantFoodSystem = new PlantFoodSystem(board);
    plantingSystem = new PlantingSystem(board, null, null);
    engine.register(combatSystem);
    placeDefensivePlants(stageNumber);
  }

  @Override
  public List<ZombieDefinition> chooseAvailableZombies(int stageNumber) {
    requireValidStage(stageNumber);
    List<ZombieDefinition> selected = new ArrayList<>();
    Set<String> usedAliases = new HashSet<>();
    for (String alias : ZOMBIES_BY_STAGE[stageNumber - 1]) {
      ZombieDefinition definition = zombieDefinitions.findByAlias(alias);
      addSelectableZombie(selected, usedAliases, definition);
    }
    fillMissingZombieDefinitions(selected, usedAliases, stageNumber);
    if (selected.size() != IZombieStageConfig.AVAILABLE_ZOMBIE_COUNT) {
      throw new IllegalStateException("I, Zombie requires exactly " + "five selectable zombies.");
    }
    return selected;
  }

  @Override
  public int getZombieSunCost(ZombieDefinition zombieDefinition, int stageNumber) {
    requireValidStage(stageNumber);
    if (zombieDefinition == null || zombieDefinition.getType() == null) {
      return -1;
    }
    switch (zombieDefinition.getType()) {
      case BASIC:
      case IMP:
        return 50;
      case ANIMAL:
        return 75;
      case ARMORED:
        return 100;
      case SPECIAL:
        return 125;
      case GARGANTUAR:
        return 300;
      case BOSS:
        return 500;
      default:
        return -1;
    }
  }

  @Override
  public void spawnInitialSunProducerZombies(int stageNumber, IZombieMiniGame miniGame) {
    requireValidStage(stageNumber);
    if (!isReady() || miniGame == null) {
      return;
    }
    IZombieStageConfig config = stageGenerator.generateStage(stageNumber);
    ZombieDefinition producerDefinition = findSunProducerZombieDefinition();
    if (producerDefinition == null) {
      throw new IllegalStateException("No zombie definition is available for the sun producers.");
    }
    for (int userRow = 1; userRow <= BOARD_ROW_COUNT; userRow++) {
      Position position = new Position(FIRST_ZOMBIE_COLUMN - 1, userRow - 1);
      Zombie zombie = zombieFactory.create(producerDefinition, position);
      if (zombie == null) {
        continue;
      }
      IZombieSunProducerBehavior behavior = createSunProducerBehavior(miniGame, config);
      zombie.addBehavior(behavior);
      board.addZombie(zombie, position);
      if (zombie.getBoard() == board) {
        playerZombies.add(zombie);
      }
    }
  }
  private static IZombieSunProducerBehavior createSunProducerBehavior(
      IZombieMiniGame miniGame, IZombieStageConfig config) {
    return new IZombieSunProducerBehavior(
        miniGame,
        config.getSunProductionAmount(),
        config.getInitialSunProductionIntervalTicks(),
        config.getMinimumSunProductionIntervalTicks(),
        config.getSunProductionIntervalDecreaseTicks());
  }

  @Override
  public boolean isZombiePlacementBlocked(Position userPosition) {
    if (!isReady()) {
      return true;
    }
    Position boardPosition = toBoardPosition(userPosition);
    if (!board.isInsideBoard(boardPosition)) {
      return true;
    }
    Tile tile = board.getTile(boardPosition);
    if (tile == null) {
      return true;
    }
    if (tile.getPlants() != null && !tile.getPlants().isEmpty()) {
      return true;
    }
    if (tile.getZombies() == null) {
      return false;
    }
    for (Zombie zombie : tile.getZombies()) {
      if (zombie != null && !zombie.isDead()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean placeZombie(ZombieDefinition zombieDefinition, Position userPosition) {
    if (!isReady() || zombieDefinition == null || userPosition == null) {
      return false;
    }
    Position boardPosition = toBoardPosition(userPosition);
    if (!board.isInsideBoard(boardPosition) || isZombiePlacementBlocked(userPosition)) {
      return false;
    }
    Zombie zombie = zombieFactory.create(zombieDefinition, boardPosition);
    if (zombie == null) {
      return false;
    }
    board.addZombie(zombie, boardPosition);
    if (zombie.getBoard() != board) {
      return false;
    }
    playerZombies.add(zombie);
    return true;
  }

  @Override
  public void advanceOneTick() {
    if (isReady()) {
      engine.advanceTime(1);
    }
  }

  @Override
  public boolean hasAlivePlayerZombies() {
    if (!isReady()) {
      return false;
    }
    for (Zombie zombie : playerZombies) {
      if (zombie != null && !zombie.isDead() && zombie.getBoard() == board) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isBrainEaten(int userRow) {
    return board != null && board.isBrainEatenInRow(userRow);
  }
  private void placeDefensivePlants(int stageNumber) {
    List<PlantDefinition> candidates = findPlantCandidates(stageNumber);
    if (candidates.isEmpty()) {
      throw new IllegalStateException("No defensive plants are available " + "for I, Zombie.");
    }
    int plantsPerRow = Math.min(5, stageNumber + 2);
    for (int row = 0; row < BOARD_ROW_COUNT; row++) {
      List<Integer> columns = createShuffledPlantColumns();
      for (int index = 0; index < plantsPerRow; index++) {
        int column = columns.get(index);
        PlantDefinition definition = candidates.get(random.nextInt(candidates.size()));
        placePlant(definition, new Position(column, row));
      }
    }
  }
  private void placePlant(PlantDefinition definition, Position position) {
    if (definition == null || position == null) {
      return;
    }
    Plant plant = plantFactory.create(definition);
    if (plant == null || !plantingSystem.canPlant(plant, position)) {
      return;
    }
    plantingSystem.plant(plant, position);
  }
  private List<Integer> createShuffledPlantColumns() {
    List<Integer> columns = new ArrayList<>();
    for (int column = 0; column < 5; column++) {
      columns.add(column);
    }
    Collections.shuffle(columns, random);
    return columns;
  }
  private List<PlantDefinition> findPlantCandidates(int stageNumber) {
    List<PlantDefinition> candidates = new ArrayList<>();
    for (String name : PLANTS_BY_STAGE[stageNumber - 1]) {
      PlantDefinition definition = findPlantIgnoringCase(name);
      if (definition != null) {
        candidates.add(definition);
      }
    }
    return candidates;
  }
  private PlantDefinition findPlantIgnoringCase(String plantName) {
    for (PlantDefinition definition : plantDefinitions.findAll()) {
      if (definition != null
          && definition.getName() != null
          && definition.getName().equalsIgnoreCase(plantName)) {
        return definition;
      }
    }
    return null;
  }
  private ZombieDefinition findSunProducerZombieDefinition() {
    String[] preferredAliases = {
      "ZombieTutorialArmor2Default", "ZombieMummyArmor2Default", "ZombieArmor2"
    };
    for (String alias : preferredAliases) {
      ZombieDefinition definition = zombieDefinitions.findByAlias(alias);
      if (definition != null) {
        return definition;
      }
    }
    ZombieDefinition strongestArmoredZombie = null;
    for (ZombieDefinition definition : zombieDefinitions.findAll()) {
      if (definition == null || definition.getType() != ZombieType.ARMORED) {
        continue;
      }
      if (strongestArmoredZombie == null
          || definition.getHitpoints() > strongestArmoredZombie.getHitpoints()) {
        strongestArmoredZombie = definition;
      }
    }
    if (strongestArmoredZombie != null) {
      return strongestArmoredZombie;
    }
    for (ZombieDefinition definition : zombieDefinitions.findAll()) {
      if (definition != null && definition.getType() == ZombieType.BASIC) {
        return definition;
      }
    }
    return null;
  }
  private void fillMissingZombieDefinitions(
      List<ZombieDefinition> selected, Set<String> usedAliases, int stageNumber) {
    List<ZombieDefinition> allDefinitions = new ArrayList<>();
    for (ZombieDefinition definition : zombieDefinitions.findAll()) {
      if (isSelectableZombie(definition)) {
        allDefinitions.add(definition);
      }
    }
    if (allDefinitions.isEmpty()) {
      return;
    }
    int startIndex = ((stageNumber - 1) * 5) % allDefinitions.size();
    for (int offset = 0;
        offset < allDefinitions.size()
            && selected.size() < IZombieStageConfig.AVAILABLE_ZOMBIE_COUNT;
        offset++) {
      int index = (startIndex + offset) % allDefinitions.size();
      addSelectableZombie(selected, usedAliases, allDefinitions.get(index));
    }
  }
  private void addSelectableZombie(
      List<ZombieDefinition> selected, Set<String> usedAliases, ZombieDefinition definition) {
    if (!isSelectableZombie(definition)
        || selected.size() >= IZombieStageConfig.AVAILABLE_ZOMBIE_COUNT) {
      return;
    }
    String normalizedAlias = definition.getAlias().trim().toLowerCase();
    if (!usedAliases.add(normalizedAlias)) {
      return;
    }
    selected.add(definition);
  }
  private boolean isSelectableZombie(ZombieDefinition definition) {
    return definition != null
        && definition.getAlias() != null
        && !definition.getAlias().trim().isEmpty()
        && definition.getType() != null
        && definition.getType() != ZombieType.BOSS;
  }
  private Position toBoardPosition(Position userPosition) {
    if (userPosition == null) {
      return null;
    }
    return new Position(userPosition.getX() - 1, userPosition.getY() - 1);
  }
  private void requireValidStage(int stageNumber) {
    if (stageNumber < 1 || stageNumber > 3) {
      throw new IllegalArgumentException("I, Zombie stage must be " + "between 1 and 3.");
    }
  }
  private static BundledDefinitions loadBundledDefinitions() {
    try {
      PlantDefinitionRepository plants =
          CsvPlantDefinitionRepository.fromClasspath(PLANTS_RESOURCE);
      JsonZombieDefinitionRepository zombies =
          JsonZombieDefinitionRepository.fromClasspath(ZOMBIES_RESOURCE, ARMOR_RESOURCE);
      return new BundledDefinitions(plants, zombies);
    } catch (IOException exception) {
      throw new IllegalStateException("Could not load I, Zombie data.", exception);
    }
  }
  private static final class BundledDefinitions {
    private final PlantDefinitionRepository plants;
    private final ZombieDefinitionRepository zombies;
    private BundledDefinitions(
        PlantDefinitionRepository plants, ZombieDefinitionRepository zombies) {
      this.plants = plants;
      this.zombies = zombies;
    }
  }
}
