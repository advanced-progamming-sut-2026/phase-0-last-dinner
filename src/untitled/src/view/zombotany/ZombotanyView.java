package view.zombotany;

import lombok.Setter;
import model.mechanism.Position;
import model.minigame.beghouledminigame.PlantUpgradeOption;
import model.minigame.zombotanyminigame.ZombotanyTrait;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyActionStatus;
import model.minigame.zombotanyminigame.ZombotanyStateResult;
import model.minigame.zombotanyminigame.ZombotanyZombieState;
import model.plant.PlantDefinition;
import model.zombie.ZombieDefinition;
import view.CommandHandler;

import java.util.Map;
import java.util.regex.Matcher;

@Setter
public class ZombotanyView implements CommandHandler {

    private static final int CELL_WIDTH = 13;

    private ZombotanyViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Zombotany controller is not connected."
            );
            return;
        }
        if (handleStartAndListCommand(input)
                || handleSelectionCommand(input)
                || handleGameCommand(input)
                || handleNavigationCommand(input)) {
            return;
        }
        System.out.println("Invalid Zombotany command.");
        showCommandHelp();
    }

    private boolean handleStartAndListCommand(String input) {
        Matcher matcher = ZombotanyCommands.START.getMatcher(input);
        if (matcher != null) {
            handleStart(matcher);
            return true;
        }
        matcher = ZombotanyCommands.SHOW_ALL_PLANTS.getMatcher(input);
        if (matcher != null) {
            showPlantList("All stage plants", observer.onShowAvailablePlantsRequested());
            return true;
        }
        matcher = ZombotanyCommands.SHOW_AVAILABLE_PLANTS.getMatcher(input);
        if (matcher != null) {
            showPlantList("Available plants", observer.onShowAvailablePlantsRequested());
            return true;
        }
        matcher = ZombotanyCommands.SHOW_SELECTED_PLANTS.getMatcher(input);
        if (matcher != null) {
            showPlantList("Selected plants", observer.onShowSelectedPlantsRequested());
            return true;
        }
        return false;
    }

    private boolean handleSelectionCommand(String input) {
        Matcher matcher = ZombotanyCommands.ADD_PLANT.getMatcher(input);
        if (matcher != null) {
            showActionResult(observer.onAddPlantRequested(matcher.group("plant").trim()));
            return true;
        }
        matcher = ZombotanyCommands.REMOVE_PLANT.getMatcher(input);
        if (matcher != null) {
            showActionResult(observer.onRemovePlantRequested(matcher.group("plant").trim()));
            return true;
        }
        matcher = ZombotanyCommands.START_GAME.getMatcher(input);
        if (matcher != null) {
            showActionResult(observer.onStartGameRequested());
            return true;
        }
        return false;
    }

    private boolean handleGameCommand(String input) {
        Matcher matcher = ZombotanyCommands.PLANT.getMatcher(input);
        if (matcher != null) {
            handlePlant(matcher);
            return true;
        }
        matcher = ZombotanyCommands.COLLECT_SUN
                .getMatcher(input);
        if (matcher != null) {
            handleCollectSun(matcher);
            return true;
        }
        matcher = ZombotanyCommands.USE_PLANT_FOOD
                .getMatcher(input);
        if (matcher != null) {
            handlePlantFood(matcher);
            return true;
        }
        matcher = ZombotanyCommands.ADVANCE_TIME
                .getMatcher(input);
        if (matcher != null) {
            handleAdvance(matcher);
            return true;
        }
        matcher = ZombotanyCommands.SHOW.getMatcher(input);
        if (matcher != null) {
            showState(observer.onShowRequested());
            return true;
        }
        return false;
    }

    private boolean handleNavigationCommand(String input) {
        Matcher matcher = ZombotanyCommands.HELP.getMatcher(input);
        if (matcher != null) {
            showCommandHelp();
            return true;
        }
        matcher = ZombotanyCommands.BACK.getMatcher(input);
        if (matcher != null) {
            System.out.println(
                    "Returning to minigame menu."
            );
            return true;
        }
        return false;
    }

    private void handleStart(Matcher matcher) {
        try {
            String stageText =
                    matcher.group("stage");
            int stage = stageText == null
                    ? 1
                    : Integer.parseInt(stageText);
            showActionResult(
                    observer.onStartRequested(stage)
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid Zombotany stage number."
            );
        }
    }

    private void handlePlant(Matcher matcher) {
        try {
            String plantName =
                    matcher.group("plant").trim();
            Position position =
                    readPosition(matcher);
            showActionResult(
                    observer.onPlantRequested(
                            plantName,
                            position
                    )
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid plant position."
            );
        }
    }

    private void handleCollectSun(Matcher matcher) {
        try {
            showActionResult(
                    observer.onCollectSunRequested(
                            readPosition(matcher)
                    )
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid sun position."
            );
        }
    }

    private void handlePlantFood(Matcher matcher) {
        try {
            showActionResult(
                    observer.onUsePlantFoodRequested(
                            readPosition(matcher)
                    )
            );
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Invalid Plant Food position."
            );
        }
    }

    private void handleAdvance(Matcher matcher) {
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

    private Position readPosition(Matcher matcher) {
        return new Position(
                Integer.parseInt(matcher.group("x")),
                Integer.parseInt(matcher.group("y"))
        );
    }

    private void showActionResult(
            ZombotanyActionResult result
    ) {
        if (result == null) {
            System.out.println(
                    "No Zombotany result was returned."
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
                "Wave: "
                        + result.getCurrentWave()
                        + "/"
                        + result.getWaveCount()
        );
        System.out.println(
                "Sun: " + result.getSunAmount()
        );
        System.out.println(
                "Plant Food: "
                        + result.getPlantFoodAmount()
        );
        System.out.println(
                "Alive zombies: "
                        + result.getAliveZombieCount()
        );
        showActionStatus(result);
    }

    private void showActionStatus(ZombotanyActionResult result) {
        if (result.getStatus()
                == ZombotanyActionStatus.STAGE_WON) {
            System.out.println(
                    "The next Zombotany stage is unlocked."
            );
        }
        if (result.getStatus()
                == ZombotanyActionStatus.GAME_WON) {
            System.out.println(
                    "You completed all Zombotany stages!"
            );
        }
        if (result.getStatus()
                == ZombotanyActionStatus.GAME_LOST) {
            System.out.println(
                    "The zombies reached the house."
            );
        }
    }

    private void showState(
            ZombotanyStateResult state
    ) {
        if (state == null) {
            System.out.println(
                    "No Zombotany state was returned."
            );
            return;
        }
        System.out.println("Zombotany state:");
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
                "Wave: "
                        + state.getCurrentWave()
                        + "/"
                        + state.getWaveCount()
        );
        System.out.println(
                "Sun: " + state.getSunAmount()
        );
        System.out.println(
                "Plant Food: "
                        + state.getPlantFoodAmount()
        );
        showBoard(state);
        showAvailablePlants(state);
        showZombieTypes(state);
        showAliveZombies(state);
        showSuns(state);
        System.out.println(
                "State: " + getStateText(state)
        );
    }

    private void showBoard(
            ZombotanyStateResult state
    ) {
        System.out.println("Board:");
        System.out.print(pad("Row/Col"));
        for (int x = 1;
             x <= ZombotanyStateResult.COLUMN_COUNT;
             x++) {
            System.out.print(pad("C" + x));
        }
        System.out.println();
        for (int y = 1;
             y <= ZombotanyStateResult.ROW_COUNT;
             y++) {
            System.out.print(pad("R" + y));
            for (int x = 1;
                 x <= ZombotanyStateResult.COLUMN_COUNT;
                 x++) {
                String value =
                        state.getPlantNameAt(
                                new Position(x, y)
                        );
                System.out.print(
                        pad(
                                value.trim().isEmpty()
                                        ? "."
                                        : value
                        )
                );
            }
            System.out.println();
        }
    }

    private void showAvailablePlants(
            ZombotanyStateResult state
    ) {
        showPlantList("Available plants", state.getAvailablePlants());
    }

    private void showPlantList(String title, java.util.List<PlantDefinition> plants) {
        System.out.println(title + ":");
        if (plants == null || plants.isEmpty()) {
            System.out.println("- None");
            return;
        }
        for (PlantDefinition definition
                : plants) {
            System.out.println(
                    "- "
                            + definition.getName()
                            + " | cost: "
                            + definition.getCost()
            );
        }
    }

    private void showZombieTypes(
            ZombotanyStateResult state
    ) {
        System.out.println("Zombotany zombie types:");
        for (Map.Entry<ZombieDefinition, ZombotanyTrait>
                entry : state.getZombieTraits().entrySet()) {
            ZombieDefinition definition =
                    entry.getKey();
            String name =
                    definition.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                name = definition.getAlias();
            }
            System.out.println(
                    "- "
                            + name
                            + " | trait: "
                            + entry.getValue()
            );
        }
    }

    private void showAliveZombies(
            ZombotanyStateResult state
    ) {
        System.out.println("Alive zombies:");
        if (state.getZombies().isEmpty()) {
            System.out.println("- None");
            return;
        }
        for (ZombotanyZombieState zombie
                : state.getZombies()) {
            System.out.println(
                    "- "
                            + zombie.getZombieName()
                            + " | trait: "
                            + zombie.getTrait()
                            + " | health: "
                            + zombie.getHealth()
                            + " | position: "
                            + zombie.getPosition()
            );
        }
    }

    private void showSuns(
            ZombotanyStateResult state
    ) {
        System.out.println("Collectible suns:");
        if (state.getCollectibleSunPositions().isEmpty()) {
            System.out.println("- None");
            return;
        }
        for (Position position
                : state.getCollectibleSunPositions()) {
            System.out.println("- " + position);
        }
    }

    private String getStateText(
            ZombotanyStateResult state
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
        String safeValue =
                value == null ? "" : value;
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
                "- zombotany start -s <stage>"
        );
        System.out.println("- show all plants");
        System.out.println("- show available plants");
        System.out.println("- show selected plants");
        System.out.println("- add plant -t <type>");
        System.out.println("- remove plant -t <type>");
        System.out.println("- start game");
        System.out.println(
                "- zombotany plant -p <plant> -l (x, y)"
        );
        System.out.println(
                "- zombotany collect -l (x, y)"
        );
        System.out.println(
                "- zombotany plant-food -l (x, y)"
        );
        System.out.println(
                "- zombotany advance -t <ticks>"
        );
        System.out.println(
                "- zombotany show"
        );
        System.out.println(
                "- zombotany help"
        );
        System.out.println(
                "- Back to minigame menu"
        );
    }
}
