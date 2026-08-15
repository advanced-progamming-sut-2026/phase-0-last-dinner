package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public final class ZombieAnimationCatalog {
    private static final String CATALOG_PATH = "animations.json";
    private static final Map<String, String> ALIASES = createAliases();

    private final Map<String, AnimationInfo> animationsByName;

    public ZombieAnimationCatalog() {
        this(Gdx.files.internal(CATALOG_PATH));
    }

    ZombieAnimationCatalog(FileHandle catalogFile) {
        if (catalogFile == null || !catalogFile.exists()) {
            throw new IllegalArgumentException("Animation catalog was not found");
        }
        this.animationsByName = new HashMap<>();
        this.load(catalogFile);
    }

    public AnimationInfo find(String zombieAlias) {
        String alias = ALIASES.get(normalize(zombieAlias));
        if (alias == null) {
            return null;
        }
        return this.animationsByName.get(normalize(alias));
    }

    private void load(FileHandle catalogFile) {
        JsonObject root = JsonParser.parseString(catalogFile.readString("UTF-8")).getAsJsonObject();
        JsonArray animations = root.getAsJsonArray("animations");
        if (animations == null) {
            return;
        }
        for (JsonElement element : animations) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            AnimationInfo info = this.readAnimation(element.getAsJsonObject());
            if (info == null) {
                continue;
            }
            String key = normalize(info.getName());
            AnimationInfo current = this.animationsByName.get(key);
            if (current == null || this.score(info) > this.score(current)) {
                this.animationsByName.put(key, info);
            }
        }
    }

    private int score(AnimationInfo info) {
        String path = info.getPath().toUpperCase(Locale.ROOT);
        int result = path.contains("/ZOMBIE/") ? 150 : 0;
        if (path.contains("/EFFECTS/") || path.contains("/NPC/")) {
            result -= 150;
        }
        if (Gdx.files.internal("IMAGES/" + info.getPath()).exists()) {
            result += 40;
        }
        if (info.getPreviewClip() != null) {
            result += 20;
        }
        return result;
    }

    private AnimationInfo readAnimation(JsonObject object) {
        if (!object.has("name") || !object.has("path")) {
            return null;
        }
        JsonArray canvas = object.getAsJsonArray("canvas");
        if (canvas == null || canvas.size() < 2) {
            return null;
        }
        Set<String> clips = new LinkedHashSet<>();
        JsonObject clipObject = object.getAsJsonObject("clips");
        if (clipObject != null) {
            clips.addAll(clipObject.keySet());
        }
        return new AnimationInfo(
                object.get("name").getAsString(),
                object.get("path").getAsString().replace('\\', '/'),
                canvas.get(0).getAsFloat(),
                canvas.get(1).getAsFloat(),
                clips
        );
    }

    private static Map<String, String> createAliases() {
        Map<String, String> map = new HashMap<>();
        addCoreAliases(map);
        addEgyptAliases(map);
        addIceAgeAliases(map);
        addBeachAliases(map);
        addDarkAliases(map);
        addZombossAliases(map);
        addProjectAliases(map);
        return map;
    }

    private static void addCoreAliases(Map<String, String> map) {
        alias(map, "ZombieTutorialDefault", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieTutorialArmor1Default", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieTutorialArmor2Default", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieTutorialArmor4Default", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieGargantuarBasic", "TUTORIAL_GARGANTUAR");
        alias(map, "ZombieTutorialImpDefault", "ZOMBIE_TUTORIAL_IMP");
        alias(map, "ZombieTutorialFlagDefault", "ZOMBIE_TUTORIAL_FLAG");
    }

    private static void addEgyptAliases(Map<String, String> map) {
        alias(map, "ZombieMummyDefault", "ZOMBIE_EGYPT_BASIC");
        alias(map, "ZombieMummyArmor1Default", "ZOMBIE_EGYPT_BASIC");
        alias(map, "ZombieMummyArmor2Default", "ZOMBIE_EGYPT_BASIC");
        alias(map, "ZombieMummyArmor4Default", "ZOMBIE_EGYPT_BASIC");
        alias(map, "ZombiePharaohDefault", "ZOMBIE_EGYPT_SARCOPHAGUS");
        alias(map, "ZombieRaDefault", "ZOMBIE_EGYPT_RA");
        alias(map, "ZombieExplorerDefault", "ZOMBIE_EXPLORER");
        alias(map, "ZombieTombRaiserDefault", "ZOMBIE_EGYPT_TOMBRAISER");
        alias(map, "ZombieCamelDefault", "ZOMBIE_EGYPT_CAMEL");
        alias(map, "ZombieEgyptGargantuar", "EGYPT_GARGANTUAR");
        alias(map, "ZombieEgyptImpDefault", "ZOMBIE_EGYPT_IMP");
    }

    private static void addIceAgeAliases(Map<String, String> map) {
        alias(map, "ZombieIceageDefault", "ZOMBIE_ICEAGE_BASIC");
        alias(map, "ZombieIceageArmor1Default", "ZOMBIE_ICEAGE_BASIC");
        alias(map, "ZombieIceageArmor2Default", "ZOMBIE_ICEAGE_BASIC");
        alias(map, "ZombieIceageArmor3Default", "ZOMBIE_ICEAGE_BASIC_BRICK");
        alias(map, "ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER");
        alias(map, "ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE");
        alias(map, "ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODORIDER");
        alias(map, "ZombieWeaselHoarderDefault", "ZOMBIE_ICEAGE_WEASELHOARDER");
        alias(map, "ZombieWeaselDefault", "ZOMBIE_ICEAGE_WEASEL");
        alias(map, "ZombieIceAgeGargantuar", "ZOMBIE_ICEAGE_GARGANTUAR");
        alias(map, "ZombieIceageImpDefault", "ZOMBIE_ICEAGE_IMP");
    }

    private static void addBeachAliases(Map<String, String> map) {
        alias(map, "ZombieBeachDefault", "ZOMBIE_BEACH_BASIC");
        alias(map, "ZombieBeachArmor1Default", "ZOMBIE_BEACH_BASIC");
        alias(map, "ZombieBeachArmor2Default", "ZOMBIE_BEACH_BASIC");
        alias(map, "ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKELER");
        alias(map, "ZombieBeachSurfer", "ZOMBIE_BEACH_SURFER");
        alias(map, "ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN");
        alias(map, "ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS");
        alias(map, "ZombieBeachGargantuar", "BEACH_GARGANTUAR");
        alias(map, "ZombieBeachImpDefault", "ZOMBIE_BEACH_IMP_MERMAID");
        alias(map, "ZombieBeachFastSwimmer", "ZOMBIE_BEACH_BASICFEM");
    }

    private static void addDarkAliases(Map<String, String> map) {
        alias(map, "ZombieDarkDefault", "ZOMBIE_DARK_BASIC");
        alias(map, "ZombieDarkArmor1Default", "ZOMBIE_DARK_BASIC");
        alias(map, "ZombieDarkArmor2Default", "ZOMBIE_DARK_BASIC");
        alias(map, "ZombieDarkArmor3Default", "ZOMBIE_DARK_BASIC");
        alias(map, "ZombieDarkArmor4Default", "ZOMBIE_DARK_BASIC_BRICK");
        alias(map, "ZombieWizardDefault", "ZOMBIE_DARK_WIZARD");
        alias(map, "ZombieDarkJugglerDefault", "ZOMBIE_DARK_JESTER");
        alias(map, "ZombieDarkKing", "ZOMBIE_DARK_KING");
        alias(map, "ZombieDarkGargantuar", "DARK_GARGANTUAR");
        alias(map, "ZombieDarkImpDefault", "ZOMBIE_DARK_IMP_MONK");
    }

    private static void addZombossAliases(Map<String, String> map) {
        alias(map, "ZombieZombossMechEgypt", "ZOMBIE_EGYPT_ZOMBOSS");
        alias(map, "ZombieZombossMechPirate", "ZOMBIE_PIRATE_ZOMBOSS");
        alias(map, "ZombieZombossMechCowboy", "ZOMBIE_COWBOY_ZOMBOSS");
        alias(map, "ZombieZombossMechDark", "ZOMBIE_DARK_ZOMBOSS");
    }


    private static void addProjectAliases(Map<String, String> map) {
        alias(map, "ZombieDefault", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieArmor1", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieArmor2", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieArmor4", "ZOMBIE_TUTORIAL");
        alias(map, "ZombieDarkArmor3", "ZOMBIE_DARK_BASIC");
        alias(map, "ZombieGargantuar", "TUTORIAL_GARGANTUAR");
        alias(map, "ZombieImp", "ZOMBIE_TUTORIAL_IMP");
        alias(map, "ZombieRa", "ZOMBIE_EGYPT_RA");
        alias(map, "ZombieExplorer", "ZOMBIE_EXPLORER");
        alias(map, "ZombieTombRaiser", "ZOMBIE_EGYPT_TOMBRAISER");
        alias(map, "ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODORIDER");
        alias(map, "ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER");
        alias(map, "ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE");
        alias(map, "ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN");
        alias(map, "ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS");
        alias(map, "ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKELER");
        alias(map, "ZombieDarkJuggler", "ZOMBIE_DARK_JESTER");
        alias(map, "ZombieWizard", "ZOMBIE_DARK_WIZARD");
        alias(map, "ZombieDarkKing", "ZOMBIE_DARK_KING");
        alias(map, "ZombieDarkImpDragon", "ZOMBIE_DARK_IMP_DRAGON");
        alias(map, "ZombieModernAllStar", "ZOMBIE_MODERN_ALLSTAR");
        alias(map, "ZombieLostCityJane", "ZOMBIE_LOSTCITY_JANE");
        alias(map, "ZombieCrystalSkull", "ZOMBIE_LOSTCITY_CRYSTALSKULL");
        alias(map, "ZombieProspector", "ZOMBIE_PROSPECTOR");
        alias(map, "ZombiePiano", "ZOMBIE_PIANO");
        alias(map, "ZombieNewspaper", "ZOMBIE_MODERN_NEWSPAPER");
        alias(map, "ZombieArcade", "ZOMBIE_80S_ARCADE");
        alias(map, "ZombieBarrelRoller", "ZOMBIE_PIRATE_BARREL_PUSHER");
    }

    private static void alias(Map<String, String> map, String source, String target) {
        map.put(normalize(source), target);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public static final class AnimationInfo {
        private final String name;
        private final String path;
        private final float canvasWidth;
        private final float canvasHeight;
        private final Set<String> clips;

        private AnimationInfo(String name, String path, float canvasWidth, float canvasHeight, Set<String> clips) {
            this.name = name;
            this.path = path;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.clips = clips;
        }

        public String getName() {
            return this.name;
        }

        public String getPath() {
            return this.path;
        }

        public float getCanvasWidth() {
            return this.canvasWidth;
        }

        public float getCanvasHeight() {
            return this.canvasHeight;
        }

        public String getPreviewClip() {
            if (this.clips.contains("idle")) {
                return "idle";
            }
            for (String clip : this.clips) {
                if (clip != null && clip.toLowerCase(Locale.ROOT).startsWith("idle")) {
                    return clip;
                }
            }
            if (this.clips.contains("walk")) {
                return "walk";
            }
            return null;
        }
    }
}
