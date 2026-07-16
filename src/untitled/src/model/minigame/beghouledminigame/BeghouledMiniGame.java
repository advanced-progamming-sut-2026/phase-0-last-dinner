package model.minigame.beghouledminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.plant.PlantDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class BeghouledMiniGame extends MiniGame {

    private static final int MIN_STAGE_NUMBER = 1;

    private static final int MAX_STAGE_NUMBER = 3;

    private static final int COLUMN_COUNT = 9;

    private static final int ROW_COUNT = 5;

    private static final int MAX_CASCADE_COUNT = 100;

    private static final int MAX_RESET_ATTEMPTS = 25;

    private final BeghouledIntegration integration;

    private final BeghouledStageGenerator stageGenerator;

    private List<PlantDefinition> availablePlantTypes;

    private List<PlantUpgradeOption> upgradeOptions;

    private Set<Position> craters;

    private int sunAmount;

    private int targetMatchCount;

    private int completedMatchCount;

    private int currentStageNumber;

    private int highestUnlockedStage;

    private boolean endlessZombieWaves;

    private boolean lost;

    public BeghouledMiniGame() {
        this(
                new PlantZombieBeghouledIntegration(),
                new BeghouledStageGenerator()
        );
    }

    public BeghouledMiniGame(
            BeghouledIntegration integration
    ) {
        this(
                integration,
                new BeghouledStageGenerator()
        );
    }

    public BeghouledMiniGame(
            BeghouledIntegration integration,
            BeghouledStageGenerator stageGenerator
    ) {
        super(MiniGameType.BEGHOULED);

        if (integration == null) {
            throw new IllegalArgumentException(
                    "Beghouled integration "
                            + "cannot be null."
            );
        }

        if (stageGenerator == null) {
            throw new IllegalArgumentException(
                    "Beghouled stage generator "
                            + "cannot be null."
            );
        }

        this.integration = integration;
        this.stageGenerator = stageGenerator;

        availablePlantTypes =
                new ArrayList<>();

        upgradeOptions =
                new ArrayList<>();

        craters = new HashSet<>();

        sunAmount = 0;
        targetMatchCount = 0;
        completedMatchCount = 0;

        currentStageNumber = 1;
        highestUnlockedStage = 1;

        endlessZombieWaves = true;
        lost = false;
    }

    @Override
    public void start() {
        startStage(currentStageNumber);
    }

    public boolean startStage(int stageNumber) {
        if (!isValidStageNumber(stageNumber)
                || !isStageUnlocked(stageNumber)) {

            return false;
        }

        BeghouledStageConfig config =
                stageGenerator.generateStage(
                        stageNumber
                );

        List<PlantDefinition> selectedPlants =
                resolvePlantDefinitions(
                        config.getPlantNames()
                );

        if (selectedPlants.size() != 5) {
            throw new IllegalStateException(
                    "All five Beghouled plant "
                            + "definitions must exist."
            );
        }

        currentStageNumber = stageNumber;

        availablePlantTypes =
                selectedPlants;

        upgradeOptions =
                integration.createUpgradeOptions(
                        stageNumber
                );

        craters.clear();

        sunAmount = 0;
        completedMatchCount = 0;
        targetMatchCount =
                config.getTargetMatchCount();

        endlessZombieWaves = true;
        lost = false;

        setCompleted(false);

        if (getStages() != null
                && getStages().size()
                >= stageNumber) {

            setCurrentStage(
                    getStages().get(
                            stageNumber - 1
                    )
            );
        }

        integration.prepareStage(
                stageNumber,
                availablePlantTypes,
                craters
        );

        if (!integration.isReady()) {
            return false;
        }

        if (integration
                instanceof PlantZombieBeghouledIntegration) {

            PlantZombieBeghouledIntegration
                    plantZombieIntegration =
                    (PlantZombieBeghouledIntegration)
                            integration;

            setBoard(
                    plantZombieIntegration.getBoard()
            );
        }

        markStarted();

        ensurePlayableBoard();

        return true;
    }

    public boolean swap(
            Position first,
            Position second
    ) {
        if (!canPerformAction()
                || !canSwapPositions(
                first,
                second
        )) {
            return false;
        }

        boolean swapped =
                integration.swapPlants(
                        first,
                        second
                );

        if (!swapped) {
            return false;
        }

        List<PlantMatch> matches =
                findMatches(false);

        if (matches.isEmpty()
                || !matchesTouchSwap(
                matches,
                first,
                second
        )) {
            integration.swapPlants(
                    first,
                    second
            );

            return false;
        }

        resolveMatchesAndCascades(matches);

        updateCompletedIfWon();

        if (!isCompleted()
                && !isLoseConditionMet()) {

            ensurePlayableBoard();
        }

        return true;
    }

    public List<PlantMatch> findMatches() {
        return findMatches(false);
    }

    public void resolveMatches(
            List<PlantMatch> matches
    ) {
        if (matches == null
                || matches.isEmpty()
                || !canPerformAction()) {

            return;
        }

        Set<Position> matchedPositions =
                new HashSet<>();

        int earnedSun = 0;

        for (PlantMatch match : matches) {
            if (match == null) {
                continue;
            }

            matchedPositions.addAll(
                    match.getPositions()
            );

            earnedSun +=
                    match.calculateSunReward();

            completedMatchCount++;
        }

        if (matchedPositions.isEmpty()) {
            return;
        }

        sunAmount = safeAdd(
                sunAmount,
                earnedSun
        );

        integration.removePlants(
                matchedPositions
        );

        refillBoard();
    }

    public void refillBoard() {
        if (!integration.isReady()) {
            return;
        }

        integration.collapseAndRefill(
                availablePlantTypes,
                craters
        );
    }

    public void resetBoard() {
        if (!integration.isReady()) {
            return;
        }

        integration.resetBoard(
                availablePlantTypes,
                craters
        );
    }

    public void createCrater(Position position) {
        if (!isValidPosition(position)
                || craters.contains(position)) {

            return;
        }

        if (integration.createCrater(position)) {
            craters.add(position);
        }
    }

    public void upgradePlants(
            PlantUpgradeOption upgradeOption
    ) {
        if (!canPerformAction()
                || upgradeOption == null
                || !upgradeOptions.contains(
                upgradeOption
        )
                || !upgradeOption.canUpgrade(
                sunAmount
        )) {
            return;
        }

        int upgradedCount =
                integration.upgradePlants(
                        upgradeOption
                );

        if (upgradedCount <= 0) {
            return;
        }

        sunAmount -=
                upgradeOption.getSunCost();

        List<PlantMatch> upgradeMatches =
                findMatches(false);

        if (!upgradeMatches.isEmpty()) {
            resolveMatchesAndCascades(
                    upgradeMatches
            );

            updateCompletedIfWon();
        }

        if (!isCompleted()
                && !isLoseConditionMet()) {

            ensurePlayableBoard();
        }
    }

    public boolean upgradePlants(
            String sourcePlantName
    ) {
        PlantUpgradeOption option =
                findUpgradeOption(
                        sourcePlantName
                );

        if (option == null
                || !option.canUpgrade(
                sunAmount
        )) {
            return false;
        }

        int previousSun = sunAmount;

        upgradePlants(option);

        return sunAmount < previousSun;
    }

    public PlantUpgradeOption findUpgradeOption(
            String sourcePlantName
    ) {
        if (sourcePlantName == null
                || sourcePlantName.trim().isEmpty()) {

            return null;
        }

        for (PlantUpgradeOption option
                : upgradeOptions) {

            if (option != null
                    && option.matchesSourcePlant(
                    sourcePlantName
            )) {
                return option;
            }
        }

        return null;
    }

    @Override
    public void onTick() {
        if (!isStarted()
                || isCompleted()
                || isLoseConditionMet()
                || !integration.isReady()) {

            return;
        }

        integration.advanceOneTick();

        Set<Position> destroyedPositions =
                integration
                        .findDestroyedPlantPositions(
                                craters
                        );

        for (Position position
                : destroyedPositions) {

            createCrater(position);
        }

        if (integration.isBrainEaten()) {
            lost = true;
            setCompleted(true);
            return;
        }

        if (!hasPossibleMove()) {
            resetBoardUntilPlayable();
        }
    }

    @Override
    public boolean isWinConditionMet() {
        return isStarted()
                && completedMatchCount
                >= targetMatchCount;
    }

    @Override
    public boolean isLoseConditionMet() {
        return lost
                || (
                integration.isReady()
                        && integration.isBrainEaten()
        );
    }

    public boolean hasPossibleMove() {
        if (!integration.isReady()) {
            return false;
        }

        for (int y = 1;
             y <= ROW_COUNT;
             y++) {

            for (int x = 1;
                 x <= COLUMN_COUNT;
                 x++) {

                Position current =
                        new Position(x, y);

                if (craters.contains(current)
                        || integration.getPlantAt(
                        current
                ) == null) {
                    continue;
                }

                Position right =
                        new Position(x + 1, y);

                if (x < COLUMN_COUNT
                        && isLegalPotentialSwap(
                        current,
                        right
                )) {
                    return true;
                }

                Position down =
                        new Position(x, y + 1);

                if (y < ROW_COUNT
                        && isLegalPotentialSwap(
                        current,
                        down
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getRemainingMatchCount() {
        return Math.max(
                0,
                targetMatchCount
                        - completedMatchCount
        );
    }

    public boolean isStageUnlocked(
            int stageNumber
    ) {
        return isValidStageNumber(stageNumber)
                && stageNumber
                <= highestUnlockedStage;
    }

    public void unlockStage(int stageNumber) {
        if (!isValidStageNumber(stageNumber)) {
            return;
        }

        highestUnlockedStage = Math.max(
                highestUnlockedStage,
                stageNumber
        );
    }

    private List<PlantMatch> findMatches(
            boolean cascade
    ) {
        List<PlantMatch> matches =
                new ArrayList<>();

        findHorizontalMatches(
                matches,
                cascade
        );

        findVerticalMatches(
                matches,
                cascade
        );

        return matches;
    }

    private void findHorizontalMatches(
            List<PlantMatch> matches,
            boolean cascade
    ) {
        for (int y = 1;
             y <= ROW_COUNT;
             y++) {

            int x = 1;

            while (x <= COLUMN_COUNT) {
                Position start =
                        new Position(x, y);

                String plantName =
                        getPlantNameAt(start);

                if (plantName == null) {
                    x++;
                    continue;
                }

                int endX = x + 1;

                while (endX <= COLUMN_COUNT
                        && plantName.equalsIgnoreCase(
                        getPlantNameAt(
                                new Position(
                                        endX,
                                        y
                                )
                        )
                )) {
                    endX++;
                }

                int matchSize = endX - x;

                if (matchSize >= 3) {
                    List<Position> positions =
                            new ArrayList<>();

                    for (int matchedX = x;
                         matchedX < endX;
                         matchedX++) {

                        positions.add(
                                new Position(
                                        matchedX,
                                        y
                                )
                        );
                    }

                    matches.add(
                            new PlantMatch(
                                    positions,
                                    cascade
                            )
                    );
                }

                x = endX;
            }
        }
    }

    private void findVerticalMatches(
            List<PlantMatch> matches,
            boolean cascade
    ) {
        for (int x = 1;
             x <= COLUMN_COUNT;
             x++) {

            int y = 1;

            while (y <= ROW_COUNT) {
                Position start =
                        new Position(x, y);

                String plantName =
                        getPlantNameAt(start);

                if (plantName == null) {
                    y++;
                    continue;
                }

                int endY = y + 1;

                while (endY <= ROW_COUNT
                        && plantName.equalsIgnoreCase(
                        getPlantNameAt(
                                new Position(
                                        x,
                                        endY
                                )
                        )
                )) {
                    endY++;
                }

                int matchSize = endY - y;

                if (matchSize >= 3) {
                    List<Position> positions =
                            new ArrayList<>();

                    for (int matchedY = y;
                         matchedY < endY;
                         matchedY++) {

                        positions.add(
                                new Position(
                                        x,
                                        matchedY
                                )
                        );
                    }

                    matches.add(
                            new PlantMatch(
                                    positions,
                                    cascade
                            )
                    );
                }

                y = endY;
            }
        }
    }

    private void resolveMatchesAndCascades(
            List<PlantMatch> initialMatches
    ) {
        List<PlantMatch> currentMatches =
                initialMatches;

        int cascadeCount = 0;

        while (currentMatches != null
                && !currentMatches.isEmpty()
                && cascadeCount
                < MAX_CASCADE_COUNT) {

            resolveMatches(currentMatches);

            cascadeCount++;

            currentMatches =
                    findMatches(true);
        }

        if (cascadeCount
                >= MAX_CASCADE_COUNT) {

            resetBoardUntilPlayable();
        }
    }

    private boolean isLegalPotentialSwap(
            Position first,
            Position second
    ) {
        if (!canSwapPositions(first, second)) {
            return false;
        }

        boolean swapped =
                integration.swapPlants(
                        first,
                        second
                );

        if (!swapped) {
            return false;
        }

        List<PlantMatch> matches =
                findMatches(false);

        integration.swapPlants(
                first,
                second
        );

        return !matches.isEmpty()
                && matchesTouchSwap(
                matches,
                first,
                second
        );
    }

    private boolean matchesTouchSwap(
            List<PlantMatch> matches,
            Position first,
            Position second
    ) {
        for (PlantMatch match : matches) {
            if (match != null
                    && (
                    match.contains(first)
                            || match.contains(second)
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean canSwapPositions(
            Position first,
            Position second
    ) {
        if (!isValidPosition(first)
                || !isValidPosition(second)
                || craters.contains(first)
                || craters.contains(second)) {

            return false;
        }

        int deltaX = Math.abs(
                first.getX() - second.getX()
        );

        int deltaY = Math.abs(
                first.getY() - second.getY()
        );

        if (deltaX + deltaY != 1) {
            return false;
        }

        return integration.getPlantAt(first)
                != null
                && integration.getPlantAt(second)
                != null;
    }

    private void ensurePlayableBoard() {
        if (!findMatches(false).isEmpty()
                || !hasPossibleMove()) {

            resetBoardUntilPlayable();
        }
    }

    private void resetBoardUntilPlayable() {
        for (int attempt = 0;
             attempt < MAX_RESET_ATTEMPTS;
             attempt++) {

            resetBoard();

            if (findMatches(false).isEmpty()
                    && hasPossibleMove()) {

                return;
            }
        }
    }

    private void updateCompletedIfWon() {
        if (isCompleted()
                || isLoseConditionMet()
                || !isWinConditionMet()) {

            return;
        }

        integration.destroyAllZombies();

        markCompleted();
        unlockNextStage();
    }

    private void unlockNextStage() {
        if (currentStageNumber
                >= MAX_STAGE_NUMBER) {

            return;
        }

        unlockStage(
                currentStageNumber + 1
        );
    }

    private List<PlantDefinition>
    resolvePlantDefinitions(
            List<String> plantNames
    ) {
        List<PlantDefinition> definitions =
                new ArrayList<>();

        Set<String> usedNames =
                new HashSet<>();

        for (String plantName : plantNames) {
            PlantDefinition definition =
                    integration
                            .findPlantDefinition(
                                    plantName
                            );

            if (definition == null
                    || definition.getName() == null) {

                continue;
            }

            String normalizedName =
                    definition.getName()
                            .trim()
                            .toLowerCase();

            if (usedNames.add(normalizedName)) {
                definitions.add(definition);
            }
        }

        return definitions;
    }

    private String getPlantNameAt(
            Position position
    ) {
        if (position == null
                || craters.contains(position)) {

            return null;
        }

        PlantDefinition definition =
                integration.getPlantAt(position);

        if (definition == null) {
            return null;
        }

        return definition.getName();
    }

    private boolean isValidPosition(
            Position position
    ) {
        return position != null
                && position.getX() >= 1
                && position.getX()
                <= COLUMN_COUNT
                && position.getY() >= 1
                && position.getY()
                <= ROW_COUNT;
    }

    private boolean isValidStageNumber(
            int stageNumber
    ) {
        return stageNumber
                >= MIN_STAGE_NUMBER
                && stageNumber
                <= MAX_STAGE_NUMBER;
    }

    private boolean canPerformAction() {
        return isStarted()
                && !isCompleted()
                && !isLoseConditionMet()
                && integration.isReady();
    }

    private int safeAdd(
            int currentValue,
            int addedValue
    ) {
        long result =
                (long) currentValue
                        + addedValue;

        return result > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) result;
    }

    public PlantDefinition getPlantAt(Position position) {
        if (position == null) {
            return null;
        }

        return integration.getPlantAt(position);
    }
}
