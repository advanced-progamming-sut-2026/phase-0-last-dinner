package view.izombie;

import lombok.Setter;
import model.mechanism.Position;
import model.minigame.izombieminigame.IZombieActionResult;
import model.minigame.izombieminigame.IZombieActionStatus;
import model.minigame.izombieminigame.IZombieStateResult;
import model.zombie.ZombieDefinition;
import view.CommandHandler;

import java.util.Map;
import java.util.regex.Matcher;

@Setter
public class IZombieView implements CommandHandler {

    private IZombieViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "I, Zombie controller is not connected."
            );
            return;
        }

        if (handleGameCommand(input) || handleNavigationCommand(input)) {
            return;
        }

        System.out.println("Invalid I, Zombie command.");
        showCommandHelp();
    }

    private boolean handleGameCommand(String input) {
        Matcher matcher = IZombieCommands.START.getMatcher(input);
        if (matcher != null) {
            IZombieActionResult result =
                    observer.onStartIZombieRequested();

            showActionResult(result);
            return true;
        }

        matcher = IZombieCommands.PLACE_ZOMBIE.getMatcher(input);
        if (matcher != null) {
            handlePlaceZombieCommand(matcher);
            return true;
        }

        matcher = IZombieCommands.SHOW.getMatcher(input);
        if (matcher != null) {
            IZombieStateResult state =
                    observer.onShowIZombieRequested();

            showState(state);
            return true;
        }

        matcher = IZombieCommands.ADVANCE_TIME.getMatcher(input);
        if (matcher != null) {
            handleAdvanceTimeCommand(matcher);
            return true;
        }

        return false;
    }

    private boolean handleNavigationCommand(String input) {
        Matcher matcher = IZombieCommands.BACK.getMatcher(input);
        if (matcher != null) {
            // TODO Menu router:
            // Connect this command to the minigame menu router.
            System.out.println(
                    "Returning to minigame menu."
            );
            return true;
        }

        return false;
    }

    private void handlePlaceZombieCommand(Matcher matcher) {
        try {
            String zombieAliasOrName =
                    matcher.group("zombie").trim();

            int x = Integer.parseInt(
                    matcher.group("x")
            );

            int y = Integer.parseInt(
                    matcher.group("y")
            );

            IZombieActionResult result =
                    observer.onPlaceZombieRequested(
                            zombieAliasOrName,
                            new Position(x, y)
                    );

            showActionResult(result);
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid zombie position."
            );
        }
    }

    private void handleAdvanceTimeCommand(Matcher matcher) {
        try {
            int ticks = Integer.parseInt(
                    matcher.group("ticks")
            );

            IZombieActionResult result =
                    observer.onAdvanceTicksRequested(ticks);

            showActionResult(result);
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid tick count."
            );
        }
    }

    private void showActionResult(
            IZombieActionResult result
    ) {
        if (result == null) {
            System.out.println(
                    "No action result was returned."
            );
            return;
        }

        if (result.isSuccessful()) {
            System.out.println(
                    result.getMessage()
            );
        } else {
            System.out.println(
                    "Action failed: " + result.getStatus()
            );

            if (!result.getMessage().trim().isEmpty()) {
                System.out.println(
                        result.getMessage()
                );
            }
        }

        if (result.hasPlacementInformation()) {
            showPlacementInformation(result);
        }

        System.out.println(
                "Remaining sun: "
                        + result.getRemainingSun()
        );

        showActionStatus(result);
    }

    private void showPlacementInformation(IZombieActionResult result) {
        ZombieDefinition definition = result.getZombieDefinition();
        String zombieName = getZombieDisplayName(definition);
        System.out.println(
                "Zombie: " + zombieName
        );
        System.out.println(
                "Position: " + formatPosition(result.getPosition())
        );
        System.out.println(
                "Sun spent: " + result.getSunSpent()
        );
    }

    private void showActionStatus(IZombieActionResult result) {
        if (result.getStatus()
                == IZombieActionStatus.STAGE_WON) {
            System.out.println(
                    "The next I, Zombie stage is ready."
            );
        }

        if (result.getStatus()
                == IZombieActionStatus.GAME_WON) {
            System.out.println(
                    "All five brains were eaten. You won I, Zombie!"
            );
        }

        if (result.getStatus()
                == IZombieActionStatus.GAME_LOST) {
            System.out.println(
                    "No zombies remain and you cannot afford another zombie."
            );
        }
    }

    private void showState(IZombieStateResult state) {
        if (state == null) {
            System.out.println(
                    "No I, Zombie state was returned."
            );
            return;
        }

        System.out.println("I, Zombie state:");
        System.out.println(
                "Stage: "
                        + state.getStageNumber()
                        + "/"
                        + state.getStageCount()
        );

        System.out.println(
                "Sun: " + state.getSunAmount()
        );

        System.out.println(
                "Red line column: "
                        + state.getRedLineColumn()
        );

        showAvailableZombies(state);
        showBrains(state);

        System.out.println(
                "Placed zombies in this stage: "
                        + state.getPlacedZombieCount()
        );

        System.out.println(
                "Alive player zombies: "
                        + (
                        state.hasAlivePlayerZombies()
                                ? "yes"
                                : "no"
                )
        );

        System.out.println(
                "State: " + getGameStateText(state)
        );
    }

    private void showAvailableZombies(
            IZombieStateResult state
    ) {
        System.out.println("Available zombies:");

        if (state.getAvailableZombies().isEmpty()) {
            System.out.println(
                    "- No zombies are currently available."
            );
            return;
        }

        for (ZombieDefinition definition
                : state.getAvailableZombies()) {
            int cost = state.getZombieCost(definition);

            String affordability =
                    state.canAfford(definition)
                            ? "affordable"
                            : "not affordable";

            System.out.println(
                    "- "
                            + getZombieDisplayName(definition)
                            + " | alias: "
                            + definition.getAlias()
                            + " | cost: "
                            + cost
                            + " | "
                            + affordability
            );
        }
    }

    private void showBrains(
            IZombieStateResult state
    ) {
        System.out.println("Brains:");

        for (Map.Entry<Integer, Boolean> entry
                : state.getBrainEatenByRow().entrySet()) {
            int row = entry.getKey();
            boolean eaten = entry.getValue();

            System.out.println(
                    "- Row "
                            + row
                            + ": "
                            + (
                            eaten
                                    ? "eaten"
                                    : "not eaten"
                    )
            );
        }

        System.out.println(
                "Remaining brains: "
                        + state.getRemainingBrainCount()
        );
    }

    private String getGameStateText(
            IZombieStateResult state
    ) {
        if (!state.isStarted()) {
            return "not started";
        }

        if (state.isWon()) {
            return "won";
        }

        if (state.isLost()) {
            return "lost";
        }

        if (state.isCompleted()) {
            return "completed";
        }

        return "running";
    }

    private String getZombieDisplayName(
            ZombieDefinition definition
    ) {
        if (definition == null) {
            return "unknown zombie";
        }

        if (definition.getDisplayName() != null
                && !definition.getDisplayName().trim().isEmpty()) {
            return definition.getDisplayName();
        }

        if (definition.getAlias() != null
                && !definition.getAlias().trim().isEmpty()) {
            return definition.getAlias();
        }

        return "unnamed zombie";
    }

    private String formatPosition(Position position) {
        if (position == null) {
            return "(unknown)";
        }

        return "("
                + position.getX()
                + ", "
                + position.getY()
                + ")";
    }

    private void showCommandHelp() {
        System.out.println("Available commands:");

        System.out.println(
                "- i-zombie start"
        );

        System.out.println(
                "- i-zombie place -z <zombie> -l (x, y)"
        );

        System.out.println(
                "- i-zombie show"
        );

        System.out.println(
                "- i-zombie advance -t <ticks>"
        );

        System.out.println(
                "- Back to minigame menu"
        );
    }
}
