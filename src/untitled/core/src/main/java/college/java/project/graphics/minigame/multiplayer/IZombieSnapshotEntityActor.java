package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.GameplayPamScale;
import college.java.project.graphics.GameplayWorldLayout;
import college.java.project.graphics.PamAnimationActor;
import college.java.project.graphics.PamAnimationCatalog;
import college.java.project.graphics.PlantPacketCatalog;
import college.java.project.graphics.ZombieAnimationCatalog;
import college.java.project.graphics.ZombiePacketCatalog;
import lombok.Getter;
import network.izombie.protocol.IZombieEntityKind;
import network.izombie.protocol.IZombieEntitySnapshot;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Getter
public final class IZombieSnapshotEntityActor extends Group {

    private static final float POSITION_SMOOTHING = 14f;
    private static final float DAMAGE_FLASH_SECONDS = 0.12f;

    private static final Color DAMAGE_FLASH = new Color(1f, 0.72f, 0.62f, 1f);

    private static final Color FROZEN_TINT = new Color(0.58f, 0.82f, 1f, 1f);

    private static final Color POISON_TINT = new Color(0.58f, 1f, 0.52f, 1f);

    private static final Color FIRE_TINT = new Color(1f, 0.58f, 0.42f, 1f);

    private final long entityId;
    private final IZombieEntityKind kind;
    private final String definitionKey;
    private final GameAssetManager assets;
    private final Skin skin;

    private final PamAnimationCatalog plantAnimations;
    private final ZombieAnimationCatalog zombieAnimations;

    private final Actor body;
    private final Image shadow;
    private final Image healthBackground;
    private final Image healthForeground;

    private final PamAnimationCatalog.AnimationInfo plantAnimation;
    private final ZombieAnimationCatalog.AnimationInfo zombieAnimation;

    private float targetX;
    private float targetY;
    private int targetRow;

    private int health;
    private int maximumHealth;
    private boolean attacking;
    private boolean dead;
    private List<String> states = Collections.emptyList();

    private String activeClip;
    private float damageFlashRemaining;
    private boolean removalStarted;

    public IZombieSnapshotEntityActor(IZombieEntitySnapshot snapshot, GameAssetManager assets,
                                      PamAnimationCatalog plantAnimations, ZombieAnimationCatalog zombieAnimations) {
        if (snapshot == null || snapshot.kind() == null || assets == null ||
            plantAnimations == null || zombieAnimations == null) {
            throw new IllegalArgumentException("Snapshot entity dependencies are required.");
        }

        this.entityId = snapshot.entityId();
        this.kind = snapshot.kind();
        this.definitionKey = snapshot.definitionKey();
        this.assets = assets;
        this.skin = PvzSkin.get();
        this.plantAnimations = plantAnimations;
        this.zombieAnimations = zombieAnimations;

        plantAnimation = kind == IZombieEntityKind.PLANT ? plantAnimations.find(definitionKey) : null;

        zombieAnimation = kind == IZombieEntityKind.ZOMBIE ? zombieAnimations.find(definitionKey) : null;

        setTouchable(Touchable.disabled);
        setTransform(true);

        shadow = createShadow();

        if (shadow != null) {
            addActor(shadow);
        }

        body = createBody();

        if (body == null) {
            throw new IllegalStateException("Could not create entity visual for " + definitionKey);
        }

        addActor(body);

        healthBackground = createHealthPart(new Color(0.10f, 0.06f, 0.04f, 0.90f));

        healthForeground = createHealthPart(new Color(0.22f, 0.86f, 0.18f, 0.95f));

        addActor(healthBackground);
        addActor(healthForeground);

        applySnapshot(snapshot, true);

        getColor().a = 0f;
        setScale(0.82f);

        addAction(Actions.parallel(Actions.fadeIn(0.16f), Actions.scaleTo(1f, 1f, 0.16f)));
    }

