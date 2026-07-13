package model.zombie;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ZombieJsonDefinitionParser {
    private ZombieJsonDefinitionParser() {
    }

    static List<ZombieDefinition> parse(Reader sourceReader, Reader armorSourceReader) throws IOException {
        if (sourceReader == null) {
            throw new IllegalArgumentException("Zombie definitions reader must not be null");
        }

        List<JsonObject> objects = readObjectArray(sourceReader, "Zombie definitions");
        Map<String, ZombieArmorDefinition> armorDefinitions = armorSourceReader == null
                ? new HashMap<String, ZombieArmorDefinition>()
                : parseArmorDefinitions(armorSourceReader);
        List<ZombieDefinition> zombieDefinitions = new ArrayList<>();
        Set<String> loadedAliases = new HashSet<>();

        for (JsonObject object : objects) {
            if (!isZombieObject(object)) {
                throw new IllegalArgumentException("Zombie definitions array contains a non-zombie object");
            }

            ZombieDefinition definition = createZombieDefinition(object, armorDefinitions);
            String aliasKey = definition.getAlias().toLowerCase(Locale.ROOT);

            if (!loadedAliases.add(aliasKey)) {
                throw new IllegalArgumentException("Duplicate zombie alias in JSON: " + definition.getAlias());
            }

            zombieDefinitions.add(definition);
        }

        if (zombieDefinitions.isEmpty()) {
            throw new IllegalArgumentException("Zombie definitions JSON contains no zombie objects");
        }

        return zombieDefinitions;
    }

    private static Map<String, ZombieArmorDefinition> parseArmorDefinitions(Reader sourceReader) throws IOException {
        List<JsonObject> objects = readObjectArray(sourceReader, "Zombie armor definitions");
        Map<String, ZombieArmorDefinition> definitions = new HashMap<>();

        for (JsonObject object : objects) {
            if (!isArmorObject(object)) {
                continue;
            }

            ZombieArmorDefinition definition = createArmorDefinition(object);
            String aliasKey = definition.getAlias().toLowerCase(Locale.ROOT);

            if (definitions.put(aliasKey, definition) != null) {
                throw new IllegalArgumentException("Duplicate zombie armor alias in JSON: "
                        + definition.getAlias());
            }
        }

        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Zombie armor JSON contains no armor objects");
        }

        return definitions;
    }

    private static ZombieDefinition createZombieDefinition(
            JsonObject object,
            Map<String, ZombieArmorDefinition> armorDefinitions
    ) {
        String alias = firstAlias(object);
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

    private static ZombieArmorDefinition createArmorDefinition(JsonObject object) {
        String alias = firstAlias(object);
        JsonObject data = getObject(object, "objdata");
        ArmorType type = inferArmorType(getString(data, "ArmorType", alias));
        EnumSet<ArmorFlag> flags = parseArmorFlags(getArray(data, "ArmorFlags"));

        if (flags.isEmpty()) {
            flags = armorFlagsFor(type);
        }

        return new ZombieArmorDefinition(
                alias,
                type,
                getInt(data, "BaseHealth", getInt(data, "Hitpoints", defaultArmorHealth(type))),
                flags
        );
    }

    private static EnumSet<ArmorFlag> parseArmorFlags(JsonArray values) {
        EnumSet<ArmorFlag> flags = EnumSet.noneOf(ArmorFlag.class);

        if (values == null) {
            return flags;
        }

        for (JsonElement element : values) {
            String value = normalize(element.getAsString()).replace("_", "").replace("-", "");

            if ("damageable".equals(value)) {
                flags.add(ArmorFlag.DAMAGEABLE);
            } else if ("droppable".equals(value)) {
                flags.add(ArmorFlag.DROPPABLE);
            } else if ("metallic".equals(value)) {
                flags.add(ArmorFlag.METALLIC);
            } else if ("helm".equals(value) || "helmet".equals(value)) {
                flags.add(ArmorFlag.HELMET);
            } else if ("passdamage".equals(value)) {
                flags.add(ArmorFlag.PASS_DAMAGE);
            }
        }

        return flags;
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
                if (!armorDefinitions.isEmpty()) {
                    throw new IllegalArgumentException("Unknown zombie armor alias: " + armorAlias);
                }

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

    private static List<JsonObject> readObjectArray(Reader reader, String label) throws IOException {
        String json = stripBom(readAll(reader)).trim();
        JsonElement root;

        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(label + " must contain valid JSON", e);
        }

        if (!root.isJsonArray()) {
            throw new IllegalArgumentException(label + " root must be a JSON array");
        }

        List<JsonObject> objects = new ArrayList<>();

        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException(label + " array entries must be JSON objects");
            }

            objects.add(element.getAsJsonObject());
        }

        return objects;
    }

    private static boolean isZombieObject(JsonObject object) {
        String objClass = getString(object, "objclass", "").toLowerCase(Locale.ROOT);
        String alias = firstAlias(object).toLowerCase(Locale.ROOT);
        return alias.startsWith("zombie") && objClass.contains("zombie") && !alias.endsWith("props");
    }

    private static boolean isArmorObject(JsonObject object) {
        String objClass = getString(object, "objclass", "").toLowerCase(Locale.ROOT);
        JsonObject data = getObject(object, "objdata");
        return object.has("aliases") && (
                objClass.contains("armorpropertysheet")
                        || objClass.contains("newspaperarmorpropertysheet")
                        || data.has("ArmorType")
                        || data.has("BaseHealth")
                        || data.has("ArmorFlags")
        );
    }

    private static ZombieType inferType(
            String alias,
            String objClass,
            List<ZombieArmorDefinition> armors
    ) {
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

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }

        return value;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[4096];
        int length;

        while ((length = reader.read(buffer)) != -1) {
            text.append(buffer, 0, length);
        }

        return text.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
