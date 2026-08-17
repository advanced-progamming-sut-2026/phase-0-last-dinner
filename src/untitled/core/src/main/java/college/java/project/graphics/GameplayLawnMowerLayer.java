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

/** Draws one mower at the left edge of each unused lane. */
public final class GameplayLawnMowerLayer extends Group {
    private static final String EGYPT_MOWER_PATH = "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM";
    private static final String EGYPT_MOWER = "IMAGE_MOWERS_MOWER_EGYPT_MOWER_EGYPT_96X63";
    private static final String ICE_MOWER = "IMAGE_MOWERS_MOWER_ICEAGE_MOWER_ICEAGE_99X85";
    private static final String BEACH_MOWER = "IMAGE_MOWERS_MOWER_BEACH_MOWER_BEACH_166X175";
    private static final String DARK_MOWER = "IMAGE_MOWERS_MOWER_DARK_MOWER_DARK_140X91";

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final Map<LawnMower, Actor> actors = new IdentityHashMap<>();
    private final Set<LawnMower> activating = Collections.newSetFromMap(new IdentityHashMap<>());

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
        actor.setColor(1f, 0.94f, 0.66f, 1f);
        actor.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(getWidth() + actor.getWidth() * 1.6f, 0f, 0.42f),
                        Actions.fadeOut(0.42f)
                ),
                Actions.run(() -> {
                    this.activating.remove(mower);
                    this.actors.remove(mower);
                    actor.remove();
                })
        ));
    }

    private Actor createMowerActor() {
        ChapterType chapterType = this.dataSource.getChapterType();
        if (chapterType == ChapterType.ANCIENT_EGYPT) {
            Actor egyptPam = createEgyptPamFallback();
            if (egyptPam != null) {
                return egyptPam;
            }
            return createStaticMower(EGYPT_MOWER);
        }

        // Prefer the exact chapter artwork. Previously Ice Caves and Medieval
        // fell through to Egypt's PAM before their own mower atlas was tried.
        Actor chapterMower = createStaticMower(mowerResource(chapterType));
        if (chapterMower != null) {
            return chapterMower;
        }

        Actor egyptPam = createEgyptPamFallback();
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

    private Actor createEgyptPamFallback() {
        FileHandle pamFile = Gdx.files.internal("IMAGES/" + EGYPT_MOWER_PATH);
        if (!pamFile.exists() || !PamTextureAvailability.allTexturesAvailable(
                this.assets.getTextureBank(),
                pamFile
        )) {
            return null;
        }
        this.assets.getPamPlayer().loadSync(EGYPT_MOWER_PATH);
        PamAnimationActor actor = new PamAnimationActor(
                this.assets.getPamPlayer(),
                EGYPT_MOWER_PATH,
                "idle",
                150f,
                150f
        );
        actor.setTouchable(Touchable.disabled);
        actor.act(0.35f);
        return actor;
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
        float size = cellHeight * 0.92f;
        float tileBottom = (
                GameplayBoardInteractionLayer.ROW_COUNT - 1 - row
        ) * cellHeight;
        float centerY = tileBottom + cellHeight / 2f;
        actor.setBounds(
                -cellWidth * 1.02f,
                centerY - size / 2f,
                size,
                size
        );
    }
}
