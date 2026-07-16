package model.plant;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
    @Getter
    private final String sourceDescription;

    public CsvPlantDefinitionRepository(Path sourceFile) throws IOException {
        super(loadDefinitions(sourceFile));
        this.sourceFile = sourceFile;
        this.sourceDescription = sourceFile.toString();
    }

    public CsvPlantDefinitionRepository(Reader sourceReader) throws IOException {
        this(sourceReader, "CSV reader");
    }

    private CsvPlantDefinitionRepository(Reader sourceReader, String sourceDescription) throws IOException {
        super(loadDefinitions(sourceReader));
        this.sourceFile = null;
        this.sourceDescription = sourceDescription;
    }

    public static CsvPlantDefinitionRepository fromClasspath(String resourceName) throws IOException {
        String resourcePath = normalizeResourcePath(resourceName);
        InputStream input = CsvPlantDefinitionRepository.class.getClassLoader().getResourceAsStream(resourcePath);

        if (input == null) {
            throw new IOException("Plant definitions resource not found on classpath: " + resourcePath);
        }

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return new CsvPlantDefinitionRepository(reader, "classpath:" + resourcePath);
        }
    }

    public static List<PlantDefinition> loadDefinitions(Path sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("Plant definitions path must not be null");
        }

        try (Reader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            return loadDefinitions(reader);
        }
    }

    public static List<PlantDefinition> loadDefinitions(Reader sourceReader) throws IOException {
        if (sourceReader == null) {
            throw new IllegalArgumentException("Plant definitions reader must not be null");
        }

        BufferedReader reader = sourceReader instanceof BufferedReader
                ? (BufferedReader) sourceReader
                : new BufferedReader(sourceReader);
        List<PlantDefinition> definitions = new ArrayList<>();
        String headerLine = reader.readLine();

        if (headerLine == null) {
            throw new IllegalArgumentException("Plant definitions CSV is empty");
        }

        List<String> headers = parseCsvLine(stripBom(headerLine));
        Map<String, Integer> headerIndexes = indexHeaders(headers);
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = parseCsvLine(line);
            definitions.add(createDefinition(values, headerIndexes, lineNumber));
        }

        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Plant definitions CSV contains no plant rows");
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

        switch (normalized) {
            case "sunproducer" -> categories.add(PlantCategory.SUN_PRODUCER);
            case "shooter" -> categories.add(PlantCategory.SHOOTER);
            case "homing" -> categories.add(PlantCategory.HOMING);
            case "strikethrough" -> categories.add(PlantCategory.STRIKE_THROUGH);
            case "lobber" -> categories.add(PlantCategory.LOBBER);
            case "explosive" -> categories.add(PlantCategory.EXPLOSIVE);
            case "melee" -> categories.add(PlantCategory.MELEE_ATTACKER);
            case "wallnut" -> categories.add(PlantCategory.DEFENDER);
            case "modifier" -> categories.add(PlantCategory.MODIFIER);
            default ->
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

        return switch (normalized) {
            case "day" -> PlantTag.DAY;
            case "night" -> PlantTag.NIGHT;
            case "shroom" -> PlantTag.SHROOM;
            case "wrampup", "warmup" -> PlantTag.WARM_UP;
            case "pea" -> PlantTag.PEA;
            case "ice" -> PlantTag.ICE;
            case "fire" -> PlantTag.FIRE;
            case "stack" -> PlantTag.STACK;
            case "charge" -> PlantTag.CHARGE;
            case "magic" -> PlantTag.MAGIC;
            case "poison" -> PlantTag.POISON;
            case "water" -> PlantTag.WATER;
            case "aoe" -> PlantTag.AOE;
            case "trap" -> PlantTag.TRAP;
            case "movezombies" -> PlantTag.MOVE_ZOMBIES;
            case "sun" -> PlantTag.SUN;
            case "explosive" -> PlantTag.EXPLOSIVE;
            default ->
                    throw new IllegalArgumentException("Unknown plant tag '" + value + "' at CSV line " + lineNumber);
        };

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

    private static String normalizeResourcePath(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Plant definitions resource name must not be blank");
        }

        String path = resourceName.trim().replace('\\', '/');

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }
}
