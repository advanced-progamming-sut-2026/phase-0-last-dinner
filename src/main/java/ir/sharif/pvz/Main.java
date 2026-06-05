package ir.sharif.pvz;

import ir.sharif.pvz.controller.ApplicationController;
import ir.sharif.pvz.model.ApplicationState;
import ir.sharif.pvz.util.CommandParser;
import ir.sharif.pvz.view.ConsoleView;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ApplicationState state = new ApplicationState();
        ConsoleView view = new ConsoleView();
        CommandParser commandParser = new CommandParser();
        ApplicationController controller =
                new ApplicationController(state, view, commandParser);

        controller.run();
    }
}
