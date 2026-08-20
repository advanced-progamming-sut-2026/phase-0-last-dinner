package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.Plant;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.ArmorType;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieCondition;
import model.zombie.behavior.BlockPusherBehavior;
import model.zombie.behavior.TorchBearerBehavior;
import model.zombie.behavior.ZombieBehavior;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Renders live zombies at native gameplay PAM scale with view-only gait correction. */
public final class GameplayZombieLayer extends Group {
    private static final float STATIC_WIDTH_FACTOR = 0.92f;
    private static final float STATIC_HEIGHT_FACTOR = 1.42f;
    private static final float STATIC_BOTTOM_FACTOR = -0.02f;
    private static final String ZOMBIE_SHADOW = "IMAGE_PLANTSHADOW";
    private static final float DAMAGE_FLASH_SECONDS = 0.12f;
    private static final float IMP_THROW_SECONDS = 1.0f;
    private static final float SPECIAL_CLIP_SECONDS = 0.82f;
    private static final float BUTTER_HINT_SECONDS = 1.65f;
    private static final float EXPLOSION_DEATH_WINDOW_SECONDS = 0.55f;
    private static final float PROSPECTOR_FLIGHT_SECONDS = 1.15f;
    private static final float ROW_SLIDE_SECONDS = 0.30f;
    private static final float PRE_ANCHOR_PLANT_BOTTOM_FACTOR = -0.01f;
    private static final float GARGANTUAR_GROUND_CORRECTION_FACTOR = 0.62f;
    private static final String ASH_MAIN = "IMAGE_EFFECTS_ZOMBIE_ASH_ZOMBIE_ASH_104X95";
    private static final String ASH_PILE = "IMAGE_EFFECTS_ZOMBIE_ASH_ZOMBIE_ASH_53X33";
    private static final String ASH_DUST_TALL = "IMAGE_EFFECTS_ZOMBIE_ASH_ZOMBIE_ASH_15X26";
    private static final String ASH_DUST_WIDE = "IMAGE_EFFECTS_ZOMBIE_ASH_ZOMBIE_ASH_18X14";
    private static final String ASH_DUST = "IMAGE_EFFECTS_ZOMBIE_ASH_ZOMBIE_ASH_7X7";
    private static final String BUTTER_SPLAT =
            "IMAGE_EFFECTS_SPLAT_KERNALPULT_BUTTER_SPLAT_KERNALPULT_BUTTER_90X50";
    private static final String FROZEN_ZOMBIE_BLOCK =
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243";
    private static final String HUNTER_SNOWBALL =
            "IMAGE_EFFECTS_ZOMBIE_HUNTER_SNOWBALL_PROJECTILE";
    private static final String HUNTER_SNOWBALL_SPLAT =
            "IMAGE_EFFECTS_ZOMBIE_HUNTER_SNOWBALL_SPLAT_ZOMBIE_HUNTER_SNOWBALL_SPLAT_104X39";
    private static final String FISHERMAN_HOOK =
            "IMAGE_EFFECTS_ZOMBIE_FISHERMAN_HOOK_ZOMBIE_FISHERMAN_HOOK_23X42";
    private static final String OCTOPUS_PROJECTILE =
            "IMAGE_EFFECTS_ZOMBIE_OCTOPUS_PROJECTILE_ZOMBIE_OCTOPUS_PROJECTILE_113X91";
    private static final String WIZARD_MAGIC =
            "IMAGE_EFFECTS_DARK_WIZARD_SHEEPENING_DARK_WIZARD_SHEEPENING_163X65";
    private static final String ARCADE_CABINET =
            "IMAGE_EFFECTS_80S_ARCADE_CABINET_80S_ARCADE_CABINET_145X247";
    private static final String BARREL_OBSTACLE =
            "IMAGE_ZOMBIE_ZOMBIE_PIRATE_BARREL_PUSHER_BARREL_ZOMBIE_PIRATE_BARREL_PUSHER_BARREL_126X126";
    private static final String ICE_BLOCK_OBSTACLE =
            "IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243";
    private static final String PIANO_FRONT = "IMAGE_ZOMBIE_PIANO_PIANO_157X107";
    private static final String PIANO_BODY = "IMAGE_ZOMBIE_PIANO_PIANO_129X68";
    private static final String PIANO_TOP = "IMAGE_ZOMBIE_PIANO_PIANO_134X39";
    private static final String PIANO_SIDE = "IMAGE_ZOMBIE_PIANO_PIANO_58X116";
    private static final String PIANO_LEG_LEFT = "IMAGE_ZOMBIE_PIANO_PIANO_48X81";
    private static final String PIANO_LEG_RIGHT = "IMAGE_ZOMBIE_PIANO_PIANO_24X70";
    private static final String PIANO_NOTE = "IMAGE_ZOMBIE_PIANO_PIANO_28X29";

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private Group renderHost;
    private boolean ownsAssets;
    private final ZombieAnimationCatalog animationCatalog;
    private final Map<Zombie, RenderedZombie> actors = new IdentityHashMap<>();
    private final Map<Plant, PlantAbilityState> plantAbilityStates = new IdentityHashMap<>();
    private final Map<Zombie, Integer> knightArmorCounts = new IdentityHashMap<>();
    private final Map<Projectile, Boolean> reflectedProjectiles = new IdentityHashMap<>();
    private Runnable gargantuarImpactListener;
    private Consumer<Plant> magnetCatchListener;
    private float explosionDeathWindow;

