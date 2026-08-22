package college.java.project.graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.Plant;
import model.plant.DamageExpressionParser;
import model.plant.Projectile;
import model.plant.ProjectileType;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/** Renders direct and lobbed projectiles using live Phase 1 projectile coordinates. */
public final class GameplayProjectileLayer extends Group {
    private static final String PEA_RESOURCE = "IMAGE_PROJECTILEPEA";

    // Round26 moved the rendered entities upward inside each tile without
    // changing Phase 1 projectile coordinates. Keep the projectile path in the
    // same visual space as its source so it does not drop back toward the old
    // tile-centre line immediately after leaving the mouth/nozzle/basket.
    private static final float PRE_ANCHOR_PLANT_BOTTOM_FACTOR = -0.01f;
    private static final float PRE_ANCHOR_ZOMBIE_BOTTOM_FACTOR = -0.03f;

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private final PamAnimationCatalog effectCatalog;
    private Group renderHost;
    private boolean ownsAssets;
    private Consumer<Projectile> spawnListener;
    private Consumer<Projectile> impactListener;
    private ToDoubleFunction<Projectile> releaseDelayProvider;
    private Function<Projectile, Vector2> launchPointProvider;
    private final Map<Projectile, RenderedProjectile> actors = new IdentityHashMap<>();

