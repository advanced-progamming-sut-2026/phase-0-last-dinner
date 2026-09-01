package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.chapters.ChapterType;
import model.mechanism.LawnMower;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.function.Consumer;

/** Draws one mower at the left edge of each unused lane. */
public final class GameplayLawnMowerLayer extends Group {
    private static final String EGYPT_MOWER_PATH = "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM";
    private static final String ICE_MOWER_PATH = "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM";
    private static final String BEACH_MOWER_PATH = "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM";
    private static final String DARK_MOWER_PATH = "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM";
    private static final String EGYPT_MOWER = "IMAGE_MOWERS_MOWER_EGYPT_MOWER_EGYPT_96X63";
    private static final String ICE_MOWER = "IMAGE_MOWERS_MOWER_ICEAGE_MOWER_ICEAGE_99X85";
    private static final String BEACH_MOWER = "IMAGE_MOWERS_MOWER_BEACH_MOWER_BEACH_166X175";
    private static final String DARK_MOWER = "IMAGE_MOWERS_MOWER_DARK_MOWER_DARK_140X91";

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final Map<LawnMower, Actor> actors = new IdentityHashMap<>();
    private final Set<LawnMower> activating = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<LawnMower> notifiedActivations = Collections.newSetFromMap(new IdentityHashMap<>());
    private Consumer<LawnMower> activationListener;

