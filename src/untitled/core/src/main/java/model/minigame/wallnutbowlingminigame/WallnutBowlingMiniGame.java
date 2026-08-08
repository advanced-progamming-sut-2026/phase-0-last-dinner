package model.minigame.wallnutbowlingminigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.minigame.StageProgressMiniGame;

@Getter
public class WallnutBowlingMiniGame extends MiniGame implements StageProgressMiniGame {
  private static final int MIN_STAGE_NUMBER = 1;
  private static final int MAX_STAGE_NUMBER = 3;

  private static final int BOARD_COLUMN_COUNT = 9;
  private static final int BOARD_ROW_COUNT = 5;

  private final List<BowlingWallnutType> conveyorBelt;

  private final List<RollingWallnut> rollingWallnuts;

  private final WallnutBowlingIntegration integration;

  private final WallnutBowlingStageGenerator stageGenerator;

  private final boolean plantSelectionEnabled;

  private final boolean skySunEnabled;

  private WallnutBowlingStageConfig currentStageConfig;

  private long currentTick;

  private long ticksSinceLastGeneration;

  private int currentStageNumber;

  private int highestUnlockedStage;

  private boolean wavesStarted;

  private boolean lost;

  public WallnutBowlingMiniGame() {
    this(new PlantZombieWallnutBowlingIntegration(), new WallnutBowlingStageGenerator());
  }

  public WallnutBowlingMiniGame(WallnutBowlingIntegration integration) {
    this(integration, new WallnutBowlingStageGenerator());
  }

  public WallnutBowlingMiniGame(WallnutBowlingIntegration integration, Random random) {
    this(integration, new WallnutBowlingStageGenerator(random));
  }

  public WallnutBowlingMiniGame(
      WallnutBowlingIntegration integration, WallnutBowlingStageGenerator stageGenerator) {
    super(MiniGameType.WALLNUT_BOWLING);

    if (integration == null) {
      this.integration = new PlantZombieWallnutBowlingIntegration();
    } else {
      this.integration = integration;
    }

    if (stageGenerator == null) {
      this.stageGenerator = new WallnutBowlingStageGenerator();
    } else {
      this.stageGenerator = stageGenerator;
    }

    this.currentStageConfig = this.stageGenerator.generateStage(1);

    this.conveyorBelt = new ArrayList<>();
    this.rollingWallnuts = new ArrayList<>();

    this.plantSelectionEnabled = false;
    this.skySunEnabled = false;

    this.currentTick = 0;
    this.ticksSinceLastGeneration = 0;

    this.currentStageNumber = 1;
    this.highestUnlockedStage = 1;

    this.wavesStarted = false;
    this.lost = false;
  }

  @Override
  public void start() {
    startStage(currentStageNumber);
  }

  public WallnutBowlingActionResult startStage(int stageNumber) {
    if (!isValidStageNumber(stageNumber)) {
      return WallnutBowlingActionResult.invalidStage(stageNumber);
    }

    if (!isStageUnlocked(stageNumber)) {
      return WallnutBowlingActionResult.stageLocked(stageNumber);
    }

    currentStageNumber = stageNumber;
    if (getStages() != null && getStages().size() >= stageNumber) {
      setCurrentStage(getStages().get(stageNumber - 1));
    }
    markStarted();
    setCompleted(false);
    resetStageState();
    currentStageConfig = stageGenerator.generateStage(stageNumber);
    integration.prepareStage(stageNumber);
    setBoard(integration.getBoard());
    if (integration.isReady()) {
      integration.startZombieWaves(stageNumber);
      wavesStarted = true;
    }
    for (int i = 0; i < currentStageConfig.getInitialWallnutCount(); i++) {
      generateWallnut();
    }
    return WallnutBowlingActionResult.started(stageNumber);
  }

  private void resetStageState() {
    currentTick = 0;
    ticksSinceLastGeneration = 0;
    lost = false;
    wavesStarted = false;
    conveyorBelt.clear();
    rollingWallnuts.clear();
  }

  public BowlingWallnutType generateWallnut() {
    if (!isStarted() || isCompleted() || isLoseConditionMet()) {
      return null;
    }

    if (conveyorBelt.size() >= currentStageConfig.getConveyorCapacity()) {

      return null;
    }

    BowlingWallnutType type = stageGenerator.chooseRandomWallnutType(currentStageConfig);

    conveyorBelt.add(type);

    ticksSinceLastGeneration = 0;

    return type;
  }

  public WallnutBowlingActionResult placeWallnutFromConveyor(int userIndex, Position position) {
    WallnutBowlingActionResult validationResult = validateAction(position);
    if (validationResult != null) {
      return validationResult;
    }
    if (!integration.isReady()) {
      return WallnutBowlingActionResult.integrationNotReady();
    }
    if (conveyorBelt.isEmpty()) {
      return WallnutBowlingActionResult.noWallnutAvailable();
    }

    int listIndex = userIndex - 1;
    if (listIndex < 0 || listIndex >= conveyorBelt.size()) {
      return WallnutBowlingActionResult.invalidConveyorIndex(userIndex);
    }
    if (!canPlaceAt(position)) {
      return WallnutBowlingActionResult.outsidePlantingArea(position);
    }

    BowlingWallnutType type = conveyorBelt.get(listIndex);
    RollingWallnut wallnut = createRollingWallnut(type, position);
    if (wallnut == null) {
      return WallnutBowlingActionResult.invalidAction();
    }

    conveyorBelt.remove(listIndex);
    rollingWallnuts.add(wallnut);
    return WallnutBowlingActionResult.placed(
        type, position, userIndex, isCompleted(), isLoseConditionMet());
  }

