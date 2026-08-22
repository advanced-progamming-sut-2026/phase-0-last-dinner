package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.Plant;
import model.mechanism.Position;
import model.plant.PlantTag;
import model.plant.Projectile;
import model.plant.behavior.ExplosiveBehavior;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.badlogic.gdx.math.Interpolation;

/** Renders planted plants, stacking and required cover/freeze state on board. */
public final class GameplayPlantLayer extends Group {
    private static final float STATIC_WIDTH_FACTOR = 0.78f;
    private static final float STATIC_HEIGHT_FACTOR = 0.86f;
    private static final float STATIC_BOTTOM_FACTOR = 0.045f;
    private static final String CHILL_ONE =
            "IMAGE_EFFECTS_FROSTBITE_CHILL_PLANT_FROSTBITE_CHILL_PLANT_153X62";
    private static final String CHILL_TWO =
            "IMAGE_EFFECTS_FROSTBITE_CHILL_PLANT_FROSTBITE_CHILL_PLANT_153X79";
    private static final String ICE_BEHIND =
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_BEHIND_FROSTBITE_ICE_BLOCK_PLANT_BEHIND_164X171";
    private static final String ICE_FRONT =
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_FROSTBITE_ICE_BLOCK_PLANT_167X172_5";
    private static final String OCTOPUS_FACE =
            "IMAGE_EFFECTS_ZOMBIE_OCTOPUS_PROJECTILE_ZOMBIE_OCTOPUS_PROJECTILE_87X61";
    private static final String OCTOPUS_TENTACLE =
            "IMAGE_EFFECTS_ZOMBIE_OCTOPUS_PROJECTILE_ZOMBIE_OCTOPUS_PROJECTILE_110X45";
    private static final String PLANT_FOOD_GLOW =
            "IMAGE_EFFECTS_PLANTFOOD_FX_PLANTFOOD_FX_164X156";
    private static final String PLANT_SHADOW = "IMAGE_PLANTSHADOW";
    private static final String SHEEP_WOOL =
            "IMAGE_EFFECTS_DARK_WIZARD_SHEEPENING_DARK_WIZARD_SHEEPENING_117X146";
    private static final String SHEEP_FACE =
            "IMAGE_EFFECTS_DARK_WIZARD_SHEEPENING_DARK_WIZARD_SHEEPENING_36X37";
    private static final String SHEEP_LEG =
            "IMAGE_EFFECTS_DARK_WIZARD_SHEEPENING_DARK_WIZARD_SHEEPENING_9X25";
    private static final float PLANT_FOOD_ANIMATION_SECONDS = 1.35f;
    private static final float ATTACK_ANIMATION_SECONDS = 0.48f;
    private static final float SUN_PRODUCTION_ANIMATION_SECONDS = 0.72f;
    private static final float INTRO_ANIMATION_SECONDS = 0.72f;
    private static final float DAMAGE_FLASH_SECONDS = 0.12f;
    private static final float EXPLOSION_SECONDS = 0.34f;
    private static final Color DAMAGE_FLASH = new Color(1f, 0.72f, 0.62f, 1f);
    private static final String EXPLOSION_OUTER =
            "IMAGE_EFFECTS_PRIMAL_POTATOMINE_EXPLOSION_PRIMAL_POTATOMINE_EXPLOSION_238X226";
    private static final String EXPLOSION_MIDDLE =
            "IMAGE_EFFECTS_PRIMAL_POTATOMINE_EXPLOSION_PRIMAL_POTATOMINE_EXPLOSION_220X209";
    private static final String EXPLOSION_INNER =
            "IMAGE_EFFECTS_PRIMAL_POTATOMINE_EXPLOSION_PRIMAL_POTATOMINE_EXPLOSION_184X176";

    /*
     * Projectile launch geometry is intentionally kept in the graphics layer.
     * Phase 1 stores projectile positions at tile centres, while the original
     * PvZ2 art fires from a mouth/nozzle/basket that is often far away from the
     * tile centre.  These profiles bridge that difference without changing the
     * model.  Whenever possible the anchor comes directly from the PAM part
     * geometry at the release frame; the normalized cell point is only a safe
     * fallback for partial asset packs or plants whose attack has no useful
     * named part.
     */
    private static final ProjectileLaunchProfile DEFAULT_LAUNCH =
            ProjectileLaunchProfile.cell(0.90f, 0.56f, 0.40f);

    private final GameplaySeedBankDataSource seedDataSource;
    private final GameplayWorldDataSource worldDataSource;
    private final GameAssetManager assets;
    private Group renderHost;
    private boolean ownsAssets;
    private final PamAnimationCatalog animationCatalog;
    private final Map<Plant, RenderedPlant> actors = new IdentityHashMap<>();
    private final Map<String, ProjectileLaunchSample> projectileLaunchSamples = new HashMap<>();
    private final Set<Integer> suppressedRemovalCells = new HashSet<>();
    private final Set<Integer> pendingIntroCells = new HashSet<>();
    private final Set<Integer> pendingPlantFoodCells = new HashSet<>();
    private Runnable explosionListener;

    private boolean animateNextBoardTransition;

    public GameplayPlantLayer(GameplaySeedBankDataSource dataSource) {
        this(dataSource, new GameplayWorldDataSource() { });
    }

    public GameplayPlantLayer(
            GameplaySeedBankDataSource seedDataSource,
            GameplayWorldDataSource worldDataSource
    ) {
        this(seedDataSource, worldDataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayPlantLayer(
            GameplaySeedBankDataSource seedDataSource,
            GameplayWorldDataSource worldDataSource,
            GameAssetManager assets
    ) {
        if (seedDataSource == null || worldDataSource == null || assets == null) {
            throw new IllegalArgumentException("Gameplay plant dependencies are required");
        }
        this.seedDataSource = seedDataSource;
        this.worldDataSource = worldDataSource;
        this.assets = assets;
        this.renderHost = this;
        this.animationCatalog = new PamAnimationCatalog();
        preloadEffectTextures();
        setTouchable(Touchable.disabled);
    }

    private void preloadEffectTextures() {
        TextureBank bank = this.assets.getTextureBank();
        if (bank == null) {
            return;
        }
        try {
            bank.region(EXPLOSION_OUTER);
            bank.region(EXPLOSION_MIDDLE);
            bank.region(EXPLOSION_INNER);
        } catch (RuntimeException ignored) {
            // optional effect art can fall back without blocking gameplay
        }
    }

    public void animateNextBoardTransition() {
        this.animateNextBoardTransition = true;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        syncPlants(Math.max(0f, delta));
    }

    public int getRenderedPlantCount() {
        return this.actors.size();
    }

    public boolean playAttack(Plant plant) {
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered != null && isBowlingBulb(plant.getName())) {
            return playBowlingAttack(rendered);
        }
        return playTemporaryAnimation(plant, AnimationKind.ATTACK);
    }

    /** Starts the attack clip that visually matches the projectile that was just created. */
    public boolean playAttack(Projectile projectile) {
        Plant plant = projectile == null ? null : projectile.getSourcePlant();
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered == null || rendered.plantFoodRemaining > 0f) {
            return false;
        }
        if (isBowlingBulb(plant.getName())) {
            return playBowlingAttack(rendered);
        }
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            playFallbackMotion(rendered, AnimationKind.ATTACK);
            return false;
        }
        String clip = attackClipForProjectile(rendered, projectile);
        if (clip == null) {
            playFallbackMotion(rendered, AnimationKind.ATTACK);
            return false;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);