    public GameplayProjectileLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayProjectileLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        this.effectCatalog = new PamAnimationCatalog();
        this.renderHost = this;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        syncProjectiles(Math.max(0f, delta));
    }

    public int getRenderedProjectileCount() {
        return this.actors.size();
    }

    public void setSpawnListener(Consumer<Projectile> spawnListener) {
        this.spawnListener = spawnListener;
    }

    public void setImpactListener(Consumer<Projectile> impactListener) {
        this.impactListener = impactListener;
    }

    public void setReleaseDelayProvider(ToDoubleFunction<Projectile> releaseDelayProvider) {
        this.releaseDelayProvider = releaseDelayProvider;
    }

    /** Supplies an exact visual launch point in lawn-local coordinates. */
    public void setLaunchPointProvider(Function<Projectile, Vector2> launchPointProvider) {
        this.launchPointProvider = launchPointProvider;
    }

    void setRenderHost(Group renderHost) {
        this.renderHost = renderHost == null ? this : renderHost;
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void syncProjectiles(float delta) {
        List<Projectile> projectiles = new ArrayList<>(this.dataSource.getProjectiles());
        projectiles.removeIf(projectile -> projectile == null || projectile.isExpired());
        removeMissing(projectiles);
        Map<Plant, int[]> newVolleyOrdinals = new IdentityHashMap<>();
        for (Projectile projectile : projectiles) {
            RenderedProjectile rendered = this.actors.get(projectile);
            if (rendered == null) {
                rendered = createProjectileActor(projectile);
                if (rendered == null) {
                    continue;
                }
                assignVolleyVisualSlot(rendered, projectile, newVolleyOrdinals);
                this.actors.put(projectile, rendered);
                this.renderHost.addActor(rendered.root);
                if (this.spawnListener != null) {
                    this.spawnListener.accept(projectile);
                }
                if (this.launchPointProvider != null) {
                    Vector2 launchPoint = this.launchPointProvider.apply(projectile);
                    if (launchPoint != null) {
                        rendered.launchX = launchPoint.x;
                        rendered.launchY = launchPoint.y;
                        rendered.hasLaunchPoint = true;
                    }
                }
                if (this.releaseDelayProvider != null) {
                    rendered.releaseDelay = Math.max(
                            0f,
                            (float) this.releaseDelayProvider.applyAsDouble(projectile)
                    );
                }
                rendered.root.setVisible(rendered.releaseDelay <= 0f);
            }
            rendered.age += delta;
            advancePamProjectile(rendered);
            updateProjectileActor(rendered, projectile);
            GameplayBoardDepthOrder.mark(
                    rendered.root,
                    (int) Math.round(projectile.getExactY()),
                    GameplayBoardDepthOrder.PROJECTILE
            );
        }
    }

    private void assignVolleyVisualSlot(
            RenderedProjectile rendered,
            Projectile projectile,
            Map<Plant, int[]> newVolleyOrdinals
    ) {
        if (rendered == null || projectile == null || projectile.getSourcePlant() == null) {
            return;
        }
        int burstSize = visualBurstSize(projectile);
        if (burstSize <= 1) {
            return;
        }
        Plant source = projectile.getSourcePlant();
        int directionBucket = projectile.getHorizontalDirection() < 0 ? 0 : 1;
        int[] counts = newVolleyOrdinals.computeIfAbsent(source, ignored -> new int[2]);
        int ordinal = counts[directionBucket]++;
        rendered.volleySize = burstSize;
        rendered.volleyIndex = ordinal % burstSize;
    }

    private int visualBurstSize(Projectile projectile) {
        if (projectile == null || projectile.getSourcePlant() == null) {
            return 1;
        }
        String name = projectile.getSourcePlant().getName() == null
                ? ""
                : projectile.getSourcePlant().getName().toLowerCase(Locale.ROOT);
        if (name.contains("mega gatling") && projectile.getHorizontalDirection() > 0) {
            return 4;
        }
        if (name.equals("repeater") && projectile.getHorizontalDirection() > 0) {
            return 2;
        }
        if (name.contains("split pea") && projectile.getHorizontalDirection() < 0) {
            return 2;
        }
        return 1;
    }

    private void removeMissing(List<Projectile> projectiles) {
        List<Projectile> removed = new ArrayList<>();
        for (Projectile projectile : this.actors.keySet()) {
            if (!containsIdentity(projectiles, projectile)) {
                removed.add(projectile);
            }
        }
        for (Projectile projectile : removed) {
            RenderedProjectile rendered = this.actors.remove(projectile);
            if (rendered != null) {
                spawnImpact(rendered);
                if (this.impactListener != null) {
                    this.impactListener.accept(projectile);
                }
                rendered.root.remove();
            }
        }
    }

    private boolean containsIdentity(List<Projectile> projectiles, Projectile wanted) {
        for (Projectile projectile : projectiles) {
            if (projectile == wanted) {
                return true;
            }
        }
        return false;
    }

    private RenderedProjectile createProjectileActor(Projectile projectile) {
        GameplayProjectileVisualCatalog.Visual visual = GameplayProjectileVisualCatalog.forProjectile(projectile);
        Group root = new Group();
        root.setTouchable(Touchable.disabled);
        root.setBounds(0f, 0f, getWidth(), getHeight());

        GameplayPamEffectSupport.Effect pam = null;
        GameplayPamEffectSupport.Effect overlayPam = null;
        Actor primary = null;
        int regionWidth = 1;
        int regionHeight = 1;

        if (visual.usesPam()) {
            pam = GameplayPamEffectSupport.create(
                    this.assets,
                    this.effectCatalog,
                    visual.getPamAnimationName(),
                    visual.isPamStartLoops(),
                    visual.getPamStartClip()
            );
            if (pam != null) {
                primary = pam.actor;
                root.addActor(primary);
                if (visual.getOverlayPamAnimationName() != null) {
                    overlayPam = GameplayPamEffectSupport.create(
                            this.assets,
                            this.effectCatalog,
                            visual.getOverlayPamAnimationName(),
                            true,
                            visual.getOverlayPamClip()
                    );
                    if (overlayPam != null) {
                        root.addActor(overlayPam.actor);
                    }
                }
            }
        }

        if (primary == null) {
            try {
                TextureBank bank = this.assets.getTextureBank();
                TextureRegion region = regionOrFallback(bank, visual.getResourceId());
                if (region == null) {
                    return null;
                }
                Image image = new Image(new TextureRegionDrawable(region));
                image.setScaling(Scaling.fit);
                image.setTouchable(Touchable.disabled);
                image.setColor(visual.getTint());
                image.setScale(0.62f);
                image.addAction(Actions.scaleTo(1f, 1f, 0.08f));
                primary = image;
                regionWidth = region.getRegionWidth();
                regionHeight = region.getRegionHeight();
                root.addActor(primary);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        primary.setColor(visual.getTint());
        if (overlayPam != null) {
            overlayPam.actor.setColor(visual.getTint());
        }
        RenderedProjectile rendered = new RenderedProjectile(
                root,
                primary,
                visual,
                regionWidth,
                regionHeight,
                pam,
                overlayPam
        );
        rendered.lastType = projectile == null ? null : projectile.getType();
        if (pam != null && !visual.isPamStartLoops()) {
            rendered.pamStartDuration = pam.duration(0.2f);
        }
        return rendered;
    }

    private TextureRegion regionOrFallback(TextureBank bank, String resourceId) {
        if (bank == null) {
            return null;
        }
        TextureRegion region = bank.region(resourceId);
        if (region != null) {
            return region;
        }
        if (!PEA_RESOURCE.equals(resourceId)) {
            return bank.region(PEA_RESOURCE);
        }
        return null;
    }

    private void advancePamProjectile(RenderedProjectile rendered) {
        if (rendered == null || rendered.pam == null || rendered.pamLoopStarted
                || rendered.visual.getPamLoopClip() == null
                || rendered.visual.isPamStartLoops()
                || rendered.age < rendered.pamStartDuration) {
            return;
        }
        String loop = rendered.visual.getPamLoopClip();
        if (loop.equalsIgnoreCase(rendered.visual.getPamStartClip())) {
            rendered.pam.actor.setLooping(true);
        } else {
            rendered.pam.actor.setAnimation(rendered.pam.animation.getPath(), loop);
            rendered.pam.actor.setLooping(true);
        }
        rendered.pamLoopStarted = true;
    }

    private void spawnImpact(RenderedProjectile rendered) {
        if (rendered == null || !rendered.hasVisualPosition) {
            return;
        }
        if (rendered.visual.usesImpactPam()) {
            if (spawnPamImpact(rendered)) {
                return;
            }
        }
        spawnStaticImpact(rendered);
    }

    private boolean spawnPamImpact(RenderedProjectile rendered) {
        GameplayPamEffectSupport.Effect impact = GameplayPamEffectSupport.create(
                this.assets,
                this.effectCatalog,
                rendered.visual.getImpactPamAnimationName(),
                false,
                rendered.visual.getImpactPamClip()
        );
        if (impact == null) {
            return false;
        }
        float size = Math.min(getWidth(), getHeight()) * 0.15f
                * rendered.visual.getImpactSizeFactor();
        GameplayPamEffectSupport.centerVisibleBounds(
                impact,
                rendered.lastCenterX,
                rendered.lastCenterY,
                size
        );
        impact.actor.setColor(rendered.visual.getTint());
        GameplayBoardDepthOrder.mark(
                impact.actor,
                rendered.lastRow,
                GameplayBoardDepthOrder.PROJECTILE + 1
        );
        this.renderHost.addActor(impact.actor);

        float firstDuration = impact.duration(0.28f);
        String followup = rendered.visual.getImpactPamFollowupClip();
        if (followup == null || followup.isBlank()) {
            impact.actor.addAction(Actions.sequence(
                    Actions.delay(Math.max(0.06f, firstDuration)),
                    Actions.removeActor()
            ));
            return true;
        }

        String nextClip = impact.animation.findClip(followup);
        float secondDuration = impact.animation.getClipDuration(nextClip, 0.28f);
        impact.actor.addAction(Actions.sequence(
                Actions.delay(Math.max(0.06f, firstDuration)),
                Actions.run(() -> {
                    if (nextClip != null) {
                        impact.actor.setAnimation(impact.animation.getPath(), nextClip);
                        impact.actor.setLooping(false);
                    }
                }),
                Actions.delay(Math.max(0.06f, secondDuration)),
                Actions.removeActor()
        ));
        return true;
    }

    private void spawnStaticImpact(RenderedProjectile rendered) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            TextureRegion region = bank == null ? null : bank.region(rendered.visual.getImpactResourceId());
            if (region == null) {
                return;
            }
            Image impact = new Image(new TextureRegionDrawable(region));
            impact.setScaling(Scaling.fit);
            impact.setTouchable(Touchable.disabled);
            impact.setColor(rendered.visual.getTint());
            float size = Math.min(getWidth(), getHeight()) * 0.15f
                    * rendered.visual.getImpactSizeFactor();
            float aspect = region.getRegionHeight() <= 0
                    ? 1f
                    : region.getRegionWidth() / (float) region.getRegionHeight();
            float width = aspect >= 1f ? size : size * aspect;
            float height = aspect >= 1f ? size / aspect : size;
            impact.setBounds(
                    rendered.lastCenterX - width / 2f,
                    rendered.lastCenterY - height / 2f,
                    width,
                    height
            );
            impact.setScale(0.72f);
            impact.setOrigin(width / 2f, height / 2f);
            impact.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.scaleTo(1.12f, 1.12f, 0.11f),
                            Actions.fadeOut(0.16f)
                    ),
                    Actions.removeActor()
            ));
            this.renderHost.addActor(impact);
        } catch (RuntimeException ignored) {
        }
    }

    private void updateProjectileActor(RenderedProjectile rendered, Projectile projectile) {
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        if (rendered.age < rendered.releaseDelay) {
            rendered.root.setVisible(false);
            return;
        }
        rendered.root.setVisible(true);

        float modelCenterX = (float) ((projectile.getExactX() + 0.5d) * cellWidth);
        float modelCenterY = (
                GameplayBoardInteractionLayer.ROW_COUNT - 0.5f - (float) projectile.getExactY()
        ) * cellHeight + lobHeight(projectile, cellHeight);
        modelCenterY += projectileVisualAnchorShift(projectile, cellHeight);

        float centerX = modelCenterX;
        float centerY = modelCenterY;
        float sinceRelease = rendered.age - rendered.releaseDelay;
        if (rendered.hasLaunchPoint) {
            // Phase 1 may already have advanced the projectile before the matching
            // animation reaches its release frame. Start exactly at the PAM
            // mouth/nozzle/basket and blend rapidly back onto the live model path.
            // Never blend backwards into the tile centre when the model has not
            // advanced yet (this was the visible "pea on the forehead" bug).
            float targetX = modelCenterX;
            float targetY = modelCenterY;
            double horizontal = projectile.getHorizontalDirection();
            if (horizontal > 0d) {
                targetX = Math.max(targetX, rendered.launchX);
            } else if (horizontal < 0d) {
                targetX = Math.min(targetX, rendered.launchX);
            } else {
                double sceneVertical = -projectile.getVerticalDirection();
                if (sceneVertical > 0d) {
                    targetY = Math.max(targetY, rendered.launchY);
                } else if (sceneVertical < 0d) {
                    targetY = Math.min(targetY, rendered.launchY);
                }
            }
            float catchUpSeconds = projectile.isLobbed() ? 0.22f : 0.18f;
            float t = clamp(sinceRelease / catchUpSeconds, 0f, 1f);
            float smooth = t * t * (3f - 2f * t);
            centerX = lerp(rendered.launchX, targetX, smooth);
            centerY = lerp(rendered.launchY, targetY, smooth);
        }

        float burstOffset = volleyVisualOffset(rendered, projectile, cellWidth);
        centerX += burstOffset;

        float targetSize = Math.min(cellWidth, cellHeight) * rendered.visual.getSizeFactor();
        if (rendered.pam != null) {
            GameplayPamEffectSupport.centerVisibleBounds(rendered.pam, centerX, centerY, targetSize);
            rendered.pam.actor.setColor(rendered.visual.getTint());
            if (rendered.overlayPam != null) {
                GameplayPamEffectSupport.centerVisibleBounds(
                        rendered.overlayPam,
                        centerX,
                        centerY,
                        targetSize * rendered.visual.getOverlaySizeMultiplier()
                );
                rendered.overlayPam.actor.setColor(rendered.visual.getTint());
            }
        } else {
            float aspect = rendered.regionHeight <= 0
                    ? 1f
                    : rendered.regionWidth / (float) rendered.regionHeight;
            float width = aspect >= 1f ? targetSize : targetSize * aspect;
            float height = aspect >= 1f ? targetSize / aspect : targetSize;
            rendered.primary.setBounds(centerX - width / 2f, centerY - height / 2f, width, height);
            rendered.primary.setColor(rendered.visual.getTint());
        }

        if (rendered.visual.shouldRotateToDirection()) {
            float sceneDx = projectile.getHorizontalDirection();
            float sceneDy = -projectile.getVerticalDirection();
            float degrees = (float) Math.toDegrees(Math.atan2(sceneDy, sceneDx));
            setActorRotation(rendered.primary, degrees);
            if (rendered.overlayPam != null) {
                setActorRotation(rendered.overlayPam.actor, degrees);
            }
        }

        rendered.lastCenterX = centerX;
        rendered.lastCenterY = centerY;
        rendered.lastRow = (int) Math.round(projectile.getExactY());
        rendered.hasVisualPosition = true;
        maybeSpawnTorchwoodTransform(rendered, projectile, centerX, centerY, cellHeight);
    }

    private float volleyVisualOffset(
            RenderedProjectile rendered,
            Projectile projectile,
            float cellWidth
    ) {
        if (rendered == null || projectile == null || rendered.volleySize <= 1) {
            return 0f;
        }
        float centeredIndex = rendered.volleyIndex - (rendered.volleySize - 1) * 0.5f;
        String name = projectile.getSourcePlant() == null || projectile.getSourcePlant().getName() == null
                ? ""
                : projectile.getSourcePlant().getName().toLowerCase(Locale.ROOT);
        float spacingFactor = name.contains("mega gatling") ? 0.062f : 0.10f;
        float direction = projectile.getHorizontalDirection() < 0 ? -1f : 1f;
        return centeredIndex * spacingFactor * cellWidth * direction;
    }

    private void maybeSpawnTorchwoodTransform(
            RenderedProjectile rendered,
            Projectile projectile,
            float centerX,
            float centerY,
            float cellHeight
    ) {
        ProjectileType current = projectile == null ? null : projectile.getType();
        ProjectileType previous = rendered.lastType;
        rendered.lastType = current;
        if (current != ProjectileType.FIRE || previous == ProjectileType.FIRE
                || projectile == null || projectile.getSourcePlant() == null) {
            return;
        }
        String source = projectile.getSourcePlant().getName() == null
                ? ""
                : projectile.getSourcePlant().getName().toLowerCase(Locale.ROOT);
        if (source.contains("fire peashooter")) {
            return;
        }
        int damage = DamageExpressionParser.parseTotalDamage(projectile.getDamageExpression());
        String clip = damage >= 60 ? "hit_power" : "hit_normal";
        GameplayPamEffectSupport.Effect effect = GameplayPamEffectSupport.create(
                this.assets,
                this.effectCatalog,
                "TORCHWOOD_HIT_EFFECTS",
                false,
                clip
        );
        if (effect == null) {
            return;
        }
        GameplayPamEffectSupport.centerVisibleBounds(effect, centerX, centerY, cellHeight * 0.55f);
        GameplayBoardDepthOrder.mark(effect.actor, rendered.lastRow, GameplayBoardDepthOrder.PROJECTILE + 1);
        this.renderHost.addActor(effect.actor);
        effect.actor.addAction(Actions.sequence(
                Actions.delay(Math.max(0.08f, effect.duration(0.24f))),
                Actions.removeActor()
        ));
    }

    private void setActorRotation(Actor actor, float degrees) {
        if (actor == null) {
            return;
        }
        actor.setOrigin(actor.getWidth() * 0.5f, actor.getHeight() * 0.5f);
        actor.setRotation(degrees);
    }

    private float projectileVisualAnchorShift(Projectile projectile, float cellHeight) {
        if (projectile == null) {
            return 0f;
        }
        if (projectile.getSourcePlant() != null) {
            return cellHeight * (
                    GameplayWorldLayout.PLANT_GROUND_ANCHOR_FACTOR
                            - PRE_ANCHOR_PLANT_BOTTOM_FACTOR
            );
        }
        if (projectile.isHostileToPlants()) {
            return cellHeight * (
                    GameplayWorldLayout.ZOMBIE_GROUND_ANCHOR_FACTOR
                            - PRE_ANCHOR_ZOMBIE_BOTTOM_FACTOR
            );
        }
        return 0f;
    }

    private float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private float lobHeight(Projectile projectile, float cellHeight) {
        if (!projectile.isLobbed()) {
            return 0f;
        }
        double span;
        if (projectile.getTarget() != null) {
            span = Math.abs(projectile.getTarget().getExactX() - projectile.getOriginX());
        } else {
            span = Math.max(1d, projectile.getMaxRange());
        }
        double progress = Math.abs(projectile.getExactX() - projectile.getOriginX())
                / Math.max(0.001d, span);
        progress = Math.max(0d, Math.min(1d, progress));
        return (float) (4d * progress * (1d - progress) * cellHeight * 1.45d);
    }

    private static final class RenderedProjectile {
        private final Group root;
        private final Actor primary;
        private final GameplayProjectileVisualCatalog.Visual visual;
        private final int regionWidth;
        private final int regionHeight;
        private final GameplayPamEffectSupport.Effect pam;
        private final GameplayPamEffectSupport.Effect overlayPam;
        private float age;
        private float releaseDelay;
        private float launchX;
        private float launchY;
        private boolean hasLaunchPoint;
        private float pamStartDuration;
        private boolean pamLoopStarted;
        private float lastCenterX;
        private float lastCenterY;
        private int lastRow;
        private boolean hasVisualPosition;
        private ProjectileType lastType;
        private int volleyIndex;
        private int volleySize = 1;

        private RenderedProjectile(
                Group root,
                Actor primary,
                GameplayProjectileVisualCatalog.Visual visual,
                int regionWidth,
                int regionHeight,
                GameplayPamEffectSupport.Effect pam,
                GameplayPamEffectSupport.Effect overlayPam
        ) {
            this.root = root;
            this.primary = primary;
            this.visual = visual;
            this.regionWidth = regionWidth;
            this.regionHeight = regionHeight;
            this.pam = pam;
            this.overlayPam = overlayPam;
        }
    }
}
