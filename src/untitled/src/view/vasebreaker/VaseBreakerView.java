package view.vasebreaker;

import lombok.Setter;
import model.mechanism.Position;
import model.minigame.vasebreakerminigame.DroppedSeedPacket;
import model.minigame.vasebreakerminigame.Vase;
import model.minigame.vasebreakerminigame.VaseContentType;
import model.minigame.vasebreakerminigame.VaseType;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerActionStatus;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;
import view.CommandHandler;

import java.util.regex.Matcher;

@Setter
public class VaseBreakerView implements CommandHandler {
    private VasebreakerViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Vasebreaker controller is not connected."
            );
            return;
        }

        Matcher matcher;

        matcher = VasebreakerCommands.START
                .getMatcher(input);

        if (matcher != null) {
            handleStartCommand(matcher);
            return;
        }

        matcher = VasebreakerCommands.BREAK_VASE
                .getMatcher(input);

        if (matcher != null) {
            handleBreakCommand(matcher);
            return;
        }

        matcher = VasebreakerCommands
                .COLLECT_SEED_PACKET
                .getMatcher(input);

        if (matcher != null) {
            handleCollectCommand(matcher);
            return;
        }

        matcher = VasebreakerCommands
                .PLANT_SEED_PACKET
                .getMatcher(input);

        if (matcher != null) {
            handlePlantCommand(matcher);
            return;
        }

        matcher = VasebreakerCommands.SHOW
                .getMatcher(input);

        if (matcher != null) {
            VasebreakerStateResult state =
                    observer.onShowVasebreakerRequested();

            showState(state);
            return;
        }

        matcher = VasebreakerCommands.ADVANCE_TIME
                .getMatcher(input);

        if (matcher != null) {
            handleAdvanceTimeCommand(matcher);
            return;
        }

        matcher = VasebreakerCommands.HELP
                .getMatcher(input);

        if (matcher != null) {
            showHelp();
            return;
        }

        matcher = VasebreakerCommands.BACK
                .getMatcher(input);

        if (matcher != null) {
            /*
             * TODO: Notify the main menu router after the
             * menu-navigation system is completed.
             */

            System.out.println(
                    "Returning to minigame menu."
            );
            return;
        }

        System.out.println(
                "Invalid command. Use 'vasebreaker help'."
        );
    }

    private void handleStartCommand(
            Matcher matcher
    ) {
        String stageText = matcher.group("stage");

        int stageNumber = 1;

        if (stageText != null) {
            stageNumber = Integer.parseInt(stageText);
        }

        VasebreakerActionResult result =
                observer.onStartVasebreakerRequested(
                        stageNumber
                );

        showActionResult(result);
    }

    private void handleBreakCommand(
            Matcher matcher
    ) {
        Position position = readPosition(
                matcher,
                "x",
                "y"
        );

        VasebreakerActionResult result =
                observer.onBreakVaseRequested(
                        position
                );

        showActionResult(result);
    }

    private void handleCollectCommand(
            Matcher matcher
    ) {
        Position position = readPosition(
                matcher,
                "x",
                "y"
        );

        VasebreakerActionResult result =
                observer.onCollectSeedPacketRequested(
                        position
                );

        showActionResult(result);
    }

    private void handlePlantCommand(
            Matcher matcher
    ) {
        String plantName = cleanPlantName(
                matcher.group("plantName")
        );

        Position position = readPosition(
                matcher,
                "x",
                "y"
        );

        VasebreakerActionResult result =
                observer.onPlantSeedPacketRequested(
                        plantName,
                        position
                );

        showActionResult(result);
    }

    private void handleAdvanceTimeCommand(
            Matcher matcher
    ) {
        int ticks = Integer.parseInt(
                matcher.group("ticks")
        );

        VasebreakerActionResult result =
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

    private String cleanPlantName(
            String plantName
    ) {
        if (plantName == null) {
            return null;
        }

        String cleanedName = plantName.trim();

        if (cleanedName.length() >= 2
                && cleanedName.startsWith("\"")
                && cleanedName.endsWith("\"")) {

            cleanedName = cleanedName.substring(
                    1,
                    cleanedName.length() - 1
            );
        }

        return cleanedName.trim();
    }

    private void showActionResult(
            VasebreakerActionResult result
    ) {
        if (result == null) {
            System.out.println("No result.");
            return;
        }

        VasebreakerActionStatus status =
                result.getStatus();

        switch (status) {
            case STARTED:
                System.out.println(
                        "Vasebreaker stage "
                                + result.getStageNumber()
                                + " started."
                );
                break;

            case VASE_BROKEN:
                showVaseBrokenResult(result);
                break;

            case SEED_PACKET_COLLECTED:
                showSeedPacketCollectedResult(result);
                break;

            case PLANT_FROM_PACKET:
                showPlantFromPacketResult(result);
                break;

            case TIME_ADVANCED:
                System.out.println(
                        "Time advanced by "
                                + result.getAdvancedTicks()
                                + " ticks."
                );
                break;

            case NO_VASE_AT_POSITION:
                System.out.println(
                        "There is no unbroken vase at "
                                + formatPosition(
                                result.getPosition()
                        )
                                + "."
                );
                break;

            case NO_SEED_PACKET_AT_POSITION:
                System.out.println(
                        "There is no available seed packet at "
                                + formatPosition(
                                result.getPosition()
                        )
                                + "."
                );
                break;

            case SEED_PACKET_NOT_AVAILABLE:
                System.out.println(
                        "This seed packet is not available."
                );
                break;

            case NO_COLLECTED_SEED_PACKET:
                System.out.println(
                        "You do not have a collected "
                                + displayPlantName(
                                result.getPlantName()
                        )
                                + " seed packet."
                );
                break;

            case INVALID_STAGE:
                System.out.println(
                        "Vasebreaker stage must be between 1 and 3."
                );
                break;

            case STAGE_LOCKED:
                System.out.println(
                        "Vasebreaker stage "
                                + result.getStageNumber()
                                + " is locked."
                );
                break;

            case GAME_NOT_STARTED:
                System.out.println(
                        "Start Vasebreaker first."
                );
                break;

            case GAME_ALREADY_FINISHED:
                System.out.println(
                        "This Vasebreaker stage is already finished."
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

            case TILE_OCCUPIED:
                System.out.println(
                        "Planting is not possible at "
                                + formatPosition(
                                result.getPosition()
                        )
                                + "."
                );
                break;

            case TILE_HAS_UNBROKEN_VASE:
                System.out.println(
                        "Break the vase at "
                                + formatPosition(
                                result.getPosition()
                        )
                                + " before planting there."
                );
                break;

            case INVALID_ACTION:
                System.out.println(
                        "This action cannot be completed."
                );
                break;

            default:
                System.out.println(
                        "Unknown Vasebreaker result."
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

    private void showVaseBrokenResult(
            VasebreakerActionResult result
    ) {
        System.out.println(
                "Vase at "
                        + formatPosition(
                        result.getPosition()
                )
                        + " was broken."
        );

        if (result.getContentType()
                == VaseContentType.EMPTY) {

            System.out.println(
                    "The vase was empty."
            );

        } else if (result.getContentType()
                == VaseContentType.SEED_PACKET) {

            System.out.println(
                    "A "
                            + displayPlantName(
                            result.getPlantName()
                    )
                            + " seed packet dropped at "
                            + formatPosition(
                            result.getPosition()
                    )
                            + "."
            );

        } else if (result.getContentType()
                == VaseContentType.ZOMBIE) {

            if (result.isZombieReleased()) {
                System.out.println(
                        "A zombie was released at "
                                + formatPosition(
                                result.getPosition()
                        )
                                + "."
                );
            } else {
                System.out.println(
                        "A zombie was inside the vase."
                );

                /*
                 * TODO: The zombie will actually be released
                 * after ZombieFactory and Board are connected.
                 */
            }
        }
    }

    private void showSeedPacketCollectedResult(
            VasebreakerActionResult result
    ) {
        System.out.println(
                displayPlantName(result.getPlantName())
                        + " seed packet collected from "
                        + formatPosition(
                        result.getPosition()
                )
                        + "."
        );
    }

    private void showPlantFromPacketResult(
            VasebreakerActionResult result
    ) {
        System.out.println(
                displayPlantName(result.getPlantName())
                        + " was planted at "
                        + formatPosition(
                        result.getPosition()
                )
                        + "."
        );
    }

    private void showState(
            VasebreakerStateResult state
    ) {
        if (state == null) {
            System.out.println(
                    "Vasebreaker state is not available."
            );
            return;
        }

        System.out.println(
                "Vasebreaker stage: "
                        + state.getStageNumber()
        );

        System.out.println(
                "Current tick: "
                        + state.getCurrentTick()
        );

        System.out.println(
                "Broken vases: "
                        + state.getBrokenVaseCount()
        );

        System.out.println(
                "Remaining vases: "
                        + state.getRemainingVaseCount()
        );

        showLawn(state);
        showDroppedSeedPackets(state);
        showCollectedSeedPackets(state);

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

    private void showLawn(
            VasebreakerStateResult state
    ) {
        System.out.println(
                "Lawn:"
        );

        System.out.println(
                "V = normal vase, P = plant vase, "
                        + "G = Gargantuar vase, s = seed packet"
        );

        System.out.println(
                "    1 2 3 4 5 6 7 8 9"
        );

        for (int y = 1; y <= 5; y++) {
            StringBuilder row = new StringBuilder();

            row.append(y).append(" | ");

            for (int x = 1; x <= 9; x++) {
                Position position = new Position(x, y);

                row.append(
                        symbolAt(state, position)
                );

                row.append(' ');
            }

            System.out.println(row);
        }
    }

    private char symbolAt(
            VasebreakerStateResult state,
            Position position
    ) {
        for (Vase vase : state.getVases()) {
            if (vase == null
                    || vase.isBroken()
                    || !vase.isAt(position)) {
                continue;
            }

            if (vase.getType()
                    == VaseType.PLANT) {
                return 'P';
            }

            if (vase.getType()
                    == VaseType.GARGANTUAR) {
                return 'G';
            }

            return 'V';
        }

        for (DroppedSeedPacket packet
                : state.getDroppedSeedPackets()) {

            if (packet != null
                    && packet.isAt(position)
                    && packet.isAvailable(
                    state.getCurrentTick()
            )) {
                return 's';
            }
        }

        return '.';
    }

    private void showDroppedSeedPackets(
            VasebreakerStateResult state
    ) {
        System.out.println(
                "Dropped seed packets:"
        );

        boolean found = false;

        for (DroppedSeedPacket packet
                : state.getDroppedSeedPackets()) {

            if (packet == null
                    || !packet.isAvailable(
                    state.getCurrentTick()
            )) {
                continue;
            }

            found = true;

            System.out.println(
                    "- "
                            + displayPlantName(
                            packet.getPlantName()
                    )
                            + " at "
                            + formatPosition(
                            packet.getPosition()
                    )
                            + " | expires in "
                            + packet.getRemainingTicks(
                            state.getCurrentTick()
                    )
                            + " ticks"
            );
        }

        if (!found) {
            System.out.println("- none");
        }
    }

    private void showCollectedSeedPackets(
            VasebreakerStateResult state
    ) {
        System.out.println(
                "Collected seed packets:"
        );

        boolean found = false;

        for (DroppedSeedPacket packet
                : state.getCollectedSeedPackets()) {

            if (packet == null
                    || !packet.isPlantable()) {
                continue;
            }

            found = true;

            System.out.println(
                    "- "
                            + displayPlantName(
                            packet.getPlantName()
                    )
            );
        }

        if (!found) {
            System.out.println("- none");
        }
    }

    private String displayPlantName(
            String plantName
    ) {
        if (plantName == null
                || plantName.isBlank()) {
            return "unknown plant";
        }

        return plantName;
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
                "Vasebreaker commands:"
        );

        System.out.println(
                "- vasebreaker start [-s <1..3>]"
        );

        System.out.println(
                "- vasebreaker show"
        );

        System.out.println(
                "- vasebreaker break -l (<x>, <y>)"
        );

        System.out.println(
                "- vasebreaker collect -l (<x>, <y>)"
        );

        System.out.println(
                "- vasebreaker plant -p <plant_name> "
                        + "-l (<x>, <y>)"
        );

        System.out.println(
                "- vasebreaker advance -t <ticks>"
        );

        System.out.println(
                "- vasebreaker help"
        );

        System.out.println(
                "- Back to minigame menu"
        );
    }
}