    public void applySnapshot(IZombieEntitySnapshot snapshot, boolean immediate) {
        if (snapshot == null || snapshot.entityId() != entityId) {
            return;
        }

        if (maximumHealth > 0 && snapshot.health() < health) {
            damageFlashRemaining = DAMAGE_FLASH_SECONDS;
        }

        health = Math.max(0, snapshot.health());

        maximumHealth = Math.max(0, snapshot.maximumHealth());

        attacking = snapshot.attacking();
        dead = snapshot.dead();

        states = snapshot.states() == null ? Collections.emptyList() : List.copyOf(snapshot.states());

        updateTargetPosition(snapshot.x(), snapshot.y());

        updateAnimationClip();
        refreshHealthBar();

        if (immediate) {
            setPosition(targetX, targetY);
        }

        if (dead) {
            beginRemoval();
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (!removalStarted) {
            smoothPosition(Math.max(0f, delta));
        }

        damageFlashRemaining = Math.max(0f, damageFlashRemaining - Math.max(0f, delta));

        refreshTint();
    }

    public void beginRemoval() {
        if (removalStarted) {
            return;
        }

        removalStarted = true;
        playDeathClip();

        clearActions();

        addAction(Actions.sequence(Actions.delay(activeClip == null ? 0f : 0.18f),
            Actions.parallel(Actions.fadeOut(0.25f), Actions.scaleTo(0.82f, 0.82f, 0.25f)),
            Actions.removeActor()));
    }

    private void updateTargetPosition(double boardX, double boardY) {
        float cellWidth = GameplayWorldLayout.cellWidth();
        float cellHeight = GameplayWorldLayout.cellHeight();

        float safeX = (float) boardX;
        float safeY = (float) boardY;

        targetRow = Math.max(0, Math.min(4, Math.round(safeY)));

        targetX = GameplayWorldLayout.LAWN_X + safeX * cellWidth;

        float tileBottom = GameplayWorldLayout.LAWN_Y + (4f - safeY) * cellHeight;

        float anchor = switch (kind) {
            case PLANT -> GameplayWorldLayout.PLANT_GROUND_ANCHOR_FACTOR;

            case ZOMBIE -> GameplayWorldLayout.ZOMBIE_GROUND_ANCHOR_FACTOR;

            case PROJECTILE -> 0f;
        };

        targetY = tileBottom + cellHeight * anchor;

        if (kind == IZombieEntityKind.PROJECTILE) {
            targetY += cellHeight * 0.48f;
        }

        setSize(cellWidth, cellHeight);
        layoutChildren();
    }

    private void smoothPosition(float delta) {
        float blend = 1f - (float) Math.exp(-POSITION_SMOOTHING * delta);

        setPosition(getX() + (targetX - getX()) * blend, getY() + (targetY - getY()) * blend);
    }

    private Actor createBody() {
        return switch (kind) {
            case PLANT -> createPlantBody();
            case ZOMBIE -> createZombieBody();
            case PROJECTILE -> createProjectileBody();
        };
    }

    private Actor createPlantBody() {
        if (plantAnimation != null && plantAnimation.getPreviewClip() != null) {
            PamAnimationActor actor = new PamAnimationActor(assets.getPamPlayer(), plantAnimation.getPath(),
                plantAnimation.getPreviewClip(), plantAnimation.getCanvasWidth(), plantAnimation.getCanvasHeight());

            actor.setTouchable(Touchable.disabled);
            activeClip = plantAnimation.getPreviewClip();

            return actor;
        }

        PlantPacketCatalog.PacketVisual visual = PlantPacketCatalog.findPacket(definitionKey);

        Drawable drawable = visual == null ? null : resourceDrawable(visual.getResourceId());

        return createImageOrFallback(drawable, new Color(0.30f, 0.72f, 0.24f, 1f));
    }

    private Actor createZombieBody() {
        if (zombieAnimation != null && zombieAnimation.getWalkClip() != null) {
            PamAnimationActor actor = new PamAnimationActor(assets.getPamPlayer(), zombieAnimation.getPath(),
                zombieAnimation.getWalkClip(), zombieAnimation.getCanvasWidth(), zombieAnimation.getCanvasHeight());

            actor.setTouchable(Touchable.disabled);
            activeClip = zombieAnimation.getWalkClip();

            return actor;
        }

        ZombiePacketCatalog.PacketVisual visual = ZombiePacketCatalog.findPacket(definitionKey);

        Drawable drawable = visual == null ? null : resourceDrawable(visual.getResourceId());

        return createImageOrFallback(drawable, new Color(0.48f, 0.62f, 0.32f, 1f));
    }

    private Actor createProjectileBody() {
        String resourceId = projectileResourceId();

        Drawable drawable = resourceDrawable(resourceId);

        return createImageOrFallback(drawable, new Color(0.48f, 0.92f, 0.24f, 1f));
    }

    private Actor createImageOrFallback(Drawable drawable, Color fallbackColour) {
        Image image = drawable == null ? new Image(skin.newDrawable("white_pixel", fallbackColour)) : new Image(drawable);

        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    private String projectileResourceId() {
        String key = definitionKey == null ? "" : definitionKey.toLowerCase(Locale.ROOT);

        if (key.contains("ice") || key.contains("snow")) {
            return "IMAGE_EFFECTS_T_SNOW_PEA_T_SNOW_PEA_64X44";
        }

        if (key.contains("fire")) {
            return "IMAGE_EFFECTS_T_FIRE_PEA_T_FIRE_PEA_43X43";
        }

        if (key.contains("fume") || key.contains("poison")) {
            return "IMAGE_EFFECTS_FUMESHROOM_BUBBLES_FUMESHROOM_BUBBLES_52X52";
        }

        return "IMAGE_PROJECTILEPEA";
    }

    private Image createShadow() {
        if (kind == IZombieEntityKind.PROJECTILE) {
            return null;
        }

        Drawable drawable = resourceDrawable("IMAGE_PLANTSHADOW");

        if (drawable == null) {
            return null;
        }

        Image image = new Image(drawable);
        image.setScaling(Scaling.stretch);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    private Image createHealthPart(Color colour) {
        Image image = new Image(skin.newDrawable("white_pixel", colour));

        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void layoutChildren() {
        float width = getWidth();
        float height = getHeight();

        if (shadow != null) {
            shadow.setBounds(width * 0.16f, -height * 0.02f, width * 0.68f, height * 0.22f);
        }

        if (kind == IZombieEntityKind.PROJECTILE) {
            float size = width * 0.24f;

            body.setBounds(width * 0.5f - size * 0.5f, -size * 0.5f, size, size);
        } else if (body instanceof PamAnimationActor) {
            layoutPamBody();
        } else {
            float widthFactor = kind == IZombieEntityKind.PLANT ? 0.78f : 0.92f;

            float heightFactor = kind == IZombieEntityKind.PLANT ? 0.88f : 1.42f;

            body.setBounds(width * (1f - widthFactor) * 0.5f, 0f, width * widthFactor, height * heightFactor);
        }

        refreshHealthBar();
    }

    private void layoutPamBody() {
        float canvasWidth;
        float canvasHeight;
        String path;
        String clip;

        if (kind == IZombieEntityKind.PLANT) {
            canvasWidth = plantAnimation.getCanvasWidth();
            canvasHeight = plantAnimation.getCanvasHeight();
            path = plantAnimation.getPath();
            clip = activeClip;
        } else {
            canvasWidth = zombieAnimation.getCanvasWidth();
            canvasHeight = zombieAnimation.getCanvasHeight();
            path = zombieAnimation.getPath();
            clip = activeClip;
        }

        float actorWidth = GameplayPamScale.actorWidth(canvasWidth);

        float actorHeight = GameplayPamScale.actorHeight(canvasHeight);

        float centreOffset = 0f;
        float groundOffset = 0f;

        try {
            Rectangle bounds = assets.getPamPlayer().bounds(path, clip);

            if (bounds != null) {
                centreOffset = -(bounds.x + bounds.width / 2f) * GameplayPamScale.WORLD_SCALE;

                groundOffset = (bounds.y + bounds.height) * GameplayPamScale.WORLD_SCALE;
            }
        } catch (RuntimeException ignored) {
        }

        body.setBounds(getWidth() / 2f + centreOffset - actorWidth / 2f, groundOffset - actorHeight / 2f, actorWidth,
            actorHeight);
    }

    private void updateAnimationClip() {
        if (!(body instanceof PamAnimationActor actor)) {
            return;
        }

        String wantedClip = activeClip;

        if (kind == IZombieEntityKind.PLANT && plantAnimation != null) {
            wantedClip = attacking ? plantAnimation.getAttackClip() : plantAnimation.getPreviewClip();
        }

        if (kind == IZombieEntityKind.ZOMBIE && zombieAnimation != null) {
            wantedClip = attacking ? zombieAnimation.getAttackClip() : zombieAnimation.getWalkClip();
        }

        if (wantedClip == null || wantedClip.equals(activeClip)) {
            return;
        }

        actor.setAnimation(kind == IZombieEntityKind.PLANT ? plantAnimation.getPath() : zombieAnimation.getPath(), wantedClip);

        actor.setLooping(true);
        activeClip = wantedClip;
        layoutChildren();
    }

    private void playDeathClip() {
        if (kind != IZombieEntityKind.ZOMBIE || zombieAnimation == null || !(body instanceof PamAnimationActor actor)) {
            return;
        }

        String deathClip = zombieAnimation.getDeathClip();

        if (deathClip == null) {
            return;
        }

        actor.setAnimation(zombieAnimation.getPath(), deathClip);

        actor.setLooping(false);
        activeClip = deathClip;
    }

    private void refreshHealthBar() {
        if (healthBackground == null || healthForeground == null) {
            return;
        }

        boolean visible = kind != IZombieEntityKind.PROJECTILE && maximumHealth > 0 && health < maximumHealth;

        healthBackground.setVisible(visible);
        healthForeground.setVisible(visible);

        if (!visible) {
            return;
        }

        float barWidth = getWidth() * 0.78f;
        float barHeight = 7f;

        healthBackground.setBounds(getWidth() * 0.11f, getHeight() * 1.03f, barWidth, barHeight);

        float ratio = Math.max(0f, Math.min(1f, (float) health / maximumHealth));

        healthForeground.setBounds(getWidth() * 0.11f, getHeight() * 1.03f, barWidth * ratio, barHeight);
    }

    private void refreshTint() {
        Color colour = Color.WHITE;

        if (damageFlashRemaining > 0f) {
            colour = DAMAGE_FLASH;
        } else if (hasState("FROZEN") || hasState("CHILLED") || hasState("ICE")) {
            colour = FROZEN_TINT;
        } else if (hasState("POISONED") || hasState("POISON")) {
            colour = POISON_TINT;
        } else if (hasState("BURNING") || hasState("FIRE")) {
            colour = FIRE_TINT;
        }

        body.setColor(colour);
    }

    private boolean hasState(String wantedState) {
        for (String state : states) {
            if (state != null && state.equalsIgnoreCase(wantedState)) {
                return true;
            }
        }

        return false;
    }

    private Drawable resourceDrawable(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }

        try {
            TextureBank bank = assets.getTextureBank();

            if (bank == null || bank.region(resourceId) == null) {
                return null;
            }

            return new TextureRegionDrawable(bank.region(resourceId));
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
