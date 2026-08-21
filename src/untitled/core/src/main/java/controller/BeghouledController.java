package controller;

import lombok.Getter;
import model.mechanism.Position;
import model.minigame.beghouledminigame.BeghouledMiniGame;
import model.minigame.beghouledminigame.PlantUpgradeOption;
import model.minigame.beghouledminigame.BeghouledActionResult;
import model.minigame.beghouledminigame.BeghouledActionStatus;
import model.minigame.beghouledminigame.BeghouledStateResult;
import model.plant.PlantDefinition;
import view.beghouled.BeghouledView;
import view.beghouled.BeghouledViewObserver;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BeghouledController implements BeghouledViewObserver {

    private final BeghouledMiniGame game;

    public BeghouledController(BeghouledMiniGame game) {
        if (game == null)
            throw new IllegalArgumentException("Beghouled mini game cannot be null.");

        this.game = game;
    }

    public BeghouledController(BeghouledView view) {
        this(view, new BeghouledMiniGame());
    }

    public BeghouledController(BeghouledView view, BeghouledMiniGame game) {
        this(game);

        if (view == null)
            throw new IllegalArgumentException("Beghouled view cannot be null.");

        view.setObserver(this);
    }

    @Override
    public BeghouledActionResult onStartBeghouledRequested(
            int stageNumber
    ) {
        if (stageNumber < 1 || stageNumber > 3) {
            return result(
                    BeghouledActionStatus.INVALID_STAGE,
                    "Beghouled stage must be between 1 and 3."
            );
        }

        if (stageNumber > game.getHighestUnlockedStage()) {
            return result(
                    BeghouledActionStatus.STAGE_LOCKED,
                    "Complete the previous Beghouled stage first."
            );
        }

        boolean started = game.startStage(stageNumber);

        if (!started) {
            return result(
                    BeghouledActionStatus.INVALID_STAGE,
                    "The requested Beghouled stage could not be started."
            );
        }

        return result(
                BeghouledActionStatus.SUCCESS,
                "Beghouled stage "
                        + stageNumber
                        + " started."
        );
    }

    @Override
    public BeghouledActionResult onSwapRequested(
            Position first,
            Position second
    ) {
        BeghouledActionResult invalidState =
                validateRunningGame();

        if (invalidState != null) {
            return invalidState;
        }

        if (!isInsideBoard(first)
                || !isInsideBoard(second)) {
            return result(
                    BeghouledActionStatus.INVALID_POSITION,
                    "Both positions must be inside the 9 by 5 board."
            );
        }

        if (!areAdjacent(first, second)) {
            return result(
                    BeghouledActionStatus.INVALID_SWAP,
                    "Only horizontally or vertically adjacent plants can be swapped."
            );
        }

        boolean swapped = game.swap(first, second);

        if (!swapped) {
            return result(
                    BeghouledActionStatus.INVALID_SWAP,
                    "The swap did not create a match of three or more plants."
            );
        }

        return successfulActionResult(
                "Plants swapped and matches resolved."
        );
    }

    @Override
    public BeghouledActionResult onUpgradeRequested(
            String sourcePlantName
    ) {
        BeghouledActionResult invalidState =
                validateRunningGame();

        if (invalidState != null) {
            return invalidState;
        }

        PlantUpgradeOption option =
                findUpgradeOption(sourcePlantName);

        if (option == null) {
            return result(
                    BeghouledActionStatus.UPGRADE_NOT_FOUND,
                    "No Beghouled upgrade matches: "
                            + sourcePlantName
            );
        }

        return upgradePlants(option);
    }

    private BeghouledActionResult upgradePlants(PlantUpgradeOption option) {
        if (!option.canUpgrade(game.getSunAmount())) {
            return result(
                    BeghouledActionStatus.NOT_ENOUGH_SUN,
                    "This upgrade costs "
                            + option.getSunCost()
                            + " sun."
            );
        }

        int sourceCountBefore =
                countPlants(option.getSourcePlant());

        if (sourceCountBefore == 0) {
            return result(
                    BeghouledActionStatus.NO_PLANTS_TO_UPGRADE,
                    "There are no "
                            + option.getSourcePlant().getName()
                            + " plants on the board."
            );
        }

        game.upgradePlants(option);

        int sourceCountAfter =
                countPlants(option.getSourcePlant());

        if (sourceCountAfter >= sourceCountBefore) {
            return result(
                    BeghouledActionStatus.NO_PLANTS_TO_UPGRADE,
                    "No plants were upgraded."
            );
        }

        return successfulActionResult(
                option.getSourcePlant().getName()
                        + " upgraded to "
                        + option.getTargetPlant().getName()
                        + "."
        );
    }

    @Override
    public BeghouledActionResult onAdvanceTicksRequested(
            int ticks
    ) {
        if (ticks <= 0) {
            return result(
                    BeghouledActionStatus.INVALID_TICK_COUNT,
                    "Tick count must be greater than zero."
            );
        }

        BeghouledActionResult invalidState =
                validateRunningGame();

        if (invalidState != null) {
            return invalidState;
        }

        int advancedTicks = 0;

        for (int tick = 0; tick < ticks; tick++) {
            game.onTick();
            advancedTicks++;

            if (game.isCompleted()) {
                break;
            }
        }

        return successfulActionResult(
                "Time advanced by "
                        + advancedTicks
                        + " ticks."
        );
    }

    @Override
    public BeghouledStateResult onShowBeghouledRequested() {
        List<List<String>> grid = createGrid();
        boolean won = game.isCompleted()
                && !game.isLost();
        boolean possibleMove = game.isStarted()
                && !game.isCompleted()
                && game.hasPossibleMove();

        return new BeghouledStateResult(
                game.getCurrentStageNumber(),
                game.getHighestUnlockedStage(),
                game.getSunAmount(),
                game.getCompletedMatchCount(),
                game.getTargetMatchCount(),
                game.getAvailablePlantTypes(),
                game.getUpgradeOptions(),
                grid,
                game.getCraters(),
                possibleMove,
                game.isEndlessZombieWaves(),
                game.isStarted(),
                game.isCompleted(),
                won,
                game.isLost()
        );
    }

    private List<List<String>> createGrid() {
        List<List<String>> grid = new ArrayList<>();

        for (int y = 1;
             y <= BeghouledStateResult.ROW_COUNT;
             y++) {

            List<String> row = new ArrayList<>();

            for (int x = 1;
                 x <= BeghouledStateResult.COLUMN_COUNT;
                 x++) {

                Position position = new Position(x, y);

                if (game.getCraters().contains(position)) {
                    row.add("CRATER");
                    continue;
                }

                PlantDefinition definition =
                        game.getPlantAt(position);

                row.add(
                        definition == null
                                ? ""
                                : definition.getName()
                );
            }

            grid.add(row);
        }

        return grid;
    }

    private BeghouledActionResult validateRunningGame() {
        if (!game.isStarted()) {
            return result(
                    BeghouledActionStatus.NOT_STARTED,
                    "Start a Beghouled stage first."
            );
        }

        if (game.isCompleted()) {
            if (game.isLost()) {
                return result(
                        BeghouledActionStatus.GAME_LOST,
                        "The zombies reached the house."
                );
            }

            return result(
                    game.getCurrentStageNumber() >= 3
                            ? BeghouledActionStatus.GAME_WON
                            : BeghouledActionStatus.STAGE_WON,
                    "This Beghouled stage is already complete."
            );
        }

        return null;
    }

    private BeghouledActionResult successfulActionResult(
            String message
    ) {
        if (game.isLost()) {
            return result(
                    BeghouledActionStatus.GAME_LOST,
                    "The zombies reached the house."
            );
        }

        if (game.isCompleted()) {
            if (game.getCurrentStageNumber() >= 3) {
                return result(
                        BeghouledActionStatus.GAME_WON,
                        message
                                + " All Beghouled stages are complete."
                );
            }

            return result(
                    BeghouledActionStatus.STAGE_WON,
                    message
                            + " Stage "
                            + game.getCurrentStageNumber()
                            + " is complete."
            );
        }

        return result(
                BeghouledActionStatus.SUCCESS,
                message
        );
    }

    private PlantUpgradeOption findUpgradeOption(
            String sourcePlantName
    ) {
        if (sourcePlantName == null
                || sourcePlantName.trim().isEmpty()) {
            return null;
        }

        for (PlantUpgradeOption option
                : game.getUpgradeOptions()) {
            if (option.matchesSourcePlant(sourcePlantName)) {
                return option;
            }
        }

        return null;
    }

    private int countPlants(
            PlantDefinition expectedDefinition
    ) {
        if (expectedDefinition == null) {
            return 0;
        }

        int count = 0;

        for (int y = 1;
             y <= BeghouledStateResult.ROW_COUNT;
             y++) {

            for (int x = 1;
                 x <= BeghouledStateResult.COLUMN_COUNT;
                 x++) {

                PlantDefinition actual =
                        game.getPlantAt(
                                new Position(x, y)
                        );

                if (hasSameName(
                        actual,
                        expectedDefinition
                )) {
                    count++;
                }
            }
        }

        return count;
    }

    private boolean hasSameName(
            PlantDefinition first,
            PlantDefinition second
    ) {
        if (first == null || second == null) {
            return false;
        }

        if (first.getName() == null
                || second.getName() == null) {
            return false;
        }

        return first.getName().equalsIgnoreCase(
                second.getName()
        );
    }

    private boolean isInsideBoard(Position position) {
        if (position == null) {
            return false;
        }

        return position.getX() >= 1
                && position.getX()
                <= BeghouledStateResult.COLUMN_COUNT
                && position.getY() >= 1
                && position.getY()
                <= BeghouledStateResult.ROW_COUNT;
    }

    private boolean areAdjacent(
            Position first,
            Position second
    ) {
        int distance =
                Math.abs(first.getX() - second.getX())
                        + Math.abs(
                        first.getY() - second.getY()
                );

        return distance == 1;
    }

    private BeghouledActionResult result(
            BeghouledActionStatus status,
            String message
    ) {
        return BeghouledActionResult.of(
                status,
                message,
                game.getCurrentStageNumber(),
                game.getSunAmount(),
                game.getCompletedMatchCount(),
                game.getTargetMatchCount()
        );
    }

}
