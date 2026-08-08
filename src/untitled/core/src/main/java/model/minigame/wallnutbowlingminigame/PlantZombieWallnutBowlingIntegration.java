package model.minigame.wallnutbowlingminigame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.GameEngine;
import model.mechanism.Position;
import model.mechanism.Wave;
import model.mechanism.WaveManager;
import model.mechanism.ZombieSpawner;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.DamageExpressionParser;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieChapter;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.ZombieType;

public class PlantZombieWallnutBowlingIntegration implements WallnutBowlingIntegration {

  private static final String PLANTS_RESOURCE = "data/plants.csv";

  private static final String ZOMBIES_RESOURCE = "data/zombies.json";

  private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";

  private static final int DEFAULT_NORMAL_ZOMBIE_HEALTH = 190;

  private static final int DEFAULT_CHERRY_BOMB_DAMAGE = 1800;

  private static final int[][] WAVE_COSTS = {
    {300, 400, 500},
    {400, 550, 700, 850},
    {500, 700, 900, 1100, 1300}
  };

  private final PlantDefinitionRepository plantDefinitions;

  private final ZombieDefinitionRepository zombieDefinitions;

  private final ZombieFactory zombieFactory;

  private Board board;

  private GameEngine engine;

  private CombatSystem combatSystem;

  private ZombieSpawner zombieSpawner;

  private WaveManager waveManager;

  private int stageNumber;

  public PlantZombieWallnutBowlingIntegration() {
    this(loadBundledDefinitions());
  }

  private PlantZombieWallnutBowlingIntegration(BundledDefinitions definitions) {
    this(definitions.plants, definitions.zombies, new ZombieFactory(definitions.zombies));
  }

  public PlantZombieWallnutBowlingIntegration(
      PlantDefinitionRepository plantDefinitions,
      ZombieDefinitionRepository zombieDefinitions,
      ZombieFactory zombieFactory) {
    if (plantDefinitions == null || zombieDefinitions == null) {

      throw new IllegalArgumentException(
          "Wallnut Bowling requires plant " + "and zombie definitions.");
    }

    this.plantDefinitions = plantDefinitions;
    this.zombieDefinitions = zombieDefinitions;

    if (zombieFactory == null) {
      this.zombieFactory = new ZombieFactory(zombieDefinitions);
    } else {
      this.zombieFactory = zombieFactory;
    }

    prepareStage(1);
  }

  @Override
  public boolean isReady() {
    return board != null
        && engine != null
        && combatSystem != null
        && zombieSpawner != null
        && waveManager != null;
  }

  @Override
  public void prepareStage(int stageNumber) {
    requireValidStage(stageNumber);

    this.stageNumber = stageNumber;

    board = new Board();
    engine = new GameEngine(board);
    combatSystem = new CombatSystem(board);

    ZombieDefinitionRepository stageRepository =
        new StageZombieRepository(chooseStageZombieDefinitions(stageNumber));

    zombieSpawner = new ZombieSpawner(zombieFactory, stageRepository, board);

    waveManager = new WaveManager(new ArrayList<>(), zombieSpawner);

    engine.register(combatSystem);
    engine.register(waveManager);
  }

  @Override
  public void startZombieWaves(int stageNumber) {
    requireValidStage(stageNumber);

    if (!isReady() || this.stageNumber != stageNumber) {
      prepareStage(stageNumber);
    }

    waveManager.configureWaves(createWaves(stageNumber));
  }

  @Override
  public int getNormalZombieHealth() {
    ZombieDefinition normalZombie =
        findZombieByAliases("ZombieTutorialDefault", "ZombieMummyDefault", "ZombieDefault");

    if (normalZombie == null) {
      for (ZombieDefinition definition : zombieDefinitions.findAll()) {

        if (definition != null && definition.getType() == ZombieType.BASIC) {

          normalZombie = definition;
          break;
        }
      }
    }

    if (normalZombie == null || normalZombie.getHitpoints() <= 0) {

      return DEFAULT_NORMAL_ZOMBIE_HEALTH;
    }

    return normalZombie.getHitpoints();
  }