    public GameplayLawnMowerLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayLawnMowerLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        syncMowers();
    }

    public int getRenderedMowerCount() {
        return this.actors.size();
    }

    public void setActivationListener(Consumer<LawnMower> activationListener) {
        this.activationListener = activationListener;
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void syncMowers() {
        List<LawnMower> mowers = new ArrayList<>(this.dataSource.getLawnMowers());
        mowers.removeIf(java.util.Objects::isNull);
        removeMissing(mowers);
        for (LawnMower mower : mowers) {
            Actor actor = this.actors.get(mower);
            if (mower.isUsed()) {
                notifyActivation(mower);
                if (actor != null && !this.activating.contains(mower)) {
                    animateActivation(mower, actor);
                }
                continue;
            }
            if (actor == null) {
                actor = createMowerActor();
                if (actor == null) {
                    continue;
                }
                this.actors.put(mower, actor);
                addActor(actor);
            }
            position(actor, mower.getRow());
        }
    }

    private void removeMissing(List<LawnMower> mowers) {
        List<LawnMower> removed = new ArrayList<>();
        for (LawnMower mower : this.actors.keySet()) {
            if (!containsIdentity(mowers, mower)) {
                removed.add(mower);
            }
        }
        for (LawnMower mower : removed) {
            if (this.activating.contains(mower)) {
                continue;
            }
            Actor actor = this.actors.remove(mower);
            if (actor != null) {
                actor.remove();
            }
        }
    }

    private boolean containsIdentity(List<LawnMower> mowers, LawnMower wanted) {
        for (LawnMower mower : mowers) {
            if (mower == wanted) {
                return true;
            }
        }
        return false;
    }

    private void animateActivation(LawnMower mower, Actor actor) {
        this.activating.add(mower);
        actor.clearActions();
        actor.setColor(1f, 1f, 1f, 1f);

        if (actor instanceof PamAnimationActor pamActor) {
            float transitionSeconds = transitionDuration(this.dataSource.getChapterType());
            pamActor.setAnimation(pamActor.getPamPath(), "transition");
            pamActor.setLooping(false);
            actor.addAction(Actions.sequence(
                    Actions.delay(transitionSeconds),
                    Actions.run(() -> {
                        pamActor.setAnimation(pamActor.getPamPath(), "attack");
                        pamActor.setLooping(true);
                    }),
                    Actions.moveBy(activationTravelDistance(), 0f, 0.42f),
                    Actions.run(() -> finishActivation(mower, actor))
            ));
            return;
        }

        actor.setColor(1f, 1f, 1f, 1f);
        actor.addAction(Actions.sequence(
                Actions.moveBy(activationTravelDistance(), 0f, 0.42f),
                Actions.run(() -> finishActivation(mower, actor))
        ));
    }

    private void notifyActivation(LawnMower mower) {
        if (mower != null && this.notifiedActivations.add(mower) && this.activationListener != null) {
            this.activationListener.accept(mower);
        }
    }


    private float activationTravelDistance() {
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float legacyVisualSize = cellHeight * 0.92f;
        return getWidth() + legacyVisualSize * 1.6f;
    }

    private void finishActivation(LawnMower mower, Actor actor) {
        this.activating.remove(mower);
        this.actors.remove(mower);
        actor.remove();
    }

    private Actor createMowerActor() {
        ChapterType chapterType = this.dataSource.getChapterType();
        Actor chapterPam = createPamMower(mowerPamPath(chapterType));
        if (chapterPam != null) {
            return chapterPam;
        }

        Actor chapterMower = createStaticMower(mowerResource(chapterType));
        if (chapterMower != null) {
            return chapterMower;
        }

        Actor egyptPam = createPamMower(EGYPT_MOWER_PATH);
        if (egyptPam != null) {
            return egyptPam;
        }
        return createStaticMower(EGYPT_MOWER);
    }

    private Actor createStaticMower(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank == null || resourceId == null || bank.region(resourceId) == null) {
                return null;
            }
            Image image = new Image(new TextureRegionDrawable(bank.region(resourceId)));
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            return image;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Actor createPamMower(String pamPath) {
        if (pamPath == null) {
            return null;
        }
        FileHandle pamFile = Gdx.files.internal("IMAGES/" + pamPath);
        if (!pamFile.exists() || !PamTextureAvailability.allTexturesAvailable(
                this.assets.getTextureBank(),
                pamFile
        )) {
            return null;
        }
        this.assets.getPamPlayer().loadSync(pamPath);
        PamAnimationActor actor = new PamAnimationActor(
                this.assets.getPamPlayer(),
                pamPath,
                "idle",
                390f,
                390f
        );
        actor.setTouchable(Touchable.disabled);
        actor.act(0.35f);
        return actor;
    }

    private String mowerPamPath(ChapterType chapterType) {
        if (chapterType == ChapterType.ICE_CAVES) {
            return ICE_MOWER_PATH;
        }
        if (chapterType == ChapterType.BIG_WAVE_BEACH) {
            return BEACH_MOWER_PATH;
        }
        if (chapterType == ChapterType.MEDIEVAL) {
            return DARK_MOWER_PATH;
        }
        return EGYPT_MOWER_PATH;
    }

    private float transitionDuration(ChapterType chapterType) {
        if (chapterType == ChapterType.ICE_CAVES) {
            return 0.4f;
        }
        if (chapterType == ChapterType.BIG_WAVE_BEACH) {
            return 0.3333f;
        }
        if (chapterType == ChapterType.MEDIEVAL) {
            return 0.3667f;
        }
        return 0.2667f;
    }

    private String mowerResource(ChapterType chapterType) {
        if (chapterType == ChapterType.ICE_CAVES) {
            return ICE_MOWER;
        }
        if (chapterType == ChapterType.BIG_WAVE_BEACH) {
            return BEACH_MOWER;
        }
        if (chapterType == ChapterType.MEDIEVAL) {
            return DARK_MOWER;
        }
        return EGYPT_MOWER;
    }

    private void position(Actor actor, int row) {
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float tileBottom = (
                GameplayBoardInteractionLayer.ROW_COUNT - 1 - row
        ) * cellHeight;
        float centerY = tileBottom + cellHeight / 2f;

        if (actor instanceof PamAnimationActor pamActor) {
            // Mower PAMs use a 390x390 authoring canvas. Render that canvas at
            // the same 768p -> 1080p scale as the chapter background instead
            // of shrinking the whole PAM into a single tile-sized square.
            float width = pamActor.getCanvasWidth() * GameplayWorldLayout.BACKGROUND_SCALE;
            float height = pamActor.getCanvasHeight() * GameplayWorldLayout.BACKGROUND_SCALE;
            float centerX = -cellWidth * 0.47f;
            actor.setBounds(
                    centerX - width / 2f,
                    centerY - height / 2f,
                    width,
                    height
            );
            return;
        }

        float size = cellHeight * 0.92f;
        actor.setBounds(
                -cellWidth * 1.02f,
                centerY - size / 2f,
                size,
                size
        );
    }
}
