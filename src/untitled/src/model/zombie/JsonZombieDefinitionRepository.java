package model.zombie;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JsonZombieDefinitionRepository implements ZombieDefinitionRepository {
    private Path sourceFile;
    private List<ZombieDefinition> definitions;

    public JsonZombieDefinitionRepository() {
        this.definitions = new ArrayList<>();
    }

    public JsonZombieDefinitionRepository(Path sourceFile) {
        this();
        this.sourceFile = sourceFile;
    }

    public void load() {
        if (this.sourceFile == null) {
            this.definitions = new ArrayList<>();
            return;
        }

        try {
            this.definitions = loadDefinitions(this.sourceFile);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load zombie definitions from " + this.sourceFile, e);
        }
    }

    public static List<ZombieDefinition> loadDefinitions(Path sourceFile) throws IOException {
        String rawText = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
        JsonElement root = parseJsonPayload(extractJsonPayload(rawText));
        List<JsonObject> objects = collectObjects(root);
        Map<String, ZombieArmorDefinition> armorDefinitions = collectArmorDefinitions(objects);
        List<ZombieDefinition> zombieDefinitions = new ArrayList<>();
        Set<String> loadedAliases = new HashSet<>();

        for (JsonObject object : objects) {
            if (!isZombieObject(object)) {
                continue;
            }

            ZombieDefinition definition = createZombieDefinition(object, armorDefinitions);

            if (definition != null) {
                zombieDefinitions.add(definition);
                loadedAliases.add(definition.getAlias().toLowerCase(Locale.ROOT));
            }
        }

        for (String alias : parseDocumentedAliases(rawText)) {
            String key = alias.toLowerCase(Locale.ROOT);

            if (!loadedAliases.contains(key)) {
                zombieDefinitions.add(createInferredDefinition(alias));
                loadedAliases.add(key);
            }
        }

        return zombieDefinitions;
    }

    @Override
    public ZombieDefinition findByAlias(String alias) {
        if (alias == null || this.definitions == null) {
            return null;
        }

        for (ZombieDefinition definition : this.definitions) {
            if (definition != null && alias.equalsIgnoreCase(definition.getAlias())) {
                return definition;
            }
        }

        return null;
    }

    @Override
    public List<ZombieDefinition> findByChapter(ZombieChapter chapter) {
        List<ZombieDefinition> result = new ArrayList<>();

        if (chapter == null || this.definitions == null) {
            return result;
        }

        for (ZombieDefinition definition : this.definitions) {
            if (definition != null && definition.getChapter() == chapter) {
                result.add(definition);
            }
        }

        return result;
    }

    @Override
    public List<ZombieDefinition> findAll() {
        return this.definitions == null ? new ArrayList<ZombieDefinition>() : this.definitions;
    }

    private static ZombieDefinition createZombieDefinition(
            JsonObject object,
            Map<String, ZombieArmorDefinition> armorDefinitions
    ) {
        String alias = firstAlias(object);

        if (alias.isEmpty()) {
            return null;
        }

        JsonObject data = getObject(object, "objdata");
        String objClass = getString(object, "objclass", "");
        List<ZombieArmorDefinition> armors = parseZombieArmors(data, armorDefinitions);
        ZombieType type = inferType(alias, objClass, armors);

        return new ZombieDefinition(
                alias,
                humanizeAlias(alias),
                getString(data, "Description", humanizeAlias(alias)),
                type,
                inferChapter(alias),
                getInt(data, "Hitpoints", defaultHitpoints(type)),
                getInt(data, "EatDPS", type == ZombieType.GARGANTUAR ? 0 : 100),
                getDouble(data, "Speed", defaultSpeed(type)),
                getInt(data, "WavePointCost", defaultWavePointCost(type)),
                getInt(data, "Weight", 1000),
                getBoolean(data, "CanSpawnPlantFood", false),
                armors,
                new ArrayList<ConditionResistance>()
        );
    }

    private static ZombieDefinition createInferredDefinition(String alias) {
        ZombieType type = inferType(alias, "", new ArrayList<ZombieArmorDefinition>());

        return new ZombieDefinition(
                alias,
                humanizeAlias(alias),
                humanizeAlias(alias),
                type,
                inferChapter(alias),
                defaultHitpoints(type),
                type == ZombieType.GARGANTUAR ? 0 : 100,
                defaultSpeed(type),
                defaultWavePointCost(type),
                1000,
                false,
                new ArrayList<ZombieArmorDefinition>(),
                new ArrayList<ConditionResistance>()
        );
    }

    private static Map<String, ZombieArmorDefinition> collectArmorDefinitions(List<JsonObject> objects) {
        Map<String, ZombieArmorDefinition> armorDefinitions = new HashMap<>();

        for (JsonObject object : objects) {
            if (!isArmorObject(object)) {
                continue;
            }

            ZombieArmorDefinition definition = createArmorDefinition(object);

            if (definition != null) {
                armorDefinitions.put(definition.getAlias().toLowerCase(Locale.ROOT), definition);
            }
        }

        return armorDefinitions;
    }

    private static ZombieArmorDefinition createArmorDefinition(JsonObject object) {
        String alias = firstAlias(object);

        if (alias.isEmpty()) {
            return null;
        }

        JsonObject data = getObject(object, "objdata");
        ArmorType type = inferArmorType(getString(data, "ArmorType", alias));

        return new ZombieArmorDefinition(
                alias,
                type,
                getInt(data, "Hitpoints", defaultArmorHealth(type)),
                armorFlagsFor(type)
        );
    }

    private static List<ZombieArmorDefinition> parseZombieArmors(
            JsonObject data,
            Map<String, ZombieArmorDefinition> armorDefinitions
    ) {
        List<ZombieArmorDefinition> armors = new ArrayList<>();
        JsonArray armorRefs = getArray(data, "ZombieArmorProps");

        if (armorRefs == null) {
            return armors;
        }

        for (JsonElement element : armorRefs) {
            String armorAlias = extractRtidAlias(element.getAsString());
            ZombieArmorDefinition definition = armorDefinitions.get(armorAlias.toLowerCase(Locale.ROOT));

            if (definition == null) {
                ArmorType type = inferArmorType(armorAlias);
                definition = new ZombieArmorDefinition(
                        armorAlias,
                        type,
                        defaultArmorHealth(type),
                        armorFlagsFor(type)
                );
            }

            armors.add(definition);
        }

        return armors;
    }

    private static JsonElement parseJsonPayload(String payload) {
        String trimmed = trimTrailingComma(payload.trim());

        try {
            return JsonParser.parseString(trimmed);
        } catch (RuntimeException ignored) {
            return JsonParser.parseString("[" + trimmed + "]");
        }
    }

    private static String extractJsonPayload(String rawText) {
        String text = stripBom(rawText);
        int fenceStart = text.indexOf("```json");

        if (fenceStart < 0) {
            return text;
        }

        int payloadStart = text.indexOf('\n', fenceStart);

        if (payloadStart < 0) {
            return text.substring(fenceStart + "```json".length());
        }

        int fenceEnd = text.indexOf("```", payloadStart + 1);

        if (fenceEnd < 0) {
            return text.substring(payloadStart + 1);
        }

        return text.substring(payloadStart + 1, fenceEnd);
    }

    private static List<JsonObject> collectObjects(JsonElement element) {
        List<JsonObject> objects = new ArrayList<>();
        collectObjects(element, objects);
        return objects;
    }

    private static List<String> parseDocumentedAliases(String rawText) {
        List<String> aliases = new ArrayList<>();

        if (rawText == null) {
            return aliases;
        }

        String[] lines = rawText.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();

            if (!trimmed.startsWith("| `Zombie")) {
                continue;
            }

            int aliasStart = trimmed.indexOf('`');
            int aliasEnd = trimmed.indexOf('`', aliasStart + 1);

            if (aliasStart < 0 || aliasEnd <= aliasStart) {
                continue;
            }

            aliases.add(trimmed.substring(aliasStart + 1, aliasEnd));
        }

        return aliases;
    }

    private static void collectObjects(JsonElement element, List<JsonObject> objects) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectObjects(child, objects);
            }
            return;
        }

        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();

        if (object.has("aliases") && object.has("objclass")) {
            objects.add(object);
            return;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectObjects(entry.getValue(), objects);
        }
    }

    private static boolean isZombieObject(JsonObject object) {
        String objClass = getString(object, "objclass", "").toLowerCase(Locale.ROOT);
        String alias = firstAlias(object).toLowerCase(Locale.ROOT);
        return alias.startsWith("zombie") && objClass.contains("zombie") && !alias.endsWith("props");
    }

    private static boolean isArmorObject(JsonObject object) {
        String objClass = getString(object, "objclass", "").toLowerCase(Locale.ROOT);
        return objClass.contains("armorpropertysheet") || objClass.contains("newspaperarmorpropertysheet");
    }

    private static ZombieType inferType(String alias, String objClass, List<ZombieArmorDefinition> armors) {
        String key = normalize(alias + " " + objClass);

        if (key.contains("zomboss") || key.contains("boss")) {
            return ZombieType.BOSS;
        }

        if (key.contains("gargantuar")) {
            return ZombieType.GARGANTUAR;
        }

        if (key.contains("imp")) {
            return ZombieType.IMP;
        }

        if (key.contains("dodo") || key.contains("weasel")) {
            return ZombieType.ANIMAL;
        }

        if (armors != null && !armors.isEmpty()) {
            return ZombieType.ARMORED;
        }

        if (key.contains("wizard") || key.contains("juggler") || key.contains("ra")
                || key.contains("tomb") || key.contains("fisher") || key.contains("octopus")
                || key.contains("snorkel") || key.contains("surfer") || key.contains("king")
                || key.contains("hunter") || key.contains("troglobite") || key.contains("explorer")) {
            return ZombieType.SPECIAL;
        }

        return ZombieType.BASIC;
    }

    private static ZombieChapter inferChapter(String alias) {
        String key = normalize(alias);

        if (key.contains("egypt") || key.contains("mummy") || key.contains("pharaoh")
                || key.contains("ra") || key.contains("tomb") || key.contains("camel")
                || key.contains("explorer")) {
            return ZombieChapter.ANCIENT_EGYPT;
        }

        if (key.contains("iceage") || key.contains("frost") || key.contains("weasel")
                || key.contains("troglobite") || key.contains("dodo") || key.contains("hunter")) {
            return ZombieChapter.FROSTBITE_CAVES;
        }

        if (key.contains("beach") || key.contains("snorkel") || key.contains("surfer")
                || key.contains("fisher") || key.contains("octopus") || key.contains("swimmer")) {
            return ZombieChapter.BIG_WAVE_BEACH;
        }

        if (key.contains("dark") || key.contains("wizard") || key.contains("juggler")
                || key.contains("king")) {
            return ZombieChapter.MEDIEVAL;
        }

        return ZombieChapter.ALL_CHAPTERS;
    }

    private static ArmorType inferArmorType(String value) {
        String key = normalize(value);

        if (key.contains("bucket")) {
            return ArmorType.BUCKET;
        }

        if (key.contains("brick")) {
            return ArmorType.BRICK;
        }

        if (key.contains("shoulder")) {
            return ArmorType.SHOULDER_ARMOR;
        }

        if (key.contains("crown")) {
            return ArmorType.CROWN;
        }

        if (key.contains("ice")) {
            return ArmorType.ICE_BLOCK;
        }

        if (key.contains("sarcophagus") || key.contains("pharaoh")) {
            return ArmorType.SARCOPHAGUS;
        }

        if (key.contains("surf")) {
            return ArmorType.SURFBOARD;
        }

        if (key.contains("newspaper")) {
            return ArmorType.NEWSPAPER;
        }

        return ArmorType.CONE;
    }

    private static EnumSet<ArmorFlag> armorFlagsFor(ArmorType type) {
        EnumSet<ArmorFlag> flags = EnumSet.of(ArmorFlag.DAMAGEABLE, ArmorFlag.DROPPABLE);

        if (type == ArmorType.BUCKET || type == ArmorType.CROWN) {
            flags.add(ArmorFlag.METALLIC);
            flags.add(ArmorFlag.HELMET);
        } else if (type == ArmorType.CONE || type == ArmorType.BRICK) {
            flags.add(ArmorFlag.HELMET);
        } else if (type == ArmorType.SHOULDER_ARMOR) {
            flags.add(ArmorFlag.PASS_DAMAGE);
        }

        return flags;
    }

    private static int defaultHitpoints(ZombieType type) {
        if (type == ZombieType.BOSS) {
            return 10000;
        }

        if (type == ZombieType.GARGANTUAR) {
            return 3600;
        }

        if (type == ZombieType.IMP || type == ZombieType.ANIMAL) {
            return 75;
        }

        return 190;
    }

    private static double defaultSpeed(ZombieType type) {
        if (type == ZombieType.IMP || type == ZombieType.ANIMAL) {
            return 0.35;
        }

        if (type == ZombieType.GARGANTUAR) {
            return 0.16;
        }

        return 0.185;
    }

    private static int defaultWavePointCost(ZombieType type) {
        if (type == ZombieType.BOSS) {
            return 5000;
        }

        if (type == ZombieType.GARGANTUAR) {
            return 1000;
        }

        if (type == ZombieType.IMP || type == ZombieType.ANIMAL) {
            return 50;
        }

        return 100;
    }

    private static int defaultArmorHealth(ArmorType type) {
        switch (type) {
            case BUCKET:
                return 1100;
            case BRICK:
                return 2200;
            case SHOULDER_ARMOR:
            case CROWN:
                return 1600;
            case ICE_BLOCK:
            case SURFBOARD:
                return 1200;
            case SARCOPHAGUS:
                return 2200;
            case NEWSPAPER:
                return 150;
            case CONE:
            default:
                return 370;
        }
    }

    private static String firstAlias(JsonObject object) {
        JsonArray aliases = getArray(object, "aliases");

        if (aliases == null || aliases.size() == 0) {
            return "";
        }

        return aliases.get(0).getAsString();
    }

    private static String extractRtidAlias(String value) {
        if (value == null) {
            return "";
        }

        int start = value.indexOf('(');
        int end = value.indexOf('@');

        if (start >= 0 && end > start) {
            return value.substring(start + 1, end);
        }

        return value;
    }

    private static String humanizeAlias(String alias) {
        if (alias == null || alias.isEmpty()) {
            return "";
        }

        return alias.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return new JsonObject();
        }

        return object.getAsJsonObject(key);
    }

    private static JsonArray getArray(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return null;
        }

        return object.getAsJsonArray(key);
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return defaultValue;
        }

        return object.get(key).getAsString();
    }

    private static int getInt(JsonObject object, String key, int defaultValue) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return defaultValue;
        }

        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static double getDouble(JsonObject object, String key, double defaultValue) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return defaultValue;
        }

        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return defaultValue;
        }

        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static String trimTrailingComma(String value) {
        String trimmed = value.trim();

        while (trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        return trimmed;
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }

        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