  @Override
  public int getCherryBombDamage() {
    PlantDefinition cherryBomb = findPlantIgnoringCase("Cherry Bomb");

    if (cherryBomb == null) {
      return DEFAULT_CHERRY_BOMB_DAMAGE;
    }

    int damage = DamageExpressionParser.parseTotalDamage(cherryBomb.getDamageExpression());

    if (damage <= 0 || damage == Integer.MAX_VALUE) {

      return DEFAULT_CHERRY_BOMB_DAMAGE;
    }

    return damage;
  }

  @Override
  public boolean hasZombieAt(Position position) {
    if (!isReady()) {
      return false;
    }

    Position boardPosition = toBoardPosition(position);

    if (!board.isInsideBoard(boardPosition)) {
      return false;
    }

    for (Zombie zombie : board.getZombiesAt(boardPosition)) {

      if (isAliveEnemyZombie(zombie)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void damageFirstZombieAt(Position position, int damage) {
    if (!isReady() || damage <= 0) {
      return;
    }

    Zombie zombie = findFirstZombieAt(position);

    if (zombie != null) {
      combatSystem.applyDamageToZombie(zombie, damage);
    }
  }

  @Override
  public void crushZombiesAt(Position position) {
    if (!isReady()) {
      return;
    }

    Position boardPosition = toBoardPosition(position);

    if (!board.isInsideBoard(boardPosition)) {
      return;
    }

    List<Zombie> zombies = new ArrayList<>(board.getZombiesAt(boardPosition));

    for (Zombie zombie : zombies) {
      if (isAliveEnemyZombie(zombie)) {
        combatSystem.killZombieIgnoringAllegiance(zombie);
      }
    }
  }

  @Override
  public void explodeAt(Position centre, int radius, int damage) {
    if (!isReady() || centre == null || radius < 0 || damage <= 0) {

      return;
    }

    Position boardCentre = toBoardPosition(centre);

    if (!board.isInsideBoard(boardCentre)) {
      return;
    }

    List<Zombie> zombies = new ArrayList<>(board.getZombiesInRadius(boardCentre, radius));

    for (Zombie zombie : zombies) {
      if (isAliveEnemyZombie(zombie)) {
        combatSystem.applyDamageToZombie(zombie, damage);
      }
    }
  }

  @Override
  public void advanceOneTick() {
    if (isReady()) {
      engine.advanceTime(1);
    }
  }

  @Override
  public boolean areAllWavesFinished() {
    if (!isReady() || waveManager.getWaves() == null || waveManager.getWaves().isEmpty()) {

      return false;
    }

    int finalWaveIndex = waveManager.getWaves().size() - 1;

    if (waveManager.getCurrentWaveIndex() != finalWaveIndex) {

      return false;
    }

    Wave finalWave = waveManager.getCurrentWave();

    return finalWave != null
        && finalWave.isStarted()
        && finalWave.getRemainingHealthPercentage() <= 0;
  }

  @Override
  public boolean hasAliveZombies() {
    if (!isReady()) {
      return false;
    }

    for (Zombie zombie : board.getAllZombies()) {
      if (isAliveEnemyZombie(zombie)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isBrainEaten() {
    return board != null && board.isBrainEaten();
  }

  public Board getBoard() {
    return board;
  }

  public GameEngine getEngine() {
    return engine;
  }

  public WaveManager getWaveManager() {
    return waveManager;
  }

  public int getStageNumber() {
    return stageNumber;
  }

  private List<Wave> createWaves(int stageNumber) {
    int[] costs = WAVE_COSTS[stageNumber - 1];

    List<Wave> waves = new ArrayList<>();

    for (int index = 0; index < costs.length; index++) {

      boolean finalWave = index == costs.length - 1;

      waves.add(new Wave(index + 1, costs[index], finalWave));
    }

    return waves;
  }

  private List<ZombieDefinition> chooseStageZombieDefinitions(int stageNumber) {
    List<ZombieDefinition> selected = new ArrayList<>();

    for (ZombieDefinition definition : zombieDefinitions.findAll()) {

      if (definition == null
          || definition.getType() == null
          || definition.getType() == ZombieType.BOSS) {

        continue;
      }

      if (stageNumber == 1 && isStageOneZombie(definition)) {

        selected.add(definition);
      } else if (stageNumber == 2 && definition.getType() != ZombieType.GARGANTUAR) {

        selected.add(definition);
      } else if (stageNumber == 3) {
        selected.add(definition);
      }
    }

    if (selected.isEmpty()) {
      for (ZombieDefinition definition : zombieDefinitions.findAll()) {

        if (definition != null && definition.getType() != ZombieType.BOSS) {

          selected.add(definition);
        }
      }
    }

    return selected;
  }

  private boolean isStageOneZombie(ZombieDefinition definition) {
    return definition.getType() == ZombieType.BASIC
        || definition.getType() == ZombieType.ARMORED
        || definition.getType() == ZombieType.IMP;
  }

  private Zombie findFirstZombieAt(Position userPosition) {
    Position boardPosition = toBoardPosition(userPosition);

    if (!board.isInsideBoard(boardPosition)) {
      return null;
    }

    for (Zombie zombie : board.getZombiesAt(boardPosition)) {

      if (isAliveEnemyZombie(zombie)) {
        return zombie;
      }
    }

    return null;
  }

  private boolean isAliveEnemyZombie(Zombie zombie) {
    return zombie != null && !zombie.isDead() && !zombie.isHypnotized();
  }

  private Position toBoardPosition(Position userPosition) {
    if (userPosition == null) {
      return null;
    }

    return new Position(userPosition.getX() - 1, userPosition.getY() - 1);
  }

  private ZombieDefinition findZombieByAliases(String... aliases) {
    for (String alias : aliases) {
      ZombieDefinition definition = zombieDefinitions.findByAlias(alias);

      if (definition != null) {
        return definition;
      }
    }

    return null;
  }

  private PlantDefinition findPlantIgnoringCase(String name) {
    for (PlantDefinition definition : plantDefinitions.findAll()) {

      if (definition != null
          && definition.getName() != null
          && definition.getName().equalsIgnoreCase(name)) {

        return definition;
      }
    }

    return null;
  }

  private void requireValidStage(int stageNumber) {
    if (stageNumber < 1 || stageNumber > 3) {
      throw new IllegalArgumentException("Wallnut Bowling stage must " + "be between 1 and 3.");
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
      throw new IllegalStateException("Could not load Wallnut Bowling data.", exception);
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

  private static final class StageZombieRepository implements ZombieDefinitionRepository {

    private final List<ZombieDefinition> definitions;

    private StageZombieRepository(List<ZombieDefinition> definitions) {
      this.definitions = new ArrayList<>(definitions);
    }

    @Override
    public ZombieDefinition findByAlias(String alias) {
      if (alias == null) {
        return null;
      }

      for (ZombieDefinition definition : definitions) {

        if (definition.getAlias() != null && definition.getAlias().equalsIgnoreCase(alias)) {

          return definition;
        }
      }

      return null;
    }

    @Override
    public List<ZombieDefinition> findByChapter(ZombieChapter chapter) {
      List<ZombieDefinition> result = new ArrayList<>();

      for (ZombieDefinition definition : definitions) {

        ZombieChapter definitionChapter = definition.getChapter();

        if (chapter == ZombieChapter.ALL_CHAPTERS
            || definitionChapter == null
            || definitionChapter == ZombieChapter.ALL_CHAPTERS
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
