package view.beghouled;

import lombok.Setter;
import model.mechanism.Position;
import model.minigame.beghouledminigame.PlantUpgradeOption;
import model.minigame.beghouledminigame.BeghouledActionResult;
import model.minigame.beghouledminigame.BeghouledActionStatus;
import model.minigame.beghouledminigame.BeghouledStateResult;
import view.CommandHandler;

import java.util.regex.Matcher;

@Setter
public class BeghouledView implements CommandHandler {

    private static final int CELL_WIDTH = 13;

    private BeghouledViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Beghouled controller is not connected."
            );
            return;
        }

        if (handleActionCommand(input) || handleUtilityCommand(input)) {
            return;
        }

        System.out.println("Invalid Beghouled command.");
        showCommandHelp();
    }

    private boolean handleActionCommand(String input) {
        Matcher matcher = BeghouledCommands.START.getMatcher(input);
        if (matcher != null) {
            handleStartCommand(matcher);
            return true;
        }

        matcher = BeghouledCommands.SWAP.getMatcher(input);
        if (matcher != null) {
            handleSwapCommand(matcher);
            return true;
        }

        matcher = BeghouledCommands.UPGRADE.getMatcher(input);
        if (matcher != null) {
            handleUpgradeCommand(matcher);
            return true;
        }

        matcher = BeghouledCommands.SHOW.getMatcher(input);
        if (matcher != null) {
            showState(
                    observer.onShowBeghouledRequested()
            );
            return true;
        }

        return false;
    }

    private boolean handleUtilityCommand(String input) {
        Matcher matcher = BeghouledCommands.ADVANCE_TIME.getMatcher(input);
        if (matcher != null) {
            handleAdvanceCommand(matcher);
            return true;
        }

        matcher = BeghouledCommands.HELP.getMatcher(input);
        if (matcher != null) {
            showCommandHelp();
            return true;
        }

        matcher = BeghouledCommands.BACK.getMatcher(input);
        if (matcher != null) {
            System.out.println(
                    "Returning to minigame menu."
            );
            return true;
        }

        return false;
    }

    private void handleStartCommand(Matcher matcher) {
        try {
            String stageText = matcher.group("stage");

            int stageNumber = stageText == null
                    ? 1
                    : Integer.parseInt(stageText);

            showActionResult(
                    observer.onStartBeghouledRequested(
                            stageNumber
                    )
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid Beghouled stage number."
            );
        }
    }

    private void handleSwapCommand(Matcher matcher) {
        try {
            Position first = new Position(
                    Integer.parseInt(
                            matcher.group("firstX")
                    ),
                    Integer.parseInt(
                            matcher.group("firstY")
                    )
            );

            Position second = new Position(
                    Integer.parseInt(
                            matcher.group("secondX")
                    ),
                    Integer.parseInt(
                            matcher.group("secondY")
                    )
            );

            showActionResult(
                    observer.onSwapRequested(
                            first,
                            second
                    )
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid Beghouled position."
            );
        }
    }

    private void handleUpgradeCommand(Matcher matcher) {
        String plantName =
                matcher.group("plant").trim();

        showActionResult(
                observer.onUpgradeRequested(plantName)
        );
    }

    private void handleAdvanceCommand(Matcher matcher) {
        try {
            int ticks = Integer.parseInt(
                    matcher.group("ticks")
            );

            showActionResult(
                    observer.onAdvanceTicksRequested(ticks)
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid tick count."
            );
        }
    }

    private void showActionResult(
            BeghouledActionResult result
    ) {
        if (result == null) {
            System.out.println(
                    "No Beghouled result was returned."
            );
            return;
        }

        if (result.isSuccessful()) {
            System.out.println(result.getMessage());
        } else {
            System.out.println(
                    "Action failed: "
                            + result.getStatus()
            );

            if (!result.getMessage().trim().isEmpty()) {
                System.out.println(result.getMessage());
            }
        }

        System.out.println(
                "Stage: " + result.getStageNumber()
        );

        System.out.println(
                "Sun: " + result.getSunAmount()
        );

        System.out.println(
                "Matches: "
                        + result.getCompletedMatchCount()
                        + "/"
                        + result.getTargetMatchCount()
        );

        showActionStatus(result);
    }

    private void showActionStatus(BeghouledActionResult result) {
        if (result.getStatus()
                == BeghouledActionStatus.STAGE_WON) {
            System.out.println(
                    "The next Beghouled stage is now unlocked."
            );
        }

        if (result.getStatus()
                == BeghouledActionStatus.GAME_WON) {
            System.out.println(
                    "You completed all Beghouled stages!"
            );
        }

        if (result.getStatus()
                == BeghouledActionStatus.GAME_LOST) {
            System.out.println(
                    "The zombies reached the house."
            );
        }
    }

    private void showState(BeghouledStateResult state) {
        if (state == null) {
            System.out.println(
                    "No Beghouled state was returned."
            );
            return;
        }

        System.out.println("Beghouled state:");

        showStateSummary(state);
        showBoard(state);
        showUpgrades(state);

        System.out.println(
                "State: " + getStateText(state)
        );
    }

    private void showStateSummary(BeghouledStateResult state) {
        System.out.println(
                "Stage: "
                        + state.getStageNumber()
                        + "/3"
        );

        System.out.println(
                "Highest unlocked stage: "
                        + state.getHighestUnlockedStage()
        );

        System.out.println(
                "Sun: " + state.getSunAmount()
        );

        System.out.println(
                "Matches: "
                        + state.getCompletedMatchCount()
                        + "/"
                        + state.getTargetMatchCount()
        );

        System.out.println(
                "Remaining matches: "
                        + state.getRemainingMatchCount()
        );

        System.out.println(
                "Possible move: "
                        + (state.isPossibleMove()
                        ? "yes"
                        : "no")
        );

        System.out.println(
                "Zombie waves: "
                        + (state.isEndlessZombieWaves()
                        ? "endless"
                        : "disabled")
        );
    }

    private void showBoard(BeghouledStateResult state) {
        System.out.println("Board:");

        System.out.print(pad("Row/Col"));

        for (int x = 1;
             x <= BeghouledStateResult.COLUMN_COUNT;
             x++) {
            System.out.print(pad("C" + x));
        }

        System.out.println();

        for (int y = 1;
             y <= BeghouledStateResult.ROW_COUNT;
             y++) {

            System.out.print(pad("R" + y));

            for (int x = 1;
                 x <= BeghouledStateResult.COLUMN_COUNT;
                 x++) {

                Position position = new Position(x, y);

                String value;

                if (state.isCrater(position)) {
                    value = "CRATER";
                } else {
                    value = state.getPlantNameAt(position);

                    if (value.trim().isEmpty()) {
                        value = ".";
                    }
                }

                System.out.print(pad(value));
            }

            System.out.println();
        }
    }

    private void showUpgrades(
            BeghouledStateResult state
    ) {
        System.out.println("Available upgrades:");

        if (state.getUpgradeOptions().isEmpty()) {
            System.out.println(
                    "- No upgrades are available."
            );
            return;
        }

        for (PlantUpgradeOption option
                : state.getUpgradeOptions()) {

            System.out.println(
                    "- "
                            + option.getSourcePlant().getName()
                            + " -> "
                            + option.getTargetPlant().getName()
                            + " | cost: "
                            + option.getSunCost()
            );
        }
    }

    private String getStateText(
            BeghouledStateResult state
    ) {
        if (!state.isStarted()) {
            return "not started";
        }

        if (state.isLost()) {
            return "lost";
        }

        if (state.isWon()) {
            return "won";
        }

        if (state.isCompleted()) {
            return "stage completed";
        }

        return "running";
    }

    private String pad(String value) {
        String safeValue = value == null
                ? ""
                : value;

        if (safeValue.length() >= CELL_WIDTH) {
            safeValue = safeValue.substring(
                    0,
                    CELL_WIDTH - 1
            );
        }

        return String.format(
                "%-" + CELL_WIDTH + "s",
                safeValue
        );
    }

    private void showCommandHelp() {
        System.out.println("Available commands:");

        System.out.println(
                "- beghouled start"
        );

        System.out.println(
                "- beghouled start -s <stage>"
        );

        System.out.println(
                "- beghouled swap -f (x, y) -t (x, y)"
        );

        System.out.println(
                "- beghouled upgrade -p <plant name>"
        );

        System.out.println(
                "- beghouled show"
        );

        System.out.println(
                "- beghouled advance -t <ticks>"
        );

        System.out.println(
                "- beghouled help"
        );

        System.out.println(
                "- Back to minigame menu"
        );
    }
}
