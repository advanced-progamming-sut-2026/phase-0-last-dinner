package view.vasebreaker;

import model.mechanism.Position;
import model.minigame.vasebreakerminigame.DroppedSeedPacket;
import model.minigame.vasebreakerminigame.Vase;
import model.minigame.vasebreakerminigame.VaseContentType;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerActionStatus;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;
import view.CommandHandler;

import java.util.regex.Matcher;

public class VaseBreakerView implements CommandHandler {
    private VasebreakerViewObserver observer;

    public void setObserver(VasebreakerViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println("Vasebreaker controller is not connected.");
            return;
        }

        Matcher matcher;

        matcher = VasebreakerCommands.START.getMatcher(input);
        if (matcher != null) {
            VasebreakerActionResult result = observer.onStartVasebreakerRequested();
            showActionResult(result);
            return;
        }

        matcher = VasebreakerCommands.BREAK_VASE.getMatcher(input);
        if (matcher != null) {
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));

            VasebreakerActionResult result = observer.onBreakVaseRequested(new Position(x, y));
            showActionResult(result);
            return;
        }

        matcher = VasebreakerCommands.COLLECT_SEED_PACKET.getMatcher(input);
        if (matcher != null) {
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));

            VasebreakerActionResult result = observer.onCollectSeedPacketRequested(new Position(x, y));
            showActionResult(result);
            return;
        }

        matcher = VasebreakerCommands.SHOW.getMatcher(input);
        if (matcher != null) {
            VasebreakerStateResult state = observer.onShowVasebreakerRequested();
            showState(state);
            return;
        }

        matcher = VasebreakerCommands.ADVANCE_TIME.getMatcher(input);
        if (matcher != null) {
            int ticks = Integer.parseInt(matcher.group("ticks"));

            VasebreakerActionResult result = observer.onAdvanceTicksRequested(ticks);
            showActionResult(result);
            return;
        }

        matcher = VasebreakerCommands.BACK.getMatcher(input);
        if (matcher != null) {
            System.out.println("Returning to minigame menu.");
            return;
        }

        System.out.println("Invalid command.");
    }

    private void showActionResult(VasebreakerActionResult result) {
        if (result == null) {
            System.out.println("No result.");
            return;
        }

        VasebreakerActionStatus status = result.getStatus();

        if (status == VasebreakerActionStatus.STARTED) {
            System.out.println("Vasebreaker started.");
        } else if (status == VasebreakerActionStatus.NO_VASE_AT_POSITION) {
            System.out.println("There is no unbroken vase at " + result.getPosition() + ".");
        } else if (status == VasebreakerActionStatus.NO_SEED_PACKET_AT_POSITION) {
            System.out.println("There is no available seed packet at " + result.getPosition() + ".");
        } else if (status == VasebreakerActionStatus.SEED_PACKET_NOT_AVAILABLE) {
            System.out.println("This seed packet is not available.");
        } else if (status == VasebreakerActionStatus.SEED_PACKET_COLLECTED) {
            System.out.println("Seed packet collected at " + result.getPosition() + ".");
        } else if (status == VasebreakerActionStatus.TIME_ADVANCED) {
            System.out.println("Time advanced by " + result.getAdvancedTicks() + " ticks.");
        } else if (status == VasebreakerActionStatus.VASE_BROKEN) {
            showVaseBrokenResult(result);
        } else if (status == VasebreakerActionStatus.PLANT_FROM_PACKET) {
            System.out.println("Plant from seed packet was planted at " + result.getPosition() + ".");
        } else {
            System.out.println("Invalid action.");
        }

        if (result.isWon()) {
            System.out.println("Another day...");
        }

        if (result.isLost()) {
            System.out.println("We ate your brainz dear humanz.");
        }
    }

    private void showVaseBrokenResult(VasebreakerActionResult result) {
        System.out.println("Vase at " + result.getPosition() + " shikast.");

        if (result.getContentType() == VaseContentType.EMPTY) {
            System.out.println("The vase was empty.");
        } else if (result.getContentType() == VaseContentType.SEED_PACKET) {
            System.out.println("A seed packet dropped at " + result.getPosition() + ".");
        } else if (result.getContentType() == VaseContentType.ZOMBIE) {
            System.out.println("A zombie was released at " + result.getPosition() + ".");
        }
    }

    private void showState(VasebreakerStateResult state) {
        System.out.println("Vasebreaker state:");
        System.out.println("Current tick: " + state.getCurrentTick());

        System.out.println("Vases:");
        for (Vase vase : state.getVases()) {
            String status = vase.isBroken() ? "broken" : "unbroken";
            System.out.println(
                    "- " + vase.getPosition()
                            + " | type: " + vase.getType()
                            + " | status: " + status
            );
        }

        System.out.println("Dropped seed packets:");
        for (DroppedSeedPacket seedPacket : state.getDroppedSeedPackets()) {
            if (seedPacket.isAvailable(state.getCurrentTick())) {
                System.out.println("- packet at " + seedPacket.getPosition());
            }
        }

        if (state.isWon()) {
            System.out.println("State: won");
        } else if (state.isLost()) {
            System.out.println("State: lost");
        } else {
            System.out.println("State: running");
        }
    }
}