package model.minigame.izombieminigame;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.zombie.ZombieDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter

public class IZombieMiniGame extends MiniGame {

    private final IZombieIntegration integration;
    private final IZombieStageGenerator stageGenerator;

    private IZombieStageConfig stageConfig;

    private int currentStageNumber;
    private int sunAmount;
    private int placedZombieCount;

    private List<ZombieDefinition> availableZombies;
    private Map<ZombieDefinition, Integer> zombieCosts;
    private List<Brain> brains;

    private boolean won;
    private boolean lost;

    private String lastSetupError;
    private IZombieActionResult lastActionResult;

    public IZombieMiniGame() {
        this(
                new PendingIZombieIntegration(),
                new IZombieStageGenerator()
        );
    }

    public IZombieMiniGame(IZombieIntegration integration) {
        this(
                integration,
                new IZombieStageGenerator()
        );
    }

    public IZombieMiniGame(
            IZombieIntegration integration,
            IZombieStageGenerator stageGenerator
    ) {
        super(MiniGameType.I_ZOMBIE);

        if (integration == null) {
            throw new IllegalArgumentException(
                    "I Zombie integration cannot be null."
            );
        }

        if (stageGenerator == null) {
            throw new IllegalArgumentException(
                    "Stage generator cannot be null."
            );
        }

        this.integration = integration;
        this.stageGenerator = stageGenerator;

        this.currentStageNumber = 1;
        this.stageConfig = stageGenerator.generateStage(1);

        this.sunAmount = 0;
        this.placedZombieCount = 0;

        this.availableZombies = new ArrayList<>();
        this.zombieCosts = new LinkedHashMap<>();
        this.brains = createBrains();

        this.won = false;
        this.lost = false;
        this.lastSetupError = "";

        this.lastActionResult = IZombieActionResult.failure(
                IZombieActionStatus.NOT_STARTED,
                "I, Zombie has not started yet.",
                0
        );
    }

    @Override
    public void start() {
        lastActionResult = startGame();
    }

