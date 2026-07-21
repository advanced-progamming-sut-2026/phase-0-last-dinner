package view;

import java.util.List;
import java.util.regex.Matcher;

public class PlantPickView implements CommandHandler {
    private PlantPickViewObserver observer;

    public void setObserver(PlantPickViewObserver observer) {
        this.observer = observer;
    }

    @Override
    public void handleCommand(String input) {
        if (this.observer == null) {
            System.out.println("Plant pick controller is not connected.");
            return;
        }

        for (PlantPickCommand command : PlantPickCommand.values()) {
            Matcher matcher = command.getMatcher(input);

            if (matcher == null) {
                continue;
            }

            switch (command) {
                case SHOW_ALL_PLANTS:
                    this.printPlants("All plants", this.observer.onShowAllPlantsRequested());
                    break;
                case SHOW_AVAILABLE_PLANTS:
                    this.printPlants("Available plants", this.observer.onShowAvailablePlantsRequested());
                    break;
                case ADD_PLANT:
                    System.out.println(this.observer.onAddPlantRequested(
                            this.cleanName(matcher.group("type"))
                    ));
                    break;
                case REMOVE_PLANT:
                    System.out.println(this.observer.onRemovePlantRequested(
                            this.cleanName(matcher.group("type"))
                    ));
                    break;
                case BOOST_PLANT:
                    System.out.println(this.observer.onBoostPlantRequested(
                            this.cleanName(matcher.group("type"))
                    ));
                    break;
                case START_GAME:
                    System.out.println(this.observer.onStartGameRequested());
                    break;
                default:
                    System.out.println("Invalid plant pick command.");
                    break;
            }

            return;
        }

        System.out.println("Invalid plant pick command.");
    }

    private void printPlants(String title, List<String> plantNames) {
        System.out.println(title + ":");

        if (plantNames == null || plantNames.isEmpty()) {
            System.out.println("none");
            return;
        }

        for (String plantName : plantNames) {
            System.out.println(plantName);
        }
    }

    private String cleanName(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = value.trim();

        if (cleanValue.length() >= 2) {
            boolean doubleQuoted = cleanValue.startsWith("\"") && cleanValue.endsWith("\"");
            boolean singleQuoted = cleanValue.startsWith("'") && cleanValue.endsWith("'");

            if (doubleQuoted || singleQuoted) {
                cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
            }
        }

        return cleanValue.trim();
    }
}