    public GameplayZombieLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayZombieLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        this.renderHost = this;
        this.animationCatalog = new ZombieAnimationCatalog();
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        float safeDelta = Math.max(0f, delta);
        syncZombies(safeDelta);
        syncSpecialAbilityVisuals();
        this.explosionDeathWindow = Math.max(0f, this.explosionDeathWindow - safeDelta);
    }

    public int getRenderedZombieCount() {
        return this.actors.size();
    }

    void setRenderHost(Group renderHost) {
        this.renderHost = renderHost == null ? this : renderHost;
    }

    void setGargantuarImpactListener(Runnable listener) {
        this.gargantuarImpactListener = listener;
    }

    void setMagnetCatchListener(Consumer<Plant> listener) {
        this.magnetCatchListener = listener;
    }

    void markExplosionDeathWindow() {
        this.explosionDeathWindow = EXPLOSION_DEATH_WINDOW_SECONDS;
    }

    public void noteProjectileImpact(Projectile projectile) {
        Zombie target = projectileTarget(projectile);
        if (target == null) {
            return;
        }
        RenderedZombie rendered = this.actors.get(target);
        if (rendered == null) {
            return;
        }
        if (isButterProjectile(projectile)) {
            rendered.butterRemaining = BUTTER_HINT_SECONDS;
        }
        if (projectile != null && projectile.getType() == ProjectileType.ICE
                && aliasContains(rendered, "prospector")) {
            rendered.prospectorExtinguished = true;
        }
    }

    private Zombie projectileTarget(Projectile projectile) {
        if (projectile == null) {
            return null;
        }
        if (projectile.getTarget() != null) {
            return projectile.getTarget();
        }
        List<Zombie> hit = projectile.getHitZombies();
        return hit == null || hit.isEmpty() ? null : hit.get(hit.size() - 1);
    }

    private boolean isButterProjectile(Projectile projectile) {
        if (projectile == null || projectile.getSourcePlant() == null) {
            return false;
        }
        String sourceName = projectile.getSourcePlant().getName();
        return sourceName != null
                && sourceName.toLowerCase(Locale.ROOT).contains("kernel")
                && projectile.getStunChancePercent() >= 100;
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void syncZombies(float delta) {
        List<Zombie> zombies = new ArrayList<>(this.dataSource.getZombiesOnBoard());
        zombies.removeIf(zombie -> zombie == null || zombie.isDead() || zombie.getPosition() == null);
        removeMissing(zombies);
        for (Zombie zombie : zombies) {
            RenderedZombie rendered = this.actors.get(zombie);
            if (rendered == null) {
                rendered = createRenderedZombie(zombie);
                if (rendered == null) {
                    continue;
                }
                this.actors.put(zombie, rendered);
                this.renderHost.addActor(rendered.root);
                prepareImpThrow(rendered, zombie);
            }
            updateRenderedZombie(rendered, zombie, delta);
            GameplayBoardDepthOrder.mark(
                    rendered.root,
                    zombie.getPosition().getY(),
                    GameplayBoardDepthOrder.ZOMBIE
            );
        }
        applyDepthOrder(zombies);
    }

    private void syncSpecialAbilityVisuals() {
        List<Plant> plants = new ArrayList<>(this.dataSource.getPlantsOnBoard());
        plants.removeIf(plant -> plant == null || plant.isDead() || plant.getPosition() == null);
        removeMissingPlantStates(plants);
        for (Plant plant : plants) {
            GameplayPlantCoverInspector.State cover = this.dataSource.getPlantCoverState(plant);
            PlantAbilityState current = PlantAbilityState.from(plant, cover);
            PlantAbilityState previous = this.plantAbilityStates.put(plant, current);
            if (previous == null) {
                continue;
            }
            if (current.freezeLevel > previous.freezeLevel) {
                playHunterThrow(plant);
            }
            if (current.octopusCovered && !previous.octopusCovered) {
                playOctopusThrow(plant);
            }
            if (current.transformed && !previous.transformed) {
                playWizardTransform(plant);
            }
            if (current.row == previous.row && current.column == previous.column + 1) {
                playFishermanReel(plant);
            }
        }
        syncKingKnighting();
        syncProjectileReflection();
    }

    private void removeMissingPlantStates(List<Plant> plants) {
        List<Plant> removed = new ArrayList<>();
        for (Plant plant : this.plantAbilityStates.keySet()) {
            if (!containsPlantIdentity(plants, plant)) {
                removed.add(plant);
            }
        }
        for (Plant plant : removed) {
            this.plantAbilityStates.remove(plant);
        }
    }

    private boolean containsPlantIdentity(List<Plant> plants, Plant wanted) {
        for (Plant plant : plants) {
            if (plant == wanted) {
                return true;
            }
        }
        return false;
    }

    private void playHunterThrow(Plant target) {
        Zombie hunter = findNearestAbilityZombie(target, true, "hunter");
        if (hunter == null) {
            return;
        }
        playAbilityClip(hunter, "throw");
        spawnAbilityProjectile(hunter, target, HUNTER_SNOWBALL, HUNTER_SNOWBALL_SPLAT, 0.30f, 0.52f, 0.62f);
    }

    private void playOctopusThrow(Plant target) {
        Zombie octopus = findNearestAbilityZombie(target, true, "octopus");
        if (octopus == null) {
            return;
        }
        playAbilityClip(octopus, "toss");
        spawnAbilityProjectile(octopus, target, OCTOPUS_PROJECTILE, null, 0.48f, 0f, 0.58f);
    }

    private void playWizardTransform(Plant target) {
        Zombie wizard = findNearestAbilityZombie(target, false, "wizard");
        if (wizard == null) {
            return;
        }
        playAbilityClip(wizard, "sheep", "special");
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float x = cellCenterX(target.getPosition().getX());
        float y = plantProjectileTargetY(target.getPosition().getY(), cellHeight);
        spawnAbilityImpact(WIZARD_MAGIC, x, y, cellWidth * 0.92f, target.getPosition().getY());
    }

    private void playFishermanReel(Plant target) {
        Zombie fisherman = findNearestAbilityZombie(target, true, "fisherman");
        if (fisherman == null) {
            return;
        }
        playAbilityClip(fisherman, "reel", "cast_loop", "cast");
        RenderedZombie rendered = this.actors.get(fisherman);
        if (rendered != null) {
            spawnFishingHook(rendered, target);
        }
    }

    private Zombie findNearestAbilityZombie(Plant target, boolean sameLane, String... tokens) {
        if (target == null || target.getPosition() == null) {
            return null;
        }
        Zombie best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : this.actors.keySet()) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null
                    || zombie.getDefinition() == null || zombie.getDefinition().getAlias() == null) {
                continue;
            }
            if (sameLane && zombie.getPosition().getY() != target.getPosition().getY()) {
                continue;
            }
            String alias = zombie.getDefinition().getAlias().toLowerCase(Locale.ROOT);
            if (!containsAny(alias, tokens)) {
                continue;
            }
            double vertical = Math.abs(zombie.getPosition().getY() - target.getPosition().getY());
            double distance = Math.abs(zombie.getExactX() - target.getPosition().getX()) + vertical * 1.6d;
            if (distance < bestDistance) {
                best = zombie;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && value.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void playAbilityClip(Zombie zombie, String... clips) {
        RenderedZombie rendered = this.actors.get(zombie);
        if (rendered == null || !(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            return;
        }
        String clip = rendered.animation.findClip(clips);
        if (clip == null) {
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), clip);
        actor.setLooping(false);
        rendered.currentClip = clip;
        rendered.specialClipRemaining = rendered.animation.getClipDuration(clip, SPECIAL_CLIP_SECONDS);
    }

    private float plantProjectileTargetY(int row, float cellHeight) {
        float anchorShift = GameplayWorldLayout.PLANT_GROUND_ANCHOR_FACTOR
                - PRE_ANCHOR_PLANT_BOTTOM_FACTOR;
        return cellBottomY(row) + cellHeight * (0.48f + anchorShift);
    }

    private void spawnAbilityProjectile(
            Zombie source,
            Plant target,
            String resourceId,
            String impactResourceId,
            float sizeFactor,
            float impactSizeFactor,
            float duration
    ) {
        RenderedZombie rendered = this.actors.get(source);
        Drawable drawable = resourceDrawable(resourceId);
        if (rendered == null || target == null || target.getPosition() == null || drawable == null) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float fromX = rendered.root.getX() + rendered.root.getWidth() * 0.43f;
        float fromY = rendered.root.getY() + rendered.root.getHeight() * 0.70f;
        float toX = cellCenterX(target.getPosition().getX());
        float toY = plantProjectileTargetY(target.getPosition().getY(), cellHeight);
        float size = Math.max(14f, cellWidth * sizeFactor);
        AbilityArcActor projectile = new AbilityArcActor(
                drawable,
                fromX,
                fromY,
                toX,
                toY,
                cellHeight * 0.72f,
                duration,
                size,
                target.getPosition().getY(),
                impactResourceId,
                impactSizeFactor
        );
        GameplayBoardDepthOrder.mark(projectile, target.getPosition().getY(), GameplayBoardDepthOrder.PROJECTILE);
        this.renderHost.addActor(projectile);
    }

    private void spawnFishingHook(RenderedZombie source, Plant target) {
        Drawable hookDrawable = resourceDrawable(FISHERMAN_HOOK);
        if (hookDrawable == null || target == null || target.getPosition() == null) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float fromX = source.root.getX() + source.root.getWidth() * 0.42f;
        float fromY = source.root.getY() + source.root.getHeight() * 0.58f;
        float toX = cellCenterX(target.getPosition().getX());
        float toY = plantProjectileTargetY(target.getPosition().getY(), cellHeight);
        Group effect = new Group();
        effect.setTouchable(Touchable.disabled);
        effect.setBounds(0f, 0f, getWidth(), getHeight());
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        Image line = new Image(PvzSkin.get().newDrawable(
                "white_pixel",
                new Color(0.24f, 0.18f, 0.12f, 0.90f)
        ));
        line.setBounds(fromX, fromY, length, Math.max(2f, cellHeight * 0.025f));
        line.setOrigin(0f, line.getHeight() * 0.5f);
        line.setRotation((float) Math.toDegrees(Math.atan2(dy, dx)));
        effect.addActor(line);
        Image hook = new Image(hookDrawable);
        hook.setScaling(Scaling.fit);
        float hookWidth = cellWidth * 0.30f;
        float hookHeight = cellHeight * 0.48f;
        hook.setBounds(toX - hookWidth * 0.5f, toY - hookHeight * 0.42f, hookWidth, hookHeight);
        effect.addActor(hook);
        GameplayBoardDepthOrder.mark(effect, target.getPosition().getY(), GameplayBoardDepthOrder.PROJECTILE);
        this.renderHost.addActor(effect);
        effect.addAction(Actions.sequence(
                Actions.delay(0.18f),
                Actions.fadeOut(0.38f),
                Actions.removeActor()
        ));
    }

    private void spawnProspectorSmoke(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null) {
            return;
        }
        int row = zombie == null || zombie.getPosition() == null ? 0 : zombie.getPosition().getY();
        float baseX = rendered.root.getX() + rendered.root.getWidth() * 0.52f;
        float baseY = rendered.root.getY() + rendered.root.getHeight() * 0.16f;
        for (int index = 0; index < 5; index++) {
            Image puff = new Image(PvzSkin.get().newDrawable(
                    "white_pixel",
                    new Color(0.32f + index * 0.035f, 0.32f + index * 0.035f, 0.32f + index * 0.035f, 0.62f)
            ));
            float size = Math.max(8f, rendered.root.getWidth() * (0.12f + index * 0.025f));
            puff.setBounds(baseX - size * 0.5f, baseY, size, size * 0.72f);
            puff.setOrigin(size * 0.5f, size * 0.36f);
            GameplayBoardDepthOrder.mark(puff, row, GameplayBoardDepthOrder.ZOMBIE);
            this.renderHost.addActor(puff);
            float side = (index - 2) * rendered.root.getWidth() * 0.10f;
            float duration = 0.42f + index * 0.05f;
            puff.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.moveBy(side, rendered.root.getHeight() * (0.30f + index * 0.05f), duration),
                            Actions.scaleTo(1.65f, 1.25f, duration),
                            Actions.fadeOut(duration)
                    ),
                    Actions.removeActor()
            ));
        }
    }

    private float cellCenterX(int column) {
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        return (column + 0.5f) * cellWidth;
    }

    private float cellBottomY(int row) {
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        return (GameplayBoardInteractionLayer.ROW_COUNT - 1 - row) * cellHeight;
    }

    private void spawnAbilityImpact(String resourceId, float x, float y, float size, int row) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null || size <= 0f) {
            return;
        }
        Image impact = new Image(drawable);
        impact.setScaling(Scaling.fit);
        impact.setTouchable(Touchable.disabled);
        impact.setBounds(x - size * 0.5f, y - size * 0.5f, size, size);
        impact.setOrigin(size * 0.5f, size * 0.5f);
        GameplayBoardDepthOrder.mark(impact, row, GameplayBoardDepthOrder.PROJECTILE);
        this.renderHost.addActor(impact);
        impact.setScale(0.72f);
        impact.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.scaleTo(1.12f, 1.12f, 0.18f),
                        Actions.fadeOut(0.48f)
                ),
                Actions.removeActor()
        ));
    }

    private void syncKingKnighting() {
        List<Zombie> live = new ArrayList<>(this.actors.keySet());
        List<Zombie> removed = new ArrayList<>();
        for (Zombie zombie : this.knightArmorCounts.keySet()) {
            if (!containsIdentity(live, zombie)) {
                removed.add(zombie);
            }
        }
        for (Zombie zombie : removed) {
            this.knightArmorCounts.remove(zombie);
        }
        for (Zombie zombie : live) {
            int current = knightArmorCount(zombie);
            Integer previous = this.knightArmorCounts.put(zombie, current);
            if (previous != null && current > previous) {
                Zombie king = findNearbyKing(zombie);
                if (king != null) {
                    playAbilityClip(king, "special");
                }
            }
        }
    }

    private int knightArmorCount(Zombie zombie) {
        if (zombie == null || zombie.getArmors() == null) {
            return 0;
        }
        int count = 0;
        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor == null || armor.isDestroyed() || armor.isDropped() || armor.getDefinition() == null) {
                continue;
            }
            ArmorType type = armor.getDefinition().getType();
            if (type == ArmorType.CROWN || type == ArmorType.SHOULDER_ARMOR) {
                count++;
            }
        }
        return count;
    }

    private Zombie findNearbyKing(Zombie target) {
        if (target == null || target.getPosition() == null) {
            return null;
        }
        Zombie best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : this.actors.keySet()) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null
                    || zombie.getDefinition() == null || zombie.getDefinition().getAlias() == null
                    || !zombie.getDefinition().getAlias().toLowerCase(Locale.ROOT).contains("darkking")) {
                continue;
            }
            int rowDistance = Math.abs(zombie.getPosition().getY() - target.getPosition().getY());
            double distance = Math.abs(zombie.getExactX() - target.getExactX()) + rowDistance * 1.4d;
            if (rowDistance <= 1 && distance <= 5.5d && distance < bestDistance) {
                best = zombie;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void syncProjectileReflection() {
        List<Projectile> projectiles = new ArrayList<>(this.dataSource.getProjectiles());
        List<Projectile> removed = new ArrayList<>();
        for (Projectile projectile : this.reflectedProjectiles.keySet()) {
            if (!containsProjectileIdentity(projectiles, projectile)) {
                removed.add(projectile);
            }
        }
        for (Projectile projectile : removed) {
            this.reflectedProjectiles.remove(projectile);
        }
        for (Projectile projectile : projectiles) {
            if (projectile == null || !projectile.isHostileToPlants()
                    || this.reflectedProjectiles.put(projectile, Boolean.TRUE) != null
                    || projectile.getPosition() == null) {
                continue;
            }
            Zombie juggler = findNearestZombieInRow(projectile.getPosition().getY(), projectile.getPosition().getX(),
                    "juggler", "jester");
            if (juggler != null) {
                playAbilityClip(juggler, "spin", "spinup", "spin_walk");
            }
        }
    }

    private boolean containsProjectileIdentity(List<Projectile> projectiles, Projectile wanted) {
        for (Projectile projectile : projectiles) {
            if (projectile == wanted) {
                return true;
            }
        }
        return false;
    }

    private Zombie findNearestZombieInRow(int row, int column, String... tokens) {
        Zombie best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : this.actors.keySet()) {
            if (zombie == null || zombie.isDead() || zombie.getPosition() == null
                    || zombie.getPosition().getY() != row || zombie.getDefinition() == null
                    || zombie.getDefinition().getAlias() == null
                    || !containsAny(zombie.getDefinition().getAlias().toLowerCase(Locale.ROOT), tokens)) {
                continue;
            }
            double distance = Math.abs(zombie.getExactX() - column);
            if (distance < bestDistance) {
                best = zombie;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void removeMissing(List<Zombie> zombies) {
        List<Zombie> removed = new ArrayList<>();
        for (Zombie zombie : this.actors.keySet()) {
            if (!containsIdentity(zombies, zombie)) {
                removed.add(zombie);
            }
        }
        for (Zombie zombie : removed) {
            RenderedZombie rendered = this.actors.remove(zombie);
            if (rendered != null) {
                playDeathAndRemove(rendered, zombie);
            }
        }
    }

    private void playDeathAndRemove(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null || rendered.root.getStage() == null) {
            if (rendered != null) {
                rendered.root.remove();
            }
            return;
        }
        if (zombie != null && zombie.isDead() && this.explosionDeathWindow > 0f) {
            playAshDeath(rendered, zombie);
            return;
        }
        if (zombie != null && zombie.isDead()
                && rendered.body instanceof PamAnimationActor
                && rendered.animation != null) {
            playBodyPartDrops(rendered, zombie);
            String deathClip = rendered.animation.getDeathClip();
            if (deathClip != null) {
                PamAnimationActor actor = (PamAnimationActor) rendered.body;
                actor.setAnimation(rendered.animation.getPath(), deathClip);
                actor.setLooping(false);
                float deathDuration = Math.min(2.4f, Math.max(0.38f,
                        rendered.animation.getClipDuration(deathClip, 0.48f)));
                rendered.root.addAction(Actions.sequence(
                        Actions.delay(deathDuration),
                        Actions.fadeOut(0.22f),
                        Actions.removeActor()
                ));
                return;
            }
        }
        rendered.root.setOrigin(rendered.root.getWidth() * 0.5f, rendered.root.getHeight() * 0.2f);
        rendered.root.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.rotateBy(-12f, 0.30f),
                        Actions.moveBy(0f, -rendered.root.getHeight() * 0.12f, 0.30f),
                        Actions.fadeOut(0.30f)
                ),
                Actions.removeActor()
        ));
    }

    private void playAshDeath(RenderedZombie rendered, Zombie zombie) {
        float rootX = rendered.root.getX();
        float rootY = rendered.root.getY();
        float width = Math.max(1f, rendered.root.getWidth());
        float height = Math.max(1f, rendered.root.getHeight());
        float sizeScale = ashSizeScale(rendered.alias);
        Image body = ashImage(ASH_MAIN);
        Image pile = ashImage(ASH_PILE);
        if (body != null) {
            placeAshBody(body, rootX, rootY, width, height, sizeScale, zombie);
        }
        if (pile != null) {
            placeAshPile(pile, rootX, rootY, width, height, sizeScale, zombie);
        }
        spawnAshDust(rootX, rootY, width, height, sizeScale, zombie);
        rendered.root.remove();
    }

    private float ashSizeScale(String alias) {
        String normalized = alias == null ? "" : alias.toLowerCase(Locale.ROOT);
        if (normalized.contains("gargantuar")) {
            return 1.35f;
        }
        return normalized.contains("imp") ? 0.72f : 1f;
    }

    private Image ashImage(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void placeAshBody(
            Image body, float rootX, float rootY, float width, float height, float scale, Zombie zombie
    ) {
        float bodyWidth = width * 1.10f * scale;
        float bodyHeight = bodyWidth * 0.92f;
        body.setBounds(
                rootX + width * 0.50f - bodyWidth * 0.50f,
                rootY + height * 0.20f,
                bodyWidth,
                bodyHeight
        );
        body.setOrigin(bodyWidth * 0.5f, bodyHeight * 0.12f);
        markAsh(body, zombie);
        this.renderHost.addActor(body);
        body.addAction(Actions.sequence(
                Actions.delay(0.10f),
                Actions.parallel(
                        Actions.moveBy(0f, -height * 0.13f, 0.34f),
                        Actions.scaleTo(1.03f, 0.42f, 0.34f)
                ),
                Actions.parallel(
                        Actions.fadeOut(0.26f),
                        Actions.scaleTo(1.12f, 0.20f, 0.26f)
                ),
                Actions.removeActor()
        ));
    }

    private void placeAshPile(
            Image pile, float rootX, float rootY, float width, float height, float scale, Zombie zombie
    ) {
        float pileWidth = width * 0.86f * scale;
        float pileHeight = pileWidth * 0.62f;
        pile.setBounds(
                rootX + width * 0.50f - pileWidth * 0.50f,
                rootY + height * 0.03f,
                pileWidth,
                pileHeight
        );
        pile.setOrigin(pileWidth * 0.5f, pileHeight * 0.5f);
        pile.getColor().a = 0f;
        markAsh(pile, zombie);
        this.renderHost.addActor(pile);
        pile.addAction(Actions.sequence(
                Actions.delay(0.22f),
                Actions.fadeIn(0.10f),
                Actions.delay(0.32f),
                Actions.parallel(
                        Actions.fadeOut(0.32f),
                        Actions.scaleTo(1.16f, 0.72f, 0.32f)
                ),
                Actions.removeActor()
        ));
    }

    private void spawnAshDust(
            float rootX, float rootY, float width, float height, float scale, Zombie zombie
    ) {
        String[] resources = {
                ASH_DUST_TALL, ASH_DUST_WIDE, ASH_DUST, ASH_DUST_TALL, ASH_DUST_WIDE, ASH_DUST
        };
        float[][] offsets = {
                {0.30f, 0.48f, -0.30f, 0.42f},
                {0.66f, 0.56f, 0.34f, 0.50f},
                {0.42f, 0.74f, -0.10f, 0.66f},
                {0.58f, 0.34f, 0.18f, 0.36f},
                {0.22f, 0.62f, -0.38f, 0.54f},
                {0.74f, 0.70f, 0.40f, 0.62f}
        };
        for (int index = 0; index < resources.length; index++) {
            spawnAshDustPiece(resources[index], offsets[index], rootX, rootY, width, height, scale, zombie, index);
        }
    }

    private void spawnAshDustPiece(
            String resourceId, float[] offset, float rootX, float rootY, float width, float height,
            float scale, Zombie zombie, int index
    ) {
        Image dust = ashImage(resourceId);
        if (dust == null) {
            return;
        }
        float dustWidth = width * (0.08f + index * 0.012f) * scale;
        float aspect = Math.max(0.35f, dust.getDrawable().getMinHeight()
                / Math.max(1f, dust.getDrawable().getMinWidth()));
        float dustHeight = dustWidth * aspect;
        dust.setBounds(
                rootX + width * offset[0] - dustWidth * 0.5f,
                rootY + height * offset[1] - dustHeight * 0.5f,
                dustWidth, dustHeight
        );
        dust.setOrigin(dustWidth * 0.5f, dustHeight * 0.5f);
        markAsh(dust, zombie);
        this.renderHost.addActor(dust);
        float duration = 0.42f + index * 0.035f;
        dust.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(width * offset[2], height * offset[3], duration),
                        Actions.rotateBy((index % 2 == 0 ? -1f : 1f) * (28f + index * 8f), duration),
                        Actions.fadeOut(duration)
                ),
                Actions.removeActor()
        ));
    }

    private void markAsh(Actor actor, Zombie zombie) {
        GameplayBoardDepthOrder.mark(
                actor,
                zombie == null || zombie.getPosition() == null ? 0 : zombie.getPosition().getY(),
                GameplayBoardDepthOrder.ZOMBIE
        );
    }

    private void playBodyPartDrops(RenderedZombie rendered, Zombie zombie) {
        if (rendered.animation == null || !(rendered.body instanceof PamAnimationActor)) {
            return;
        }
        spawnBodyPartDrop(rendered, zombie, "particle_head", -0.28f, 0.62f, -36f);
        spawnBodyPartDrop(rendered, zombie, "particle_arm", 0.30f, 0.48f, 48f);
    }

    private void spawnBodyPartDrop(
            RenderedZombie rendered,
            Zombie zombie,
            String partName,
            float horizontalFactor,
            float verticalFactor,
            float rotation
    ) {
        Map<String, Boolean> visibility = ZombieBodyPartVisibility.forParticle(
                this.assets.getPamPlayer(),
                rendered.animation.getPath(),
                partName
        );
        if (visibility.isEmpty()) {
            return;
        }
        String partClip = rendered.animation.getDeathClip();
        if (partClip == null) {
            partClip = ((PamAnimationActor) rendered.body).getClipName();
        }
        PamAnimationActor bodyPart = new PamAnimationActor(
                this.assets.getPamPlayer(),
                rendered.animation.getPath(),
                partClip,
                rendered.animation.getCanvasWidth(),
                rendered.animation.getCanvasHeight()
        );
        bodyPart.setTouchable(Touchable.disabled);
        bodyPart.setPartsVisibility(visibility);
        bodyPart.setBounds(
                rendered.body.getX(),
                rendered.body.getY(),
                rendered.body.getWidth(),
                rendered.body.getHeight()
        );
        Group drop = new Group();
        drop.setTouchable(Touchable.disabled);
        drop.setBounds(
                rendered.root.getX(),
                rendered.root.getY(),
                rendered.root.getWidth(),
                rendered.root.getHeight()
        );
        drop.addActor(bodyPart);
        GameplayBoardDepthOrder.mark(
                drop,
                zombie == null || zombie.getPosition() == null ? 0 : zombie.getPosition().getY(),
                GameplayBoardDepthOrder.ZOMBIE
        );
        this.renderHost.addActor(drop);
        drop.setOrigin(drop.getWidth() * 0.5f, drop.getHeight() * 0.45f);
        float dx = drop.getWidth() * horizontalFactor;
        float dy = drop.getHeight() * verticalFactor;
        drop.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(dx, dy, 0.25f),
                        Actions.rotateBy(rotation, 0.25f)
                ),
                Actions.parallel(
                        Actions.moveBy(dx * 0.35f, -dy * 1.18f, 0.34f),
                        Actions.rotateBy(rotation * 0.75f, 0.34f),
                        Actions.fadeOut(0.34f)
                ),
                Actions.removeActor()
        ));
    }

    private boolean containsIdentity(List<Zombie> zombies, Zombie wanted) {
        for (Zombie zombie : zombies) {
            if (zombie == wanted) {
                return true;
            }
        }
        return false;
    }

    private RenderedZombie createRenderedZombie(Zombie zombie) {
        String alias = zombie.getDefinition() == null ? "" : zombie.getDefinition().getAlias();
        ZombieAnimationCatalog.AnimationInfo animation = this.animationCatalog.find(alias);
        Actor body = createBody(alias, animation);
        if (body == null) {
            return null;
        }
        Group root = new RightEdgeClippedZombieGroup();
        root.setTouchable(Touchable.disabled);
        Image shadow = createZombieShadow();
        if (shadow != null) {
            root.addActor(shadow);
        }
        root.addActor(body);
        Actor butter = createButterOverlay();
        butter.setVisible(false);
        root.addActor(butter);
        Actor iceBlock = createFrozenBlock();
        iceBlock.setVisible(false);
        root.addActor(iceBlock);

        RenderedZombie rendered;
        if (!(body instanceof PamAnimationActor) || animation == null) {
            rendered = new RenderedZombie(root, body, butter, iceBlock, null, null, 0f, 0f, alias);
        } else {
            GroundSwatchMotion groundMotion = GroundSwatchMotion.create(
                    this.assets.getPamPlayer(),
                    animation.getPath(),
                    animation.getWalkClip()
            );
            float groundOffset = stableGroundOffset(animation);
            float centerOffset = stableCenterOffset(animation);
            rendered = new RenderedZombie(
                    root, body, butter, iceBlock, animation, groundMotion, groundOffset, centerOffset, alias
            );
        }
        rendered.shadow = shadow;
        rendered.piano = createPianoOverlay(alias);
        if (rendered.piano != null) {
            root.addActor(rendered.piano);
        }
        initializeAbilityState(rendered, zombie);
        return rendered;
    }

    private Image createZombieShadow() {
        Drawable drawable = resourceDrawable(ZOMBIE_SHADOW);
        if (drawable == null) {
            return null;
        }
        Image shadow = new Image(drawable);
        shadow.setScaling(Scaling.stretch);
        shadow.setTouchable(Touchable.disabled);
        return shadow;
    }

    private void layoutZombieShadow(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null || rendered.shadow == null || zombie == null) {
            return;
        }
        ZombieShadowProfile profile = zombieShadowProfile(rendered.alias);
        float rootWidth = rendered.root.getWidth();
        float rootHeight = rendered.root.getHeight();
        float lift = airborneVisualLift(rendered, zombie);
        float airFactor = Math.min(1f, lift / Math.max(1f, rootHeight * 1.05f));
        float widthScale = 1f - airFactor * 0.18f;
        float heightScale = 1f - airFactor * 0.30f;
        float shadowWidth = rootWidth * profile.widthFactor * widthScale;
        float shadowHeight = rootHeight * profile.heightFactor * heightScale;
        rendered.shadow.setBounds(
                rootWidth * (0.5f + profile.centerOffsetX) - shadowWidth * 0.5f,
                rootHeight * profile.yFactor - lift,
                shadowWidth,
                shadowHeight
        );
        rendered.shadow.getColor().a = profile.alpha * (1f - airFactor * 0.42f);
    }

    private float airborneVisualLift(RenderedZombie rendered, Zombie zombie) {
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float lift = 0f;

        if (rendered.prospectorFlightRemaining > 0f && rendered.prospectorFlightDuration > 0f) {
            float ratio = rendered.prospectorFlightRemaining / rendered.prospectorFlightDuration;
            float progress = 1f - ratio;
            lift += (float) Math.sin(Math.PI * progress) * cellHeight * 1.05f;
        }

        if (rendered.dodoLiftFactor > 0.001f) {
            float bob = (float) Math.sin(rendered.motionTime * 7f) * cellHeight * 0.035f;
            lift += cellHeight * 0.36f * rendered.dodoLiftFactor + bob;
        }

        if (rendered.throwRemaining > 0f && rendered.throwDuration > 0f) {
            float progress = 1f - rendered.throwRemaining / rendered.throwDuration;
            progress = Math.max(0f, Math.min(1f, progress));
            lift += 4f * progress * (1f - progress) * cellHeight * 1.55f;
        }
        return Math.max(0f, lift);
    }

    private ZombieShadowProfile zombieShadowProfile(String aliasValue) {
        String alias = normalizeAlias(aliasValue);
        ZombieShadowProfile profile = ZombieShadowProfile.of(0.66f, 0.215f, -0.040f, 0.78f);

        if (alias.contains("imp")) {
            return ZombieShadowProfile.of(0.48f, 0.175f, -0.034f, 0.72f);
        }
        if (alias.contains("gargantuar")) {
            return ZombieShadowProfile.of(1.18f, 0.300f, -0.050f, 0.84f);
        }
        if (alias.contains("allstar")) {
            return ZombieShadowProfile.of(0.92f, 0.255f, -0.045f, 0.82f);
        }
        if (alias.contains("piano")) {
            return ZombieShadowProfile.of(1.20f, 0.285f, -0.045f, 0.84f);
        }
        if (alias.contains("barrel") || alias.contains("arcade")
                || alias.contains("troglobite") || alias.contains("iceblock")) {
            return ZombieShadowProfile.of(0.96f, 0.265f, -0.044f, 0.82f);
        }
        if (alias.contains("dodo")) {
            return ZombieShadowProfile.of(0.72f, 0.205f, -0.038f, 0.72f);
        }
        if (alias.contains("fisherman") || alias.contains("octopus")
                || alias.contains("snorkel") || alias.contains("juggler")
                || alias.contains("wizard") || alias.contains("king")
                || alias.contains("hunter") || alias.contains("prospector")) {
            return ZombieShadowProfile.of(0.72f, 0.225f, -0.040f, 0.78f);
        }
        return profile;
    }

    private Actor createPianoOverlay(String alias) {
        if (alias == null || !alias.toLowerCase(Locale.ROOT).contains("piano")) {
            return null;
        }
        Drawable front = resourceDrawable(PIANO_FRONT);
        if (front == null) {
            return null;
        }
        Group group = new Group();
        group.setTouchable(Touchable.disabled);
        addPianoPart(group, "front", front);
        addPianoPart(group, "body", resourceDrawable(PIANO_BODY));
        addPianoPart(group, "top", resourceDrawable(PIANO_TOP));
        addPianoPart(group, "side", resourceDrawable(PIANO_SIDE));
        addPianoPart(group, "legLeft", resourceDrawable(PIANO_LEG_LEFT));
        addPianoPart(group, "legRight", resourceDrawable(PIANO_LEG_RIGHT));
        addPianoPart(group, "note", resourceDrawable(PIANO_NOTE));
        return group;
    }

    private void addPianoPart(Group group, String name, Drawable drawable) {
        if (group == null || drawable == null) {
            return;
        }
        Image image = new Image(drawable);
        image.setName(name);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        group.addActor(image);
    }

    private void initializeAbilityState(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null || zombie == null) {
            return;
        }
        rendered.lastExactX = zombie.getExactX();
        rendered.lastRow = zombie.getPosition() == null ? -1 : zombie.getPosition().getY();
        rendered.lastFlying = zombie.hasCondition(ZombieCondition.FLYING);
        rendered.lastCharging = isAllStarCharging(rendered, zombie);
    }

    private Actor createButterOverlay() {
        Drawable drawable = resourceDrawable(BUTTER_SPLAT);
        if (drawable != null) {
            Image image = new Image(drawable);
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            return image;
        }
        Group fallback = new Group();
        fallback.setTouchable(Touchable.disabled);
        Image center = new Image(PvzSkin.get().newDrawable(
                "white_pixel",
                new Color(1f, 0.86f, 0.25f, 0.92f)
        ));
        center.setBounds(0.18f, 0.18f, 0.64f, 0.56f);
        center.setRotation(-8f);
        fallback.addActor(center);
        Image shine = new Image(PvzSkin.get().newDrawable(
                "white_pixel",
                new Color(1f, 0.97f, 0.66f, 0.90f)
        ));
        shine.setBounds(0.30f, 0.55f, 0.34f, 0.14f);
        shine.setRotation(-12f);
        fallback.addActor(shine);
        return fallback;
    }

    private void prepareImpThrow(RenderedZombie impRendered, Zombie imp) {
        if (!looksLikeThrownImp(impRendered, imp)) {
            return;
        }
        Zombie parent = findThrowingGargantuar(imp);
        if (parent == null) {
            return;
        }
        RenderedZombie parentRendered = this.actors.get(parent);
        if (parentRendered == null) {
            return;
        }
        impRendered.throwOriginX = parent.getExactX();
        impRendered.throwDuration = IMP_THROW_SECONDS;
        impRendered.throwRemaining = IMP_THROW_SECONDS;
        playThrowClip(parentRendered);
    }

    private boolean looksLikeThrownImp(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null || zombie == null || zombie.getPosition() == null) {
            return false;
        }
        String alias = rendered.alias == null ? "" : rendered.alias.toLowerCase(Locale.ROOT);
        return alias.contains("imp")
                && Math.abs(zombie.getExactX() - 2d) <= 0.2d;
    }

    private Zombie findThrowingGargantuar(Zombie imp) {
        Zombie best = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Zombie candidate : this.actors.keySet()) {
            if (candidate == null || candidate.isDead() || candidate.getPosition() == null
                    || candidate.getDefinition() == null || imp.getPosition() == null
                    || candidate.getPosition().getY() != imp.getPosition().getY()) {
                continue;
            }
            String alias = candidate.getDefinition().getAlias();
            if (alias == null || !alias.toLowerCase(Locale.ROOT).contains("gargantuar")) {
                continue;
            }
            int maximumHealth = Math.max(1, candidate.getMaximumHealth());
            if (candidate.getHealth() > maximumHealth * 0.60f) {
                continue;
            }
            double distance = Math.abs(candidate.getExactX() - imp.getExactX());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private void playThrowClip(RenderedZombie rendered) {
        if (rendered == null) {
            return;
        }
        if (rendered.body instanceof PamAnimationActor && rendered.animation != null) {
            String clip = rendered.animation.getThrowClip();
            if (clip != null) {
                PamAnimationActor actor = (PamAnimationActor) rendered.body;
                actor.setAnimation(rendered.animation.getPath(), clip);
                actor.setLooping(false);
                rendered.currentClip = clip;
                rendered.specialClipRemaining = rendered.animation.getClipDuration(
                        clip,
                        SPECIAL_CLIP_SECONDS
                );
                return;
            }
        }
        rendered.root.clearActions();
        rendered.root.setOrigin(rendered.root.getWidth() * 0.5f, rendered.root.getHeight() * 0.2f);
        rendered.root.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.rotateBy(-7f, 0.18f),
                        Actions.scaleTo(1.03f, 0.98f, 0.18f)
                ),
                Actions.parallel(
                        Actions.rotateBy(7f, 0.22f),
                        Actions.scaleTo(1f, 1f, 0.22f)
                )
        ));
    }

    private Actor createFrozenBlock() {
        Drawable drawable = resourceDrawable(FROZEN_ZOMBIE_BLOCK);
        if (drawable != null) {
            Image image = new Image(drawable);
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            return image;
        }
        Group fallback = new Group();
        fallback.setTouchable(Touchable.disabled);
        Image fill = new Image(PvzSkin.get().newDrawable(
                "white_pixel",
                new Color(0.50f, 0.84f, 1f, 0.28f)
        ));
        fill.setBounds(0.06f, 0.04f, 0.88f, 0.92f);
        fallback.addActor(fill);
        Color edge = new Color(0.75f, 0.95f, 1f, 0.72f);
        addIceEdge(fallback, 0.04f, 0.03f, 0.92f, 0.035f, edge);
        addIceEdge(fallback, 0.04f, 0.93f, 0.92f, 0.035f, edge);
        addIceEdge(fallback, 0.04f, 0.03f, 0.035f, 0.935f, edge);
        addIceEdge(fallback, 0.925f, 0.03f, 0.035f, 0.935f, edge);
        Image shine = new Image(PvzSkin.get().newDrawable(
                "white_pixel",
                new Color(0.92f, 1f, 1f, 0.34f)
        ));
        shine.setBounds(0.16f, 0.73f, 0.12f, 0.18f);
        shine.setRotation(-18f);
        fallback.addActor(shine);
        return fallback;
    }

    private void addIceEdge(
            Group group,
            float x,
            float y,
            float width,
            float height,
            Color color
    ) {
        Image edge = new Image(PvzSkin.get().newDrawable("white_pixel", color));
        edge.setBounds(x, y, width, height);
        edge.setTouchable(Touchable.disabled);
        group.addActor(edge);
    }

    private Actor createBody(String alias, ZombieAnimationCatalog.AnimationInfo animation) {
        Actor obstacle = createObstacleBody(alias);
        if (obstacle != null) {
            return obstacle;
        }
        if (canUseAnimation(animation)) {
            PamAnimationActor actor = new PamAnimationActor(
                    this.assets.getPamPlayer(),
                    animation.getPath(),
                    animation.getWalkClip(),
                    animation.getCanvasWidth(),
                    animation.getCanvasHeight()
            );
            actor.setTouchable(Touchable.disabled);
            return actor;
        }
        ZombiePacketCatalog.PacketVisual packet = ZombiePacketCatalog.findPacket(alias);
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

    private Actor createObstacleBody(String alias) {
        String normalized = normalizeAlias(alias);
        String resourceId = switch (normalized) {
            case "iceblock" -> ICE_BLOCK_OBSTACLE;
            case "arcademachine" -> ARCADE_CABINET;
            case "barrelobstacle" -> BARREL_OBSTACLE;
            default -> null;
        };
        if (resourceId == null) {
            return null;
        }
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private String normalizeAlias(String alias) {
        return alias == null ? "" : alias.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private boolean canUseAnimation(ZombieAnimationCatalog.AnimationInfo animation) {
        if (animation == null || animation.getWalkClip() == null) {
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
            // missing optional static art leaves only that zombie unavailable
        }
        return null;
    }

    private void updateRenderedZombie(RenderedZombie rendered, Zombie zombie, float delta) {
        rendered.motionTime += delta;
        rendered.specialClipRemaining = Math.max(0f, rendered.specialClipRemaining - delta);
        updateEntityAbilityState(rendered, zombie, delta);
        notifyGargantuarImpact(rendered, zombie);
        position(rendered.root, zombie);
        applyEntityVisualMotion(rendered, zombie);
        applyImpThrowMotion(rendered, zombie, delta);
        layoutZombieShadow(rendered, zombie);
        ensureKnightVisual(rendered, zombie);
        updateClip(rendered, zombie);
        updateArmor(rendered, zombie);
        layoutBody(rendered, zombie);
        layoutPiano(rendered);
        applyFacing(rendered, zombie);
        updateButter(rendered, delta);
        updateFrozenBlock(rendered, zombie);
        updateDamageFlash(rendered, zombie, delta);
        updateStatusTint(rendered.body, zombie, rendered.damageFlashRemaining > 0f);
    }

    private void updateEntityAbilityState(RenderedZombie rendered, Zombie zombie, float delta) {
        trackRowShift(rendered, zombie, delta);
        trackProspectorState(rendered, zombie, delta);
        trackDodoState(rendered, zombie, delta);
        trackAllStarState(rendered, zombie);
    }

    private void trackRowShift(RenderedZombie rendered, Zombie zombie, float delta) {
        if (zombie == null || zombie.getPosition() == null) {
            return;
        }
        int row = zombie.getPosition().getY();
        if (rendered.lastRow >= 0 && row != rendered.lastRow) {
            rendered.rowSlideOffsetFactor = row - rendered.lastRow;
            rendered.rowSlideRemaining = ROW_SLIDE_SECONDS;
            if (!aliasContains(rendered, "piano")) {
                Zombie piano = findLivingZombie("piano");
                if (piano != null) {
                    playAbilityClip(piano, "play");
                }
            }
        }
        rendered.lastRow = row;
        rendered.rowSlideRemaining = Math.max(0f, rendered.rowSlideRemaining - delta);
    }

    private void trackProspectorState(RenderedZombie rendered, Zombie zombie, float delta) {
        if (!aliasContains(rendered, "prospector") || zombie == null) {
            return;
        }
        double currentX = zombie.getExactX();
        if (!Double.isNaN(rendered.lastExactX) && rendered.lastExactX - currentX > 2.0d) {
            rendered.prospectorFlightOriginX = rendered.lastExactX;
            rendered.prospectorFlightDuration = PROSPECTOR_FLIGHT_SECONDS;
            rendered.prospectorFlightRemaining = PROSPECTOR_FLIGHT_SECONDS;
            rendered.prospectorLandingPlayed = false;
            rendered.prospectorSpent = true;
            playAbilityClip(zombie, "blastoff");
            spawnProspectorSmoke(rendered, zombie);
        }
        rendered.lastExactX = currentX;
        rendered.prospectorFlightRemaining = Math.max(0f, rendered.prospectorFlightRemaining - delta);
        if (rendered.prospectorFlightRemaining > 0f
                && rendered.prospectorFlightRemaining <= 0.34f
                && !rendered.prospectorLandingPlayed
                && rendered.specialClipRemaining <= 0f) {
            rendered.prospectorLandingPlayed = true;
            playAbilityClip(zombie, "land");
        }
    }

    private void trackDodoState(RenderedZombie rendered, Zombie zombie, float delta) {
        if (!aliasContains(rendered, "dodo") || zombie == null) {
            return;
        }
        boolean flying = zombie.hasCondition(ZombieCondition.FLYING);
        if (flying && !rendered.lastFlying) {
            playAbilityClip(zombie, "fly_start");
        } else if (!flying && rendered.lastFlying) {
            playAbilityClip(zombie, "fly_end");
        }
        rendered.lastFlying = flying;
        float target = flying ? 1f : 0f;
        float step = Math.min(1f, Math.max(0f, delta) * 4.5f);
        rendered.dodoLiftFactor += (target - rendered.dodoLiftFactor) * step;
    }

    private void trackAllStarState(RenderedZombie rendered, Zombie zombie) {
        if (!aliasContains(rendered, "allstar") || zombie == null) {
            return;
        }
        boolean charging = isAllStarCharging(rendered, zombie);
        if (!charging && rendered.lastCharging) {
            playAbilityClip(zombie, "tackle");
        }
        rendered.lastCharging = charging;
    }

    private boolean isAllStarCharging(RenderedZombie rendered, Zombie zombie) {
        if (!aliasContains(rendered, "allstar") || zombie == null || zombie.getDefinition() == null) {
            return false;
        }
        double baseSpeed = Math.max(0.0001d, zombie.getDefinition().getSpeed());
        return zombie.getCurrentSpeed() > baseSpeed * 1.20d;
    }

    private void applyEntityVisualMotion(RenderedZombie rendered, Zombie zombie) {
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        if (rendered.rowSlideRemaining > 0f) {
            float ratio = rendered.rowSlideRemaining / ROW_SLIDE_SECONDS;
            rendered.root.moveBy(0f, rendered.rowSlideOffsetFactor * cellHeight * ratio);
        }
        if (rendered.prospectorFlightRemaining > 0f && rendered.prospectorFlightDuration > 0f) {
            float ratio = rendered.prospectorFlightRemaining / rendered.prospectorFlightDuration;
            float progress = 1f - ratio;
            float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
            float horizontal = (float) (rendered.prospectorFlightOriginX - zombie.getExactX())
                    * cellWidth * ratio;
            float arc = (float) Math.sin(Math.PI * progress) * cellHeight * 1.05f;
            rendered.root.moveBy(horizontal, arc);
        }
        if (rendered.dodoLiftFactor > 0.001f) {
            float bob = (float) Math.sin(rendered.motionTime * 7f) * cellHeight * 0.035f;
            rendered.root.moveBy(0f, cellHeight * 0.36f * rendered.dodoLiftFactor + bob);
        }
    }

    private void applyFacing(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null || rendered.body == null || zombie == null) {
            return;
        }
        ZombieBehavior behavior = zombie.getBehavior();
        int direction = behavior == null
                ? (zombie.hasCondition(ZombieCondition.HYPNOTIZED) ? 1 : -1)
                : behavior.getMovementDirection(zombie);
        rendered.body.setOrigin(rendered.body.getWidth() * 0.5f, rendered.body.getHeight() * 0.5f);
        rendered.body.setScaleX(direction > 0 ? -1f : 1f);
    }

    private void layoutPiano(RenderedZombie rendered) {
        if (!(rendered.piano instanceof Group)) {
            return;
        }
        Group piano = (Group) rendered.piano;
        float rootWidth = rendered.root.getWidth();
        float rootHeight = rendered.root.getHeight();
        float width = rootWidth * 1.48f;
        float height = rootHeight * 1.12f;
        piano.setBounds(-rootWidth * 0.82f, -rootHeight * 0.08f, width, height);
        setPianoPartBounds(piano.findActor("front"), width * 0.03f, height * 0.20f, width * 0.82f, height * 0.58f);
        setPianoPartBounds(piano.findActor("body"), width * 0.08f, height * 0.08f, width * 0.72f, height * 0.40f);
        setPianoPartBounds(piano.findActor("top"), width * 0.01f, height * 0.60f, width * 0.84f, height * 0.18f);
        setPianoPartBounds(piano.findActor("side"), width * 0.70f, height * 0.09f, width * 0.27f, height * 0.68f);
        setPianoPartBounds(piano.findActor("legLeft"), width * 0.12f, height * 0.01f, width * 0.15f, height * 0.38f);
        setPianoPartBounds(piano.findActor("legRight"), width * 0.68f, height * 0.01f, width * 0.11f, height * 0.36f);
        Actor note = piano.findActor("note");
        float noteY = height * (0.82f + (float) Math.sin(rendered.motionTime * 6f) * 0.035f);
        setPianoPartBounds(note, width * 0.43f, noteY, width * 0.12f, height * 0.13f);
    }

    private void setPianoPartBounds(Actor actor, float x, float y, float width, float height) {
        if (actor != null) {
            actor.setBounds(x, y, width, height);
        }
    }

    private Zombie findLivingZombie(String token) {
        for (Zombie candidate : this.actors.keySet()) {
            if (candidate == null || candidate.isDead() || candidate.getDefinition() == null
                    || candidate.getDefinition().getAlias() == null) {
                continue;
            }
            if (candidate.getDefinition().getAlias().toLowerCase(Locale.ROOT).contains(token)) {
                return candidate;
            }
        }
        return null;
    }

    private void applyImpThrowMotion(RenderedZombie rendered, Zombie zombie, float delta) {
        if (rendered.throwRemaining <= 0f || rendered.throwDuration <= 0f || zombie == null) {
            return;
        }
        float progress = 1f - rendered.throwRemaining / rendered.throwDuration;
        progress = Math.max(0f, Math.min(1f, progress));
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float horizontalOffset = (float) (rendered.throwOriginX - zombie.getExactX())
                * cellWidth * (1f - progress);
        float arc = 4f * progress * (1f - progress) * cellHeight * 1.55f;
        rendered.root.moveBy(horizontalOffset, arc);
        rendered.root.setRotation((1f - progress) * -8f + progress * 8f);
        rendered.throwRemaining = Math.max(0f, rendered.throwRemaining - delta);
        if (rendered.throwRemaining <= 0f) {
            rendered.root.setRotation(0f);
        }
    }

    private void notifyGargantuarImpact(RenderedZombie rendered, Zombie zombie) {
        boolean attacking = zombie != null && zombie.isAttacking();
        boolean startedAttack = attacking && !rendered.lastAttacking;
        rendered.lastAttacking = attacking;
        if (!startedAttack || this.gargantuarImpactListener == null) {
            return;
        }
        String alias = rendered.alias == null ? "" : rendered.alias.toLowerCase(Locale.ROOT);
        if (alias.contains("gargantuar")) {
            this.gargantuarImpactListener.run();
        }
    }

    private void position(Group root, Zombie zombie) {
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float centerX = (float) ((zombie.getExactX() + 0.5d) * cellWidth);
        float tileBottom = (
                GameplayBoardInteractionLayer.ROW_COUNT - 1 - zombie.getPosition().getY()
        ) * cellHeight;
        root.setBounds(
                centerX - cellWidth / 2f,
                tileBottom + cellHeight * GameplayWorldLayout.ZOMBIE_GROUND_ANCHOR_FACTOR,
                cellWidth,
                cellHeight
        );
    }

    private void layoutBody(RenderedZombie rendered, Zombie zombie) {
        if (layoutObstacleBody(rendered)) {
            return;
        }
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            GameplayStaticEntityLayout.groundedFit(
                    rendered.body,
                    rendered.root.getWidth(),
                    rendered.root.getHeight(),
                    STATIC_WIDTH_FACTOR,
                    STATIC_HEIGHT_FACTOR,
                    STATIC_BOTTOM_FACTOR
            );
            float phase = rendered.motionTime * (zombie.isAttacking() ? 9f : 5f);
            float wave = (float) Math.sin(phase);
            float step = zombie.isAttacking() ? -Math.abs(wave) * 0.055f : wave * 0.018f;
            float bob = zombie.isAttacking() ? 0f : Math.abs(wave) * 0.018f;
            rendered.body.moveBy(
                    rendered.root.getWidth() * step,
                    rendered.root.getHeight() * bob
            );
            rendered.body.setOrigin(rendered.body.getWidth() * 0.5f, rendered.body.getHeight() * 0.18f);
            rendered.body.setRotation(wave * (zombie.isAttacking() ? 2.8f : 1.2f));
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        float actorWidth = GameplayPamScale.actorWidth(rendered.animation.getCanvasWidth());
        float actorHeight = GameplayPamScale.actorHeight(rendered.animation.getCanvasHeight());
        float gaitOffset = zombie.isAttacking() || rendered.groundMotion == null
                ? 0f : rendered.groundMotion.offsetX(actor.getStateTime());
        float groundCorrection = normalizeAlias(rendered.alias).contains("gargantuar")
                ? rendered.root.getHeight() * GARGANTUAR_GROUND_CORRECTION_FACTOR
                : 0f;
        actor.setBounds(
                rendered.root.getWidth() / 2f + rendered.centerOffset + gaitOffset - actorWidth / 2f,
                rendered.groundOffset - actorHeight / 2f - groundCorrection,
                actorWidth,
                actorHeight
        );
    }

    private boolean layoutObstacleBody(RenderedZombie rendered) {
        String alias = normalizeAlias(rendered.alias);
        float widthFactor;
        float heightFactor;
        float bottomFactor;
        switch (alias) {
            case "arcademachine" -> {
                widthFactor = 0.88f;
                heightFactor = 1.28f;
                bottomFactor = -0.01f;
            }
            case "iceblock" -> {
                widthFactor = 0.82f;
                heightFactor = 1.20f;
                bottomFactor = -0.02f;
            }
            case "barrelobstacle" -> {
                widthFactor = 0.82f;
                heightFactor = 0.86f;
                bottomFactor = 0.01f;
            }
            default -> {
                return false;
            }
        }
        GameplayStaticEntityLayout.groundedFit(
                rendered.body,
                rendered.root.getWidth(),
                rendered.root.getHeight(),
                widthFactor,
                heightFactor,
                bottomFactor
        );
        return true;
    }

    private float stableCenterOffset(ZombieAnimationCatalog.AnimationInfo animation) {
        String clip = animation.getPreviewClip();
        if (clip == null) {
            clip = animation.getWalkClip();
        }
        try {
            Rectangle bounds = this.assets.getPamPlayer().bounds(animation.getPath(), clip);
            if (bounds != null) {
                return -(bounds.x + bounds.width / 2f) * GameplayPamScale.WORLD_SCALE;
            }
        } catch (RuntimeException ignored) {
        }
        return 0f;
    }

    private float stableGroundOffset(ZombieAnimationCatalog.AnimationInfo animation) {
        String clip = animation.getPreviewClip();
        if (clip == null) {
            clip = animation.getWalkClip();
        }
        try {
            Rectangle bounds = this.assets.getPamPlayer().bounds(animation.getPath(), clip);
            if (bounds != null) {
                return (bounds.y + bounds.height) * GameplayPamScale.WORLD_SCALE;
            }
        } catch (RuntimeException ignored) {
            // pam center rooye lane baseline mimone agar clip bounds nabashe
        }
        return 0f;
    }

    private void updateButter(RenderedZombie rendered, float delta) {
        rendered.butterRemaining = Math.max(0f, rendered.butterRemaining - delta);
        boolean visible = rendered.butterRemaining > 0f;
        rendered.butter.setVisible(visible);
        if (!visible) {
            return;
        }
        float width = rendered.root.getWidth() * 0.78f;
        float height = rendered.root.getHeight() * 0.54f;
        rendered.butter.setBounds(
                rendered.root.getWidth() * 0.5f - width * 0.5f,
                rendered.root.getHeight() * 0.72f,
                width,
                height
        );
        if (rendered.butter instanceof Group) {
            Group group = (Group) rendered.butter;
            if (group.getChildren().size >= 2) {
                group.getChildren().get(0).setBounds(
                        width * 0.18f, height * 0.18f, width * 0.64f, height * 0.56f
                );
                group.getChildren().get(1).setBounds(
                        width * 0.30f, height * 0.55f, width * 0.34f, height * 0.14f
                );
            }
        }
    }

    private void updateFrozenBlock(RenderedZombie rendered, Zombie zombie) {
        boolean frozen = zombie.isTerrainFrozen();
        rendered.body.setVisible(true);
        rendered.iceBlock.setVisible(frozen);
        if (!frozen) {
            return;
        }
        float width = rendered.root.getWidth() * 1.20f;
        float height = rendered.root.getHeight() * 1.85f;
        rendered.iceBlock.setBounds(
                rendered.root.getWidth() / 2f - width / 2f,
                -rendered.root.getHeight() * 0.08f,
                width,
                height
        );
        if (rendered.iceBlock instanceof Group) {
            layoutFallbackIce((Group) rendered.iceBlock, width, height);
        }
    }

    private void layoutFallbackIce(Group group, float width, float height) {
        if (group.getChildren().size < 6) {
            return;
        }
        group.getChildren().get(0).setBounds(width * 0.06f, height * 0.04f, width * 0.88f, height * 0.92f);
        group.getChildren().get(1).setBounds(width * 0.04f, height * 0.03f, width * 0.92f, height * 0.035f);
        group.getChildren().get(2).setBounds(width * 0.04f, height * 0.93f, width * 0.92f, height * 0.035f);
        group.getChildren().get(3).setBounds(width * 0.04f, height * 0.03f, width * 0.035f, height * 0.935f);
        group.getChildren().get(4).setBounds(width * 0.925f, height * 0.03f, width * 0.035f, height * 0.935f);
        group.getChildren().get(5).setBounds(width * 0.16f, height * 0.73f, width * 0.12f, height * 0.18f);
    }

    private void ensureKnightVisual(RenderedZombie rendered, Zombie zombie) {
        if (rendered == null || zombie == null || knightArmorCount(zombie) == 0) {
            return;
        }
        ZombieAnimationCatalog.AnimationInfo knightAnimation = this.animationCatalog.find("ZombieDarkArmor3");
        if (knightAnimation == null || knightAnimation == rendered.animation || !canUseAnimation(knightAnimation)) {
            return;
        }
        Actor knightBody = createBody("ZombieDarkArmor3", knightAnimation);
        if (!(knightBody instanceof PamAnimationActor)) {
            return;
        }
        rendered.body.remove();
        rendered.root.addActorAt(0, knightBody);
        rendered.body = knightBody;
        rendered.animation = knightAnimation;
        rendered.groundMotion = GroundSwatchMotion.create(
                this.assets.getPamPlayer(),
                knightAnimation.getPath(),
                knightAnimation.getWalkClip()
        );
        rendered.groundOffset = stableGroundOffset(knightAnimation);
        rendered.centerOffset = stableCenterOffset(knightAnimation);
        rendered.currentClip = knightAnimation.getWalkClip();
        rendered.specialClipRemaining = 0f;
    }

    private void updateClip(RenderedZombie rendered, Zombie zombie) {
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null
                || rendered.specialClipRemaining > 0f) {
            return;
        }
        String wanted = preferredMovementClip(rendered, zombie);
        if (wanted == null || wanted.equals(rendered.currentClip)) {
            return;
        }
        PamAnimationActor actor = (PamAnimationActor) rendered.body;
        actor.setAnimation(rendered.animation.getPath(), wanted);
        actor.setLooping(true);
        rendered.currentClip = wanted;
    }

    private String preferredMovementClip(RenderedZombie rendered, Zombie zombie) {
        if (aliasContains(rendered, "piano")) {
            String play = rendered.animation.findClip("play");
            if (play != null) {
                return play;
            }
        }
        if (aliasContains(rendered, "dodo") && zombie != null
                && zombie.hasCondition(ZombieCondition.FLYING)) {
            String fly = rendered.animation.findClip("fly_loop", "fly");
            if (fly != null) {
                return fly;
            }
        }
        if (aliasContains(rendered, "prospector") && rendered.prospectorFlightRemaining > 0f) {
            String fly = rendered.animation.findClip("fly");
            if (fly != null) {
                return fly;
            }
        }
        if (aliasContains(rendered, "allstar") && isAllStarCharging(rendered, zombie)) {
            String run = rendered.animation.findClip("run");
            if (run != null) {
                return run;
            }
        }
        if (aliasContains(rendered, "newspaper")) {
            boolean intact = hasLiveArmorOfType(zombie, ArmorType.NEWSPAPER);
            String newspaper = zombie != null && zombie.isAttacking()
                    ? rendered.animation.findClip("eat_newspaper")
                    : rendered.animation.findClip("walk_newspaper");
            if (intact && newspaper != null) {
                return newspaper;
            }
        }
        if (zombie != null && zombie.isAttacking()) {
            return rendered.animation.getAttackClip();
        }
        BlockPusherBehavior pusher = zombie == null ? null : zombie.findBehavior(BlockPusherBehavior.class);
        if (pusher != null && hasLivingPushedObstacle(pusher)) {
            String push = rendered.animation.findClip("push");
            if (push != null) {
                return push;
            }
        }
        return rendered.animation.getWalkClip();
    }

    private boolean hasLivingPushedObstacle(BlockPusherBehavior pusher) {
        if (pusher == null || pusher.getObstacles() == null) {
            return false;
        }
        for (Zombie obstacle : pusher.getObstacles()) {
            if (obstacle != null && !obstacle.isDead()) {
                return true;
            }
        }
        return false;
    }

    private void updateArmor(RenderedZombie rendered, Zombie zombie) {
        if (!(rendered.body instanceof PamAnimationActor) || rendered.animation == null) {
            updateStaticArmor(rendered, zombie);
            return;
        }
        trackArmorDrops(rendered, zombie);
        Map<String, Boolean> visibility = new HashMap<>(ZombieArmorVisibility.forArmors(
                this.assets.getPamPlayer(),
                rendered.animation.getPath(),
                zombie.getArmors()
        ));
        applyTorchPartVisibility(visibility, rendered, zombie);
        applyProspectorDynamiteVisibility(visibility, rendered);
        ((PamAnimationActor) rendered.body).setPartsVisibility(visibility);
    }

    private void applyTorchPartVisibility(
            Map<String, Boolean> visibility,
            RenderedZombie rendered,
            Zombie zombie
    ) {
        TorchBearerBehavior torch = zombie == null ? null : zombie.findBehavior(TorchBearerBehavior.class);
        if (torch == null || torch.isTorchLit() || rendered.animation == null) {
            return;
        }
        try {
            PamPlayer.AnimationPart root = this.assets.getPamPlayer().getParts(rendered.animation.getPath());
            hideTorchFireParts(root, visibility);
        } catch (RuntimeException ignored) {
        }
    }

    private void hideTorchFireParts(PamPlayer.AnimationPart part, Map<String, Boolean> visibility) {
        if (part == null) {
            return;
        }
        if (part.name != null) {
            String name = part.name.toLowerCase(Locale.ROOT);
            if (name.contains("torch_fire") || name.contains("torch_end_lit")
                    || name.contains("torch_flame") || name.contains("flame")) {
                visibility.put(part.name, Boolean.FALSE);
            }
        }
        for (PamPlayer.AnimationPart child : part.children) {
            hideTorchFireParts(child, visibility);
        }
    }

    private void applyProspectorDynamiteVisibility(
            Map<String, Boolean> visibility, RenderedZombie rendered
    ) {
        if (!aliasContains(rendered, "prospector") || rendered.animation == null) {
            return;
        }
        try {
            PamPlayer.AnimationPart root = this.assets.getPamPlayer().getParts(rendered.animation.getPath());
            applyProspectorDynamiteVisibility(root, visibility, rendered);
        } catch (RuntimeException ignored) {
        }
    }

    private void applyProspectorDynamiteVisibility(
            PamPlayer.AnimationPart part, Map<String, Boolean> visibility, RenderedZombie rendered
    ) {
        if (part == null) {
            return;
        }
        if (part.name != null) {
            String name = part.name.toLowerCase(Locale.ROOT);
            if (name.contains("dynamite")) {
                boolean burning = name.contains("burning") || name.contains("fuse");
                boolean extinguished = name.contains("extinguished");
                boolean burnt = name.contains("burnt");
                boolean main = name.contains("dynamite_main");
                if (rendered.prospectorSpent) {
                    if (burning || extinguished || main) {
                        visibility.put(part.name, Boolean.FALSE);
                    }
                    if (burnt) {
                        visibility.put(part.name, Boolean.TRUE);
                    }
                } else if (rendered.prospectorExtinguished) {
                    if (burning || burnt) {
                        visibility.put(part.name, Boolean.FALSE);
                    }
                    if (extinguished || main) {
                        visibility.put(part.name, Boolean.TRUE);
                    }
                } else if (extinguished || burnt) {
                    visibility.put(part.name, Boolean.FALSE);
                }
            }
        }
        for (PamPlayer.AnimationPart child : part.children) {
            applyProspectorDynamiteVisibility(child, visibility, rendered);
        }
    }

    private boolean aliasContains(RenderedZombie rendered, String token) {
        return rendered != null && token != null && rendered.alias != null
                && rendered.alias.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean hasLiveArmorOfType(Zombie zombie, ArmorType type) {
        if (zombie == null || type == null || zombie.getArmors() == null) {
            return false;
        }
        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor != null && armor.getDefinition() != null
                    && armor.getDefinition().getType() == type
                    && !armor.isDestroyed() && !armor.isDropped()) {
                return true;
            }
        }
        return false;
    }

    private Plant findNearestMagnet(Zombie zombie) {
        if (zombie == null || zombie.getPosition() == null) {
            return null;
        }
        Plant best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Plant plant : this.dataSource.getPlantsOnBoard()) {
            if (plant == null || plant.isDead() || plant.getPosition() == null || plant.getName() == null
                    || !plant.getName().toLowerCase(Locale.ROOT).contains("magnet")) {
                continue;
            }
            int dx = Math.abs(plant.getPosition().getX() - zombie.getPosition().getX());
            int dy = Math.abs(plant.getPosition().getY() - zombie.getPosition().getY());
            int distance = Math.max(dx, dy);
            if (distance <= 3 && distance < bestDistance) {
                best = plant;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void playMagnetArmorPull(
            RenderedZombie rendered, ZombieArmor armor, Zombie zombie, Plant magnet
    ) {
        if (rendered.animation == null || magnet == null || magnet.getPosition() == null) {
            return;
        }
        Map<String, Boolean> visibility = ZombieArmorVisibility.forPulledArmor(
                this.assets.getPamPlayer(), rendered.animation.getPath(), armor
        );
        if (visibility.isEmpty()) {
            return;
        }
        String clip = rendered.body instanceof PamAnimationActor
                ? ((PamAnimationActor) rendered.body).getClipName()
                : rendered.animation.getWalkClip();
        PamAnimationActor piece = new PamAnimationActor(
                this.assets.getPamPlayer(), rendered.animation.getPath(), clip,
                rendered.animation.getCanvasWidth(), rendered.animation.getCanvasHeight()
        );
        piece.setTouchable(Touchable.disabled);
        piece.setPartsVisibility(visibility);
        piece.setBounds(
                rendered.body.getX(), rendered.body.getY(),
                rendered.body.getWidth(), rendered.body.getHeight()
        );
        Group pull = new Group();
        pull.setTouchable(Touchable.disabled);
        pull.setBounds(rendered.root.getX(), rendered.root.getY(), rendered.root.getWidth(), rendered.root.getHeight());
        pull.addActor(piece);
        int row = zombie == null || zombie.getPosition() == null ? 0 : zombie.getPosition().getY();
        GameplayBoardDepthOrder.mark(pull, row, GameplayBoardDepthOrder.COVER_PLANT);
        this.renderHost.addActor(pull);
        float targetX = cellCenterX(magnet.getPosition().getX());
        float targetY = cellBottomY(magnet.getPosition().getY()) + pull.getHeight() * 0.46f;
        float dx = targetX - (pull.getX() + pull.getWidth() * 0.5f);
        float dy = targetY - (pull.getY() + pull.getHeight() * 0.5f);
        pull.setOrigin(pull.getWidth() * 0.5f, pull.getHeight() * 0.5f);
        pull.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(dx, dy, 0.48f),
                        Actions.rotateBy(240f, 0.48f),
                        Actions.scaleTo(0.55f, 0.55f, 0.48f)
                ),
                Actions.fadeOut(0.12f),
                Actions.removeActor()
        ));
    }

    private void trackArmorDrops(RenderedZombie rendered, Zombie zombie) {
        if (zombie == null || zombie.getArmors() == null) {
            return;
        }
        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor == null) {
                continue;
            }
            boolean live = !armor.isDestroyed() && !armor.isDropped();
            Boolean previous = rendered.armorLiveState.put(armor, live);
            if (!Boolean.TRUE.equals(previous) || live) {
                continue;
            }
            if (armor.getDefinition() != null && armor.getDefinition().getType() == ArmorType.NEWSPAPER
                    && aliasContains(rendered, "newspaper")) {
                playAbilityClip(zombie, "newspaper_defeat");
            }
            Plant magnet = armor.isDropped() && armor.getCurrentHealth() > 0
                    ? findNearestMagnet(zombie) : null;
            if (magnet != null) {
                playMagnetArmorPull(rendered, armor, zombie, magnet);
                if (this.magnetCatchListener != null) {
                    this.magnetCatchListener.accept(magnet);
                }
            } else {
                playArmorDrop(rendered, armor, zombie.getPosition() == null ? 0 : zombie.getPosition().getY());
            }
        }
    }

    private void playArmorDrop(RenderedZombie rendered, ZombieArmor armor, int row) {
        Map<String, Boolean> visibility = ZombieArmorVisibility.forDroppedArmor(
                this.assets.getPamPlayer(),
                rendered.animation.getPath(),
                armor
        );
        if (visibility.isEmpty()) {
            return;
        }
        String clip = rendered.body instanceof PamAnimationActor
                ? ((PamAnimationActor) rendered.body).getClipName()
                : rendered.animation.getWalkClip();
        PamAnimationActor piece = new PamAnimationActor(
                this.assets.getPamPlayer(),
                rendered.animation.getPath(),
                clip,
                rendered.animation.getCanvasWidth(),
                rendered.animation.getCanvasHeight()
        );
        piece.setTouchable(Touchable.disabled);
        piece.setPartsVisibility(visibility);
        piece.setBounds(
                rendered.body.getX(),
                rendered.body.getY(),
                rendered.body.getWidth(),
                rendered.body.getHeight()
        );

        Group drop = new Group();
        drop.setTouchable(Touchable.disabled);
        drop.setBounds(
                rendered.root.getX(),
                rendered.root.getY(),
                rendered.root.getWidth(),
                rendered.root.getHeight()
        );
        drop.addActor(piece);
        GameplayBoardDepthOrder.mark(drop, row, GameplayBoardDepthOrder.COVER_PLANT);
        this.renderHost.addActor(drop);
        drop.setOrigin(drop.getWidth() * 0.5f, drop.getHeight() * 0.45f);
        float horizontal = Math.max(16f, drop.getWidth() * 0.38f);
        float vertical = Math.max(24f, drop.getHeight() * 0.58f);
        drop.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(horizontal, vertical, 0.30f),
                        Actions.rotateBy(68f, 0.30f)
                ),
                Actions.parallel(
                        Actions.moveBy(horizontal * 0.55f, -vertical * 1.05f, 0.34f),
                        Actions.rotateBy(72f, 0.34f),
                        Actions.fadeOut(0.34f)
                ),
                Actions.removeActor()
        ));
    }

    private void updateStaticArmor(RenderedZombie rendered, Zombie zombie) {
        if (!(rendered.body instanceof Image)) {
            return;
        }
        ZombiePacketCatalog.PacketVisual packet = ZombiePacketCatalog.findGameplayPacket(
                rendered.alias,
                hasLiveArmor(zombie)
        );
        if (packet == null || packet.getResourceId().equals(rendered.staticResourceId)) {
            return;
        }
        Drawable drawable = resourceDrawable(packet.getResourceId());
        if (drawable == null) {
            return;
        }
        ((Image) rendered.body).setDrawable(drawable);
        rendered.staticResourceId = packet.getResourceId();
    }

    private boolean hasLiveArmor(Zombie zombie) {
        if (zombie == null || zombie.getArmors() == null) {
            return false;
        }
        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor != null && !armor.isDestroyed() && !armor.isDropped()) {
                return true;
            }
        }
        return false;
    }

    private void updateDamageFlash(RenderedZombie rendered, Zombie zombie, float delta) {
        int health = zombie == null ? 0 : zombie.getHealth();
        if (rendered.lastHealth >= 0 && health < rendered.lastHealth) {
            rendered.damageFlashRemaining = DAMAGE_FLASH_SECONDS;
        }
        rendered.lastHealth = health;
        rendered.damageFlashRemaining = Math.max(0f, rendered.damageFlashRemaining - delta);
    }

    private void updateStatusTint(Actor body, Zombie zombie, boolean damageFlash) {
        Color color = Color.WHITE;
        String alias = zombie == null || zombie.getDefinition() == null
                ? "" : normalizeAlias(zombie.getDefinition().getAlias());
        boolean obstacle = alias.equals("iceblock") || alias.equals("arcademachine")
                || alias.equals("barrelobstacle");
        if (zombie.isTerrainFrozen() || zombie.hasCondition(ZombieCondition.FROZEN)) {
            color = new Color(0.55f, 0.86f, 1f, 1f);
        } else if (zombie.hasCondition(ZombieCondition.CHILLED)) {
            color = new Color(0.70f, 0.90f, 1f, 1f);
        } else if (zombie.hasCondition(ZombieCondition.POISONED)) {
            color = new Color(0.66f, 1f, 0.56f, 1f);
        } else if (zombie.hasCondition(ZombieCondition.STUNNED)) {
            color = new Color(1f, 0.90f, 0.45f, 1f);
        } else if (zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
            color = new Color(1f, 0.58f, 0.92f, 1f);
        } else if (!obstacle && zombie.getExactX() <= 1.35d) {
            color = new Color(1f, 0.58f, 0.52f, 1f);
        }
        if (damageFlash) {
            color = new Color(1f, Math.min(color.g, 0.72f), Math.min(color.b, 0.62f), color.a);
        }
        body.setColor(color);
    }

    private void applyDepthOrder(List<Zombie> zombies) {
        if (this.renderHost != this) {
            return;
        }
        zombies.sort(Comparator
                .comparingInt((Zombie zombie) -> zombie.getPosition().getY())
                .thenComparingDouble(Zombie::getExactX));
        for (int index = 0; index < zombies.size(); index++) {
            RenderedZombie rendered = this.actors.get(zombies.get(index));
            if (rendered != null) {
                rendered.root.setZIndex(index);
            }
        }
    }

    private final class AbilityArcActor extends Image {
        private final float fromX;
        private final float fromY;
        private final float toX;
        private final float toY;
        private final float arcHeight;
        private final float duration;
        private final int row;
        private final String impactResourceId;
        private final float impactSizeFactor;
        private float elapsed;

        private AbilityArcActor(
                Drawable drawable,
                float fromX,
                float fromY,
                float toX,
                float toY,
                float arcHeight,
                float duration,
                float size,
                int row,
                String impactResourceId,
                float impactSizeFactor
        ) {
            super(drawable);
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.arcHeight = arcHeight;
            this.duration = Math.max(0.08f, duration);
            this.row = row;
            this.impactResourceId = impactResourceId;
            this.impactSizeFactor = impactSizeFactor;
            setScaling(Scaling.fit);
            setTouchable(Touchable.disabled);
            setSize(size, size);
            setOrigin(size * 0.5f, size * 0.5f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            this.elapsed += Math.max(0f, delta);
            float progress = Math.min(1f, this.elapsed / this.duration);
            float centerX = this.fromX + (this.toX - this.fromX) * progress;
            float centerY = this.fromY + (this.toY - this.fromY) * progress
                    + 4f * progress * (1f - progress) * this.arcHeight;
            setPosition(centerX - getWidth() * 0.5f, centerY - getHeight() * 0.5f);
            setRotation(progress * 18f - 9f);
            if (progress >= 1f) {
                if (this.impactResourceId != null && this.impactSizeFactor > 0f) {
                    float cellWidth = GameplayZombieLayer.this.getWidth()
                            / GameplayBoardInteractionLayer.COLUMN_COUNT;
                    GameplayZombieLayer.this.spawnAbilityImpact(
                            this.impactResourceId,
                            this.toX,
                            this.toY,
                            cellWidth * this.impactSizeFactor,
                            this.row
                    );
                }
                remove();
            }
        }
    }

    private static final class PlantAbilityState {
        private final int column;
        private final int row;
        private final int freezeLevel;
        private final boolean octopusCovered;
        private final boolean transformed;

        private PlantAbilityState(
                int column,
                int row,
                int freezeLevel,
                boolean octopusCovered,
                boolean transformed
        ) {
            this.column = column;
            this.row = row;
            this.freezeLevel = freezeLevel;
            this.octopusCovered = octopusCovered;
            this.transformed = transformed;
        }

        private static PlantAbilityState from(Plant plant, GameplayPlantCoverInspector.State cover) {
            return new PlantAbilityState(
                    plant.getPosition().getX(),
                    plant.getPosition().getY(),
                    cover == null ? 0 : cover.getFreezeLevel(),
                    cover != null && cover.isOctopusCovered(),
                    plant.isTransformed()
            );
        }
    }

    private static final class ZombieShadowProfile {
        private final float widthFactor;
        private final float heightFactor;
        private final float yFactor;
        private final float centerOffsetX;
        private final float alpha;

        private ZombieShadowProfile(
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

        private static ZombieShadowProfile of(
                float widthFactor,
                float heightFactor,
                float yFactor,
                float alpha
        ) {
            return new ZombieShadowProfile(widthFactor, heightFactor, yFactor, 0f, alpha);
        }
    }

    private static final class RightEdgeClippedZombieGroup extends Group {
        private static final float OPEN_CLIP_MARGIN = 4096f;

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Group parent = getParent();
            if (parent == null || getX() + getWidth() <= parent.getWidth()) {
                super.draw(batch, parentAlpha);
                return;
            }

            if (parent.clipBegin(
                    -OPEN_CLIP_MARGIN,
                    -OPEN_CLIP_MARGIN,
                    parent.getWidth() + OPEN_CLIP_MARGIN,
                    parent.getHeight() + OPEN_CLIP_MARGIN * 2f
            )) {
                super.draw(batch, parentAlpha);
                parent.clipEnd();
            }
        }
    }

    private static final class RenderedZombie {
        private final Group root;
        private Actor body;
        private Image shadow;
        private final Actor butter;
        private final Actor iceBlock;
        private Actor piano;
        private ZombieAnimationCatalog.AnimationInfo animation;
        private GroundSwatchMotion groundMotion;
        private float groundOffset;
        private float centerOffset;
        private final String alias;
        private final Map<ZombieArmor, Boolean> armorLiveState = new IdentityHashMap<>();
        private String currentClip;
        private String staticResourceId;
        private float motionTime;
        private float damageFlashRemaining;
        private float specialClipRemaining;
        private double throwOriginX;
        private float throwDuration;
        private float throwRemaining;
        private float butterRemaining;
        private double lastExactX = Double.NaN;
        private int lastRow = -1;
        private float rowSlideOffsetFactor;
        private float rowSlideRemaining;
        private double prospectorFlightOriginX;
        private float prospectorFlightDuration;
        private float prospectorFlightRemaining;
        private boolean prospectorLandingPlayed;
        private boolean prospectorExtinguished;
        private boolean prospectorSpent;
        private float dodoLiftFactor;
        private boolean lastFlying;
        private boolean lastCharging;
        private int lastHealth = -1;
        private boolean lastAttacking;

        private RenderedZombie(
                Group root,
                Actor body,
                Actor butter,
                Actor iceBlock,
                ZombieAnimationCatalog.AnimationInfo animation,
                GroundSwatchMotion groundMotion,
                float groundOffset,
                float centerOffset,
                String alias
        ) {
            this.root = root;
            this.body = body;
            this.butter = butter;
            this.iceBlock = iceBlock;
            this.animation = animation;
            this.groundMotion = groundMotion;
            this.groundOffset = groundOffset;
            this.centerOffset = centerOffset;
            this.alias = alias;
            this.currentClip = animation == null ? null : animation.getWalkClip();
            ZombiePacketCatalog.PacketVisual packet = ZombiePacketCatalog.findPacket(alias);
            this.staticResourceId = packet == null ? null : packet.getResourceId();
        }
    }
}