    public IZombieActionResult startGame() {
        if (isStarted() && !isCompleted()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.ALREADY_STARTED,
                            "I, Zombie has already started.",
                            sunAmount
                    )
            );
        }

        if (!integration.isReady()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INTEGRATION_NOT_READY,
                            "I, Zombie integration is not ready.",
                            sunAmount
                    )
            );
        }

        currentStageNumber = 1;
        won = false;
        lost = false;
        setCompleted(false);

        if (!setupStage(currentStageNumber)) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INTEGRATION_NOT_READY,
                            lastSetupError,
                            sunAmount
                    )
            );
        }

        markStarted();

        return remember(
                IZombieActionResult.success(
                        "I, Zombie stage 1 started.",
                        sunAmount
                )
        );
    }

    public IZombieActionResult placeZombie(
            ZombieDefinition definition,
            Position position
    ) {
        if (!isStarted()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.NOT_STARTED,
                            "Start I, Zombie before placing a zombie.",
                            sunAmount
                    )
            );
        }

        if (isCompleted()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.GAME_ALREADY_COMPLETED,
                            "I, Zombie has already finished.",
                            sunAmount
                    )
            );
        }

        if (!integration.isReady()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INTEGRATION_NOT_READY,
                            "I, Zombie integration is not ready.",
                            sunAmount
                    )
            );
        }

        if (definition == null) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INVALID_ZOMBIE,
                            "Zombie definition cannot be null.",
                            sunAmount
                    )
            );
        }

        if (!availableZombies.contains(definition)) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.ZOMBIE_NOT_AVAILABLE,
                            "This zombie is not available in the current stage.",
                            sunAmount
                    )
            );
        }

        if (position == null) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INVALID_POSITION,
                            "Zombie position cannot be null.",
                            sunAmount
                    )
            );
        }

        if (!isInsideBoard(position)) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INVALID_POSITION,
                            "Position must be inside the 9 by 5 board.",
                            sunAmount
                    )
            );
        }

        if (!stageConfig.isZombiePlacementColumn(position.getX())) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.POSITION_BEFORE_RED_LINE,
                            "Zombies must be placed after the red line.",
                            sunAmount
                    )
            );
        }

        if (integration.isZombiePlacementBlocked(position)) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.POSITION_BLOCKED,
                            "The selected position is blocked.",
                            sunAmount
                    )
            );
        }

        int cost = getZombieCost(definition);

        if (cost < 0) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INVALID_ZOMBIE,
                            "The selected zombie does not have a valid cost.",
                            sunAmount
                    )
            );
        }

        if (sunAmount < cost) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.NOT_ENOUGH_SUN,
                            "There is not enough sun to place this zombie.",
                            sunAmount
                    )
            );
        }

        boolean placed;

        try {
            placed = integration.placeZombie(definition, position);
        } catch (RuntimeException exception) {
            placed = false;
        }

        if (!placed) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.ZOMBIE_PLACEMENT_FAILED,
                            "The zombie could not be placed.",
                            sunAmount
                    )
            );
        }

        sunAmount -= cost;
        placedZombieCount++;

        return remember(
                IZombieActionResult.placementSuccess(
                        "Zombie placed successfully.",
                        definition,
                        position,
                        cost,
                        sunAmount
                )
        );
    }

    @Override
    public void onTick() {
        lastActionResult = advanceOneTick();
    }

    public IZombieActionResult advanceOneTick() {
        if (!isStarted()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.NOT_STARTED,
                            "Start I, Zombie before advancing the game.",
                            sunAmount
                    )
            );
        }

        if (isCompleted()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.GAME_ALREADY_COMPLETED,
                            "I, Zombie has already finished.",
                            sunAmount
                    )
            );
        }

        if (!integration.isReady()) {
            return remember(
                    IZombieActionResult.failure(
                            IZombieActionStatus.INTEGRATION_NOT_READY,
                            "I, Zombie integration is not ready.",
                            sunAmount
                    )
            );
        }

        integration.advanceOneTick();
        updateBrainStates();

        if (isWinConditionMet()) {
            return remember(handleStageWin());
        }

        if (isLoseConditionMet()) {
            lost = true;
            won = false;
            markCompleted();

            return remember(
                    IZombieActionResult.gameLost(
                            "You lost I, Zombie.",
                            sunAmount
                    )
            );
        }

        return remember(
                IZombieActionResult.success(
                        "I, Zombie advanced by one tick.",
                        sunAmount
                )
        );
    }

    private IZombieActionResult handleStageWin() {
        if (currentStageNumber >= stageGenerator.getStageCount()) {
            won = true;
            lost = false;
            markCompleted();

            return IZombieActionResult.gameWon(
                    "All I, Zombie stages completed.",
                    sunAmount
            );
        }

        int completedStageNumber = currentStageNumber;
        int nextStageNumber = currentStageNumber + 1;

        if (!setupStage(nextStageNumber)) {
            return IZombieActionResult.failure(
                    IZombieActionStatus.INTEGRATION_NOT_READY,
                    "Stage " + completedStageNumber
                            + " completed, but the next stage could not start. "
                            + lastSetupError,
                    sunAmount
            );
        }

        return IZombieActionResult.stageWon(
                "Stage " + completedStageNumber
                        + " completed. Stage "
                        + nextStageNumber + " started.",
                sunAmount
        );
    }

    private boolean setupStage(int stageNumber) {
        lastSetupError = "";

        try {
            IZombieStageConfig generatedConfig =
                    stageGenerator.generateStage(stageNumber);

            integration.prepareStage(stageNumber);

            List<ZombieDefinition> selectedZombies =
                    integration.chooseAvailableZombies(stageNumber);

            if (!hasFiveDistinctZombies(selectedZombies)) {
                lastSetupError =
                        "Integration must provide exactly five distinct zombies.";
                return false;
            }

            Map<ZombieDefinition, Integer> generatedCosts =
                    new LinkedHashMap<>();

            for (ZombieDefinition definition : selectedZombies) {
                int cost = integration.getZombieSunCost(
                        definition,
                        stageNumber
                );

                if (cost <= 0) {
                    lastSetupError =
                            "Every available zombie must have a positive sun cost.";
                    return false;
                }

                generatedCosts.put(definition, cost);
            }

            currentStageNumber = stageNumber;
            stageConfig = generatedConfig;
            availableZombies = new ArrayList<>(selectedZombies);
            zombieCosts = generatedCosts;
            brains = createBrains();

            sunAmount = stageConfig.getStartingSun();
            placedZombieCount = 0;

            if (getStages() != null
                    && getStages().size() >= stageNumber) {
                setCurrentStage(
                        getStages().get(stageNumber - 1)
                );
            }

            integration.spawnInitialSunProducerZombies(
                    stageNumber,
                    this
            );

            return true;
        } catch (RuntimeException exception) {
            lastSetupError =
                    "Stage setup failed: " + exception.getMessage();
            return false;
        }
    }

    private boolean hasFiveDistinctZombies(
            List<ZombieDefinition> definitions
    ) {
        if (definitions == null
                || definitions.size()
                != IZombieStageConfig.AVAILABLE_ZOMBIE_COUNT) {
            return false;
        }

        Set<String> aliases = new HashSet<>();

        for (ZombieDefinition definition : definitions) {
            if (definition == null
                    || definition.getAlias() == null
                    || definition.getAlias().trim().isEmpty()) {
                return false;
            }

            String normalizedAlias =
                    definition.getAlias().trim().toLowerCase();

            if (!aliases.add(normalizedAlias)) {
                return false;
            }
        }

        return true;
    }

    private List<Brain> createBrains() {
        List<Brain> createdBrains = new ArrayList<>();

        for (int row = 1;
             row <= IZombieStageConfig.BOARD_ROW_COUNT;
             row++) {
            createdBrains.add(new Brain(row));
        }

        return createdBrains;
    }

    private void updateBrainStates() {
        for (Brain brain : brains) {
            if (!brain.isEaten()
                    && integration.isBrainEaten(brain.getRow())) {
                brain.eat();
            }
        }
    }

    private boolean isInsideBoard(Position position) {
        return position.getX() >= 1
                && position.getX() <= 9
                && position.getY() >= 1
                && position.getY()
                <= IZombieStageConfig.BOARD_ROW_COUNT;
    }

    public void addSun(int amount) {
        if (amount <= 0 || isCompleted()) {
            return;
        }

        long updatedAmount = (long) sunAmount + amount;

        sunAmount = updatedAmount > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) updatedAmount;
    }

    public boolean canAfford(ZombieDefinition definition) {
        int cost = getZombieCost(definition);

        return cost >= 0 && sunAmount >= cost;
    }

    public boolean canAffordAnyAvailableZombie() {
        for (ZombieDefinition definition : availableZombies) {
            if (canAfford(definition)) {
                return true;
            }
        }

        return false;
    }

    public int getZombieCost(ZombieDefinition definition) {
        if (definition == null) {
            return -1;
        }

        return zombieCosts.getOrDefault(definition, -1);
    }

    public ZombieDefinition findAvailableZombie(String aliasOrName) {
        if (aliasOrName == null || aliasOrName.trim().isEmpty()) {
            return null;
        }

        String searchedValue = aliasOrName.trim();

        for (ZombieDefinition definition : availableZombies) {
            if (definition.getAlias() != null
                    && definition.getAlias().equalsIgnoreCase(
                    searchedValue
            )) {
                return definition;
            }

            if (definition.getDisplayName() != null
                    && definition.getDisplayName().equalsIgnoreCase(
                    searchedValue
            )) {
                return definition;
            }
        }

        return null;
    }

    @Override
    public boolean isWinConditionMet() {
        if (brains.size()
                != IZombieStageConfig.BOARD_ROW_COUNT) {
            return false;
        }

        for (Brain brain : brains) {
            if (!brain.isEaten()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isLoseConditionMet() {
        if (!isStarted() || isCompleted()) {
            return false;
        }

        return !canAffordAnyAvailableZombie()
                && !integration.hasAlivePlayerZombies();
    }

    public IZombieStateResult getState() {
        Map<Integer, Boolean> brainStates =
                new LinkedHashMap<>();

        for (Brain brain : brains) {
            brainStates.put(
                    brain.getRow(),
                    brain.isEaten()
            );
        }

        boolean hasAlivePlayerZombies =
                integration.isReady()
                        && integration.hasAlivePlayerZombies();

        return new IZombieStateResult(
                currentStageNumber,
                stageGenerator.getStageCount(),
                sunAmount,
                stageConfig.getRedLineColumn(),
                availableZombies,
                zombieCosts,
                brainStates,
                placedZombieCount,
                hasAlivePlayerZombies,
                isStarted(),
                isCompleted(),
                won,
                lost
        );
    }

    private IZombieActionResult remember(
            IZombieActionResult result
    ) {
        lastActionResult = result;
        return result;
    }

    public List<ZombieDefinition> getAvailableZombies() {
        return Collections.unmodifiableList(
                new ArrayList<>(availableZombies)
        );
    }

    public Map<ZombieDefinition, Integer> getZombieCosts() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(zombieCosts)
        );
    }

    public List<Brain> getBrains() {
        return Collections.unmodifiableList(brains);
    }
}
