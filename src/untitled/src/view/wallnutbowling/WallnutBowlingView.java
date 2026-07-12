package view.wallnutbowling;

import model.mechanism.Position;
import model.minigame.wallnutbowlingminigame.BowlingWallnutType;
import model.minigame.wallnutbowlingminigame.RollingWallnut;
import model.minigame.wallnutbowlingminigame.WallnutBowlingActionResult;
import model.minigame.wallnutbowlingminigame.WallnutBowlingActionStatus;
import model.minigame.wallnutbowlingminigame.WallnutBowlingStateResult;
import view.CommandHandler;

import java.util.regex.Matcher;

public class WallnutBowlingView
        implements CommandHandler {

    private WallnutBowlingViewObserver observer;

    public void setObserver(
            WallnutBowlingViewObserver observer
    ) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Wallnut Bowling controller "
                            + "is not connected."
            );
            return;
        }

        Matcher matcher;

        matcher = WallnutBowlingCommands.START
                .getMatcher(input);

        if (matcher != null) {
            handleStartCommand(matcher);
            return;
        }

        matcher = WallnutBowlingCommands
                .PLACE_WALLNUT
                .getMatcher(input);

        if (matcher != null) {
            handlePlaceCommand(matcher);
            return;
        }

        matcher = WallnutBowlingCommands.SHOW
                .getMatcher(input);

        if (matcher != null) {
            WallnutBowlingStateResult state =
                    observer
                            .onShowWallnutBowlingRequested();

            showState(state);
            return;
        }

        matcher = WallnutBowlingCommands
                .ADVANCE_TIME
                .getMatcher(input);

        if (matcher != null) {
            handleAdvanceCommand(matcher);
            return;
        }

        matcher = WallnutBowlingCommands.HELP
                .getMatcher(input);

        if (matcher != null) {
            showHelp();
            return;
        }

        matcher = WallnutBowlingCommands.BACK
                .getMatcher(input);

        if (matcher != null) {
            /*
             * TODO: Notify the main menu router after
             * menu navigation is implemented.
             */

            System.out.println(
                    "Returning to minigame menu."
            );
            return;
        }

        System.out.println(
                "Invalid command. Use "
                        + "'wallnut bowling help'."
        );
    }

    private void handleStartCommand(
            Matcher matcher
    ) {
        String stageText = matcher.group("stage");

        int stageNumber = 1;

        if (stageText != null) {
            stageNumber = Integer.parseInt(
                    stageText
            );
        }

        WallnutBowlingActionResult result =
                observer
                        .onStartWallnutBowlingRequested(
                                stageNumber
                        );

        showActionResult(result);
    }

    private void handlePlaceCommand(
            Matcher matcher
    ) {
        int conveyorIndex = Integer.parseInt(
                matcher.group("index")
        );

        Position position = readPosition(
                matcher,
                "x",
                "y"
        );

        WallnutBowlingActionResult result =
                observer.onPlaceWallnutRequested(
                        conveyorIndex,
                        position
                );

        showActionResult(result);
    }

    private void handleAdvanceCommand(
            Matcher matcher
    ) {
        int ticks = Integer.parseInt(
                matcher.group("ticks")
        );

        WallnutBowlingActionResult result =
                observer.onAdvanceTicksRequested(
                        ticks
                );

        showActionResult(result);
    }

    private Position readPosition(
            Matcher matcher,
            String xGroup,
            String yGroup
    ) {
        int x = Integer.parseInt(
                matcher.group(xGroup)
        );

        int y = Integer.parseInt(
                matcher.group(yGroup)
        );

        return new Position(x, y);
    }

    private void showActionResult(
            WallnutBowlingActionResult result
    ) {
        if (result == null) {
            System.out.println("No result.");
            return;
        }

        WallnutBowlingActionStatus status =
                result.getStatus();

        switch (status) {
            case STARTED:
                System.out.println(
                        "Wallnut Bowling stage "
                                + result.getStageNumber()
                                + " started."
                );
                break;

            case WALLNUT_GENERATED:
                System.out.println(
                        displayWallnutType(
                                result.getWallnutType()
                        )
                                + " was added to "
                                + "the conveyor belt."
                );
                break;

            case WALLNUT_PLACED:
                System.out.println(
                        displayWallnutType(
                                result.getWallnutType()
                        )
                                + " from conveyor slot "
                                + result.getConveyorIndex()
                                + " was placed at "
                                + formatPosition(
                                result.getPosition()
                        )
                                + "."
                );
                break;

            case TIME_ADVANCED:
                System.out.println(
                        "Time advanced by "
                                + result.getAdvancedTicks()
                                + " ticks."
                );
                break;

            case NO_WALLNUT_AVAILABLE:
                System.out.println(
                        "The conveyor belt is empty."
                );
                break;

            case INVALID_CONVEYOR_INDEX:
                System.out.println(
                        "There is no wallnut at conveyor "
                                + "slot "
                                + result.getConveyorIndex()
                                + "."
                );
                break;

            case INVALID_STAGE:
                System.out.println(
                        "Wallnut Bowling stage must "
                                + "be between 1 and 3."
                );
                break;

            case STAGE_LOCKED:
                System.out.println(
                        "Wallnut Bowling stage "
                                + result.getStageNumber()
                                + " is locked."
                );
                break;

            case GAME_NOT_STARTED:
                System.out.println(
                        "Start Wallnut Bowling first."
                );
                break;

            case GAME_ALREADY_FINISHED:
                System.out.println(
                        "This Wallnut Bowling stage "
                                + "is already finished."
                );
                break;

            case INVALID_POSITION:
                System.out.println(
                        "Position "
                                + formatPosition(
                                result.getPosition()
                        )
                                + " is outside the 9x5 lawn."
                );
                break;

            case OUTSIDE_PLANTING_AREA:
                System.out.println(
                        "Wallnuts can only be placed "
                                + "between the house and "
                                + "the red line."
                );
                break;

            case INTEGRATION_NOT_READY:
                System.out.println(
                        "Wallnut Bowling is waiting for "
                                + "Board and Zombie integration."
                );
                break;

            case INVALID_ACTION:
                System.out.println(
                        "This action cannot be completed."
                );
                break;

            default:
                System.out.println(
                        "Unknown Wallnut Bowling result."
                );
                break;
        }

        if (result.isWon()) {
            System.out.println("Another day...");
        }

        if (result.isLost()) {
            System.out.println(
                    "We ate your brainz dear humanz."
            );
        }
    }

    private void showState(
            WallnutBowlingStateResult state
    ) {
        if (state == null) {
            System.out.println(
                    "Wallnut Bowling state "
                            + "is not available."
            );
            return;
        }

        System.out.println(
                "Wallnut Bowling stage: "
                        + state.getStageNumber()
        );

        System.out.println(
                "Current tick: "
                        + state.getCurrentTick()
        );

        System.out.println(
                "Planting boundary: columns 1 to "
                        + state.getPlantingBoundaryColumn()
        );

        System.out.println(
                "Ticks until next wallnut: "
                        + state.getTicksUntilNextGeneration()
        );

        System.out.println(
                "Integration ready: "
                        + state.isIntegrationReady()
        );

        showConveyorBelt(state);
        showLawn(state);
        showRollingWallnuts(state);

        if (!state.isStarted()) {
            System.out.println(
                    "State: not started"
            );
        } else if (state.isWon()) {
            System.out.println(
                    "State: won"
            );
        } else if (state.isLost()) {
            System.out.println(
                    "State: lost"
            );
        } else {
            System.out.println(
                    "State: running"
            );
        }
    }

    private void showConveyorBelt(
            WallnutBowlingStateResult state
    ) {
        System.out.println("Conveyor belt:");

        if (state.getConveyorBelt().isEmpty()) {
            System.out.println("- empty");
            return;
        }

        for (int i = 0;
             i < state.getConveyorBelt().size();
             i++) {

            BowlingWallnutType type =
                    state.getConveyorBelt().get(i);

            System.out.println(
                    "- slot "
                            + (i + 1)
                            + ": "
                            + displayWallnutType(type)
            );
        }
    }

    private void showRollingWallnuts(
            WallnutBowlingStateResult state
    ) {
        System.out.println(
                "Moving wallnuts:"
        );

        boolean found = false;

        for (RollingWallnut wallnut
                : state.getRollingWallnuts()) {

            if (wallnut == null
                    || !wallnut.isMoving()) {
                continue;
            }

            found = true;

            System.out.println(
                    "- "
                            + displayWallnutType(
                            wallnut.getType()
                    )
                            + " at "
                            + formatPosition(
                            wallnut.getPosition()
                    )
                            + " | direction: "
                            + wallnut.getDirectionAngle()
                            + " degrees"
                            + " | collisions: "
                            + wallnut.getCollisionCount()
            );
        }

        if (!found) {
            System.out.println("- none");
        }
    }

    private void showLawn(
            WallnutBowlingStateResult state
    ) {
        System.out.println(
                "Lawn:"
        );

        System.out.println(
                "N = normal, E = explosive, "
                        + "G = giant, | = red line"
        );

        System.out.println(
                "    1 2 3 | 4 5 6 7 8 9"
        );

        for (int y = 1; y <= 5; y++) {
            StringBuilder row = new StringBuilder();

            row.append(y).append(" | ");

            for (int x = 1; x <= 9; x++) {
                Position position = new Position(x, y);

                row.append(
                        wallnutSymbolAt(
                                state,
                                position
                        )
                );

                row.append(' ');

                if (x == state
                        .getPlantingBoundaryColumn()) {

                    row.append("| ");
                }
            }

            System.out.println(row);
        }
    }

    private char wallnutSymbolAt(
            WallnutBowlingStateResult state,
            Position position
    ) {
        for (RollingWallnut wallnut
                : state.getRollingWallnuts()) {

            if (wallnut == null
                    || wallnut.getPosition() == null
                    || !samePosition(
                    wallnut.getPosition(),
                    position
            )) {
                continue;
            }

            if (wallnut.getType()
                    == BowlingWallnutType
                    .EXPLODE_O_NUT) {

                return 'E';
            }

            if (wallnut.getType()
                    == BowlingWallnutType
                    .GIANT_WALLNUT) {

                return 'G';
            }

            return 'N';
        }

        return '.';
    }

    private boolean samePosition(
            Position first,
            Position second
    ) {
        if (first == null || second == null) {
            return false;
        }

        return first.getX() == second.getX()
                && first.getY() == second.getY();
    }

    private String displayWallnutType(
            BowlingWallnutType type
    ) {
        if (type == null) {
            return "Unknown Wallnut";
        }

        return switch (type) {
            case BOWLING_WALLNUT ->
                    "Bowling Wallnut";

            case EXPLODE_O_NUT ->
                    "Explode-O-Nut";

            case GIANT_WALLNUT ->
                    "Giant Wallnut";
        };
    }

    private String formatPosition(
            Position position
    ) {
        if (position == null) {
            return "(unknown)";
        }

        return "("
                + position.getX()
                + ", "
                + position.getY()
                + ")";
    }

    private void showHelp() {
        System.out.println(
                "Wallnut Bowling commands:"
        );

        System.out.println(
                "- wallnut bowling start [-s <1..3>]"
        );

        System.out.println(
                "- wallnut bowling show"
        );

        System.out.println(
                "- wallnut bowling place "
                        + "-i <conveyor_slot> "
                        + "-l (<x>, <y>)"
        );

        System.out.println(
                "- wallnut bowling advance -t <ticks>"
        );

        System.out.println(
                "- wallnut bowling help"
        );

        System.out.println(
                "- Back to minigame menu"
        );
    }
}