package ir.sharif.pvz.controller;

import ir.sharif.pvz.model.ApplicationState;
import ir.sharif.pvz.model.Menu;
import ir.sharif.pvz.util.Command;
import ir.sharif.pvz.util.CommandParser;
import ir.sharif.pvz.view.ConsoleView;

import java.util.List;
import java.util.Locale;

public class ApplicationController {
    private final ApplicationState state;
    private final ConsoleView view;
    private final CommandParser commandParser;

    public ApplicationController(ApplicationState state, ConsoleView view,
                                 CommandParser commandParser) {
        this.state = state;
        this.view = view;
        this.commandParser = commandParser;
    }

    public void run() {
        view.showWelcome();

        while (state.isRunning()) {
            Command command = commandParser.parse(view.readCommand());
            handle(command);
        }
    }

    private void handle(Command command) {
        switch (command.getName()) {
            case "":
                return;
            case "help":
                showHelp();
                return;
            case "exit":
                state.stop();
                view.showMessage("Goodbye.");
                return;
            case "menu":
                handleMenuCommand(command.getArguments());
                return;
            default:
                view.showError("Invalid command.");
        }
    }

    private void handleMenuCommand(List<String> arguments) {
        if (arguments.size() == 2
                && "show".equalsIgnoreCase(arguments.get(0))
                && "current".equalsIgnoreCase(arguments.get(1))) {
            view.showMessage("Current menu: "
                    + state.getCurrentMenu().name().toLowerCase(Locale.ROOT));
            return;
        }

        if (arguments.size() == 2
                && "enter".equalsIgnoreCase(arguments.get(0))) {
            enterMenu(arguments.get(1));
            return;
        }

        view.showError("Invalid menu command.");
    }

    private void enterMenu(String menuName) {
        try {
            Menu menu = Menu.valueOf(menuName.toUpperCase(Locale.ROOT));
            state.setCurrentMenu(menu);
            view.showMessage("Entered " + menuName.toLowerCase(Locale.ROOT) + " menu.");
        } catch (IllegalArgumentException exception) {
            view.showError("Unknown menu: " + menuName);
        }
    }

    private void showHelp() {
        view.showMessage("Available commands:");
        view.showMessage("  menu show current");
        view.showMessage("  menu enter <register|login|main|game>");
        view.showMessage("  help");
        view.showMessage("  exit");
    }
}