  private RollingWallnut createRollingWallnut(BowlingWallnutType type, Position position) {
    int normalZombieHealth = integration.getNormalZombieHealth();

    int cherryBombDamage = integration.getCherryBombDamage();

    return new RollingWallnut(
        type,
        position,
        normalZombieHealth,
        cherryBombDamage,
        currentStageConfig.getMovementIntervalTicks(),
        integration);
  }

  public boolean canPlaceAt(Position position) {
    if (!isValidPosition(position)) {
      return false;
    }

    return position.getX() <= currentStageConfig.getPlantingBoundaryColumn();
  }

  @Override
  public void onTick() {
    if (!isStarted() || isCompleted() || isLoseConditionMet()) {
      return;
    }

    currentTick++;
    ticksSinceLastGeneration++;

    startWavesIfReady();

    if (integration.isReady()) {
      integration.advanceOneTick();

      if (integration.isBrainEaten()) {
        markLost();
      }
    }

    tickRollingWallnuts();

    generateWallnutIfNeeded();

    updateCompletedIfWon();
  }

  private void startWavesIfReady() {
    if (wavesStarted || !integration.isReady()) {
      return;
    }

    integration.startZombieWaves(currentStageNumber);

    wavesStarted = true;
  }

  private void tickRollingWallnuts() {
    List<RollingWallnut> wallnutsCopy = new ArrayList<>(rollingWallnuts);

    for (RollingWallnut wallnut : wallnutsCopy) {

      if (wallnut != null && wallnut.isMoving()) {

        wallnut.onTick();
      }
    }

    rollingWallnuts.removeIf(
        wallnut -> wallnut == null || !wallnut.isMoving() || wallnut.isOutsideBoard());
  }

  private void generateWallnutIfNeeded() {
    if (conveyorBelt.size() >= currentStageConfig.getConveyorCapacity()) {
      return;
    }

    if (ticksSinceLastGeneration < currentStageConfig.getGenerationIntervalTicks()) {
      return;
    }

    generateWallnut();
  }

  @Override
  public boolean isWinConditionMet() {
    if (!isStarted() || !integration.isReady() || !wavesStarted) {
      return false;
    }

    return integration.areAllWavesFinished() && !integration.hasAliveZombies();
  }

  @Override
  public boolean isLoseConditionMet() {
    return lost;
  }

  public WallnutBowlingStateResult getState() {
    long ticksUntilNextGeneration =
        Math.max(0, currentStageConfig.getGenerationIntervalTicks() - ticksSinceLastGeneration);

    return new WallnutBowlingStateResult(
        currentStageNumber,
        currentTick,
        conveyorBelt,
        rollingWallnuts,
        currentStageConfig.getPlantingBoundaryColumn(),
        ticksUntilNextGeneration,
        isStarted(),
        integration.isReady(),
        isCompleted(),
        isLoseConditionMet());
  }

  public boolean isStageUnlocked(int stageNumber) {
    return isValidStageNumber(stageNumber) && stageNumber <= highestUnlockedStage;
  }

  public void unlockStage(int stageNumber) {
    if (!isValidStageNumber(stageNumber)) {
      return;
    }

    highestUnlockedStage = Math.max(highestUnlockedStage, stageNumber);
  }

  public void markLost() {
    if (!isStarted() || isCompleted() || lost) {
      return;
    }

    lost = true;
  }

  private WallnutBowlingActionResult validateAction(Position position) {
    if (!isStarted()) {
      return WallnutBowlingActionResult.gameNotStarted();
    }

    if (isCompleted() || isLoseConditionMet()) {

      return WallnutBowlingActionResult.gameAlreadyFinished(isCompleted(), isLoseConditionMet());
    }

    if (!isValidPosition(position)) {
      return WallnutBowlingActionResult.invalidPosition(position);
    }

    return null;
  }

  private boolean isValidPosition(Position position) {
    if (position == null) {
      return false;
    }

    return position.getX() >= 1
        && position.getX() <= BOARD_COLUMN_COUNT
        && position.getY() >= 1
        && position.getY() <= BOARD_ROW_COUNT;
  }

  private boolean isValidStageNumber(int stageNumber) {
    return stageNumber >= MIN_STAGE_NUMBER && stageNumber <= MAX_STAGE_NUMBER;
  }

  private void updateCompletedIfWon() {
    if (isCompleted() || isLoseConditionMet() || !isWinConditionMet()) {
      return;
    }

    if (currentStageNumber >= MAX_STAGE_NUMBER) {
      markAllStagesCompleted();
    } else {
      markCompleted();
    }
    unlockNextStage();
  }

  private void unlockNextStage() {
    if (currentStageNumber >= MAX_STAGE_NUMBER) {
      return;
    }

    unlockStage(currentStageNumber + 1);
  }

  @Override
  public void restoreHighestUnlockedStage(int stageNumber) {
    this.highestUnlockedStage = Math.max(1, Math.min(MAX_STAGE_NUMBER, stageNumber));
  }
}
