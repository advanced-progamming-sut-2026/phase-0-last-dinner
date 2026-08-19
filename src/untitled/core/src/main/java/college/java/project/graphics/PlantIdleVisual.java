package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable, model-independent plant idle renderer.
 *
 * The actor owns no gameplay state.  Give it a plant name and it resolves the
 * authentic PAM, validates all PAM texture dependencies, exposes every idle
 * variant in that PAM, loops the selected idle, and falls back to packet art if
 * the original idle animation is unavailable.
 *
 * Recommended team usage is to share one {@link GameAssetManager} between all
 * PlantIdleVisual instances in a screen and dispose that manager with the screen.
 */
public final class PlantIdleVisual extends Group implements Disposable {
    private static final float DEFAULT_PADDING = 8f;
    private static final float MIN_AUTO_CYCLE_SECONDS = 0.25f;
    private static final Color DEFAULT_TINT = Color.WHITE;

    private final GameAssetManager assets;
    private final boolean ownsAssets;
    private final PamAnimationCatalog animationCatalog;

    private String plantName = "";
    private PamAnimationCatalog.AnimationInfo animation;
    private List<String> idleClips = List.of();
    private int idleClipIndex = -1;
    private Actor visual;
    private boolean usingStaticFallback;
    private boolean grounded;
    private float padding = DEFAULT_PADDING;
    private float autoCycleSeconds;
    private float autoCycleRemaining;
    private Color visualTint = new Color(DEFAULT_TINT);

    /** Convenience constructor for a standalone preview. */
    public PlantIdleVisual(String plantName) {
        this(new GameAssetManager(), plantName, true);
    }

    /**
     * Preferred constructor for screens containing multiple plant previews.
     * The caller keeps ownership of {@code assets} and disposes it once.
     */
    public PlantIdleVisual(GameAssetManager assets, String plantName) {
        this(assets, plantName, false);
    }

    private PlantIdleVisual(GameAssetManager assets, String plantName, boolean ownsAssets) {
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.assets = assets;
        this.ownsAssets = ownsAssets;
        this.animationCatalog = new PamAnimationCatalog();
        setTouchable(Touchable.disabled);
        setPlant(plantName);
    }

    /** Switches this reusable actor to another plant while keeping its bounds. */
    public void setPlant(String plantName) {
        this.plantName = plantName == null ? "" : plantName.trim();
        this.animation = this.animationCatalog.find(this.plantName);
        this.idleClips = this.animation == null ? List.of() : this.animation.getIdleClips();
        this.idleClipIndex = this.idleClips.isEmpty() ? -1 : 0;
        this.autoCycleRemaining = this.autoCycleSeconds;
        rebuildVisual();
    }

    public String getPlantName() {
        return this.plantName;
    }

    /** Every authored idle/state-idle variant available for the current plant. */
    public List<String> getAvailableIdleClips() {
        return this.idleClips;
    }

    public int getIdleVariantCount() {
        return this.idleClips.size();
    }

    public String getCurrentIdleClip() {
        return this.idleClipIndex < 0 || this.idleClipIndex >= this.idleClips.size()
                ? null
                : this.idleClips.get(this.idleClipIndex);
    }

    /**
     * Selects a specific idle clip by exact/case-insensitive name.
     * Returns false rather than playing a non-idle clip accidentally.
     */
    public boolean setIdleClip(String clipName) {
        if (clipName == null) {
            return false;
        }
        for (int index = 0; index < this.idleClips.size(); index++) {
            if (clipName.equalsIgnoreCase(this.idleClips.get(index))) {
                return setIdleVariant(index);
            }
        }
        return false;
    }

    /** Selects an authored idle variant by index. */
    public boolean setIdleVariant(int index) {
        if (index < 0 || index >= this.idleClips.size()) {
            return false;
        }
        this.idleClipIndex = index;
        this.autoCycleRemaining = this.autoCycleSeconds;
        if (this.visual instanceof PamAnimationActor actor && this.animation != null) {
            actor.setAnimation(this.animation.getPath(), this.idleClips.get(index));
            actor.setLooping(true);
            layoutVisual();
            return true;
        }
        rebuildVisual();
        return this.visual instanceof PamAnimationActor;
    }

