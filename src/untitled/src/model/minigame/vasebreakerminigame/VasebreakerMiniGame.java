package model.minigame.vasebreakerminigame;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.minigame.StageProgressMiniGame;

@Getter
public class VasebreakerMiniGame extends MiniGame implements StageProgressMiniGame {
  private static final long SEED_PACKET_LIFETIME_TICKS = 300;

  private static final int MIN_STAGE_NUMBER = 1;

  private static final int MAX_STAGE_NUMBER = 3;

  private static final int BOARD_COLUMN_COUNT = 9;

  private static final int BOARD_ROW_COUNT = 5;

  private final List<Vase> vases;

  private final List<DroppedSeedPacket> droppedSeedPackets;

  private final List<DroppedSeedPacket> collectedSeedPackets;

  private final VasebreakerIntegration integration;

  private final VaseBreakerStageGenerator stageGenerator;

  private final boolean plantSelectionEnabled;

  private final boolean skySunEnabled;

  private boolean lost;

  private long currentTick;

  private int currentStageNumber;

  private int highestUnlockedStage;

  public VasebreakerMiniGame() {
    this(new PlantZombieVasebreakerIntegration());
  }

  public VasebreakerMiniGame(VasebreakerIntegration integration) {
    super(MiniGameType.VASEBREAKER);

    if (integration == null) {
      this.integration = new PlantZombieVasebreakerIntegration();
    } else {
      this.integration = integration;
    }

    this.stageGenerator = new VaseBreakerStageGenerator(this.integration);

    this.vases = new ArrayList<>();
    this.droppedSeedPackets = new ArrayList<>();
    this.collectedSeedPackets = new ArrayList<>();

    this.plantSelectionEnabled = false;
    this.skySunEnabled = false;

    this.lost = false;
    this.currentTick = 0;

    this.currentStageNumber = 1;
    this.highestUnlockedStage = 1;
  }

  @Override
  public void start() {
    startStage(currentStageNumber);
  }

  public VasebreakerActionResult startStage(int stageNumber) {
    if (!isValidStageNumber(stageNumber)) {
      return VasebreakerActionResult.invalidStage(stageNumber);
    }

    if (!isStageUnlocked(stageNumber)) {
      return VasebreakerActionResult.stageLocked(stageNumber);
    }

    currentStageNumber = stageNumber;
    if (getStages() != null && getStages().size() >= stageNumber) {
      setCurrentStage(getStages().get(stageNumber - 1));
    }

    markStarted();
    setCompleted(false);

    lost = false;
    currentTick = 0;

    vases.clear();
    droppedSeedPackets.clear();
    collectedSeedPackets.clear();

    integration.prepareStage(stageNumber);
    setBoard(integration.getBoard());

    vases.addAll(stageGenerator.generateStage(stageNumber));

    return VasebreakerActionResult.started(stageNumber, false, false);
  }

  public VasebreakerActionResult breakVase(Position position) {
    VasebreakerActionResult validationResult = validateAction(position);

    if (validationResult != null) {
      return validationResult;
    }

    Vase vase = findUnbrokenVase(position);

    if (vase == null) {
      return VasebreakerActionResult.noVase(position);
    }

    vase.breakVase();

    DroppedSeedPacket droppedSeedPacket = null;
    boolean zombieReleased = false;

    if (vase.getContentType() == VaseContentType.SEED_PACKET) {

      droppedSeedPacket = dropSeedPacket(vase);

    } else if (vase.getContentType() == VaseContentType.ZOMBIE) {

      zombieReleased = releaseZombie(vase);
    }

    updateCompletedIfWon();

    return VasebreakerActionResult.vaseBroken(
        position,
        vase.getContentType(),
        droppedSeedPacket,
        zombieReleased,
        isCompleted(),
        isLoseConditionMet());
  }

  public VasebreakerActionResult collectSeedPacket(Position position) {
    VasebreakerActionResult validationResult = validateAction(position);

    if (validationResult != null) {
      return validationResult;
    }

    DroppedSeedPacket seedPacket = findAvailableSeedPacket(position);

    if (seedPacket == null) {
      return VasebreakerActionResult.noSeedPacket(position);
    }

    boolean collected = seedPacket.collect(currentTick);

    if (!collected) {
      return VasebreakerActionResult.seedPacketNotAvailable(position);
    }

    droppedSeedPackets.remove(seedPacket);
    collectedSeedPackets.add(seedPacket);

    updateCompletedIfWon();

    return VasebreakerActionResult.seedPacketCollected(
        position, seedPacket.getPlantName(), isCompleted(), isLoseConditionMet());
  }

  public VasebreakerActionResult plantFromCollectedPacket(
      String plantName, Position targetPosition) {
    VasebreakerActionResult validationResult = validateAction(targetPosition);

    if (validationResult != null) {
      return validationResult;
    }

    DroppedSeedPacket seedPacket = findCollectedSeedPacket(plantName);

    if (seedPacket == null) {
      return VasebreakerActionResult.noCollectedSeedPacket(plantName);
    }

    return plantPacket(seedPacket, targetPosition);
  }

  public VasebreakerActionResult plantFromPacket(
      DroppedSeedPacket seedPacket, Position targetPosition) {
    VasebreakerActionResult validationResult = validateAction(targetPosition);

    if (validationResult != null) {
      return validationResult;
    }

    if (seedPacket == null || !collectedSeedPackets.contains(seedPacket)) {

      return VasebreakerActionResult.invalidAction(targetPosition);
    }

    if (!seedPacket.isPlantable()) {
      return VasebreakerActionResult.seedPacketNotAvailable(targetPosition);
    }

    return plantPacket(seedPacket, targetPosition);
  }

