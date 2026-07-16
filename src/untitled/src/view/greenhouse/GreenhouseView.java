package view.greenhouse;

import lombok.Getter;
import lombok.Setter;
import model.Greenhouse.GreenhouseActionResult;
import model.Greenhouse.GreenhousePotState;
import model.Greenhouse.GreenhouseStateResult;
import model.mechanism.Position;
import view.CommandHandler;

import java.util.Set;
import java.util.regex.Matcher;

@Setter
@Getter
public class GreenhouseView implements CommandHandler {

    private GreenhouseViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Greenhouse controller is not connected."
            );
            return;
        }

        Matcher matcher;

        matcher = GreenhouseCommands
                .SHOW_GREENHOUSE
                .getMatcher(input);

        if (matcher != null) {
            showGreenhouse();
            return;
        }

        matcher = GreenhouseCommands
                .PLANT_POT
                .getMatcher(input);

        if (matcher != null) {
            handlePlant(matcher);
            return;
        }

        matcher = GreenhouseCommands
                .COLLECT
                .getMatcher(input);

        if (matcher != null) {
            handleCollect(matcher);
            return;
        }

        matcher = GreenhouseCommands
                .GROW
                .getMatcher(input);

        if (matcher != null) {
            handleGrow(matcher);
            return;
        }

        System.out.println(
                "Invalid greenhouse command."
        );
    }

    public void showGreenhouse() {
        GreenhouseStateResult state =
                observer.onShowGreenhouseRequested();

        if (state == null) {
            System.out.println(
                    "Greenhouse is not available."
            );
            return;
        }

        printGreenhouse(state);
    }

    private void handlePlant(Matcher matcher) {
        Position position =
                parsePosition(matcher);

        GreenhouseActionResult result =
                observer.onPlantPotRequested(
                        position
                );

        printActionResult(result);
    }

    private void handleCollect(Matcher matcher) {
        Position position =
                parsePosition(matcher);

        GreenhouseActionResult result =
                observer.onCollectRequested(
                        position
                );

        printActionResult(result);
    }

    private void handleGrow(Matcher matcher) {
        Position position =
                parsePosition(matcher);

        GreenhouseActionResult result =
                observer.onGrowRequested(
                        position
                );

        printActionResult(result);
    }

    private Position parsePosition(
            Matcher matcher
    ) {
        try {
            int x = Integer.parseInt(
                    matcher.group("x")
            );

            int y = Integer.parseInt(
                    matcher.group("y")
            );

            return new Position(x, y);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void printActionResult(
            GreenhouseActionResult result
    ) {
        if (result == null) {
            System.out.println(
                    "Greenhouse action failed."
            );
            return;
        }

        System.out.println(result.getMessage());

        if (result.isSuccessful()) {
            System.out.println(
                    "Coins: "
                            + result.getRemainingCoins()
                            + " | Diamonds: "
                            + result.getRemainingDiamonds()
            );
        }
    }

    private void printGreenhouse(
            GreenhouseStateResult state
    ) {
        System.out.println("Greenhouse");
        System.out.println(
                "Coins: "
                        + state.getCoins()
                        + " | Diamonds: "
                        + state.getDiamonds()
        );

        System.out.println(
                "--------------------------------------------------"
        );

        for (int y = 1; y <= 4; y++) {
            StringBuilder row =
                    new StringBuilder();

            row.append("Row ")
                    .append(y)
                    .append(": ");

            for (int x = 1; x <= 5; x++) {
                GreenhousePotState pot =
                        state.getPot(x, y);

                row.append(
                        formatPot(pot, x, y)
                );

                if (x < 5) {
                    row.append(" | ");
                }
            }

            System.out.println(row);
        }

        System.out.println(
                "--------------------------------------------------"
        );

        printStoredBoosts(
                state.getStoredBoostPlantNames()
        );
    }

    private String formatPot(
            GreenhousePotState pot,
            int x,
            int y
    ) {
        String coordinate =
                "(" + x + ", " + y + ")";

        if (pot == null) {
            return coordinate + " UNKNOWN";
        }

        if (!pot.isUnlocked()) {
            return coordinate + " LOCKED";
        }

        if (pot.isEmpty()) {
            return coordinate + " EMPTY";
        }

        if (pot.isReady()) {
            return coordinate
                    + " "
                    + pot.getPlantName()
                    + " READY";
        }

        return coordinate
                + " "
                + pot.getPlantName()
                + " "
                + pot.getRemainingGrowthHours()
                + "h remaining";
    }

    private void printStoredBoosts(
            Set<String> storedBoosts
    ) {
        if (storedBoosts == null
                || storedBoosts.isEmpty()) {

            System.out.println(
                    "Stored boosts: none"
            );
            return;
        }

        System.out.println(
                "Stored boosts: "
                        + String.join(
                        ", ",
                        storedBoosts
                )
        );
    }
}