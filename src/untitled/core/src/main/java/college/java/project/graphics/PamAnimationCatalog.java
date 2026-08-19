package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        Map<String, Float> clipDurations = new HashMap<>();
        JsonObject clipObject = animation.getAsJsonObject("clips");
        if (clipObject != null) {
            for (Map.Entry<String, JsonElement> entry : clipObject.entrySet()) {
                clips.add(entry.getKey());
                if (entry.getValue() != null && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    clipDurations.put(entry.getKey(), Math.max(0f, entry.getValue().getAsFloat()));
                }
            }
        }
        return new AnimationInfo(
                name,
                path.replace('\\', '/'),
                canvas.get(0).getAsFloat(),
                canvas.get(1).getAsFloat(),
                clips,
                clipDurations
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
        private final Map<String, Float> clipDurations;

        private AnimationInfo(
                String name,
                String path,
                float canvasWidth,
                float canvasHeight,
                Set<String> clips,
                Map<String, Float> clipDurations
        ) {
            this.name = name;
            this.path = path;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.clips = clips;
            this.clipDurations = clipDurations == null ? Map.of() : Map.copyOf(clipDurations);
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

        public String findClip(String... candidates) {
            if (candidates == null) {
                return null;
            }
            for (String candidate : candidates) {
                String exact = this.findExactIgnoreCase(candidate);
                if (exact != null) {
                    return exact;
                }
            }
            return null;
        }

        /**
         * Returns every authored idle-like clip for this PAM in catalog order.
         *
         * This intentionally includes state-specific idles (for example
         * idle_damage, idle_plantfood and stage-specific idles) so reusable UI
         * components can expose the complete set instead of silently choosing
         * only one variant.  If a PAM has no clip containing "idle" but does
         * provide a generic loop clip, loop is exposed as the final safe idle
         * fallback.
         */
        public List<String> getIdleClips() {
            List<String> idleClips = new ArrayList<>();
            for (String clip : this.clips) {
                if (clip == null) {
                    continue;
                }
                String lower = clip.toLowerCase(Locale.ROOT);
                if (lower.equals("idle")
                        || lower.startsWith("idle")
                        || lower.endsWith("_idle")
                        || lower.contains("_idle_")) {
                    idleClips.add(clip);
                }
            }
            if (idleClips.isEmpty()) {
                String loop = this.findExactIgnoreCase("loop");
                if (loop != null) {
                    idleClips.add(loop);
                }
            }
            return List.copyOf(idleClips);
        }

        /** Returns all clips authored in this PAM, preserving catalog order. */
        public List<String> getAllClips() {
            return List.copyOf(this.clips);
        }

        public String getIdleClip() {
            if (this.clips.contains("idle")) {
                return "idle";
            }
            return this.findIdleVariant();
        }


        public float getClipDuration(String clipName, float fallbackSeconds) {
            if (clipName == null) {
                return Math.max(0f, fallbackSeconds);
            }
            Float duration = this.clipDurations.get(clipName);
            return duration == null || duration <= 0f
                    ? Math.max(0f, fallbackSeconds)
                    : duration;
        }

        public String getUnarmedClip() {
            for (String wanted : new String[] {"plant_idle", "plant", "recover"}) {
                String exact = this.findExactIgnoreCase(wanted);
                if (exact != null) {
                    return exact;
                }
            }
            return null;
        }

        public String getRecoverClip() {
            return this.findExactIgnoreCase("recover");
        }

        public String getBowlingShotClip(int bulbIndex) {
            int safe = Math.max(0, Math.min(2, bulbIndex));
            String wanted = safe == 2 ? "special3" : safe == 1 ? "special2" : "special";
            String exact = this.findExactIgnoreCase(wanted);
            return exact == null ? this.getAttackClip() : exact;
        }

        public String getBowlingReloadClip(int bulbIndex) {
            int safe = Math.max(0, Math.min(2, bulbIndex));
            String wanted = safe == 2 ? "reload3" : safe == 1 ? "reload2" : "reload";
            return this.findExactIgnoreCase(wanted);
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

        public String getAttackClip() {
            for (String wanted : new String[] {
                    "attack", "attack1", "attack2", "bite", "jump_down_right", "special"
            }) {
                String exact = this.findExactIgnoreCase(wanted);
                if (exact != null) {
                    return exact;
                }
            }
            String candidate = null;
            for (String clip : this.clips) {
                if (clip == null) {
                    continue;
                }
                String lower = clip.toLowerCase(Locale.ROOT);
                if ((!lower.startsWith("attack") && !lower.startsWith("bite"))
                        || lower.contains("plantfood")
                        || lower.contains("idle")) {
                    continue;
                }
                candidate = shorter(candidate, clip);
            }
            return candidate;
        }

        public String getTriggeredRemovalClip() {
            for (String wanted : new String[] {
                    "jump_down_right", "jump_down_left", "attack", "bite", "special"
            }) {
                String exact = this.findExactIgnoreCase(wanted);
                if (exact != null) {
                    return exact;
                }
            }
            return getAttackClip();
        }

        public String getSunProductionClip() {
            for (String wanted : new String[] {"special", "produce", "production", "sun"}) {
                String exact = this.findExactIgnoreCase(wanted);
                if (exact != null) {
                    return exact;
                }
            }
            String candidate = null;
            for (String clip : this.clips) {
                if (clip == null) {
                    continue;
                }
                String lower = clip.toLowerCase(Locale.ROOT);
                if (lower.startsWith("special_") || lower.startsWith("produce")) {
                    candidate = shorter(candidate, clip);
                }
            }
            return candidate;
        }

        public String getIntroClip() {
            for (String wanted : new String[] {"intro", "plant", "recover"}) {
                String exact = this.findExactIgnoreCase(wanted);
                if (exact != null) {
                    return exact;
                }
            }
            return null;
        }

        public String getDamageClip(int stage) {
            int safeStage = Math.max(1, Math.min(3, stage));
            for (int index = safeStage; index >= 1; index--) {
                String[] wanted = index == 1
                        ? new String[] {"damage", "idle_damage1", "idle_damage"}
                        : new String[] {"damage" + index, "idle_damage" + index};
                for (String candidate : wanted) {
                    String exact = this.findExactIgnoreCase(candidate);
                    if (exact != null) {
                        return exact;
                    }
                }
            }
            return null;
        }

        public String getPlantFoodClip() {
            for (String wanted : new String[] {"plantfood", "pf", "plantfood_on", "idle_plantfood"}) {
                String exact = this.findExactIgnoreCase(wanted);
                if (exact != null) {
                    return exact;
                }
            }
            String candidate = null;
            for (String clip : this.clips) {
                if (clip == null) {
                    continue;
                }
                String lower = clip.toLowerCase(Locale.ROOT);
                if (!lower.startsWith("plantfood")
                        || lower.contains("_on")
                        || lower.contains("_off")
                        || lower.contains("idle")) {
                    continue;
                }
                candidate = shorter(candidate, clip);
            }
            return candidate;
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
