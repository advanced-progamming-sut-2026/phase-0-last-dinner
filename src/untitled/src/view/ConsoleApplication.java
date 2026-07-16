package view;

import controller.ApplicationController;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class ConsoleApplication {
    private final ApplicationController controller;
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleApplication(
            ApplicationController controller,
            InputStream input,
            PrintStream output
    ) {
        if (controller == null || input == null || output == null) {
            throw new IllegalArgumentException("Controller input and output are required");
        }

        this.controller = controller;
        this.scanner = new Scanner(input, "UTF-8");
        this.output = output;
    }

    public void run() {
        try {
            while (this.controller.isApplicationRunning() && this.scanner.hasNextLine()) {
                String result = this.controller.execute(this.scanner.nextLine());

                if (result != null && !result.isEmpty()) {
                    this.output.println(result);
                }
            }
        } finally {
            this.controller.close();
        }
    }
}
