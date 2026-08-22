package college.java.project.graphics.minigame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.PlantIdleVisual;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import model.mechanism.Board;
import model.minigame.behavior.ZombotanyJalapenoBehavior;
import model.minigame.behavior.ZombotanySquashBehavior;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.minigame.zombotanyminigame.ZombotanyTrait;
import model.plant.Projectile;
import model.zombie.Zombie;
import pvz.skin.PvzSkin;

import java.util.*;

public final class ZombotanyTraitLayer extends Group {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    private final ZombotanyMiniGame game;
    private final GameAssetManager assets;
    private final Map<Zombie, TraitVisual> visuals;

    private boolean disposed;
    private final Map<Projectile, Boolean> knownProjectiles;

    public ZombotanyTraitLayer(ZombotanyMiniGame game) {
        if (game == null) throw new IllegalArgumentException("Zombotany game is required.");

        this.game = game;
        this.assets = new GameAssetManager();
        this.visuals = new IdentityHashMap<>();
        this.knownProjectiles = new IdentityHashMap<>();

        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (this.disposed) return;

        this.assets.update();
        syncTraits();
    }

    private void syncTraits() {
        Board board = this.game.getBoard();

        if (board == null) {
            clearTraitVisuals();
            return;
        }

        List<Zombie> zombies = new ArrayList<>(board.getAllZombies());

        zombies.removeIf(zombie -> zombie == null || zombie.isDead() || zombie.getPosition() == null);

        removeMissing(zombies);

        for (Zombie zombie : zombies)
            updateZombieTrait(zombie);

        syncPeashooterShots(board, zombies);

        applyDrawOrder(zombies);
    }

    private void syncPeashooterShots(Board board, List<Zombie> zombies) {
        List<Projectile> projectiles = new ArrayList<>(board.getProjectiles());

        projectiles.removeIf(Objects::isNull);
        removeMissingProjectiles(projectiles);

        for (Projectile projectile : projectiles) {
            if (this.knownProjectiles.containsKey(projectile)) continue;

            this.knownProjectiles.put(projectile, true);

            if (projectile.getSourcePlant() != null) continue;

            Zombie shooter = findPeashooterZombie(projectile, zombies);

            if (shooter != null) playPeashooterShot(shooter);
        }
    }

    private Zombie findPeashooterZombie(Projectile projectile, List<Zombie> zombies) {
        if (projectile.getPosition() == null) return null;

        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null || this.game.getTrait(zombie) != ZombotanyTrait.PEASHOOTER) {
                continue;
            }

            if (zombie.getPosition().getY() != projectile.getPosition().getY()) {
                continue;
            }

            double distance = Math.abs(zombie.getExactX() - projectile.getPosition().getX());

            if (distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }

