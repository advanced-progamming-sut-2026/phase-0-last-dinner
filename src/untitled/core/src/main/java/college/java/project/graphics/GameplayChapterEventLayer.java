package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import model.Plant;
import model.chapters.ChapterType;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * View-only chapter event effects inferred from live Phase 1 state changes.
 * <p>
 * Phase 1 stays authoritative; this layer only detects chapter events and
 * renders the matching PAM effects when the resources are available. If any
 * PAM is missing, a lightweight colored-strip fallback remains available.
 */
public final class GameplayChapterEventLayer extends Group {
    private static final Color ICE_WIND_FALLBACK = new Color(0.58f, 0.92f, 1f, 0.30f);
    private static final Color SANDSTORM_FALLBACK = new Color(0.91f, 0.70f, 0.36f, 0.34f);

    private static final int ROW_COUNT = 5;
    private static final float ICE_EVENT_DURATION = 2.57f;
    private static final float SANDSTORM_INTRO = 0.3333f;
    private static final float SANDSTORM_LOOP_HOLD = 0.95f;
    private static final float SANDSTORM_OUTRO = 0.3333f;
    private static final float SANDSTORM_DURATION = SANDSTORM_INTRO + SANDSTORM_LOOP_HOLD + SANDSTORM_OUTRO;

    private final GameplaySeedBankDataSource seedDataSource;
    private final GameplayWorldDataSource worldDataSource;
    private final GameAssetManager assets;
    private final PamAnimationCatalog effectCatalog;
    private final Map<Plant, Integer> freezeLevels = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> seenZombies = new IdentityHashMap<>();
    private final float[] iceRowCooldown = new float[ROW_COUNT];
    private final float[] sandRowCooldown = new float[ROW_COUNT];
    private boolean initialized;

    public GameplayChapterEventLayer(
            GameplaySeedBankDataSource seedDataSource,
            GameplayWorldDataSource worldDataSource,
            GameAssetManager assets
    ) {
        this.seedDataSource = seedDataSource;
        this.worldDataSource = worldDataSource;
        this.assets = assets;
        this.effectCatalog = new PamAnimationCatalog();
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        tickCooldowns(delta);
        detectIceWind();
        detectEgyptSandstorm();
        cleanupIdentityMaps();
        this.initialized = true;
    }

    private void tickCooldowns(float delta) {
        for (int row = 0; row < ROW_COUNT; row++) {
            this.iceRowCooldown[row] = Math.max(0f, this.iceRowCooldown[row] - delta);
            this.sandRowCooldown[row] = Math.max(0f, this.sandRowCooldown[row] - delta);
        }
    }

    private void detectIceWind() {
        if (this.worldDataSource.getChapterType() != ChapterType.ICE_CAVES) {
            rememberFreezeLevels();
            return;
        }
        boolean[] affectedRows = new boolean[ROW_COUNT];
        List<Plant> plants = this.seedDataSource.getPlantsOnBoard();
        for (Plant plant : plants) {
            if (plant == null || plant.getPosition() == null) {
                continue;
            }
            int level = this.worldDataSource.getPlantCoverState(plant).getFreezeLevel();
            int previous = this.freezeLevels.getOrDefault(plant, level);
            if (this.initialized && level > previous) {
                int row = plant.getPosition().getY();
                if (row >= 0 && row < affectedRows.length) {
                    affectedRows[row] = true;
                }
            }
            this.freezeLevels.put(plant, level);
        }
        for (int row = 0; row < affectedRows.length; row++) {
            if (affectedRows[row]) {
                showIceWind(row);
            }
        }
    }

    private void rememberFreezeLevels() {
        for (Plant plant : this.seedDataSource.getPlantsOnBoard()) {
            if (plant != null) {
                this.freezeLevels.put(
                        plant,
                        this.worldDataSource.getPlantCoverState(plant).getFreezeLevel()
                );
            }
        }
    }

