package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Touchable;

/** Shared helpers for small gameplay PAM effects and animated projectiles. */
final class GameplayPamEffectSupport {
    private GameplayPamEffectSupport() {
    }

    static Effect create(
            GameAssetManager assets,
            PamAnimationCatalog catalog,
            String animationName,
            boolean looping,
            String... clipCandidates
    ) {
        if (assets == null || catalog == null || animationName == null || animationName.isBlank()) {
            return null;
        }
        PamAnimationCatalog.AnimationInfo animation = catalog.find(animationName);
        if (animation == null) {
            return null;
        }
        String clip = animation.findClip(clipCandidates);
        if (clip == null) {
            clip = animation.getPreviewClip();
        }
        if (clip == null && !animation.getAllClips().isEmpty()) {
            clip = animation.getAllClips().get(0);
        }
        if (clip == null) {
            return null;
        }
        String resolvedPath = resolvePamPath(animation);
        FileHandle pamFile = Gdx.files.internal("IMAGES/" + resolvedPath);
        if (!pamFile.exists() || !PamTextureAvailability.allTexturesAvailable(assets.getTextureBank(), pamFile)) {
            return null;
        }
        PamAnimationActor actor = new PamAnimationActor(
                assets.getPamPlayer(),
                resolvedPath,
                clip,
                animation.getCanvasWidth(),
                animation.getCanvasHeight()
        );
        actor.setLooping(looping);
        actor.setTouchable(Touchable.disabled);
        Rectangle bounds = null;
        try {
            bounds = assets.getPamPlayer().bounds(resolvedPath, clip);
        } catch (RuntimeException ignored) {
            // A full-canvas fallback still renders safely when bounds are unavailable.
        }
        return new Effect(actor, animation, clip, bounds);
    }

    private static String resolvePamPath(PamAnimationCatalog.AnimationInfo animation) {
        String original = animation.getPath();
        if (Gdx.files.internal("IMAGES/" + original).exists()) {
            return original;
        }
        String normalized = original.replace('\\', '/');
        int effects = normalized.indexOf("/EFFECTS/");
        int slash = normalized.lastIndexOf('/');
        if (effects < 0 || slash < 0 || slash + 1 >= normalized.length()) {
            return original;
        }
        String flattened = normalized.substring(0, effects + "/EFFECTS".length() + 1)
                + normalized.substring(slash + 1);
        return Gdx.files.internal("IMAGES/" + flattened).exists() ? flattened : original;
    }

    static void centerVisibleBounds(Effect effect, float centerX, float centerY, float visibleSize) {
        if (effect == null) {
            return;
        }
        centerVisibleBounds(effect.actor, effect.animation, effect.bounds, centerX, centerY, visibleSize);
    }

    static void centerVisibleBounds(
            PamAnimationActor actor,
            PamAnimationCatalog.AnimationInfo animation,
            Rectangle bounds,
            float centerX,
            float centerY,
            float visibleSize
    ) {
        if (actor == null || animation == null || visibleSize <= 0f) {
            return;
        }
        if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
            float width = visibleSize;
            float height = visibleSize;
            float aspect = animation.getCanvasHeight() <= 0f
                    ? 1f
                    : animation.getCanvasWidth() / animation.getCanvasHeight();
            if (aspect >= 1f) {
                height /= aspect;
            } else {
                width *= aspect;
            }
            actor.setBounds(centerX - width * 0.5f, centerY - height * 0.5f, width, height);
            return;
        }

        float contentMax = Math.max(bounds.width, bounds.height);
        float baseScale = visibleSize / Math.max(1f, contentMax);
        float actorWidth = animation.getCanvasWidth() * baseScale;
        float actorHeight = animation.getCanvasHeight() * baseScale;
        float contentCenterX = (bounds.x + bounds.width * 0.5f) * baseScale;
        float contentCenterY = -(bounds.y + bounds.height * 0.5f) * baseScale;
        actor.setBounds(
                centerX - contentCenterX - actorWidth * 0.5f,
                centerY - contentCenterY - actorHeight * 0.5f,
                actorWidth,
                actorHeight
        );
    }

    static final class Effect {
        final PamAnimationActor actor;
        final PamAnimationCatalog.AnimationInfo animation;
        final String clip;
        final Rectangle bounds;

        private Effect(
                PamAnimationActor actor,
                PamAnimationCatalog.AnimationInfo animation,
                String clip,
                Rectangle bounds
        ) {
            this.actor = actor;
            this.animation = animation;
            this.clip = clip;
            this.bounds = bounds == null ? null : new Rectangle(bounds);
        }

        float duration(float fallback) {
            return this.animation.getClipDuration(this.clip, fallback);
        }
    }
}
