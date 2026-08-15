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

public final class PamAnimationCatalog {
    private static final String CATALOG_PATH = "animations.json";
    private static final Map<String, String> ALIASES = createAliases();

    private final Map<String, AnimationInfo> animationsByName;

    public PamAnimationCatalog() {
        this(Gdx.files.internal(CATALOG_PATH));
    }

    PamAnimationCatalog(FileHandle catalogFile) {
        if (catalogFile == null || !catalogFile.exists()) {
            throw new IllegalArgumentException("Animation catalog was not found");
        }
        this.animationsByName = new HashMap<>();
        this.load(catalogFile);
    }

    public AnimationInfo find(String animationName) {
        String key = normalizeValue(animationName);
        String alias = ALIASES.get(key);
        return this.animationsByName.get(alias == null ? key : alias);
    }

    private void load(FileHandle catalogFile) {
        JsonObject root = JsonParser.parseString(catalogFile.readString("UTF-8"))
                .getAsJsonObject();
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
            String key = normalizeValue(info.getName());
            AnimationInfo current = this.animationsByName.get(key);
            if (current == null || this.score(info) > this.score(current)) {
                this.animationsByName.put(key, info);
            }
        }
    }

    private int score(AnimationInfo animation) {
        String path = animation.getPath().toUpperCase(Locale.ROOT);
        int score = 0;
        if (path.contains("/EMPOWERMINTS/PLANT/")) {
            score += 130;
        } else if (path.contains("/PLANT/")) {
            score += 120;
        }
        if (path.contains("/NPC/")) {
            score -= 120;
        }
        if (path.contains("/EFFECTS/")) {
            score -= 100;
        }
        if (this.animationFileExists(animation)) {
            score += 40;
        }
        if (animation.getPreviewClip() != null) {
            score += 20;
        }
        return score;
    }

    private boolean animationFileExists(AnimationInfo animation) {
        return Gdx.files.internal("IMAGES/" + animation.getPath()).exists();
    }

    private AnimationInfo readAnimation(JsonObject animation) {
        if (!animation.has("name") || !animation.has("path")) {
            return null;
        }
        String name = animation.get("name").getAsString();
        String path = animation.get("path").getAsString();
        JsonArray canvas = animation.getAsJsonArray("canvas");
        if (!validHeader(name, path, canvas)) {
            return null;
        }

        Set<String> clips = new LinkedHashSet<>();
        JsonObject clipObject = animation.getAsJsonObject("clips");
        if (clipObject != null) {
            clips.addAll(clipObject.keySet());
        }
        return new AnimationInfo(
                name,
                path.replace('\\', '/'),
                canvas.get(0).getAsFloat(),
                canvas.get(1).getAsFloat(),
                clips
        );
    }

    private boolean validHeader(String name, String path, JsonArray canvas) {
        return name != null && !name.trim().isEmpty()
                && path != null && !path.trim().isEmpty()
                && canvas != null && canvas.size() >= 2;
    }

    private static Map<String, String> createAliases() {
        Map<String, String> aliases = new HashMap<>();
        alias(aliases, "Twin Sunflower", "SUNFLOWER_TWIN");
        alias(aliases, "Rotobaga", "ROTORUTABAGA");
        alias(aliases, "Mega Gatling Pea", "MEGAGATLING");
        alias(aliases, "Kernel-pult", "KERNALPULT");
        alias(aliases, "Iceberg Lettuce", "ICEBURG");
        alias(aliases, "Phat Beet", "PHATBEETS");
        alias(aliases, "Pierce-mint", "SPEARMINT");
        alias(aliases, "catTail-mint", "SPEARMINT");
        return aliases;
    }

    private static void alias(Map<String, String> aliases, String source, String target) {
        aliases.put(normalizeValue(source), normalizeValue(target));
    }

    private static String normalizeValue(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    public static final class AnimationInfo {
        private final String name;
        private final String path;
        private final float canvasWidth;
        private final float canvasHeight;
        private final Set<String> clips;

        private AnimationInfo(
                String name,
                String path,
                float canvasWidth,
                float canvasHeight,
                Set<String> clips
        ) {
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

        public boolean hasClip(String clipName) {
            return clipName != null && this.clips.contains(clipName);
        }

        public String getIdleClip() {
            if (this.clips.contains("idle")) {
                return "idle";
            }
            return this.findIdleVariant();
        }

        public String getPreviewClip() {
            String idle = this.getIdleClip();
            if (idle != null) {
                return idle;
            }
            if (this.clips.contains("loop")) {
                return "loop";
            }
            if (this.name.equalsIgnoreCase("GRAVEBUSTER") && this.clips.contains("attack1")) {
                return "attack1";
            }
            return null;
        }

        private String findIdleVariant() {
            String stageOne = this.findExactIgnoreCase("idle_stage1");
            if (stageOne != null) {
                return stageOne;
            }

            String bestPrefix = null;
            String bestSuffix = null;
            for (String clip : this.clips) {
                if (clip == null) {
                    continue;
                }
                String lower = clip.toLowerCase(Locale.ROOT);
                if (lower.startsWith("idle") && !lower.contains("plantfood")) {
                    bestPrefix = shorter(bestPrefix, clip);
                } else if (lower.endsWith("_idle") || lower.contains("_idle_")) {
                    bestSuffix = shorter(bestSuffix, clip);
                }
            }
            return bestPrefix == null ? bestSuffix : bestPrefix;
        }

        private String findExactIgnoreCase(String wanted) {
            for (String clip : this.clips) {
                if (wanted.equalsIgnoreCase(clip)) {
                    return clip;
                }
            }
            return null;
        }

        private static String shorter(String current, String candidate) {
            if (current == null || candidate.length() < current.length()) {
                return candidate;
            }
            if (candidate.length() == current.length()
                    && candidate.compareToIgnoreCase(current) < 0) {
                return candidate;
            }
            return current;
        }
    }
}
