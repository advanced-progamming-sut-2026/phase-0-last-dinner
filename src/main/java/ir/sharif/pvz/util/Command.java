package ir.sharif.pvz.util;

import java.util.Collections;
import java.util.List;

public class Command {
    private final String name;
    private final List<String> arguments;

    public Command(String name, List<String> arguments) {
        this.name = name;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    public String getName() {
        return name;
    }

    public List<String> getArguments() {
        return arguments;
    }
}