    private void detectEgyptSandstorm() {
        List<Zombie> zombies = this.worldDataSource.getZombiesOnBoard();
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }
            boolean isNew = !this.seenZombies.containsKey(zombie);
            this.seenZombies.put(zombie, Boolean.TRUE);
            if (!this.initialized || !isNew
                    || this.worldDataSource.getChapterType() != ChapterType.ANCIENT_EGYPT) {
                continue;
            }
            if (zombie.getExactX() < 7.5d) {
                showSandstorm(zombie.getPosition().getY());
            }
        }
    }

    private void showIceWind(int row) {
        if (!validRow(row) || this.iceRowCooldown[row] > 0f) {
            return;
        }
        float rowHeight = rowHeight();
        float localY = rowBaseY(row);
        ChapterEventStrip strip = createIceWindStrip(localY, rowHeight);
        if (strip == null) {
            addActor(ProceduralEventStrip.createIceWind(getWidth(), rowHeight, localY));
            this.iceRowCooldown[row] = 2.2f;
            return;
        }
        addActor(strip);
        this.iceRowCooldown[row] = strip.getDuration();
    }

    private void showSandstorm(int row) {
        if (!validRow(row) || this.sandRowCooldown[row] > 0f) {
            return;
        }
        float rowHeight = rowHeight();
        float localY = rowBaseY(row);
        ChapterEventStrip strip = createSandstormStrip(localY, rowHeight);
        if (strip == null) {
            addActor(ProceduralEventStrip.createSandstorm(getWidth(), rowHeight, localY));
            this.sandRowCooldown[row] = 1.6f;
            return;
        }
        addActor(strip);
        this.sandRowCooldown[row] = strip.getDuration();
    }

    private ChapterEventStrip createIceWindStrip(float localY, float rowHeight) {
        if (this.assets == null) {
            return null;
        }
        GameplayPamEffectSupport.Effect sample = GameplayPamEffectSupport.create(
                this.assets, this.effectCatalog, "FROSTBITE_CHILL_WIND", false, "animation"
        );
        if (sample == null) {
            return null;
        }
        ChapterEventStrip strip = new ChapterEventStrip(getWidth(), rowHeight, localY, ICE_EVENT_DURATION);
        float actorHeight = rowHeight * 1.80f;
        float actorWidth = actorHeight * safeAspect(sample);
        float baseY = (rowHeight - actorHeight) * 0.5f;
        int copies = 5;
        float startX = -actorWidth * 0.12f;
        float step = (getWidth() + actorWidth * 0.24f) / (copies - 1f);
        for (int index = 0; index < copies; index++) {
            GameplayPamEffectSupport.Effect effect = index == 0
                    ? sample
                    : GameplayPamEffectSupport.create(
                            this.assets, this.effectCatalog, "FROSTBITE_CHILL_WIND", false, "animation"
                    );
            if (effect == null) {
                continue;
            }
            effect.actor.setBounds(startX + step * index, baseY, actorWidth, actorHeight);
            effect.actor.setColor(1f, 1f, 1f, index % 2 == 0 ? 0.94f : 0.82f);
            effect.actor.setStateTime(index * 0.17f);
            strip.add(effect.actor);
        }
        return strip.hasActors() ? strip : null;
    }

    private ChapterEventStrip createSandstormStrip(float localY, float rowHeight) {
        if (this.assets == null) {
            return null;
        }
        ChapterEventStrip strip = new ChapterEventStrip(getWidth(), rowHeight, localY, SANDSTORM_DURATION);
        float actorHeight = rowHeight * 1.65f;
        float baseY = (rowHeight - actorHeight) * 0.5f;
        int copies = 8;

        GameplayPamEffectSupport.Effect rearSample = GameplayPamEffectSupport.create(
                this.assets, this.effectCatalog, "SANDSTORM_REAR", false, "intro"
        );
        GameplayPamEffectSupport.Effect topSample = GameplayPamEffectSupport.create(
                this.assets, this.effectCatalog, "SANDSTORM_TOP", false, "intro"
        );
        if (rearSample == null || topSample == null) {
            return null;
        }
        float rearWidth = actorHeight * safeAspect(rearSample);
        float topWidth = actorHeight * safeAspect(topSample);
        float rearStartX = -rearWidth * 0.20f;
        float topStartX = -topWidth * 0.10f;
        float rearStep = (getWidth() + rearWidth * 0.40f) / (copies - 1f);
        float topStep = (getWidth() + topWidth * 0.20f) / (copies - 1f);

        for (int index = 0; index < copies; index++) {
            GameplayPamEffectSupport.Effect rear = index == 0
                    ? rearSample
                    : GameplayPamEffectSupport.create(
                            this.assets, this.effectCatalog, "SANDSTORM_REAR", false, "intro"
                    );
            GameplayPamEffectSupport.Effect top = index == 0
                    ? topSample
                    : GameplayPamEffectSupport.create(
                            this.assets, this.effectCatalog, "SANDSTORM_TOP", false, "intro"
                    );
            if (rear == null || top == null) {
                continue;
            }
            float rearX = rearStartX + rearStep * index;
            float topX = topStartX + topStep * index;
            rear.actor.setBounds(rearX, baseY, rearWidth, actorHeight);
            rear.actor.setColor(1f, 1f, 1f, 0.78f);
            rear.actor.setStateTime(index * 0.03f);
            strip.addPhased(rear.actor, rear.animation, SANDSTORM_INTRO, SANDSTORM_LOOP_HOLD, SANDSTORM_OUTRO);

            top.actor.setBounds(topX, baseY - rowHeight * 0.05f, topWidth, actorHeight);
            top.actor.setColor(1f, 1f, 1f, 0.96f);
            top.actor.setStateTime(index * 0.03f);
            strip.addPhased(top.actor, top.animation, SANDSTORM_INTRO, SANDSTORM_LOOP_HOLD, SANDSTORM_OUTRO);
        }
        return strip.hasActors() ? strip : null;
    }

    private float safeAspect(GameplayPamEffectSupport.Effect effect) {
        float h = effect.animation.getCanvasHeight();
        return h <= 0f ? 1f : effect.animation.getCanvasWidth() / h;
    }

    private float rowHeight() {
        return getHeight() / ROW_COUNT;
    }

    private float rowBaseY(int row) {
        return (ROW_COUNT - 1 - row) * rowHeight();
    }

    private boolean validRow(int row) {
        return row >= 0 && row < ROW_COUNT;
    }

    private void cleanupIdentityMaps() {
        this.freezeLevels.keySet().removeIf(plant -> !containsIdentity(
                this.seedDataSource.getPlantsOnBoard(), plant));
        this.seenZombies.keySet().removeIf(zombie -> !containsIdentity(
                this.worldDataSource.getZombiesOnBoard(), zombie));
    }

    private <T> boolean containsIdentity(List<T> list, T wanted) {
        if (list == null) {
            return false;
        }
        for (T item : list) {
            if (item == wanted) {
                return true;
            }
        }
        return false;
    }

    private static final class ProceduralEventStrip extends Group {
        private static ProceduralEventStrip createSandstorm(float width, float rowHeight, float localY) {
            ProceduralEventStrip strip = new ProceduralEventStrip();
            strip.setTouchable(Touchable.disabled);
            strip.setBounds(0f, localY, width, rowHeight);
            strip.getColor().a = 0f;

            com.badlogic.gdx.scenes.scene2d.ui.Image wash = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                    pvz.skin.PvzSkin.get().newDrawable("white_pixel", new Color(0.80f, 0.69f, 0.34f, 0.22f))
            );
            wash.setBounds(0f, 0f, width, rowHeight);
            strip.addActor(wash);

            for (int index = 0; index < 14; index++) {
                float bandWidth = 130f + (index % 5) * 55f;
                float bandHeight = 8f + (index % 4) * 6f;
                float y = rowHeight * (0.08f + (index % 6) * 0.14f);
                float x = width * index / 14f;
                float speed = -(150f + (index % 5) * 45f);
                Color tone = index % 3 == 0
                        ? new Color(1f, 0.94f, 0.72f, 0.24f)
                        : new Color(0.73f, 0.58f, 0.24f, 0.30f);
                LoopingDriftImage band = new LoopingDriftImage(
                        pvz.skin.PvzSkin.get().newDrawable("white_pixel", tone),
                        x, y, bandWidth, bandHeight, speed, width, bandWidth * 0.6f
                );
                band.setRotation(8f);
                strip.addActor(band);
            }

            for (int index = 0; index < 7; index++) {
                float cloudWidth = 180f + (index % 3) * 60f;
                float cloudHeight = 36f + (index % 2) * 10f;
                float y = rowHeight * (0.12f + index * 0.10f);
                float x = width * index / 7f - cloudWidth * 0.5f;
                float speed = -(70f + index * 10f);
                LoopingDriftImage haze = new LoopingDriftImage(
                        pvz.skin.PvzSkin.get().newDrawable("white_pixel", new Color(0.62f, 0.50f, 0.22f, 0.14f)),
                        x, y, cloudWidth, cloudHeight, speed, width, cloudWidth
                );
                strip.addActor(haze);
            }

            strip.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.10f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(1.00f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.45f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
            ));
            return strip;
        }

        private static ProceduralEventStrip createIceWind(float width, float rowHeight, float localY) {
            ProceduralEventStrip strip = new ProceduralEventStrip();
            strip.setTouchable(Touchable.disabled);
            strip.setBounds(0f, localY, width, rowHeight);
            strip.getColor().a = 0f;

            com.badlogic.gdx.scenes.scene2d.ui.Image wash = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                    pvz.skin.PvzSkin.get().newDrawable("white_pixel", new Color(0.64f, 0.92f, 1f, 0.18f))
            );
            wash.setBounds(0f, 0f, width, rowHeight);
            strip.addActor(wash);

            for (int index = 0; index < 16; index++) {
                float streakWidth = 90f + (index % 4) * 65f;
                float streakHeight = 5f + (index % 3) * 3f;
                float y = rowHeight * (0.08f + (index % 7) * 0.12f);
                float x = width * index / 16f;
                float speed = -(210f + (index % 5) * 55f);
                Color tone = index % 2 == 0
                        ? new Color(1f, 1f, 1f, 0.34f)
                        : new Color(0.72f, 0.95f, 1f, 0.32f);
                LoopingDriftImage streak = new LoopingDriftImage(
                        pvz.skin.PvzSkin.get().newDrawable("white_pixel", tone),
                        x, y, streakWidth, streakHeight, speed, width, streakWidth * 0.4f
                );
                streak.setRotation(-8f);
                strip.addActor(streak);
            }

            for (int index = 0; index < 26; index++) {
                float size = 4f + (index % 3) * 2f;
                float y = rowHeight * (0.06f + (index % 10) * 0.085f);
                float x = width * index / 26f;
                float speed = -(130f + (index % 4) * 25f);
                LoopingDriftImage flake = new LoopingDriftImage(
                        pvz.skin.PvzSkin.get().newDrawable("white_pixel", new Color(0.92f, 0.98f, 1f, 0.38f)),
                        x, y, size, size, speed, width, size * 4f
                );
                strip.addActor(flake);
            }

            strip.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.10f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(1.35f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.55f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
            ));
            return strip;
        }
    }

    private static final class LoopingDriftImage extends com.badlogic.gdx.scenes.scene2d.ui.Image {
        private final float speedX;
        private final float wrapWidth;
        private final float wrapPadding;

        private LoopingDriftImage(
                com.badlogic.gdx.scenes.scene2d.utils.Drawable drawable,
                float x,
                float y,
                float width,
                float height,
                float speedX,
                float wrapWidth,
                float wrapPadding
        ) {
            super(drawable);
            this.speedX = speedX;
            this.wrapWidth = wrapWidth;
            this.wrapPadding = wrapPadding;
            setBounds(x, y, width, height);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            moveBy(this.speedX * delta, 0f);
            if (getX() + getWidth() < -this.wrapPadding) {
                setX(this.wrapWidth + this.wrapPadding);
            }
        }
    }

    private static final class ChapterEventStrip extends Group {
        private final float duration;
        private final List<PhasedPam> phased = new ArrayList<>();
        private float elapsed;

        private ChapterEventStrip(float width, float rowHeight, float localY, float duration) {
            this.duration = duration;
            setTouchable(Touchable.disabled);
            setBounds(0f, localY, width, rowHeight);
        }

        float getDuration() {
            return this.duration;
        }

        boolean hasActors() {
            return getChildren().size > 0;
        }

        void add(PamAnimationActor actor) {
            addActor(actor);
        }

        void addPhased(
                PamAnimationActor actor,
                PamAnimationCatalog.AnimationInfo animation,
                float intro,
                float loopHold,
                float outro
        ) {
            addActor(actor);
            this.phased.add(new PhasedPam(actor, animation, intro, loopHold, outro));
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            this.elapsed += Math.max(0f, delta);
            for (PhasedPam phasedPam : this.phased) {
                phasedPam.update(this.elapsed);
            }
            if (this.elapsed >= this.duration) {
                remove();
            }
        }
    }

    private static final class PhasedPam {
        private final PamAnimationActor actor;
        private final String pamPath;
        private final float introEnd;
        private final float outroStart;
        private int phase; // 0=intro, 1=loop, 2=outro

        private PhasedPam(
                PamAnimationActor actor,
                PamAnimationCatalog.AnimationInfo animation,
                float intro,
                float loopHold,
                float outro
        ) {
            this.actor = actor;
            this.pamPath = actor.getPamPath();
            this.introEnd = intro;
            this.outroStart = intro + loopHold;
            this.phase = 0;
            actor.setLooping(false);
            String introClip = animation.findClip("intro");
            if (introClip != null) {
                actor.setAnimation(this.pamPath, introClip);
            }
        }

        private void update(float elapsed) {
            if (this.phase == 0 && elapsed >= this.introEnd) {
                this.actor.setAnimation(this.pamPath, "loop");
                this.actor.setLooping(true);
                this.phase = 1;
            }
            if (this.phase == 1 && elapsed >= this.outroStart) {
                this.actor.setAnimation(this.pamPath, "outro");
                this.actor.setLooping(false);
                this.phase = 2;
            }
        }
    }
}