    public boolean nextIdleVariant() {
        if (this.idleClips.isEmpty()) {
            return false;
        }
        int next = this.idleClipIndex < 0 ? 0 : (this.idleClipIndex + 1) % this.idleClips.size();
        return setIdleVariant(next);
    }

    public boolean previousIdleVariant() {
        if (this.idleClips.isEmpty()) {
            return false;
        }
        int previous = this.idleClipIndex <= 0
                ? this.idleClips.size() - 1
                : this.idleClipIndex - 1;
        return setIdleVariant(previous);
    }

    public boolean randomIdleVariant() {
        if (this.idleClips.isEmpty()) {
            return false;
        }
        return setIdleVariant(ThreadLocalRandom.current().nextInt(this.idleClips.size()));
    }

    /**
     * Automatically switches between authored idle variants. Pass <= 0 to disable.
     * Each selected clip continues looping until the next switch.
     */
    public void setAutoCycleSeconds(float seconds) {
        this.autoCycleSeconds = seconds <= 0f ? 0f : Math.max(MIN_AUTO_CYCLE_SECONDS, seconds);
        this.autoCycleRemaining = this.autoCycleSeconds;
    }

    public float getAutoCycleSeconds() {
        return this.autoCycleSeconds;
    }

    /**
     * Centered mode is best for cards/details. Grounded mode pins the visible
     * animation to the lower edge, which is useful in greenhouse/gameplay-like UI.
     */
    public void setGrounded(boolean grounded) {
        if (this.grounded == grounded) {
            return;
        }
        this.grounded = grounded;
        layoutVisual();
    }

    public boolean isGrounded() {
        return this.grounded;
    }

    public void setContentPadding(float padding) {
        this.padding = Math.max(0f, padding);
        layoutVisual();
    }

    public float getContentPadding() {
        return this.padding;
    }

    public void setVisualTint(Color tint) {
        this.visualTint.set(tint == null ? DEFAULT_TINT : tint);
        if (this.visual != null) {
            this.visual.setColor(this.visualTint);
        }
    }

    public boolean isAnimated() {
        return this.visual instanceof PamAnimationActor;
    }

    public boolean isUsingStaticFallback() {
        return this.usingStaticFallback;
    }

