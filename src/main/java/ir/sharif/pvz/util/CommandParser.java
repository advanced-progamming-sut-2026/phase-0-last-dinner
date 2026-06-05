package ir.sharif.pvz.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandParser {
    public Command parse(String input) {
        String normalizedInput = input == null ? "" : input.trim();
        if (normalizedInput.isEmpty()) {
            return new Command("", new ArrayList<String>());
        }

        List<String> parts = new ArrayList<String>(
                Arrays.asList(normalizedInput.split("\\s+")));
        String name = parts.remove(0).toLowerCase(Locale.ROOT);
        return new Command(name, parts);
    }
}