        float duration = rendered.animation.getClipDuration(clip, ATTACK_ANIMATION_SECONDS);
        float releaseFraction = resolvedReleaseFraction(rendered.animation, clip,
                projectileLaunchProfile(plant, projectile));
        float releaseTime = duration * releaseFraction;
        float releaseDelay = visualReleaseDelay(projectile, releaseTime);
        float startTime = Math.max(0f, releaseTime - releaseDelay);
        // The Phase 1 model creates/moves its projectile immediately. Starting
        // the visual attack a little into the source clip lets the authentic
        // PAM release frame line up with the projectile without delaying the
        // model by half a second.
        actor.setStateTime(startTime);
        rendered.temporaryAnimationRemaining = Math.max(0.08f, duration - startTime);
        if (normalize(plant.getName()).equals("rotobaga")) {
            Vector2 launch = getProjectileLaunchPoint(projectile);
            if (launch != null) {
                spawnPamEffect(
                        "ROTORUTABAGA_MUZZLE_BURST",
                        "animation",
                        launch.x,
                        launch.y,
                        Math.min(getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT,
                                getHeight() / GameplayBoardInteractionLayer.ROW_COUNT) * 0.54f,
                        rendered.lastRow,
                        0f,
                        false
                );
            }
        }
        return true;
    }

    public float getAttackReleaseDelay(Plant plant) {
        return getAttackReleaseDelay(plant, null);
    }

    public float getAttackReleaseDelay(Projectile projectile) {
        Plant plant = projectile == null ? null : projectile.getSourcePlant();
        return getAttackReleaseDelay(plant, projectile);
    }

    /**
     * Returns the projectile centre in lawn-local coordinates at the visual
     * moment it leaves the plant.  This is deliberately independent of the
     * Phase 1 projectile coordinate, which starts at the tile centre.
     */
    public Vector2 getProjectileLaunchPoint(Projectile projectile) {
        Plant plant = projectile == null ? null : projectile.getSourcePlant();
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered == null || plant.getPosition() == null) {
            return null;
        }

        ProjectileLaunchProfile profile = projectileLaunchProfile(plant, projectile);
        if (profile.directional) {
            return directionalLaunchPoint(rendered, plant, projectile, profile);
        }

        if (rendered.body instanceof PamAnimationActor && rendered.animation != null
                && profile.partCandidates.length > 0) {
            PamAnimationActor actor = (PamAnimationActor) rendered.body;
            String clip = actor.getClipName();
            ProjectileLaunchSample sample = launchSample(rendered.animation, clip, profile);
            if (sample != null && sample.bounds != null) {
                float baseScale = Math.min(
                        rendered.body.getWidth() / rendered.animation.getCanvasWidth(),
                        rendered.body.getHeight() / rendered.animation.getCanvasHeight()
                );
                float sourceX = sample.bounds.x + sample.bounds.width * profile.partX;
                float sourceY = sample.bounds.y + sample.bounds.height * profile.partY;
                float localX = rendered.root.getX()
                        + rendered.body.getX() + rendered.body.getWidth() * 0.5f
                        + sourceX * baseScale;
                // PAM authoring coordinates grow downward, Scene2D grows upward.
                float localY = rendered.root.getY()
                        + rendered.body.getY() + rendered.body.getHeight() * 0.5f
                        - sourceY * baseScale;
                return clampLaunchPoint(rendered, localX, localY);
            }
        }
        return fallbackLaunchPoint(rendered, plant, projectile, profile);
    }

    private float getAttackReleaseDelay(Plant plant, Projectile projectile) {
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered == null) {
            return 0.10f;
        }
        float duration = ATTACK_ANIMATION_SECONDS;
        String clip = null;
        if (rendered.body instanceof PamAnimationActor && rendered.animation != null) {
            clip = ((PamAnimationActor) rendered.body).getClipName();
            duration = rendered.animation.getClipDuration(clip, ATTACK_ANIMATION_SECONDS);
        }
        ProjectileLaunchProfile profile = projectileLaunchProfile(plant, projectile);
        float releaseTime = duration * resolvedReleaseFraction(rendered.animation, clip, profile);
        return visualReleaseDelay(projectile, releaseTime);
    }

    private float resolvedReleaseFraction(
            PamAnimationCatalog.AnimationInfo animation,
            String clip,
            ProjectileLaunchProfile profile
    ) {
        float fraction = profile == null ? 0.40f : profile.releaseFraction;
        if (animation != null && clip != null && profile != null
                && profile.partCandidates.length > 0) {
            ProjectileLaunchSample sample = launchSample(animation, clip, profile);
            if (sample != null && sample.releaseFraction >= 0f) {
                fraction = sample.releaseFraction;
            }
        }
        return clamp(fraction, 0.08f, 0.88f);
    }

    private float visualReleaseDelay(Projectile projectile, float originalReleaseTime) {
        // A long graphics-only delay would let the fast Phase 1 projectile cross
        // several tiles invisibly. Keep the delay short and seek the PAM clip so
        // the release still occurs on the authentic mouth/nozzle/basket frame.
        float cap = projectile != null && projectile.isLobbed() ? 0.20f : 0.16f;
        return Math.max(0.04f, Math.min(cap, Math.max(0f, originalReleaseTime)));
    }

    private String attackClipForProjectile(RenderedPlant rendered, Projectile projectile) {
        if (rendered == null || rendered.animation == null) {
            return null;
        }
        String name = normalize(projectile == null || projectile.getSourcePlant() == null
                ? null : projectile.getSourcePlant().getName());

        if (name.contains("kernel-pult") || name.contains("kernel pult")) {
            if (projectile != null && projectile.getStunChancePercent() >= 100) {
                String butter = rendered.animation.findClip("attack2", "attack 2");
                if (butter != null) {
                    return butter;
                }
            }
        }

        if (name.contains("split pea") && projectile != null
                && projectile.getHorizontalDirection() < 0) {
            String backward = rendered.animation.findClip("attack2", "attack 2", "attack3");
            if (backward != null) {
                return backward;
            }
        }

        if (name.contains("puff-shroom")) {
            String puffAttack = rendered.animation.findClip(
                    "special_stage1", "special_stage2", "special_stage3", "special"
            );
            if (puffAttack != null) {
                return puffAttack;
            }
        }

        if (name.contains("pea pod")) {
            int stackCount = countSamePlantAt(projectile == null ? null : projectile.getSourcePlant());
            if (stackCount > 1) {
                String stacked = rendered.animation.findClip(
                        "attack " + Math.min(5, stackCount),
                        "attack" + Math.min(5, stackCount)
                );
                if (stacked != null) {
                    return stacked;
                }
            }
        }
        return rendered.animation.getAttackClip();
    }

    private int countSamePlantAt(Plant source) {
        if (source == null || source.getPosition() == null || source.getName() == null) {
            return 1;
        }
        int count = 0;
        for (Plant candidate : this.seedDataSource.getPlantsOnBoard()) {
            if (candidate == null || candidate.getPosition() == null || candidate.getName() == null) {
                continue;
            }
            if (candidate.getPosition().equals(source.getPosition())
                    && candidate.getName().equalsIgnoreCase(source.getName())) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private ProjectileLaunchProfile projectileLaunchProfile(Plant plant, Projectile projectile) {
        String name = normalize(plant == null ? null : plant.getName());
        ProjectileLaunchProfile profile = peaFamilyLaunchProfile(name, projectile);
        if (profile != null) {
            return profile;
        }
        profile = specialShooterLaunchProfile(name);
        if (profile != null) {
            return profile;
        }
        profile = lobberLaunchProfile(name);
        if (profile != null) {
            return profile;
        }
        return utilityLaunchProfile(name);
    }

    private ProjectileLaunchProfile peaFamilyLaunchProfile(
            String name,
            Projectile projectile
    ) {
        if (name.equals("peashooter") || name.equals("repeater")
                || name.equals("fire peashooter") || name.equals("pea pod")) {
            return ProjectileLaunchProfile.transientPart(
                    1.00f, 0.50f, 0.53f, "peashooter_spit", "threepeater_spit"
            );
        }
        if (name.equals("snow pea")) {
            return ProjectileLaunchProfile.transientPart(
                    1.00f, 0.50f, 0.53f, "snowpea_spit"
            );
        }
        if (name.equals("threepeater")) {
            return ProjectileLaunchProfile.directional(1.03f, 0.56f, 0.50f);
        }
        if (name.equals("split pea")) {
            if (projectile != null && projectile.getHorizontalDirection() < 0) {
                return ProjectileLaunchProfile.directional(0.02f, 0.57f, 0.52f);
            }
            return ProjectileLaunchProfile.transientPart(
                    1.00f, 0.50f, 0.53f, "peashooter_spit"
            );
        }
        if (name.equals("mega gatling pea")) {
            return ProjectileLaunchProfile.transientPart(
                    1.00f, 0.50f, 0.48f,
                    "peashooter_spit", "gatlingpea_muzzle_blast", "mgp_mouth_gun_attack"
            );
        }
        return extraPeaFamilyLaunchProfile(name);
    }

    private ProjectileLaunchProfile extraPeaFamilyLaunchProfile(String name) {
        if (name.equals("goo peashooter")) {
            return ProjectileLaunchProfile.fixedPart(
                    1.00f, 0.50f, 0.51f, 0.96f, 0.56f,
                    "peashooter_lips", "peashooter_mouth"
            );
        }
        if (name.equals("cactus")) {
            return ProjectileLaunchProfile.transientPart(
                    1.00f, 0.50f, 0.50f, "peashooter_spit", "spike_light_attack_base"
            );
        }
        return null;
    }

    private ProjectileLaunchProfile specialShooterLaunchProfile(String name) {
        if (name.equals("fume-shroom")) {
            return ProjectileLaunchProfile.fixedPart(
                    1.00f, 0.50f, 0.35f, 0.94f, 0.58f,
                    "Fume_Nozzle_Hole", "Fume_Nozzle"
            );
        }
        if (name.equals("puff-shroom")) {
            return ProjectileLaunchProfile.fixedPart(
                    1.00f, 0.50f, 0.38f, 0.88f, 0.50f,
                    "Puffshroom_Mouth", "Puffshroom_Lips"
            );
        }
        if (name.equals("sea-shroom")) {
            return ProjectileLaunchProfile.cell(0.86f, 0.50f, 0.38f);
        }
        if (name.equals("citron")) {
            return ProjectileLaunchProfile.fixedPart(
                    1.00f, 0.50f, 0.58f, 0.94f, 0.61f,
                    "Citron_Mouth", "Citron_Mouth_Charge"
            );
        }
        return directionalOrHomingLaunchProfile(name);
    }

    private ProjectileLaunchProfile directionalOrHomingLaunchProfile(String name) {
        if (name.equals("starfruit")) {
            return ProjectileLaunchProfile.directional(0.52f, 0.56f, 0.42f);
        }
        if (name.equals("rotobaga")) {
            return ProjectileLaunchProfile.directional(0.52f, 0.57f, 0.40f);
        }
        if (name.equals("bowling bulb")) {
            return ProjectileLaunchProfile.cell(0.72f, 0.26f, 0.43f);
        }
        if (name.equals("caulipower")) {
            return ProjectileLaunchProfile.cell(0.62f, 0.76f, 0.48f);
        }
        if (name.equals("electric blueberry")) {
            return ProjectileLaunchProfile.cell(0.56f, 0.78f, 0.48f);
        }
        return null;
    }

    private ProjectileLaunchProfile lobberLaunchProfile(String name) {
        if (name.equals("cabbage-pult")) {
            return ProjectileLaunchProfile.fixedPart(
                    0.50f, 0.50f, 0.36f, 0.88f, 0.78f, "cabbage_projectile"
            );
        }
        if (name.equals("kernel-pult")) {
            return ProjectileLaunchProfile.fixedPart(
                    0.58f, 0.42f, 0.36f, 0.88f, 0.80f,
                    "kernelpult_basket_front", "kernelpult_pult_basketback"
            );
        }
        if (name.equals("melon-pult") || name.equals("winter melon")) {
            return ProjectileLaunchProfile.fixedPart(
                    0.50f, 0.50f, 0.35f, 0.86f, 0.82f, "melonpult_projectile"
            );
        }
        if (name.equals("pepper-pult")) {
            return ProjectileLaunchProfile.transientPart(
                    0.55f, 0.50f, 0.38f, "cannon_smoke_cloud"
            ).withFallback(0.90f, 0.80f);
        }
        return null;
    }

    private ProjectileLaunchProfile utilityLaunchProfile(String name) {
        if (name.contains("mint")) {
            return ProjectileLaunchProfile.cell(0.74f, 0.68f, 0.36f);
        }
        if (name.contains("cat-tail") || name.contains("cattail")) {
            return ProjectileLaunchProfile.cell(0.78f, 0.68f, 0.42f);
        }
        if (name.contains("magnet-shroom")) {
            return ProjectileLaunchProfile.cell(0.60f, 0.72f, 0.42f);
        }
        return DEFAULT_LAUNCH;
    }

    private ProjectileLaunchSample launchSample(
            PamAnimationCatalog.AnimationInfo animation,
            String clip,
            ProjectileLaunchProfile profile
    ) {
        if (animation == null || clip == null || profile == null || profile.partCandidates.length == 0) {
            return null;
        }
        String key = animation.getPath() + "|" + clip + "|"
                + String.join(",", profile.partCandidates) + "|"
                + profile.releaseFraction + "|" + profile.transientPart;
        if (this.projectileLaunchSamples.containsKey(key)) {
            ProjectileLaunchSample cached = this.projectileLaunchSamples.get(key);
            return cached == ProjectileLaunchSample.MISSING ? null : cached;
        }

        for (String partName : profile.partCandidates) {
            Rectangle[] frames = PamPartGeometry.partBoundsByFrame(
                    this.assets.getPamPlayer(),
                    animation.getPath(),
                    clip,
                    partName
            );
            if (frames.length == 0) {
                continue;
            }
            ProjectileLaunchSample sample = profile.transientPart
                    ? firstVisibleSample(frames)
                    : fixedFrameSample(frames, profile.releaseFraction);
            if (sample != null) {
                this.projectileLaunchSamples.put(key, sample);
                return sample;
            }
        }
        this.projectileLaunchSamples.put(key, ProjectileLaunchSample.MISSING);
        return null;
    }

    private ProjectileLaunchSample firstVisibleSample(Rectangle[] frames) {
        for (int index = 0; index < frames.length; index++) {
            Rectangle rectangle = frames[index];
            if (rectangle == null || rectangle.width <= 0f || rectangle.height <= 0f) {
                continue;
            }
            float fraction = frames.length <= 1 ? 0f : index / (float) (frames.length - 1);
            return new ProjectileLaunchSample(new Rectangle(rectangle), fraction);
        }
        return null;
    }

    private ProjectileLaunchSample fixedFrameSample(Rectangle[] frames, float fraction) {
        if (frames.length == 0) {
            return null;
        }
        int wanted = Math.round(clamp(fraction, 0f, 1f) * (frames.length - 1));
        for (int radius = 0; radius < frames.length; radius++) {
            int before = wanted - radius;
            if (before >= 0 && frames[before] != null
                    && frames[before].width > 0f && frames[before].height > 0f) {
                return new ProjectileLaunchSample(new Rectangle(frames[before]), fraction);
            }
            int after = wanted + radius;
            if (after < frames.length && frames[after] != null
                    && frames[after].width > 0f && frames[after].height > 0f) {
                return new ProjectileLaunchSample(new Rectangle(frames[after]), fraction);
            }
        }
        return null;
    }

    private Vector2 directionalLaunchPoint(
            RenderedPlant rendered,
            Plant plant,
            Projectile projectile,
            ProjectileLaunchProfile profile
    ) {
        float x = profile.fallbackX;
        float y = profile.fallbackY;
        String name = normalize(plant.getName());

        if (name.equals("threepeater")) {
            int laneDelta = projectile == null
                    ? 0
                    : (int) Math.round(projectile.getOriginY() - plant.getPosition().getY());
            y = laneDelta < 0 ? 0.80f : laneDelta > 0 ? 0.36f : 0.58f;
            x = 1.02f;
        } else if (name.equals("split pea") && projectile != null
                && projectile.getHorizontalDirection() < 0) {
            x = 0.00f;
            y = 0.57f;
        } else if ((name.equals("starfruit") || name.equals("rotobaga")) && projectile != null) {
            float dx = Math.signum(projectile.getHorizontalDirection());
            // Model Y grows downward while Scene2D Y grows upward.
            float dy = -Math.signum(projectile.getVerticalDirection());
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length > 0f) {
                dx /= length;
                dy /= length;
                float radiusX = name.equals("starfruit") ? 0.43f : 0.37f;
                float radiusY = name.equals("starfruit") ? 0.34f : 0.31f;
                x = 0.52f + dx * radiusX;
                y = 0.56f + dy * radiusY;
            }
        }
        return cellPoint(rendered, x, y);
    }

    private Vector2 fallbackLaunchPoint(
            RenderedPlant rendered,
            Plant plant,
            Projectile projectile,
            ProjectileLaunchProfile profile
    ) {
        float x = profile.fallbackX;
        float y = profile.fallbackY;
        if (projectile != null && projectile.getHorizontalDirection() < 0
                && !normalize(plant.getName()).contains("split pea")) {
            x = 1f - x;
        }
        return cellPoint(rendered, x, y);
    }

    private Vector2 cellPoint(RenderedPlant rendered, float xFactor, float yFactor) {
        return new Vector2(
                rendered.root.getX() + rendered.root.getWidth() * xFactor,
                rendered.root.getY() + rendered.root.getHeight() * yFactor
        );
    }

    private Vector2 clampLaunchPoint(RenderedPlant rendered, float x, float y) {
        float minX = rendered.root.getX() - rendered.root.getWidth() * 0.20f;
        float maxX = rendered.root.getX() + rendered.root.getWidth() * 1.30f;
        float minY = rendered.root.getY() - rendered.root.getHeight() * 0.08f;
        float maxY = rendered.root.getY() + rendered.root.getHeight() * 1.28f;
        return new Vector2(clamp(x, minX, maxX), clamp(y, minY, maxY));
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public boolean playSunProduction(Plant plant) {
        return playTemporaryAnimation(plant, AnimationKind.SUN_PRODUCTION);
    }

    public boolean playMagnetCatch(Plant plant) {
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered == null || rendered.plantFoodRemaining > 0f
                || !(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            return false;
        }
        String clip = rendered.animation.findClip("catch", "special", "busy");
        if (clip == null) {
            return false;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);
        rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(clip, 0.82f);
        return true;
    }

    public boolean playIntro(Plant plant) {
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered != null && isPotatoMine(plant.getName())) {
            rendered.mineArmRemaining = mineArmSeconds(plant);
            rendered.mineRecoverPlayed = false;
        }
        return playTemporaryAnimation(plant, AnimationKind.INTRO);
    }

    void setExplosionListener(Runnable listener) {
        this.explosionListener = listener;
    }

    void suppressRemovalEffectAt(int column, int row) {
        this.suppressedRemovalCells.add(cellKey(column, row));
    }

    void playIntroAt(int column, int row) {
        this.pendingIntroCells.add(cellKey(column, row));
    }

    public boolean playPlantFoodAt(int column, int row) {
        Plant plant = this.seedDataSource.getTopPlantAt(column, row);
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered == null) {
            this.pendingPlantFoodCells.add(cellKey(column, row));
            return false;
        }
        showPlantFoodGlow(rendered);
        playAuthenticPlantFoodEffect(rendered, plant);
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            rendered.body.clearActions();
            rendered.body.setOrigin(
                    rendered.body.getWidth() / 2f,
                    rendered.body.getHeight() / 2f
            );
            rendered.body.addAction(Actions.sequence(
                    Actions.scaleTo(1.10f, 1.10f, 0.10f),
                    Actions.scaleTo(1f, 1f, 0.16f)
            ));
            return false;
        }
        String clip = rendered.animation.getPlantFoodClip();
        if (clip == null) {
            return false;
        }
        if (isPotatoMine(plant.getName())) {
            rendered.mineArmRemaining = 0f;
            rendered.mineRecoverPlayed = true;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);
        rendered.plantFoodRemaining = rendered.animation.getClipDuration(
                clip,
                PLANT_FOOD_ANIMATION_SECONDS
        );
        return true;
    }

    void setRenderHost(Group renderHost) {
        this.renderHost = renderHost == null ? this : renderHost;
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void syncPlants(float delta) {
        List<Plant> plants = new ArrayList<>(this.seedDataSource.getPlantsOnBoard());
        boolean animateTransition = this.animateNextBoardTransition;

        removeMissing(plants);

        for (Plant plant : plants) {
            if (plant == null || plant.getPosition() == null)
                continue;

            RenderedPlant rendered = this.actors.get(plant);
            boolean newlyCreated = rendered == null;

            if (newlyCreated) {
                rendered = createRenderedPlant(plant.getName());

                if (rendered == null) {
                    continue;
                }
                rendered.firstSeenTick = this.worldDataSource.getCurrentTick();
                rendered.meleeVisualElapsed = 0f;
                this.actors.put(plant, rendered);
                this.renderHost.addActor(rendered.root);
            }

            Position position = plant.getPosition();
            boolean positionChanged = rendered.lastColumn != position.getX() || rendered.lastRow != position.getY();

            if (newlyCreated) {
                positionPlantActor(rendered.root, position);

                if (animateTransition)
                    animatePlantFromTop(rendered.root);
            } else if (positionChanged) {
                if (animateTransition)
                    animatePlantMovement(rendered.root, position);
                else
                    positionPlantActor(rendered.root, position);
            }

            rendered.lastColumn = position.getX();
            rendered.lastRow = position.getY();

            int cellKey = cellKey(rendered.lastColumn, rendered.lastRow);

            if (this.pendingIntroCells.remove(cellKey))
                playIntro(plant);

            if (this.pendingPlantFoodCells.remove(cellKey))
                playPlantFoodAt(rendered.lastColumn, rendered.lastRow);

            GameplayBoardDepthOrder.mark(rendered.root, position.getY(), depthPriority(plant.getName()));

            advancePlantFoodAnimation(rendered, delta);
            advanceTemporaryAnimation(rendered, delta);
            advanceBowlingRecharge(rendered, delta);
            advanceMineArming(rendered, plant, delta);
            updateDamageFlash(rendered, plant, delta);
            updateDamageStage(rendered, plant);
            syncSpecialPlantVisuals(rendered, plant, delta);
            layoutRenderedPlant(rendered, plant.getName());
            updateTransformation(rendered, plant);
            updateCover(rendered, plant);
        }

        this.animateNextBoardTransition = false;
        applyBoardDepthOrder(plants);
    }

    private void removeMissing(List<Plant> plants) {
        List<Plant> removedPlants = new ArrayList<>();

        for (Plant plant : this.actors.keySet()) {
            if (!containsIdentity(plants, plant))
                removedPlants.add(plant);
        }

        for (Plant plant : removedPlants) {
            RenderedPlant rendered = this.actors.remove(plant);

            if (rendered == null)
                continue;

            int key = cellKey(rendered.lastColumn, rendered.lastRow);
            boolean suppressed = this.suppressedRemovalCells.remove(key);
            if (!suppressed && finishLiveExplosiveBody(plant, rendered)) {
                continue;
            }
            if (!suppressed && playAuthenticRemovalVisual(plant, rendered)) {
                rendered.root.remove();
                continue;
            }
            if (!suppressed && shouldPlayExplosion(plant)) {
                playExplosion(rendered);
                rendered.root.remove();
                continue;
            }

            if (!suppressed && playTriggeredRemovalAnimation(plant, rendered))
                continue;

            rendered.root.remove();
        }
    }


    private void syncSpecialPlantVisuals(RenderedPlant rendered, Plant plant, float delta) {
        if (rendered == null || plant == null || plant.getPosition() == null) {
            return;
        }
        String name = normalize(plant.getName());
        syncExplosiveDetonationVisual(rendered, plant, name, delta);
        if (name.equals("kiwibeast")) {
            syncKiwibeastStage(rendered, plant);
        }
        if (plant.isDisabled() || plant.isDead()) {
            return;
        }
        if (name.equals("phat beet") || name.equals("kiwibeast")) {
            syncPulseMeleeVisual(rendered, plant, delta, name.equals("kiwibeast"));
        }
        if (name.equals("phat beet")) {
            rendered.idlePulseElapsed += delta;
            if (rendered.idlePulseElapsed >= 1.30f) {
                rendered.idlePulseElapsed = 0f;
                spawnPamAtPlant(rendered, "PHATBEETS_IDLE_PULSE", "animation", 1.05f, 0f, false);
            }
        }
    }

    private void syncExplosiveDetonationVisual(
            RenderedPlant rendered,
            Plant plant,
            String normalizedName,
            float delta
    ) {
        if (!(plant.getBehavior() instanceof ExplosiveBehavior explosive)
                || !explosive.isDetonationStarted()) {
            return;
        }

        if (!rendered.detonationVisualStarted) {
            rendered.detonationVisualStarted = true;
            rendered.detonationVisualElapsed = 0f;
            rendered.detonationVisualDuration = Math.max(0.1f, explosive.getDetonationDelayTicks() / 10f);

            if (rendered.body instanceof PamAnimationActor && rendered.animation != null) {
                String clip = normalizedName.equals("doom-shroom")
                        ? rendered.animation.findClip(
                                "stage3_explode_short", "stage3_explode", "stage2_explode", "stage1_explode"
                        )
                        : rendered.animation.getAttackClip();
                if (clip != null) {
                    PamAnimationActor actor = (PamAnimationActor) rendered.body;
                    actor.setAnimation(rendered.animation.getPath(), clip);
                    actor.setLooping(false);
                    rendered.detonationVisualDuration = rendered.animation.getClipDuration(
                            clip, rendered.detonationVisualDuration
                    );
                    // idle ghabl az release damage restore nemishe
                    rendered.temporaryAnimationRemaining = Math.max(
                            rendered.temporaryAnimationRemaining, rendered.detonationVisualDuration
                    );
                } else {
                    playFallbackMotion(rendered, AnimationKind.ATTACK);
                }
            } else {
                playFallbackMotion(rendered, AnimationKind.ATTACK);
            }
        }

        rendered.detonationVisualElapsed += Math.max(0f, delta);
    }

    private void syncKiwibeastStage(RenderedPlant rendered, Plant plant) {
        long ageTicks = rendered.firstSeenTick < 0L
                ? 0L
                : Math.max(0L, this.worldDataSource.getCurrentTick() - rendered.firstSeenTick);
        int stage = ageTicks >= 720L ? 3 : ageTicks >= 240L ? 2 : 1;
        if (stage == rendered.kiwiStage) {
            return;
        }
        int previous = rendered.kiwiStage;
        rendered.kiwiStage = stage;
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        String growth = previous == 1 && stage == 2
                ? rendered.animation.findClip("growth_stage1")
                : previous == 2 && stage == 3
                ? rendered.animation.findClip("growth_stage2")
                : null;
        if (growth != null) {
            actor.setAnimation(rendered.animation.getPath(), growth);
            actor.setLooping(false);
            rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(growth, 0.75f);
            return;
        }
        String idle = kiwibeastIdleClip(rendered.animation, stage);
        if (idle != null && rendered.temporaryAnimationRemaining <= 0f
                && rendered.plantFoodRemaining <= 0f) {
            actor.setAnimation(rendered.animation.getPath(), idle);
            actor.setLooping(true);
        }
    }

    private String kiwibeastIdleClip(PamAnimationCatalog.AnimationInfo animation, int stage) {
        if (animation == null) {
            return null;
        }
        int safe = Math.max(1, Math.min(3, stage));
        return animation.findClip(
                "idle_stage" + safe + "_1",
                "idle_stage" + safe + "_",
                "idle_stage" + safe,
                "stage" + safe + "_idle",
                "idle"
        );
    }

    private String kiwibeastAttackClip(PamAnimationCatalog.AnimationInfo animation, int stage) {
        if (animation == null) {
            return null;
        }
        int safe = Math.max(1, Math.min(3, stage));
        return animation.findClip("attack_stage" + safe, "stage" + safe + "_attack", "attack");
    }

    private void syncPulseMeleeVisual(
            RenderedPlant rendered,
            Plant plant,
            float delta,
            boolean kiwibeast
    ) {
        rendered.meleeVisualElapsed += delta;
        float interval = (float) Math.max(0.12d, plant.getActionIntervalSeconds());
        if (rendered.meleeVisualElapsed < interval) {
            return;
        }
        rendered.meleeVisualElapsed %= interval;
        int radius = kiwibeast ? Math.max(1, rendered.kiwiStage) : 1;
        if (!hasHostileZombieInRadius(plant, radius)) {
            return;
        }
        if (rendered.body instanceof PamAnimationActor && rendered.animation != null
                && rendered.plantFoodRemaining <= 0f) {
            String attack = kiwibeast
                    ? kiwibeastAttackClip(rendered.animation, rendered.kiwiStage)
                    : rendered.animation.getAttackClip();
            if (attack != null) {
                PamAnimationActor actor = (PamAnimationActor) rendered.body;
                actor.setAnimation(rendered.animation.getPath(), attack);
                actor.setLooping(false);
                rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(
                        attack, ATTACK_ANIMATION_SECONDS
                );
            }
        }
        String pulse = kiwibeast ? "KIWIBEAST_ATTACK_PULSE" : "PHATBEETS_ATTACK_PULSE";
        String tileHit = kiwibeast ? "KIWIBEAST_TILE_HIT" : "PHATBEETS_TILE_HIT";
        spawnPamAtPlant(rendered, pulse, "animation", 1.45f + radius * 0.18f, 0f, false);
        spawnMeleeTileHits(plant, radius, tileHit);
    }

    private boolean hasHostileZombieInRadius(Plant plant, int radius) {
        if (plant == null || plant.getPosition() == null) {
            return false;
        }
        int px = plant.getPosition().getX();
        int py = plant.getPosition().getY();
        for (model.zombie.Zombie zombie : this.worldDataSource.getZombiesOnBoard()) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }
            int dx = Math.abs(zombie.getPosition().getX() - px);
            int dy = Math.abs(zombie.getPosition().getY() - py);
            if (dx <= radius && dy <= radius) {
                return true;
            }
        }
        return false;
    }

    private void spawnMeleeTileHits(Plant plant, int radius, String animationName) {
        if (plant == null || plant.getPosition() == null) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        for (model.zombie.Zombie zombie : this.worldDataSource.getZombiesOnBoard()) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null) {
                continue;
            }
            int dx = Math.abs(zombie.getPosition().getX() - plant.getPosition().getX());
            int dy = Math.abs(zombie.getPosition().getY() - plant.getPosition().getY());
            if (dx > radius || dy > radius) {
                continue;
            }
            float x = (float) ((zombie.getExactX() + 0.5d) * cellWidth);
            float y = (GameplayBoardInteractionLayer.ROW_COUNT - 0.5f - zombie.getPosition().getY()) * cellHeight;
            spawnPamEffect(animationName, "animation", x, y, Math.min(cellWidth, cellHeight) * 0.62f,
                    zombie.getPosition().getY(), 0f, false);
        }
    }

    private void playAuthenticPlantFoodEffect(RenderedPlant rendered, Plant plant) {
        if (rendered == null || plant == null) {
            return;
        }
        String name = normalize(plant.getName());
        if (name.equals("phat beet")) {
            spawnPamAtPlant(rendered, "PHATBEETS_PF_PULSE", "animation", 1.80f, 0f, false);
        } else if (name.equals("kiwibeast")) {
            spawnPamAtPlant(rendered, "KIWIBEAST_PF_PULSE", "animation", 2.00f, 0f, false);
        } else if (name.equals("sun bean")) {
            spawnPamAtPlant(rendered, "SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1", "animation", 1.28f, 0f, false);
            spawnPamAtPlant(rendered, "SUNBEAN_PLANTFOOD_EFFECT_OVERLAY2", "animation", 1.38f, 0f, false);
        }
    }

    private boolean finishLiveExplosiveBody(Plant plant, RenderedPlant rendered) {
        if (plant == null || rendered == null || !rendered.detonationVisualStarted
                || !normalize(plant.getName()).equals("doom-shroom")) {
            return false;
        }
        // damage va crater vasate clip release mishan va nime dovom clip edame peyda mikone
        fireExplosionVisualEvent();
        float remaining = Math.max(0.08f, rendered.detonationVisualDuration - rendered.detonationVisualElapsed);
        rendered.root.addAction(Actions.sequence(
                Actions.delay(remaining),
                Actions.removeActor()
        ));
        return true;
    }

    private boolean playAuthenticRemovalVisual(Plant plant, RenderedPlant rendered) {
        if (plant == null || rendered == null) {
            return false;
        }
        String name = normalize(plant.getName());
        if (name.equals("potato mine")) {
            spawnPamAtPlant(rendered, "POTATOMINE_EXPLOSION", "animation", 1.85f, 0f, false);
            fireExplosionVisualEvent();
            return true;
        }
        if (name.equals("primal potato mine")) {
            spawnPamAtPlant(rendered, "PRIMAL_POTATOMINE_EXPLOSION", "animation", 2.15f, 0f, false);
            fireExplosionVisualEvent();
            return true;
        }
        if (name.equals("cherry bomb")) {
            spawnPamAtPlant(rendered, "CHERRYBOMB_EXPLOSION_REAR", "explosion", 2.15f, 0f, false);
            spawnPamAtPlant(rendered, "CHERRYBOMB_EXPLOSION_TOP", "explosion", 2.15f, 0.01f, false);
            fireExplosionVisualEvent();
            return true;
        }
        if (name.equals("jalapeno")) {
            spawnJalapenoLaneFire(rendered.lastRow);
            fireExplosionVisualEvent();
            return true;
        }
        if (name.equals("doom-shroom")) {
            String clip = rendered.animation == null ? "stage1_explode"
                    : rendered.animation.findClip("stage3_explode_short", "stage3_explode", "stage2_explode", "stage1_explode");
            if (spawnPamAtPlant(rendered, "DOOMSHROOM", clip, 2.25f, 0f, false)) {
                fireExplosionVisualEvent();
                return true;
            }
        }
        if (name.equals("grapeshot")) {
            spawnPamAtPlant(rendered, "ESCAPEROOT_EXPLOSION_GRAPESHOT", "animation", 1.95f, 0f, false);
            fireExplosionVisualEvent();
            return true;
        }
        if (name.equals("hot potato")) {
            spawnPamAtPlant(rendered, "HOTPOTATO_STEAMFX", "animation", 1.15f, 0f, false);
            spawnPamAtPlant(rendered, "HOTPOTATO_ICEBLOCK_STEAMFX", "animation", 1.18f, 0.02f, false);
            spawnPamAtPlant(rendered, "HOTPOTATO_ICEBLOCK_PUDDLE", "animation", 1.10f, 0.03f, false);
            return true;
        }
        if (name.equals("grave buster")) {
            spawnPamAtPlant(rendered, "GRAVEBUSTER_DIRT", "gravebuster_dirt_fade", 1.25f, 0f, false);
            return true;
        }
        if (name.equals("explode-o-nut") && plant.getHealth() <= 0) {
            spawnPamAtPlant(rendered, "EXPLODEONUT_BLINK", "animation", 1.12f, 0f, false);
            playExplosion(rendered);
            return true;
        }
        return false;
    }

    private void spawnJalapenoLaneFire(int row) {
        if (row < 0) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float y = (GameplayBoardInteractionLayer.ROW_COUNT - 0.5f - row) * cellHeight;
        for (int column = 0; column < GameplayBoardInteractionLayer.COLUMN_COUNT; column++) {
            float x = (column + 0.5f) * cellWidth;
            spawnPamEffect("JALAPENO_FIRE", "idle", x, y, Math.min(cellWidth, cellHeight) * 1.18f,
                    row, column * 0.018f, false);
        }
    }

    private boolean spawnPamAtPlant(
            RenderedPlant rendered,
            String animationName,
            String clip,
            float sizeFactor,
            float delay,
            boolean looping
    ) {
        if (rendered == null) {
            return false;
        }
        float centerX = rendered.root.getX() + rendered.root.getWidth() * 0.5f;
        float centerY = rendered.root.getY() + rendered.root.getHeight() * 0.48f;
        float size = Math.min(rendered.root.getWidth(), rendered.root.getHeight()) * sizeFactor;
        return spawnPamEffect(animationName, clip, centerX, centerY, size, rendered.lastRow, delay, looping);
    }

    private boolean spawnPamEffect(
            String animationName,
            String clip,
            float centerX,
            float centerY,
            float visibleSize,
            int row,
            float delay,
            boolean looping
    ) {
        GameplayPamEffectSupport.Effect effect = GameplayPamEffectSupport.create(
                this.assets, this.animationCatalog, animationName, looping, clip
        );
        if (effect == null) {
            return false;
        }
        GameplayPamEffectSupport.centerVisibleBounds(effect, centerX, centerY, visibleSize);
        GameplayBoardDepthOrder.mark(effect.actor, Math.max(0, row), GameplayBoardDepthOrder.PROJECTILE + 3);
        this.renderHost.addActor(effect.actor);
        float duration = effect.duration(0.42f);
        if (!looping) {
            effect.actor.addAction(Actions.sequence(
                    Actions.delay(Math.max(0f, delay)),
                    Actions.delay(Math.max(0.08f, duration)),
                    Actions.removeActor()
            ));
        }
        return true;
    }

    private void fireExplosionVisualEvent() {
        if (this.explosionListener != null) {
            this.explosionListener.run();
        }
    }

    private boolean shouldPlayExplosion(Plant plant) {
        if (plant == null) {
            return false;
        }
        boolean explodeOnDeath = plant.getTags() != null
                && plant.getTags().contains(PlantTag.EXPLOSIVE)
                && normalize(plant.getName()).contains("explode-o-nut")
                && plant.getHealth() <= 0;
        boolean triggeredBlast = plant.getBehavior() instanceof ExplosiveBehavior
                && hasBlastVisual(plant.getName())
                && (plant.getHealth() > 0 || plant.getMaximumHealth() <= 0);
        return explodeOnDeath || triggeredBlast;
    }

    private boolean playTriggeredRemovalAnimation(Plant plant, RenderedPlant rendered) {
        if (plant == null || rendered == null || plant.getHealth() <= 0
                || !normalize(plant.getName()).contains("squash")
                || !(rendered.body instanceof PamAnimationActor)
                || rendered.animation == null) {
            return false;
        }
        String clip = rendered.animation.getTriggeredRemovalClip();
        if (clip == null) {
            return false;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);
        rendered.root.setOrigin(rendered.root.getWidth() * 0.5f, rendered.root.getHeight() * 0.15f);
        rendered.root.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(rendered.root.getWidth() * 0.22f, 0f, 0.34f),
                        Actions.scaleTo(1.08f, 0.96f, 0.34f)
                ),
                Actions.delay(0.18f),
                Actions.fadeOut(0.12f),
                Actions.removeActor()
        ));
        return true;
    }

    private boolean hasBlastVisual(String plantName) {
        String normalized = normalize(plantName);
        return normalized.contains("potato mine")
                || normalized.contains("cherry bomb")
                || normalized.contains("grapeshot")
                || normalized.contains("jalapeno")
                || normalized.contains("doom-shroom");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int cellKey(int column, int row) {
        return row * GameplayBoardInteractionLayer.COLUMN_COUNT + column;
    }

    private void playExplosion(RenderedPlant rendered) {
        Image outer = explosionImage(EXPLOSION_OUTER);
        Image middle = explosionImage(EXPLOSION_MIDDLE);
        Image inner = explosionImage(EXPLOSION_INNER);
        if (outer == null && middle == null && inner == null) {
            return;
        }
        Group burst = new Group();
        burst.setTouchable(Touchable.disabled);
        float width = rendered.root.getWidth() * 2.05f;
        float height = rendered.root.getHeight() * 2.05f;
        burst.setBounds(
                rendered.root.getX() + rendered.root.getWidth() * 0.5f - width * 0.5f,
                rendered.root.getY() - rendered.root.getHeight() * 0.22f,
                width,
                height
        );
        addExplosionPart(burst, outer, 0.02f, 0.02f, 0.96f, 0.96f, -8f);
        addExplosionPart(burst, middle, 0.10f, 0.10f, 0.80f, 0.80f, 7f);
        addExplosionPart(burst, inner, 0.20f, 0.19f, 0.60f, 0.60f, -4f);
        burst.setOrigin(width * 0.5f, height * 0.45f);
        burst.getColor().a = 0.98f;
        burst.setScale(0.62f);
        burst.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.scaleTo(1.08f, 1.08f, 0.12f),
                        Actions.rotateBy(5f, 0.12f)
                ),
                Actions.parallel(
                        Actions.scaleTo(1.22f, 1.22f, EXPLOSION_SECONDS - 0.12f),
                        Actions.fadeOut(EXPLOSION_SECONDS - 0.12f)
                ),
                Actions.removeActor()
        ));
        this.renderHost.addActor(burst);
        GameplayBoardDepthOrder.mark(burst, rendered.lastRow, GameplayBoardDepthOrder.PROJECTILE + 4);
        if (this.explosionListener != null) {
            this.explosionListener.run();
        }
    }

    private Image explosionImage(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void addExplosionPart(
            Group burst,
            Image image,
            float x,
            float y,
            float width,
            float height,
            float rotation
    ) {
        if (image == null) {
            return;
        }
        image.setBounds(
                burst.getWidth() * x,
                burst.getHeight() * y,
                burst.getWidth() * width,
                burst.getHeight() * height
        );
        image.setOrigin(image.getWidth() * 0.5f, image.getHeight() * 0.5f);
        image.setRotation(rotation);
        burst.addActor(image);
    }

    private boolean containsIdentity(List<Plant> plants, Plant wanted) {
        for (Plant plant : plants) {
            if (plant == wanted) {
                return true;
            }
        }
        return false;
    }

    private RenderedPlant createRenderedPlant(String plantName) {
        PamAnimationCatalog.AnimationInfo animation = this.animationCatalog.find(plantName);
        Actor body = createPlantActor(plantName, animation);
        if (body == null) {
            return null;
        }

        Group root = new Group();
        root.setTouchable(Touchable.disabled);
        Image iceBehind = optionalImage(ICE_BEHIND);
        Image chillOne = optionalImage(CHILL_ONE);
        Image chillTwo = optionalImage(CHILL_TWO);
        Image iceFront = optionalImage(ICE_FRONT);
        Image plantFoodGlow = optionalImage(PLANT_FOOD_GLOW);
        Image shadow = optionalImage(PLANT_SHADOW);
        Group sheep = createSheepOverlay();
        Group octopus = createOctopusOverlay();

        if (shadow != null) {
            shadow.setVisible(showPlantShadow(plantName));
            root.addActor(shadow);
        }
        addOptional(root, iceBehind);
        addOptional(root, plantFoodGlow);
        root.addActor(body);
        if (sheep != null) {
            sheep.setVisible(false);
            root.addActor(sheep);
        }
        addOptional(root, chillOne);
        addOptional(root, chillTwo);
        addOptional(root, iceFront);
        if (octopus != null) {
            root.addActor(octopus);
        }
        PamAnimationCatalog.AnimationInfo activeAnimation = body instanceof PamAnimationActor
                ? animation : null;
        return new RenderedPlant(
                root,
                body,
                iceBehind,
                chillOne,
                chillTwo,
                iceFront,
                plantFoodGlow,
                shadow,
                sheep,
                octopus,
                activeAnimation,
                activeAnimation == null ? 0f : plantCenterOffset(activeAnimation),
                activeAnimation == null ? 0f : plantGroundOffset(activeAnimation)
        );
    }

    private void addOptional(Group root, Actor actor) {
        if (actor != null) {
            actor.setVisible(false);
            root.addActor(actor);
        }
    }

    private Actor createPlantActor(
            String plantName,
            PamAnimationCatalog.AnimationInfo animation
    ) {
        if (canUseAnimation(animation)) {
            PamAnimationActor actor = new PamAnimationActor(
                    this.assets.getPamPlayer(),
                    animation.getPath(),
                    animation.getPreviewClip(),
                    animation.getCanvasWidth(),
                    animation.getCanvasHeight()
            );
            actor.setTouchable(Touchable.disabled);
            applyIdlePartVisibility(actor, plantName);
            return actor;
        }

        PlantPacketCatalog.PacketVisual packet = PlantPacketCatalog.findPacket(plantName);
        if (packet == null) {
            return null;
        }
        Drawable drawable = resourceDrawable(packet.getResourceId());
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void applyIdlePartVisibility(PamAnimationActor actor, String plantName) {
        String name = normalize(plantName);
        if (name.contains("magnet-shroom")) {
            actor.setPartsVisibility(Map.of("Magnet_Item", Boolean.FALSE));
        }
    }

    private Image optionalImage(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Group createSheepOverlay() {
        Image wool = optionalImage(SHEEP_WOOL);
        Image face = optionalImage(SHEEP_FACE);
        Image frontLeg = optionalImage(SHEEP_LEG);
        Image backLeg = optionalImage(SHEEP_LEG);
        if (wool == null || face == null) {
            return null;
        }
        Group group = new Group();
        group.setTouchable(Touchable.disabled);
        wool.setVisible(true);
        face.setVisible(true);
        group.addActor(wool);
        if (frontLeg != null) {
            frontLeg.setVisible(true);
            group.addActor(frontLeg);
        }
        if (backLeg != null) {
            backLeg.setVisible(true);
            group.addActor(backLeg);
        }
        group.addActor(face);
        return group;
    }

    private Group createOctopusOverlay() {
        Image face = optionalImage(OCTOPUS_FACE);
        Image tentacle = optionalImage(OCTOPUS_TENTACLE);
        if (face == null && tentacle == null) {
            return null;
        }
        Group group = new Group();
        group.setTouchable(Touchable.disabled);
        group.setVisible(false);
        if (tentacle != null) {
            group.addActor(tentacle);
        }
        if (face != null) {
            group.addActor(face);
        }
        return group;
    }

    private boolean canUseAnimation(PamAnimationCatalog.AnimationInfo animation) {
        if (animation == null || animation.getPreviewClip() == null) {
            return false;
        }
        FileHandle pamFile = Gdx.files.internal("IMAGES/" + animation.getPath());
        return pamFile.exists()
                && PamTextureAvailability.allTexturesAvailable(
                        this.assets.getTextureBank(),
                        pamFile
                );
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // missing optional status art never prevents the base plant from rendering
        }
        return null;
    }

    private void animatePlantMovement(Actor actor, Position position) {
        float startX = actor.getX();
        float startY = actor.getY();

        positionPlantActor(actor, position);

        float targetX = actor.getX();
        float targetY = actor.getY();
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;

        float horizontalCells = Math.abs(targetX - startX) / cellWidth;
        float verticalCells = Math.abs(targetY - startY) / cellHeight;
        float duration = Math.min(0.42f, 0.16f + Math.max(horizontalCells, verticalCells) * 0.07f);

        actor.clearActions();
        actor.setPosition(startX, startY);
        actor.addAction(Actions.moveTo(targetX, targetY, duration, Interpolation.smooth));
    }

    private void animatePlantFromTop(Actor actor) {
        float targetX = actor.getX();
        float targetY = actor.getY();
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float startY = getHeight() + cellHeight * 0.25f;
        float travelledCells = Math.max(1f, (startY - targetY) / cellHeight);
        float duration = Math.min(0.68f, 0.22f + travelledCells * 0.055f);

        actor.clearActions();
        actor.setPosition(targetX, startY);
        actor.getColor().a = 0f;

        actor.addAction(Actions.parallel(
            Actions.moveTo(targetX, targetY, duration, Interpolation.smooth),
            Actions.fadeIn(Math.min(0.22f, duration))
        ));
    }

    private void positionPlantActor(Actor actor, Position position) {
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float tileX = position.getX() * cellWidth;
        float tileBottom = (
                GameplayBoardInteractionLayer.ROW_COUNT - 1 - position.getY()
        ) * cellHeight;
        actor.setBounds(
                tileX,
                tileBottom + cellHeight * GameplayWorldLayout.PLANT_GROUND_ANCHOR_FACTOR,
                cellWidth,
                cellHeight
        );
    }

    private void layoutRenderedPlant(RenderedPlant rendered, String plantName) {
        float width = rendered.root.getWidth();
        float height = rendered.root.getHeight();
        if (rendered.body instanceof PamAnimationActor && rendered.animation != null) {
            layoutPamBody(rendered, plantName, width);
        } else {
            layoutStaticBody(rendered, plantName, width, height);
        }
        layoutPlantShadow(rendered.shadow, plantName, width, height);
        setBounds(rendered.iceBehind, -width * 0.12f, -height * 0.03f, width * 1.24f, height * 1.32f);
        setBounds(rendered.chillOne, -width * 0.06f, -height * 0.01f, width * 1.12f, height * 0.52f);
        setBounds(rendered.chillTwo, -width * 0.08f, -height * 0.01f, width * 1.16f, height * 0.66f);
        setBounds(rendered.iceFront, -width * 0.12f, -height * 0.03f, width * 1.24f, height * 1.32f);
        setBounds(rendered.plantFoodGlow, -width * 0.22f, -height * 0.16f, width * 1.44f, height * 1.52f);
        layoutSheep(rendered.sheep, width, height);
        layoutOctopus(rendered.octopus, width, height);
    }


    private boolean showPlantShadow(String plantName) {
        // The original PvZ2 keeps a soft contact shadow under every planted
        // gameplay entity. Water/ground-hugging plants use a lighter, flatter
        // footprint instead of removing the shadow entirely.
        return plantName != null && !plantName.isBlank();
    }

    private void layoutPlantShadow(Image shadow, String plantName, float width, float height) {
        if (shadow == null || !shadow.isVisible()) {
            return;
        }
        PlantShadowProfile profile = plantShadowProfile(plantName);
        float shadowWidth = width * profile.widthFactor;
        float shadowHeight = height * profile.heightFactor;
        shadow.setBounds(
                width * (0.5f + profile.centerOffsetX) - shadowWidth * 0.5f,
                height * profile.yFactor,
                shadowWidth,
                shadowHeight
        );
        shadow.getColor().a = profile.alpha;
    }

    private PlantShadowProfile plantShadowProfile(String plantName) {
        String name = normalize(plantName);
        PlantShadowProfile profile = PlantShadowProfile.of(0.68f, 0.225f, -0.020f, 0.82f);

        // Very low or compact plants keep a short contact footprint.
        if (name.contains("potato mine") || name.contains("iceberg")
                || name.contains("puff-shroom") || name.contains("sun bean")
                || name.contains("hot potato") || name.contains("sea-shroom")) {
            return PlantShadowProfile.of(0.52f, 0.175f, -0.012f, 0.72f);
        }

        // Water plants in PvZ2 have a faint footprint/reflection-like contact
        // shadow instead of the strong oval used by land plants.
        if (name.contains("lily pad") || name.contains("tangle kelp")
                || name.contains("cat-tail")) {
            return PlantShadowProfile.of(0.72f, 0.155f, -0.006f, 0.42f);
        }

        // Wide defensive plants occupy most of the tile footprint.
        if (name.contains("tall-nut") || name.contains("wall-nut")
                || name.contains("endurian") || name.contains("pumpkin")
                || name.contains("explode-o-nut") || name.contains("garlic")) {
            return PlantShadowProfile.of(0.78f, 0.245f, -0.018f, 0.84f);
        }

        // Plants with broad lateral animation need a wider shadow than their
        // stem/contact point would otherwise suggest.
        if (name.contains("bonk choy") || name.contains("chomper")
                || name.contains("squash") || name.contains("wasabi")
                || name.contains("kiwibeast") || name.contains("phat beet")) {
            return PlantShadowProfile.of(0.84f, 0.245f, -0.018f, 0.82f);
        }

        // Lobbers and other large-bodied plants sit on a broad base in the
        // original game.
        if (name.contains("melon") || name.contains("winter melon")
                || name.contains("cabbage") || name.contains("kernel")
                || name.contains("pepper-pult") || name.contains("citron")
                || name.contains("caulipower") || name.contains("electric blueberry")
                || name.contains("bowling bulb")) {
            return PlantShadowProfile.of(0.80f, 0.235f, -0.018f, 0.82f);
        }

        if (name.contains("threepeater") || name.contains("pea pod")
                || name.contains("split pea") || name.contains("starfruit")
                || name.contains("rotobaga")) {
            return PlantShadowProfile.of(0.74f, 0.225f, -0.018f, 0.80f);
        }

        if (name.contains("mint")) {
            return PlantShadowProfile.of(0.76f, 0.220f, -0.016f, 0.78f);
        }

        if (name.contains("sunflower") || name.contains("sun-shroom")
                || name.contains("gold bloom")) {
            return PlantShadowProfile.of(0.64f, 0.215f, -0.018f, 0.80f);
        }

        return profile;
    }

    private void layoutStaticBody(RenderedPlant rendered, String plantName, float width, float height) {
        float maxWidth = STATIC_WIDTH_FACTOR;
        float maxHeight = STATIC_HEIGHT_FACTOR;
        float bottom = STATIC_BOTTOM_FACTOR;
        String name = normalize(plantName);

        if (name.contains("tall-nut")) {
            maxWidth = 0.78f;
            maxHeight = 1.04f;
            bottom = 0.025f;
        } else if (name.contains("lily pad") || name.contains("pumpkin")
                || name.contains("potato mine")) {
            maxWidth = 0.82f;
            maxHeight = 0.62f;
            bottom = 0.035f;
        } else if (name.contains("cherry bomb")) {
            maxWidth = 0.78f;
            maxHeight = 0.72f;
            bottom = 0.045f;
        } else if (name.contains("bonk choy") || name.contains("squash")) {
            maxWidth = 0.82f;
            maxHeight = 0.90f;
            bottom = 0.035f;
        } else if (name.contains("torchwood")) {
            maxWidth = 0.74f;
            maxHeight = 0.92f;
            bottom = 0.025f;
        }

        GameplayStaticEntityLayout.groundedFit(
                rendered.body,
                width,
                height,
                maxWidth,
                maxHeight,
                bottom
        );
    }

    private boolean playTemporaryAnimation(Plant plant, AnimationKind kind) {
        RenderedPlant rendered = plant == null ? null : this.actors.get(plant);
        if (rendered == null || rendered.plantFoodRemaining > 0f) {
            return false;
        }
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            playFallbackMotion(rendered, kind);
            return false;
        }
        String clip = switch (kind) {
            case ATTACK -> rendered.animation.getAttackClip();
            case SUN_PRODUCTION -> rendered.animation.getSunProductionClip();
            case INTRO -> rendered.animation.getIntroClip();
        };
        if (clip == null) {
            playFallbackMotion(rendered, kind);
            return false;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);
        float fallbackDuration = switch (kind) {
            case ATTACK -> ATTACK_ANIMATION_SECONDS;
            case SUN_PRODUCTION -> SUN_PRODUCTION_ANIMATION_SECONDS;
            case INTRO -> INTRO_ANIMATION_SECONDS;
        };
        rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(clip, fallbackDuration);
        return true;
    }

    private void playFallbackMotion(RenderedPlant rendered, AnimationKind kind) {
        if (rendered == null) {
            return;
        }
        rendered.body.clearActions();
        rendered.body.setOrigin(rendered.body.getWidth() / 2f, rendered.body.getHeight() * 0.18f);
        if (kind == AnimationKind.ATTACK) {
            rendered.body.addAction(Actions.sequence(
                    Actions.rotateTo(-4f, 0.07f),
                    Actions.rotateTo(3f, 0.09f),
                    Actions.rotateTo(0f, 0.10f)
            ));
            return;
        }
        rendered.body.addAction(Actions.sequence(
                Actions.scaleTo(1.06f, 1.10f, 0.11f),
                Actions.scaleTo(1f, 1f, 0.18f)
        ));
    }

    private void advanceTemporaryAnimation(RenderedPlant rendered, float delta) {
        if (rendered.temporaryAnimationRemaining <= 0f || rendered.plantFoodRemaining > 0f) {
            return;
        }
        rendered.temporaryAnimationRemaining = Math.max(0f, rendered.temporaryAnimationRemaining - delta);
        if (rendered.temporaryAnimationRemaining > 0f
                || !(rendered.body instanceof PamAnimationActor)
                || rendered.animation == null) {
            return;
        }
        restoreStableClip(rendered);
        rendered.damageStage = -1;
    }

    private void advancePlantFoodAnimation(RenderedPlant rendered, float delta) {
        if (rendered.plantFoodRemaining <= 0f) {
            return;
        }
        rendered.plantFoodRemaining = Math.max(0f, rendered.plantFoodRemaining - delta);
        if (rendered.plantFoodRemaining > 0f
                || !(rendered.body instanceof PamAnimationActor)
                || rendered.animation == null) {
            return;
        }
        restoreStableClip(rendered);
        rendered.damageStage = -1;
    }

    private boolean playBowlingAttack(RenderedPlant rendered) {
        if (rendered == null || rendered.plantFoodRemaining > 0f
                || !(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            return false;
        }
        int bulbIndex = -1;
        for (int index = 2; index >= 0; index--) {
            if (rendered.bowlingRecharge[index] <= 0f) {
                bulbIndex = index;
                break;
            }
        }
        if (bulbIndex < 0) {
            bulbIndex = 0;
        }
        String clip = rendered.animation.getBowlingShotClip(bulbIndex);
        if (clip == null) {
            return false;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);
        rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(
                clip,
                ATTACK_ANIMATION_SECONDS
        );
        rendered.bowlingRecharge[bulbIndex] = bowlingRechargeSeconds(bulbIndex);
        rendered.damageStage = -1;
        return true;
    }

    private void advanceBowlingRecharge(RenderedPlant rendered, float delta) {
        if (rendered == null || rendered.animation == null
                || !"BOWLINGBULB".equalsIgnoreCase(rendered.animation.getName())) {
            return;
        }
        for (int index = 0; index < rendered.bowlingRecharge.length; index++) {
            float before = rendered.bowlingRecharge[index];
            if (before <= 0f) {
                continue;
            }
            rendered.bowlingRecharge[index] = Math.max(0f, before - delta);
            if (before > 0f && rendered.bowlingRecharge[index] <= 0f) {
                rendered.pendingBowlingReloadMask |= 1 << index;
            }
        }
        if (rendered.pendingBowlingReloadMask == 0
                || rendered.temporaryAnimationRemaining > 0f
                || rendered.plantFoodRemaining > 0f
                || !(rendered.body instanceof PamAnimationActor)) {
            return;
        }
        for (int index = 2; index >= 0; index--) {
            int mask = 1 << index;
            if ((rendered.pendingBowlingReloadMask & mask) == 0) {
                continue;
            }
            rendered.pendingBowlingReloadMask &= ~mask;
            String clip = rendered.animation.getBowlingReloadClip(index);
            if (clip != null) {
                PamAnimationActor actor = (PamAnimationActor) rendered.body;
                actor.setAnimation(rendered.animation.getPath(), clip);
                actor.setLooping(false);
                rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(clip, 0.55f);
            }
            break;
        }
    }

    private float bowlingRechargeSeconds(int bulbIndex) {
        if (bulbIndex >= 2) {
            return 10f;
        }
        return bulbIndex == 1 ? 5f : 2f;
    }

    private void advanceMineArming(RenderedPlant rendered, Plant plant, float delta) {
        if (rendered == null || plant == null || !isPotatoMine(plant.getName())
                || rendered.mineArmRemaining <= 0f || rendered.plantFoodRemaining > 0f) {
            return;
        }
        rendered.mineArmRemaining = Math.max(0f, rendered.mineArmRemaining - delta);
        if (rendered.mineArmRemaining > 0f
                || rendered.mineRecoverPlayed
                || rendered.temporaryAnimationRemaining > 0f
                || !(rendered.body instanceof PamAnimationActor)
                || rendered.animation == null) {
            return;
        }
        rendered.mineRecoverPlayed = true;
        String recover = rendered.animation.getRecoverClip();
        if (recover == null) {
            restoreStableClip(rendered);
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), recover);
        actor.setLooping(false);
        rendered.temporaryAnimationRemaining = rendered.animation.getClipDuration(recover, 0.85f);
    }

    private void restoreStableClip(RenderedPlant rendered) {
        if (rendered == null || !(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            return;
        }
        String clip;
        if (rendered.mineArmRemaining > 0f) {
            clip = rendered.animation.getUnarmedClip();
        } else if (rendered.kiwiStage > 0) {
            clip = kiwibeastIdleClip(rendered.animation, rendered.kiwiStage);
        } else {
            clip = rendered.animation.getPreviewClip();
        }
        if (clip == null) {
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(true);
    }

    private boolean isPotatoMine(String plantName) {
        String name = normalize(plantName);
        return name.equals("potato mine") || name.equals("primal potato mine");
    }

    private boolean isBowlingBulb(String plantName) {
        return "bowling bulb".equals(normalize(plantName));
    }

    private float mineArmSeconds(Plant plant) {
        if (plant == null) {
            return 0f;
        }
        boolean primal = normalize(plant.getName()).equals("primal potato mine");
        float seconds = primal ? 5f : 15f;
        if (plant.getLevel() >= 2) {
            seconds -= primal ? 1f : 3f;
        }
        return Math.max(0f, seconds);
    }

    private void updateDamageStage(RenderedPlant rendered, Plant plant) {
        if (rendered == null
                || plant == null
                || !(rendered.body instanceof PamAnimationActor)
                || rendered.animation == null
                || rendered.temporaryAnimationRemaining > 0f
                || rendered.plantFoodRemaining > 0f
                || rendered.mineArmRemaining > 0f) {
            return;
        }
        int maximumHealth = plant.getMaximumHealth();
        if (maximumHealth <= 0) {
            return;
        }
        float healthRatio = Math.max(0f, Math.min(1f, plant.getHealth() / (float) maximumHealth));
        int stage = healthRatio <= 0.18f ? 3 : healthRatio <= 0.36f ? 2 : healthRatio <= 0.68f ? 1 : 0;
        if (stage == rendered.damageStage) {
            return;
        }
        String clip = stage == 0 ? rendered.animation.getPreviewClip() : rendered.animation.getDamageClip(stage);
        if (clip == null && isPumpkin(plant.getName())) {
            clip = pumpkinDamageClip(rendered.animation, stage);
        }
        if (clip == null) {
            if (stage == 0) {
                rendered.damageStage = 0;
            }
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(true);
        rendered.damageStage = stage;
    }

    private String pumpkinDamageClip(PamAnimationCatalog.AnimationInfo animation, int stage) {
        if (stage >= 2 && animation.hasClip("idle3")) {
            return "idle3";
        }
        if (stage >= 1 && animation.hasClip("idle2")) {
            return "idle2";
        }
        return animation.getPreviewClip();
    }

    private boolean isPumpkin(String plantName) {
        return normalize(plantName).contains("pumpkin");
    }

    private void updateDamageFlash(RenderedPlant rendered, Plant plant, float delta) {
        int health = plant == null ? 0 : plant.getHealth();
        if (rendered.lastHealth >= 0 && health < rendered.lastHealth) {
            rendered.damageFlashRemaining = DAMAGE_FLASH_SECONDS;
        }
        rendered.lastHealth = health;
        rendered.damageFlashRemaining = Math.max(0f, rendered.damageFlashRemaining - delta);
        rendered.body.setColor(rendered.damageFlashRemaining > 0f ? DAMAGE_FLASH : Color.WHITE);
    }

    private void showPlantFoodGlow(RenderedPlant rendered) {
        if (rendered.plantFoodGlow == null) {
            return;
        }
        rendered.plantFoodGlow.clearActions();
        rendered.plantFoodGlow.setVisible(true);
        rendered.plantFoodGlow.getColor().a = 0.92f;
        rendered.plantFoodGlow.addAction(Actions.sequence(
                Actions.fadeOut(PLANT_FOOD_ANIMATION_SECONDS),
                Actions.visible(false),
                Actions.alpha(1f)
        ));
    }

    private void layoutPamBody(RenderedPlant rendered, String plantName, float rootWidth) {
        PamAnimationCatalog.AnimationInfo animation = rendered.animation;
        float actorWidth = GameplayPamScale.actorWidth(animation.getCanvasWidth());
        float actorHeight = GameplayPamScale.actorHeight(animation.getCanvasHeight());
        float horizontalCorrection = plantPamHorizontalCorrection(plantName, rootWidth);
        rendered.body.setBounds(
                rootWidth / 2f + rendered.pamCenterOffsetX + horizontalCorrection - actorWidth / 2f,
                rendered.pamGroundOffset - actorHeight / 2f,
                actorWidth,
                actorHeight
        );
    }

    private float plantPamHorizontalCorrection(String plantName, float rootWidth) {
        String name = normalize(plantName);
        if (name.contains("wasabi")) {
            return -rootWidth * 0.28f;
        }
        if (name.contains("sun bean")) {
            return rootWidth * 0.22f;
        }
        return 0f;
    }

    private float plantCenterOffset(PamAnimationCatalog.AnimationInfo animation) {
        try {
            Rectangle bounds = this.assets.getPamPlayer().bounds(
                    animation.getPath(),
                    animation.getPreviewClip()
            );
            if (bounds != null) {
                return -(bounds.x + bounds.width / 2f) * GameplayPamScale.WORLD_SCALE;
            }
        } catch (RuntimeException ignored) {
        }
        return 0f;
    }

    private float plantGroundOffset(PamAnimationCatalog.AnimationInfo animation) {
        try {
            Rectangle bounds = this.assets.getPamPlayer().bounds(
                    animation.getPath(),
                    animation.getPreviewClip()
            );
            if (bounds != null) {
                return (bounds.y + bounds.height) * GameplayPamScale.WORLD_SCALE;
            }
        } catch (RuntimeException ignored) {
            // centering at the tile baseline keeps the plant anchor stable
        }
        return 0f;
    }

    private void setBounds(Actor actor, float x, float y, float width, float height) {
        if (actor != null) {
            actor.setBounds(x, y, width, height);
        }
    }

    private void layoutSheep(Group sheep, float width, float height) {
        if (sheep == null || sheep.getChildren().size < 2) {
            return;
        }
        sheep.setBounds(0f, 0f, width, height);
        Actor wool = sheep.getChildren().get(0);
        wool.setBounds(width * 0.20f, height * 0.05f, width * 0.66f, height * 0.90f);
        int faceIndex = sheep.getChildren().size - 1;
        Actor face = sheep.getChildren().get(faceIndex);
        face.setBounds(width * 0.63f, height * 0.34f, width * 0.26f, height * 0.34f);
        if (sheep.getChildren().size >= 4) {
            Actor frontLeg = sheep.getChildren().get(1);
            Actor backLeg = sheep.getChildren().get(2);
            frontLeg.setBounds(width * 0.56f, height * 0.03f, width * 0.08f, height * 0.28f);
            backLeg.setBounds(width * 0.34f, height * 0.03f, width * 0.08f, height * 0.28f);
        }
    }

    private void layoutOctopus(Group octopus, float width, float height) {
        if (octopus == null) {
            return;
        }
        octopus.setBounds(-width * 0.08f, height * 0.03f, width * 1.16f, height * 0.94f);
        if (octopus.getChildren().size > 0) {
            Actor tentacle = octopus.getChildren().get(0);
            tentacle.setBounds(0f, 0f, octopus.getWidth(), octopus.getHeight() * 0.42f);
        }
        if (octopus.getChildren().size > 1) {
            Actor face = octopus.getChildren().get(1);
            face.setBounds(
                    octopus.getWidth() * 0.08f,
                    octopus.getHeight() * 0.24f,
                    octopus.getWidth() * 0.88f,
                    octopus.getHeight() * 0.66f
            );
        }
    }

    private void updateTransformation(RenderedPlant rendered, Plant plant) {
        boolean transformed = plant != null && plant.isTransformed() && rendered.sheep != null;
        rendered.body.setVisible(!transformed);
        if (rendered.sheep != null) {
            rendered.sheep.setVisible(transformed);
        }
        if (rendered.shadow != null) {
            rendered.shadow.setVisible(transformed || showPlantShadow(plant == null ? null : plant.getName()));
        }
    }

    private void updateCover(RenderedPlant rendered, Plant plant) {
        GameplayPlantCoverInspector.State state = this.worldDataSource.getPlantCoverState(plant);
        int freeze = state == null ? 0 : state.getFreezeLevel();
        setVisible(rendered.chillOne, freeze == 1);
        setVisible(rendered.chillTwo, freeze == 2);
        setVisible(rendered.iceBehind, freeze >= 3);
        setVisible(rendered.iceFront, freeze >= 3);
        if (rendered.octopus != null) {
            rendered.octopus.setVisible(state != null && state.isOctopusCovered());
        }
    }

    private void setVisible(Actor actor, boolean visible) {
        if (actor != null) {
            actor.setVisible(visible);
        }
    }

    private void applyBoardDepthOrder(List<Plant> plants) {
        if (this.renderHost != this) {
            return;
        }
        plants.removeIf(plant -> plant == null || plant.getPosition() == null);
        plants.sort(Comparator
                .comparingInt((Plant plant) -> plant.getPosition().getY())
                .thenComparingInt(plant -> stackPriority(plant.getName()))
                .thenComparingInt(plant -> plant.getPosition().getX()));
        for (int index = 0; index < plants.size(); index++) {
            RenderedPlant rendered = this.actors.get(plants.get(index));
            if (rendered != null) {
                rendered.root.setZIndex(index);
            }
        }
    }

    private int depthPriority(String plantName) {
        int priority = stackPriority(plantName);
        if (priority == 0) {
            return GameplayBoardDepthOrder.BASE_PLANT;
        }
        if (priority == 2) {
            return GameplayBoardDepthOrder.COVER_PLANT;
        }
        return GameplayBoardDepthOrder.PLANT;
    }

    private int stackPriority(String plantName) {
        String name = plantName == null ? "" : plantName.toLowerCase(Locale.ROOT);
        if (name.contains("lily pad") || name.contains("flower pot")) {
            return 0;
        }
        if (name.contains("pumpkin")) {
            return 2;
        }
        return 1;
    }

    private enum AnimationKind {
        ATTACK,
        SUN_PRODUCTION,
        INTRO
    }

    private static final class PlantShadowProfile {
        private final float widthFactor;
        private final float heightFactor;
        private final float yFactor;
        private final float centerOffsetX;
        private final float alpha;

        private PlantShadowProfile(
                float widthFactor,
                float heightFactor,
                float yFactor,
                float centerOffsetX,
                float alpha
        ) {
            this.widthFactor = widthFactor;
            this.heightFactor = heightFactor;
            this.yFactor = yFactor;
            this.centerOffsetX = centerOffsetX;
            this.alpha = alpha;
        }

        private static PlantShadowProfile of(
                float widthFactor,
                float heightFactor,
                float yFactor,
                float alpha
        ) {
            return new PlantShadowProfile(widthFactor, heightFactor, yFactor, 0f, alpha);
        }
    }

    private static final class ProjectileLaunchProfile {
        private final String[] partCandidates;
        private final float partX;
        private final float partY;
        private final float releaseFraction;
        private final float fallbackX;
        private final float fallbackY;
        private final boolean transientPart;
        private final boolean directional;

        private ProjectileLaunchProfile(
                String[] partCandidates,
                float partX,
                float partY,
                float releaseFraction,
                float fallbackX,
                float fallbackY,
                boolean transientPart,
                boolean directional
        ) {
            this.partCandidates = partCandidates == null
                    ? new String[0]
                    : partCandidates.clone();
            this.partX = partX;
            this.partY = partY;
            this.releaseFraction = releaseFraction;
            this.fallbackX = fallbackX;
            this.fallbackY = fallbackY;
            this.transientPart = transientPart;
            this.directional = directional;
        }

        private static ProjectileLaunchProfile transientPart(
                float partX,
                float partY,
                float releaseFraction,
                String... partCandidates
        ) {
            return new ProjectileLaunchProfile(
                    partCandidates, partX, partY, releaseFraction,
                    0.90f, 0.56f, true, false
            );
        }

        private static ProjectileLaunchProfile fixedPart(
                float partX,
                float partY,
                float releaseFraction,
                float fallbackX,
                float fallbackY,
                String... partCandidates
        ) {
            return new ProjectileLaunchProfile(
                    partCandidates, partX, partY, releaseFraction,
                    fallbackX, fallbackY, false, false
            );
        }

        private static ProjectileLaunchProfile cell(
                float fallbackX,
                float fallbackY,
                float releaseFraction
        ) {
            return new ProjectileLaunchProfile(
                    new String[0], 0.5f, 0.5f, releaseFraction,
                    fallbackX, fallbackY, false, false
            );
        }

        private static ProjectileLaunchProfile directional(
                float fallbackX,
                float fallbackY,
                float releaseFraction
        ) {
            return new ProjectileLaunchProfile(
                    new String[0], 0.5f, 0.5f, releaseFraction,
                    fallbackX, fallbackY, false, true
            );
        }

        private ProjectileLaunchProfile withFallback(float fallbackX, float fallbackY) {
            return new ProjectileLaunchProfile(
                    this.partCandidates, this.partX, this.partY, this.releaseFraction,
                    fallbackX, fallbackY, this.transientPart, this.directional
            );
        }
    }

    private static final class ProjectileLaunchSample {
        private static final ProjectileLaunchSample MISSING =
                new ProjectileLaunchSample(null, -1f);

        private final Rectangle bounds;
        private final float releaseFraction;

        private ProjectileLaunchSample(Rectangle bounds, float releaseFraction) {
            this.bounds = bounds;
            this.releaseFraction = releaseFraction;
        }
    }

    private static final class RenderedPlant {
        private final Group root;
        private final Actor body;
        private final Image iceBehind;
        private final Image chillOne;
        private final Image chillTwo;
        private final Image iceFront;
        private final Image plantFoodGlow;
        private final Image shadow;
        private final Group sheep;
        private final Group octopus;
        private final PamAnimationCatalog.AnimationInfo animation;
        private final float pamCenterOffsetX;
        private final float pamGroundOffset;
        private float plantFoodRemaining;
        private float temporaryAnimationRemaining;
        private float damageFlashRemaining;
        private float mineArmRemaining;
        private boolean mineRecoverPlayed;
        private final float[] bowlingRecharge = new float[3];
        private int pendingBowlingReloadMask;
        private int damageStage = -1;
        private int lastHealth = -1;
        private boolean detonationVisualStarted;
        private float detonationVisualElapsed;
        private float detonationVisualDuration;
        private int lastColumn = -1;
        private int lastRow = -1;
        private long firstSeenTick = -1L;
        private float meleeVisualElapsed;
        private float idlePulseElapsed;
        private int kiwiStage;

        private RenderedPlant(
                Group root,
                Actor body,
                Image iceBehind,
                Image chillOne,
                Image chillTwo,
                Image iceFront,
                Image plantFoodGlow,
                Image shadow,
                Group sheep,
                Group octopus,
                PamAnimationCatalog.AnimationInfo animation,
                float pamCenterOffsetX,
                float pamGroundOffset
        ) {
            this.root = root;
            this.body = body;
            this.iceBehind = iceBehind;
            this.chillOne = chillOne;
            this.chillTwo = chillTwo;
            this.iceFront = iceFront;
            this.plantFoodGlow = plantFoodGlow;
            this.shadow = shadow;
            this.sheep = sheep;
            this.octopus = octopus;
            this.animation = animation;
            this.pamCenterOffsetX = pamCenterOffsetX;
            this.pamGroundOffset = pamGroundOffset;
        }
    }
}