    /** True when the original PAM and all of its referenced texture atlases exist. */
    public boolean hasUsableIdleAnimation() {
        return this.animation != null && !this.idleClips.isEmpty() && canUseAnimation(this.animation);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        if (this.autoCycleSeconds <= 0f || this.idleClips.size() <= 1) {
            return;
        }
        this.autoCycleRemaining -= Math.max(0f, delta);
        if (this.autoCycleRemaining <= 0f) {
            nextIdleVariant();
            this.autoCycleRemaining = this.autoCycleSeconds;
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        layoutVisual();
    }

    private void rebuildVisual() {
        clearChildren();
        this.visual = null;
        this.usingStaticFallback = false;

        Actor animationVisual = createAnimationVisual();
        if (animationVisual != null) {
            this.visual = animationVisual;
        } else {
            this.visual = createStaticFallback();
            this.usingStaticFallback = this.visual != null;
        }

        if (this.visual != null) {
            this.visual.setTouchable(Touchable.disabled);
            this.visual.setColor(this.visualTint);
            addActor(this.visual);
            layoutVisual();
        }
    }

    private Actor createAnimationVisual() {
        if (this.animation == null || this.idleClipIndex < 0 || !canUseAnimation(this.animation)) {
            return null;
        }
        PamAnimationActor actor = new PamAnimationActor(
                this.assets.getPamPlayer(),
                this.animation.getPath(),
                this.idleClips.get(this.idleClipIndex),
                this.animation.getCanvasWidth(),
                this.animation.getCanvasHeight()
        );
        actor.setLooping(true);
        return actor;
    }

    private boolean canUseAnimation(PamAnimationCatalog.AnimationInfo info) {
        if (info == null || info.getIdleClips().isEmpty()) {
            return false;
        }
        FileHandle pamFile = Gdx.files.internal("IMAGES/" + info.getPath());
        return pamFile.exists()
                && PamTextureAvailability.allTexturesAvailable(this.assets.getTextureBank(), pamFile);
    }

    private Actor createStaticFallback() {
        PlantPacketCatalog.PacketVisual packet = PlantPacketCatalog.findPacket(this.plantName);
        if (packet == null) {
            return null;
        }
        TextureBank bank = this.assets.getTextureBank();
        TextureRegion region;
        try {
            region = bank.region(packet.getResourceId());
        } catch (RuntimeException exception) {
            return null;
        }
        if (region == null) {
            return null;
        }
        Image image = new Image(new TextureRegionDrawable(region));
        image.setScaling(Scaling.fit);
        return image;
    }

    private void layoutVisual() {
        if (this.visual == null || getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }
        float availableWidth = Math.max(1f, getWidth() - this.padding * 2f);
        float availableHeight = Math.max(1f, getHeight() - this.padding * 2f);

        if (this.visual instanceof PamAnimationActor actor && this.animation != null) {
            layoutPam(actor, availableWidth, availableHeight);
            return;
        }
        layoutStatic(availableWidth, availableHeight);
    }

    private void layoutPam(PamAnimationActor actor, float availableWidth, float availableHeight) {
        Rectangle bounds = readBounds(this.animation, getCurrentIdleClip());
        float canvasWidth = this.animation.getCanvasWidth();
        float canvasHeight = this.animation.getCanvasHeight();
        if (!validBounds(bounds)) {
            float fit = Math.min(availableWidth / canvasWidth, availableHeight / canvasHeight);
            actor.setBounds(
                    this.padding + (availableWidth - canvasWidth) / 2f,
                    this.padding + (availableHeight - canvasHeight) / 2f,
                    canvasWidth,
                    canvasHeight
            );
            actor.setScale(Math.max(0.01f, fit));
            return;
        }

        float scale = Math.min(availableWidth / bounds.width, availableHeight / bounds.height);
        scale = Math.max(0.01f, scale);
        float targetCenterX = this.padding + availableWidth / 2f;
        float originX = targetCenterX - (bounds.x + bounds.width / 2f) * scale;

        float originY;
        if (this.grounded) {
            originY = this.padding + (bounds.y + bounds.height) * scale;
        } else {
            float targetCenterY = this.padding + availableHeight / 2f;
            originY = targetCenterY - (bounds.y + bounds.height / 2f) * scale;
        }

        actor.setBounds(
                originX - canvasWidth / 2f,
                originY - canvasHeight / 2f,
                canvasWidth,
                canvasHeight
        );
        actor.setScale(scale);
    }

    private Rectangle readBounds(PamAnimationCatalog.AnimationInfo info, String clip) {
        if (info == null || clip == null) {
            return null;
        }
        try {
            return this.assets.getPamPlayer().bounds(info.getPath(), clip);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean validBounds(Rectangle bounds) {
        return bounds != null && bounds.width > 0f && bounds.height > 0f;
    }

    private void layoutStatic(float availableWidth, float availableHeight) {
        if (!(this.visual instanceof Image image)) {
            this.visual.setBounds(this.padding, this.padding, availableWidth, availableHeight);
            return;
        }
        float sourceWidth = Math.max(1f, image.getDrawable().getMinWidth());
        float sourceHeight = Math.max(1f, image.getDrawable().getMinHeight());
        float scale = Math.min(availableWidth / sourceWidth, availableHeight / sourceHeight);
        float width = sourceWidth * scale;
        float height = sourceHeight * scale;
        float y = this.grounded
                ? this.padding
                : this.padding + (availableHeight - height) / 2f;
        image.setBounds(
                this.padding + (availableWidth - width) / 2f,
                y,
                width,
                height
        );
    }

    @Override
    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }
}