  private VasebreakerActionResult plantPacket(
      DroppedSeedPacket seedPacket, Position targetPosition) {
    if (!integration.isReady()) {
      return VasebreakerActionResult.invalidAction(targetPosition);
    }
    if (seedPacket.getPlantDefinition() == null) {
      return VasebreakerActionResult.invalidAction(targetPosition);
    }
    if (findUnbrokenVase(targetPosition) != null) {
      return VasebreakerActionResult.tileHasUnbrokenVase(targetPosition);
    }
    if (integration.isPlantingPositionOccupied(targetPosition)) {
      return VasebreakerActionResult.tileOccupied(targetPosition);
    }

    boolean planted =
        integration.plantFromSeedPacket(seedPacket.getPlantDefinition(), targetPosition);
    if (!planted) {
      return VasebreakerActionResult.invalidAction(targetPosition);
    }
    if (!seedPacket.use()) {
      return VasebreakerActionResult.seedPacketNotAvailable(targetPosition);
    }

    collectedSeedPackets.remove(seedPacket);
    updateCompletedIfWon();
    return VasebreakerActionResult.plantFromPacket(
        targetPosition, seedPacket.getPlantName(), isCompleted(), isLoseConditionMet());
  }

  @Override
  public void onTick() {
    if (!isStarted() || isCompleted() || isLoseConditionMet()) {
      return;
    }

    currentTick++;

    removeExpiredSeedPackets();

    if (integration.isReady()) {
      integration.advanceOneTick();

      if (integration.isBrainEaten()) {
        markLost();
      }
    }

    updateCompletedIfWon();
  }

  @Override
  public boolean isWinConditionMet() {
    if (!isStarted()) {
      return false;
    }

    if (!integration.isReady()) {
      return false;
    }

    return areAllVasesBroken() && !integration.hasAliveVasebreakerZombies();
  }

  @Override
  public boolean isLoseConditionMet() {
    return lost;
  }

  public VasebreakerStateResult getState() {
    return new VasebreakerStateResult(
        currentStageNumber,
        currentTick,
        vases,
        droppedSeedPackets,
        collectedSeedPackets,
        isStarted(),
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

  public boolean isIntegrationReady() {
    return integration.isReady();
  }

  private VasebreakerActionResult validateAction(Position position) {
    if (!isStarted()) {
      return VasebreakerActionResult.gameNotStarted();
    }

    if (isCompleted() || isLoseConditionMet()) {

      return VasebreakerActionResult.gameAlreadyFinished(isCompleted(), isLoseConditionMet());
    }

    if (!isValidPosition(position)) {
      return VasebreakerActionResult.invalidPosition(position);
    }

    return null;
  }

  private boolean isValidStageNumber(int stageNumber) {
    return stageNumber >= MIN_STAGE_NUMBER && stageNumber <= MAX_STAGE_NUMBER;
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

  private Vase findUnbrokenVase(Position position) {
    for (Vase vase : vases) {
      if (vase != null && !vase.isBroken() && vase.isAt(position)) {

        return vase;
      }
    }

    return null;
  }

  private DroppedSeedPacket findAvailableSeedPacket(Position position) {
    for (DroppedSeedPacket seedPacket : droppedSeedPackets) {

      if (seedPacket != null && seedPacket.isAt(position) && seedPacket.isAvailable(currentTick)) {
        return seedPacket;
      }
    }

    return null;
  }

  private DroppedSeedPacket findCollectedSeedPacket(String plantName) {
    for (DroppedSeedPacket seedPacket : collectedSeedPackets) {

      if (seedPacket == null || !seedPacket.isPlantable()) {
        continue;
      }

      if (plantName == null || plantName.trim().isEmpty()) {

        return seedPacket;
      }

      String packetPlantName = seedPacket.getPlantName();

      if (packetPlantName != null && packetPlantName.equalsIgnoreCase(plantName.trim())) {

        return seedPacket;
      }
    }

    return null;
  }

  private DroppedSeedPacket dropSeedPacket(Vase vase) {
    DroppedSeedPacket seedPacket =
        new DroppedSeedPacket(
            vase.getPlantDefinition(),
            vase.getPosition(),
            currentTick + SEED_PACKET_LIFETIME_TICKS);

    droppedSeedPackets.add(seedPacket);

    return seedPacket;
  }

  private boolean releaseZombie(Vase vase) {
    if (!integration.isReady()) {
      return false;
    }

    if (vase == null || vase.getZombieDefinition() == null) {
      return false;
    }

    return integration.releaseZombie(vase.getZombieDefinition(), vase.getPosition());
  }

  private boolean areAllVasesBroken() {
    for (Vase vase : vases) {
      if (vase != null && !vase.isBroken()) {
        return false;
      }
    }

    return true;
  }

  private void removeExpiredSeedPackets() {
    droppedSeedPackets.removeIf(
        seedPacket -> seedPacket == null || seedPacket.isExpired(currentTick));
  }

  private void updateCompletedIfWon() {
    if (isCompleted() || isLoseConditionMet()) {
      return;
    }

    if (!isWinConditionMet()) {
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
