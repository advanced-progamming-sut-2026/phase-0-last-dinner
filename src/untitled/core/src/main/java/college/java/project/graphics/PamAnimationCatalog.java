package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PamAnimationCatalog {
    private static final String CATALOG_PATH = "animations.json";

    private final Map<String, AnimationInfo> animationsByName;

    public PamAnimationCatalog() {
        this(Gdx.files.internal(CATALOG_PATH));
    }

    PamAnimationCatalog(FileHandle catalogFile) {
        if (catalogFile == null || !catalogFile.exists()) {
            throw new IllegalArgumentException(
                    "Animation catalog was not found"
            );
        }

        this.animationsByName = new HashMap<>();
        this.load(catalogFile);
    }

    public AnimationInfo find(String animationName) {
        return this.animationsByName.get(this.normalize(animationName));
    }

    private void load(FileHandle catalogFile) {
        JsonObject root = JsonParser.parseString(
                catalogFile.readString("UTF-8")
        ).getAsJsonObject();
        JsonArray animations = root.getAsJsonArray("animations");

        if (animations == null) {
            return;
        }

        for (JsonElement element : animations) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            AnimationInfo info = this.readAnimation(
                    element.getAsJsonObject()
            );

            if (info != null) {
                this.animationsByName.put(
                        this.normalize(info.getName()),
                        info
                );
            }
        }
    }

    private AnimationInfo readAnimation(JsonObject animation) {
        if (!animation.has("name") || !animation.has("path")) {
            return null;
        }

        String name = animation.get("name").getAsString();
        String path = animation.get("path").getAsString();
        JsonArray canvas = animation.getAsJsonArray("canvas");

        if (name == null || name.trim().isEmpty()
                || path == null || path.trim().isEmpty()
                || canvas == null || canvas.size() < 2) {
            return null;
        }

        Set<String> clips = new HashSet<>();
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

    private String normalize(String value) {
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
    }
}
