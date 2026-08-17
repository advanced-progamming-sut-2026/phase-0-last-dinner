package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.mechanism.Sun;
import model.mechanism.SunType;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.function.Consumer;

/** Shows falling/ground suns and collects them when the pointer passes over them. */
public final class GameplaySunLayer extends Group {
    private static final String SUN_RESOURCE = "IMAGE_EFFECTS_SUN_SUN_166X166";

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private Consumer<Sun> spawnListener;
    private final Map<Sun, Image> actors = new IdentityHashMap<>();
    private final Set<Sun> collecting = Collections.newSetFromMap(new IdentityHashMap<>());

    public GameplaySunLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplaySunLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        setTouchable(Touchable.childrenOnly);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        syncSuns();
    }

    public int getRenderedSunCount() {
        return this.actors.size();
    }

    public void setSpawnListener(Consumer<Sun> spawnListener) {
        this.spawnListener = spawnListener;
    }

    public boolean collectSun(Sun sun) {
        return sun != null && this.dataSource.collectSun(sun);
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void syncSuns() {
        List<Sun> suns = new ArrayList<>(this.dataSource.getGroundSuns());
        suns.removeIf(sun -> sun == null || sun.isCollected() || sun.getPosition() == null);
        removeMissing(suns);
        for (Sun sun : suns) {
            Image actor = this.actors.get(sun);
            if (actor == null) {
                actor = createSunActor(sun);
                if (actor == null) {
                    continue;
                }
                this.actors.put(sun, actor);
                addActor(actor);
                if (this.spawnListener != null) {
                    this.spawnListener.accept(sun);
                }
            }
            positionSun(actor, sun);
        }
    }

    private void removeMissing(List<Sun> suns) {
        List<Sun> removed = new ArrayList<>();
        for (Sun sun : this.actors.keySet()) {
            if (!containsIdentity(suns, sun)) {
                removed.add(sun);
            }
        }
        for (Sun sun : removed) {
            if (this.collecting.contains(sun)) {
                continue;
            }
            Image actor = this.actors.remove(sun);
            if (actor != null) {
                actor.remove();
            }
        }
    }

    private boolean containsIdentity(List<Sun> suns, Sun wanted) {
        for (Sun sun : suns) {
            if (sun == wanted) {
                return true;
            }
        }
        return false;
    }

    private Image createSunActor(Sun sun) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank == null || bank.region(SUN_RESOURCE) == null) {
                return null;
            }
            Image image = new Image(new TextureRegionDrawable(bank.region(SUN_RESOURCE)));
            image.setScaling(Scaling.fit);
            image.setColor(sunColor(sun.getType()));
            image.setTouchable(Touchable.enabled);
            image.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (collecting.contains(sun) || !collectSun(sun)) {
                        return;
                    }
                    collecting.add(sun);
                    image.setTouchable(Touchable.disabled);
                    image.setOrigin(image.getWidth() / 2f, image.getHeight() / 2f);
                    image.clearActions();
                    image.addAction(Actions.sequence(
                            Actions.parallel(
                                    Actions.fadeOut(0.18f),
                                    Actions.scaleTo(0.35f, 0.35f, 0.18f),
                                    Actions.moveBy(0f, 54f, 0.18f)
                            ),
                            Actions.run(() -> {
                                collecting.remove(sun);
                                actors.remove(sun);
                                image.remove();
                            })
                    ));
                }
            });
            return image;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void positionSun(Image actor, Sun sun) {
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float centerX = (sun.getPosition().getX() + 0.5f) * cellWidth;
        float groundY = (
                GameplayBoardInteractionLayer.ROW_COUNT - 0.5f - sun.getPosition().getY()
        ) * cellHeight;
        float centerY = sun.isFalling() ? fallingY(sun, groundY) : groundY;
        float size = cellHeight * sizeFactor(sun.getType());
        actor.setBounds(centerX - size / 2f, centerY - size / 2f, size, size);
        actor.setColor(sunColor(sun.getType()));
    }

    private float fallingY(Sun sun, float groundY) {
        long start = sun.getSpawnTick();
        long end = Math.max(start + 1L, sun.getLandingTick());
        long now = this.dataSource.getCurrentTick();
        float progress = (float) (now - start) / (float) (end - start);
        progress = Math.max(0f, Math.min(1f, progress));
        float startY = getHeight() + 90f;
        float eased = 1f - (1f - progress) * (1f - progress);
        return startY + (groundY - startY) * eased;
    }

    private float sizeFactor(SunType type) {
        if (type == SunType.SPECIAL) {
            return 0.78f;
        }
        if (type == SunType.RADIOACTIVE) {
            return 0.66f;
        }
        return 0.58f;
    }

    private Color sunColor(SunType type) {
        if (type == SunType.RADIOACTIVE) {
            return new Color(0.82f, 0.44f, 1f, 1f);
        }
        if (type == SunType.SPECIAL) {
            return new Color(1f, 0.92f, 0.45f, 1f);
        }
        return Color.WHITE;
    }
}
