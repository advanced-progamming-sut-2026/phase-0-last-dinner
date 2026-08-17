package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.level.LevelType;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Original-art conveyor selector used by Conveyor Belt levels instead of the normal seed bank. */
public final class GameplayConveyorBelt extends Group {
    private static final String BELT = "IMAGE_UI_CONVEYOR_CONVEYOR_BELT";
    private static final String SIDE = "IMAGE_UI_CONVEYOR_CONVEYOR_SIDE";
    private static final String TOP = "IMAGE_UI_CONVEYOR_CONVEYOR_TOP";
    private static final Color FALLBACK_BELT = new Color(0.20f, 0.16f, 0.11f, 0.97f);

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final Skin skin = PvzSkin.get();
    private final List<String> renderedNames = new ArrayList<>();
    private final List<Image> beltSegments = new ArrayList<>();
    private PacketSelectionListener selectionListener;
    private float beltOffset;
    private float beltSegmentWidth;

    public GameplayConveyorBelt(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayConveyorBelt(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null || assets == null) {
            throw new IllegalArgumentException("Gameplay conveyor dependencies are required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        setTouchable(Touchable.childrenOnly);
    }

    public void setPacketSelectionListener(PacketSelectionListener listener) {
        this.selectionListener = listener;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        if (this.dataSource.getLevelType() != LevelType.CONVEYOR_BELT) {
            return;
        }
        advanceBelt(Math.max(0f, delta));
        List<String> names = safeNames(this.dataSource.getConveyorPlantNames());
        if (!sameNames(names)) {
            rebuild(names);
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        if (this.dataSource.getLevelType() == LevelType.CONVEYOR_BELT) {
            rebuild(safeNames(this.dataSource.getConveyorPlantNames()));
            return;
        }
        clearChildren();
        this.renderedNames.clear();
    }

    public int getPacketCount() {
        return this.renderedNames.size();
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void rebuild(List<String> names) {
        List<String> previousNames = new ArrayList<>(this.renderedNames);
        boolean[] previousUsed = new boolean[previousNames.size()];
        clearChildren();
        this.renderedNames.clear();
        this.beltSegments.clear();
        this.renderedNames.addAll(names);
        addBeltChrome();
        float packetWidth = Math.min(118f, Math.max(88f, getHeight() * 0.72f));
        float packetHeight = Math.min(140f, Math.max(102f, getHeight() * 0.88f));
        float gap = 10f;
        float firstX = 58f;
        float x = firstX;
        int index = 0;
        for (String name : names) {
            Group packet = createPacket(name, packetWidth, packetHeight);
            float targetX = x;
            int previousIndex = matchedPreviousIndex(name, previousNames, previousUsed);
            float startX = previousIndex >= 0
                    ? firstX + previousIndex * (packetWidth + gap)
                    : Math.max(getWidth() + 24f, targetX + 180f + index * 26f);
            packet.setBounds(startX, 6f, packetWidth, packetHeight);
            addActor(packet);
            float duration = previousIndex >= 0
                    ? 0.20f + Math.min(Math.abs(previousIndex - index), 4) * 0.035f
                    : 0.28f + Math.min(index, 5) * 0.045f;
            packet.addAction(Actions.sequence(
                    Actions.moveTo(targetX, 6f, duration, Interpolation.smooth),
                    Actions.sequence(
                            Actions.scaleTo(1.035f, 1.035f, 0.06f),
                            Actions.scaleTo(1f, 1f, 0.08f)
                    )
            ));
            x += packetWidth + gap;
            index++;
            if (x + packetWidth > getWidth() - 30f) {
                break;
            }
        }
    }

    private int matchedPreviousIndex(
            String name,
            List<String> previousNames,
            boolean[] previousUsed
    ) {
        String wanted = normalize(name);
        for (int index = 0; index < previousNames.size(); index++) {
            if (!previousUsed[index] && wanted.equals(normalize(previousNames.get(index)))) {
                previousUsed[index] = true;
                return index;
            }
        }
        return -1;
    }

    private void addBeltChrome() {
        Drawable belt = resourceDrawable(BELT);
        if (belt != null) {
            this.beltSegmentWidth = 127f * 1.25f;
            int count = Math.max(2, (int) Math.ceil(getWidth() / this.beltSegmentWidth) + 2);
            for (int index = 0; index < count; index++) {
                Image image = new Image(belt);
                image.setScaling(Scaling.stretch);
                image.setBounds(
                        index * this.beltSegmentWidth - this.beltSegmentWidth,
                        0f,
                        this.beltSegmentWidth,
                        getHeight()
                );
                image.setTouchable(Touchable.disabled);
                addActor(image);
                this.beltSegments.add(image);
            }
        } else {
            this.beltSegmentWidth = 0f;
            Image fallback = new Image(this.skin.newDrawable("white_pixel", FALLBACK_BELT));
            fallback.setBounds(0f, 0f, getWidth(), getHeight());
            fallback.setTouchable(Touchable.disabled);
            addActor(fallback);
        }
        addChromeImage(SIDE, 0f, 0f, 56f, getHeight());
        addChromeImage(TOP, 0f, getHeight() - 28f, getWidth(), 28f);
    }

    private void advanceBelt(float delta) {
        if (this.beltSegments.isEmpty() || this.beltSegmentWidth <= 0f) {
            return;
        }
        this.beltOffset = (this.beltOffset + delta * 54f) % this.beltSegmentWidth;
        for (int index = 0; index < this.beltSegments.size(); index++) {
            this.beltSegments.get(index).setX(
                    index * this.beltSegmentWidth - this.beltSegmentWidth - this.beltOffset
            );
        }
    }

    private Group createPacket(String plantName, float width, float height) {
        Group root = new Group();
        root.setTouchable(Touchable.enabled);
        PlantPacketCatalog.PacketVisual visual = PlantPacketCatalog.findPacket(plantName);
        Drawable drawable = visual == null ? null : resourceDrawable(visual.getResourceId());
        if (drawable != null) {
            Image image = new Image(drawable);
            image.setScaling(Scaling.fit);
            image.setBounds(0f, 0f, width, height);
            image.setTouchable(Touchable.disabled);
            root.addActor(image);
        } else {
            Image fallback = new Image(this.skin.newDrawable(
                    "white_pixel",
                    new Color(0.25f, 0.18f, 0.10f, 1f)
            ));
            fallback.setBounds(4f, 5f, width - 8f, height - 10f);
            fallback.setTouchable(Touchable.disabled);
            root.addActor(fallback);
            Label label = new Label(plantName, this.skin, "default");
            label.setAlignment(Align.center);
            label.setFontScale(0.58f);
            label.setWrap(true);
            label.setBounds(7f, 8f, width - 14f, height - 16f);
            root.addActor(label);
        }
        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectionListener != null) {
                    selectionListener.onPacketSelected(plantName);
                }
            }
        });
        CollectionUiAnimator.installHoverScale(root);
        return root;
    }

    private void addChromeImage(String resourceId, float x, float y, float width, float height) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.stretch);
        image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled);
        addActor(image);
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // A color fallback keeps the mandatory selector usable if an optional atlas is absent.
        }
        return null;
    }

    private List<String> safeNames(List<String> names) {
        List<String> result = new ArrayList<>();
        if (names == null) {
            return result;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                result.add(name);
            }
        }
        return result;
    }

    private boolean sameNames(List<String> names) {
        if (names.size() != this.renderedNames.size()) {
            return false;
        }
        for (int index = 0; index < names.size(); index++) {
            if (!normalize(names.get(index)).equals(normalize(this.renderedNames.get(index)))) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public interface PacketSelectionListener {
        void onPacketSelected(String plantName);
    }
}
