package model.plant;

import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CsvPlantDefinitionRepository extends PlantDefinitionRepository {
    @Getter
    private final Path sourceFile;

    public CsvPlantDefinitionRepository(Path sourceFile) throws IOException {
        super(loadDefinitions(sourceFile));
        this.sourceFile = sourceFile;
    }

    public static List<PlantDefinition> loadDefinitions(Path sourceFile) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile, StandardCharsets.UTF_8);
        List<PlantDefinition> definitions = new ArrayList<>();

        if (lines.isEmpty()) {
            return definitions;
        }

        List<String> headers = parseCsvLine(stripBom(lines.get(0)));
        Map<String, Integer> headerIndexes = indexHeaders(headers);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            List<String> values = parseCsvLine(line);
            definitions.add(createDefinition(values, headerIndexes, i + 1));
        }

        return definitions;
    }

    private static PlantDefinition createDefinition(
            List<String> values,
            Map<String, Integer> headerIndexes,
            int lineNumber
    ) {
        String name = getRequired(values, headerIndexes, "Name", lineNumber);
        Set<PlantCategory> categories = parseCategories(
                name,
                getRequired(values, headerIndexes, "Category", lineNumber),
                lineNumber
        );
        Set<PlantTag> tags = parseTags(get(values, headerIndexes, "Tags"), lineNumber);
        List<String> levelUpEffects = new ArrayList<>();
        addIfPresent(levelUpEffects, get(values, headerIndexes, "Lvl 2"));
        addIfPresent(levelUpEffects, get(values, headerIndexes, "Lvl 3"));
        addIfPresent(levelUpEffects, get(values, headerIndexes, "Lvl 4"));

        return new PlantDefinition(
                name,
                categories,
                tags,
                parseInt(getRequired(values, headerIndexes, "Cost", lineNumber), "Cost", lineNumber),
                parseInt(getRequired(values, headerIndexes, "Base HP", lineNumber), "Base HP", lineNumber),
                getRequired(values, headerIndexes, "Damage", lineNumber),
                get(values, headerIndexes, "Base Ability"),
                get(values, headerIndexes, "Plant Food Effect"),
                levelUpEffects,
                parseDouble(getRequired(values, headerIndexes, "Action Interval (s)", lineNumber), "Action Interval (s)", lineNumber),
                parseDouble(getRequired(values, headerIndexes, "Recharge (s)", lineNumber), "Recharge (s)", lineNumber)
        );
    }

    private static Set<PlantCategory> parseCategories(String plantName, String value, int lineNumber) {
        Set<PlantCategory> categories = EnumSet.noneOf(PlantCategory.class);
        String normalized = normalize(value);

        if ("sunproducer".equals(normalized)) {
            categories.add(PlantCategory.SUN_PRODUCER);
        } else if ("shooter".equals(normalized)) {
            categories.add(PlantCategory.SHOOTER);
        } else if ("homing".equals(normalized)) {
            categories.add(PlantCategory.HOMING);
        } else if ("strikethrough".equals(normalized)) {
            categories.add(PlantCategory.STRIKE_THROUGH);
        } else if ("lobber".equals(normalized)) {
            categories.add(PlantCategory.LOBBER);
        } else if ("explosive".equals(normalized)) {
            categories.add(PlantCategory.EXPLOSIVE);
        } else if ("melee".equals(normalized)) {
            categories.add(PlantCategory.MELEE_ATTACKER);
        } else if ("wallnut".equals(normalized)) {
            categories.add(PlantCategory.DEFENDER);
        } else if ("modifier".equals(normalized)) {
            categories.add(PlantCategory.MODIFIER);
        } else {
            throw new IllegalArgumentException("Unknown plant category '" + value + "' at CSV line " + lineNumber);
        }

        if (plantName != null && plantName.toLowerCase(Locale.ROOT).contains("mint")) {
            categories.add(PlantCategory.MINT);
        }

        return categories;
    }

    private static Set<PlantTag> parseTags(String value, int lineNumber) {
        Set<PlantTag> tags = EnumSet.noneOf(PlantTag.class);

        if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
            return tags;
        }

        String[] parts = value.split(",");

        for (String part : parts) {
            String tag = part.trim();

            if (tag.isEmpty() || "-".equals(tag)) {
                continue;
            }

            tags.add(parseTag(tag, lineNumber));
        }

        return tags;
    }

    private static PlantTag parseTag(String value, int lineNumber) {
        String normalized = normalize(value);

        if ("day".equals(normalized)) {
            return PlantTag.DAY;
        } else if ("night".equals(normalized)) {
            return PlantTag.NIGHT;
        } else if ("shroom".equals(normalized)) {
            return PlantTag.SHROOM;
        } else if ("wrampup".equals(normalized) || "warmup".equals(normalized)) {
            return PlantTag.WARM_UP;
        } else if ("pea".equals(normalized)) {
            return PlantTag.PEA;
        } else if ("ice".equals(normalized)) {
            return PlantTag.ICE;
        } else if ("fire".equals(normalized)) {
            return PlantTag.FIRE;
        } else if ("stack".equals(normalized)) {
            return PlantTag.STACK;
        } else if ("charge".equals(normalized)) {
            return PlantTag.CHARGE;
        } else if ("magic".equals(normalized)) {
            return PlantTag.MAGIC;
        } else if ("poison".equals(normalized)) {
            return PlantTag.POISON;
        } else if ("water".equals(normalized)) {
            return PlantTag.WATER;
        } else if ("aoe".equals(normalized)) {
            return PlantTag.AOE;
        } else if ("trap".equals(normalized)) {
            return PlantTag.TRAP;
        } else if ("movezombies".equals(normalized)) {
            return PlantTag.MOVE_ZOMBIES;
        } else if ("sun".equals(normalized)) {
            return PlantTag.SUN;
        } else if ("explosive".equals(normalized)) {
            return PlantTag.EXPLOSIVE;
        }

        throw new IllegalArgumentException("Unknown plant tag '" + value + "' at CSV line " + lineNumber);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);

            if (currentChar == '"') {
                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (currentChar == ',' && !insideQuotes) {
                values.add(currentValue.toString());
                currentValue.setLength(0);
            } else {
                currentValue.append(currentChar);
            }
        }

        values.add(currentValue.toString());
        return values;
    }

    private static Map<String, Integer> indexHeaders(List<String> headers) {
        Map<String, Integer> headerIndexes = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            headerIndexes.put(headers.get(i).trim(), i);
        }

        requireHeader(headerIndexes, "Name");
        requireHeader(headerIndexes, "Category");
        requireHeader(headerIndexes, "Tags");
        requireHeader(headerIndexes, "Cost");
        requireHeader(headerIndexes, "Base HP");
        requireHeader(headerIndexes, "Damage");
        requireHeader(headerIndexes, "Base Ability");
        requireHeader(headerIndexes, "Plant Food Effect");
        requireHeader(headerIndexes, "Action Interval (s)");
        requireHeader(headerIndexes, "Recharge (s)");

        return headerIndexes;
    }

    private static void requireHeader(Map<String, Integer> headerIndexes, String header) {
        if (!headerIndexes.containsKey(header)) {
            throw new IllegalArgumentException("Missing required CSV header: " + header);
        }
    }

    private static String get(List<String> values, Map<String, Integer> headerIndexes, String header) {
        Integer index = headerIndexes.get(header);

        if (index == null || index >= values.size()) {
            return "";
        }

        return values.get(index).trim();
    }

    private static String getRequired(
            List<String> values,
            Map<String, Integer> headerIndexes,
            String header,
            int lineNumber
    ) {
        String value = get(values, headerIndexes, header);

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing value for '" + header + "' at CSV line " + lineNumber);
        }

        return value;
    }

    private static int parseInt(String value, String fieldName, int lineNumber) {
        if ("-".equals(value.trim())) {
            return 0;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for '" + fieldName + "' at CSV line " + lineNumber + ": " + value, e);
        }
    }

    private static double parseDouble(String value, String fieldName, int lineNumber) {
        if ("-".equals(value.trim())) {
            return 0;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for '" + fieldName + "' at CSV line " + lineNumber + ": " + value, e);
        }
    }

    private static void addIfPresent(List<String> values, String value) {
        if (value != null && !value.trim().isEmpty() && !"-".equals(value.trim())) {
            values.add(value.trim());
        }
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }

        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }
}
