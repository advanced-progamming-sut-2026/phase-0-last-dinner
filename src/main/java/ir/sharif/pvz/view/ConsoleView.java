package ir.sharif.pvz.view;

import java.util.Scanner;

public class ConsoleView {
    private final Scanner scanner;

    public ConsoleView() {
        scanner = new Scanner(System.in);
    }

    public void showWelcome() {
        System.out.println("Plants vs. Zombies 2");
        System.out.println("Type 'help' to see the starter commands.");
    }

    public String readCommand() {
        System.out.print("> ");
        return scanner.nextLine();
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        System.out.println("Error: " + message);
    }
}