        return nearestDistance <= 1.25d ? nearest : null;
    }

    private void playPeashooterShot(Zombie zombie) {
        TraitVisual visual = this.visuals.get(zombie);

        if (visual == null) return;

        visual.actor.setOrigin(visual.actor.getWidth() * 0.5f, visual.actor.getHeight() * 0.5f);

        visual.actor.addAction(Actions.sequence(Actions.scaleTo(1.18f, 0.88f, 0.07f, Interpolation.pow2Out),
            Actions.scaleTo(0.92f, 1.08f, 0.06f), Actions.scaleTo(1f, 1f, 0.10f, Interpolation.sine)));
    }

    private void removeMissingProjectiles(List<Projectile> projectiles) {
        List<Projectile> removed = new ArrayList<>();

        for (Projectile projectile : this.knownProjectiles.keySet()) {
            if (!containsProjectileIdentity(projectiles, projectile)) removed.add(projectile);
        }

        for (Projectile projectile : removed)
            this.knownProjectiles.remove(projectile);
    }

    private boolean containsProjectileIdentity(List<Projectile> projectiles, Projectile wanted) {
        for (Projectile projectile : projectiles) {
            if (projectile == wanted) return true;
        }

        return false;
    }

    private void updateZombieTrait(Zombie zombie) {
        ZombotanyTrait trait = this.game.getTrait(zombie);

        if (trait == null) {
            removeVisual(zombie);
            return;
        }

        TraitVisual rendered = this.visuals.get(zombie);

        if (rendered == null || rendered.trait != trait) {
            removeVisual(zombie);
            rendered = createVisual(trait);

            if (rendered == null) return;

            this.visuals.put(zombie, rendered);
            addActor(rendered.actor);
            playSpawnAnimation(rendered.actor);
        }

        positionVisual(rendered.actor, zombie, trait);
        updateTraitCondition(rendered, zombie);
    }

    private void updateTraitCondition(TraitVisual visual, Zombie zombie) {
        if (visual.trait != ZombotanyTrait.WALLNUT || zombie.getMaximumHealth() <= 0) {
            return;
        }

        float healthRatio = Math.max(0f, Math.min(1f, (float) zombie.getHealth() / zombie.getMaximumHealth()));

        int damageStage;

        if (healthRatio > 0.66f) {
            damageStage = 0;
        } else if (healthRatio > 0.33f) {
            damageStage = 1;
        } else {
            damageStage = 2;
        }

        if (visual.damageStage == damageStage) return;

        boolean firstUpdate = visual.damageStage < 0;
        visual.damageStage = damageStage;

        applyWallnutDamageAppearance(visual.actor, damageStage);

        if (!firstUpdate) playWallnutDamageReaction(visual.actor);
    }

    private void applyWallnutDamageAppearance(PlantIdleVisual actor, int damageStage) {
        if (actor.getIdleVariantCount() > damageStage) {
            actor.setVisualTint(Color.WHITE);
            actor.setIdleVariant(damageStage);
            return;
        }

        switch (damageStage) {
            case 1:
                actor.setVisualTint(new Color(1f, 0.78f, 0.48f, 1f));
                break;

            case 2:
                actor.setVisualTint(new Color(0.78f, 0.32f, 0.20f, 1f));
                break;

            default:
                actor.setVisualTint(Color.WHITE);
                break;
        }
    }

    private void playWallnutDamageReaction(PlantIdleVisual actor) {
        actor.setOrigin(actor.getWidth() * 0.5f, actor.getHeight() * 0.45f);

        actor.addAction(Actions.sequence(Actions.parallel(Actions.scaleTo(1.10f, 0.90f, 0.06f),
            Actions.rotateTo(-5f, 0.06f)), Actions.parallel(Actions.scaleTo(0.94f, 1.06f, 0.07f),
            Actions.rotateTo(5f, 0.07f)), Actions.parallel(Actions.scaleTo(1f, 1f, 0.10f),
            Actions.rotateTo(0f, 0.10f))));
    }

    private TraitVisual createVisual(ZombotanyTrait trait) {
        String plantName = getPlantName(trait);

        if (plantName == null) return null;

        PlantIdleVisual actor = new PlantIdleVisual(this.assets, plantName);

        actor.setGrounded(true);
        actor.setContentPadding(0f);
        actor.setTouchable(Touchable.disabled);

        return new TraitVisual(trait, actor);
    }

    private String getPlantName(ZombotanyTrait trait) {
        return switch (trait) {
            case PEASHOOTER -> "Peashooter";
            case WALLNUT -> "Wall-nut";
            case JALAPENO -> "Jalapeno";
            case SQUASH -> "Squash";
            default -> null;
        };
    }

    private void positionVisual(PlantIdleVisual actor, Zombie zombie, ZombotanyTrait trait) {
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;

        float centreX = (float) ((zombie.getExactX() + 0.5d) * cellWidth);

        int row = zombie.getPosition().getY();
        float tileBottom = (ROW_COUNT - 1 - row) * cellHeight;

        TraitLayout layout = getLayout(trait);

        float width = cellWidth * layout.widthFactor;
        float height = cellHeight * layout.heightFactor;

        float x = centreX - width * 0.5f + cellWidth * layout.offsetX;

        float y = tileBottom + cellHeight * layout.offsetY;

        actor.setBounds(x, y, width, height);
        actor.setOrigin(width * 0.5f, height * 0.5f);
    }

    private TraitLayout getLayout(ZombotanyTrait trait) {
        return switch (trait) {
            case PEASHOOTER -> new TraitLayout(0.62f, 0.78f, 0.03f, 0.58f);
            case WALLNUT -> new TraitLayout(0.55f, 0.65f, 0.01f, 0.60f);
            case JALAPENO -> new TraitLayout(0.50f, 0.82f, 0.01f, 0.55f);
            case SQUASH -> new TraitLayout(0.72f, 0.75f, 0.01f, 0.57f);
            default -> new TraitLayout(0.55f, 0.70f, 0f, 0.58f);
        };
    }

    private void playSpawnAnimation(PlantIdleVisual actor) {
        actor.setScale(0.25f);
        actor.setColor(new Color(1f, 1f, 1f, 0f));

        actor.addAction(Actions.parallel(Actions.fadeIn(0.18f), Actions.scaleTo(1f, 1f, 0.24f,
            Interpolation.swingOut)));
    }

    private void removeMissing(List<Zombie> zombies) {
        List<Zombie> removed = new ArrayList<>();

        for (Zombie zombie : this.visuals.keySet()) {
            if (!containsIdentity(zombies, zombie)) removed.add(zombie);
        }

        for (Zombie zombie : removed) {
            TraitVisual visual = this.visuals.get(zombie);

            if (visual != null) playRemovalEffect(zombie, visual);

            removeVisual(zombie);
        }
    }

    private void playRemovalEffect(Zombie zombie, TraitVisual visual) {
        ZombotanyJalapenoBehavior jalapeno = zombie.findBehavior(ZombotanyJalapenoBehavior.class);

        if (jalapeno != null && jalapeno.isExploded()) {
            playJalapenoExplosion(zombie);
            return;
        }

        ZombotanySquashBehavior squash = zombie.findBehavior(ZombotanySquashBehavior.class);

        if (squash != null && squash.isSquashed()) playSquashImpact(visual.actor);
    }

    private void playSquashImpact(PlantIdleVisual source) {
        if (source == null) return;

        PlantIdleVisual impact = new PlantIdleVisual(this.assets, "Squash");

        impact.setGrounded(true);
        impact.setContentPadding(0f);
        impact.setTouchable(Touchable.disabled);

        impact.setBounds(source.getX(), source.getY(), source.getWidth(), source.getHeight());

        impact.setOrigin(impact.getWidth() * 0.5f, impact.getHeight() * 0.25f);

        addActor(impact);

        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;

        impact.addAction(Actions.sequence(Actions.parallel(Actions.moveBy(-cellWidth * 0.72f,
                -cellHeight * 0.18f, 0.16f, Interpolation.pow2In), Actions.scaleTo(1.30f, 0.68f, 0.16f),
            Actions.rotateBy(-12f, 0.16f)), Actions.parallel(Actions.scaleTo(1.08f, 1.08f, 0.10f)
            , Actions.rotateTo(0f, 0.10f)), Actions.fadeOut(0.16f), Actions.removeActor()));
    }

    private void playJalapenoExplosion(Zombie zombie) {
        if (zombie.getPosition() == null || getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }

        float cellHeight = getHeight() / ROW_COUNT;
        int row = zombie.getPosition().getY();

        float tileBottom = (ROW_COUNT - 1 - row) * cellHeight;

        Group fire = new Group();

        fire.setTouchable(Touchable.disabled);
        fire.setBounds(0f, tileBottom, getWidth(), cellHeight);

        Image glow = createColourImage(new Color(1f, 0.18f, 0.02f, 0.72f));

        glow.setBounds(0f, cellHeight * 0.18f, getWidth(), cellHeight * 0.58f);

        fire.addActor(glow);

        int flameCount = 14;
        float flameAreaWidth = getWidth() / flameCount;

        for (int index = 0; index < flameCount; index++) {
            Image flame = createFlame(index);

            float flameWidth = flameAreaWidth * (0.55f + index % 3 * 0.12f);

            float flameHeight = cellHeight * (0.38f + index % 4 * 0.08f);

            float x = index * flameAreaWidth + (flameAreaWidth - flameWidth) * 0.5f;

            flame.setBounds(x, cellHeight * 0.24f, flameWidth, flameHeight);

            flame.setOrigin(flameWidth * 0.5f, 0f);

            flame.addAction(Actions.parallel(Actions.moveBy(0f, cellHeight * 0.18f, 0.34f),
                Actions.scaleTo(0.45f, 1.35f, 0.34f), Actions.fadeOut(0.38f)));

            fire.addActor(flame);
        }

        fire.getColor().a = 0f;

        fire.addAction(Actions.sequence(Actions.fadeIn(0.08f), Actions.delay(0.28f),
            Actions.fadeOut(0.20f), Actions.removeActor()));

        addActor(fire);
    }

    private Image createFlame(int index) {
        Color colour;

        if (index % 3 == 0) {
            colour = new Color(1f, 0.92f, 0.18f, 0.95f);
        } else if (index % 3 == 1) {
            colour = new Color(1f, 0.48f, 0.05f, 0.95f);
        } else {
            colour = new Color(0.95f, 0.12f, 0.02f, 0.92f);
        }

        return createColourImage(colour);
    }

    private Image createColourImage(Color colour) {
        return new Image(PvzSkin.get().newDrawable("white_pixel", colour));
    }

    private boolean containsIdentity(List<Zombie> zombies, Zombie wanted) {
        for (Zombie zombie : zombies) {
            if (zombie == wanted) return true;
        }

        return false;
    }

    private void applyDrawOrder(List<Zombie> zombies) {
        zombies.sort(Comparator.comparingInt((Zombie zombie) ->
            zombie.getPosition().getY()).thenComparingDouble(Zombie::getExactX));

        for (int index = 0; index < zombies.size(); index++) {
            TraitVisual visual = this.visuals.get(zombies.get(index));

            if (visual != null) visual.actor.setZIndex(index);
        }
    }

    private void removeVisual(Zombie zombie) {
        TraitVisual visual = this.visuals.remove(zombie);

        if (visual != null) {
            visual.actor.clearActions();
            visual.actor.remove();
        }
    }

    private void clearTraitVisuals() {
        for (TraitVisual visual : this.visuals.values()) {
            visual.actor.clearActions();
            visual.actor.remove();
        }

        this.visuals.clear();
        this.knownProjectiles.clear();
    }

    public void dispose() {
        if (this.disposed) return;

        this.disposed = true;
        clearTraitVisuals();
        this.assets.dispose();
    }

    private static final class TraitVisual {
        private final ZombotanyTrait trait;
        private final PlantIdleVisual actor;
        private int damageStage = -1;

        private TraitVisual(ZombotanyTrait trait, PlantIdleVisual actor) {
            this.trait = trait;
            this.actor = actor;
        }
    }

    private static final class TraitLayout {
        private final float widthFactor;
        private final float heightFactor;
        private final float offsetX;
        private final float offsetY;

        private TraitLayout(float widthFactor, float heightFactor, float offsetX, float offsetY) {
            this.widthFactor = widthFactor;
            this.heightFactor = heightFactor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }
}
