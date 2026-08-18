package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.Locale;

/**
 * Shared mouse interaction layer for normal planting, shovel and Plant Food.
 * Board coordinates follow Phase 1: column/x first, row/y second.
 */
public final class GameplayBoardInteractionLayer extends Group {
    public static final int COLUMN_COUNT = 9;
    public static final int ROW_COUNT = 5;

    private static final String WHITE_PIXEL = "white_pixel";
    private static final String SHOVEL_ICON = "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    private static final String PLANT_FOOD_ICON = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";

    private static final Color AXIS_COLOR = new Color(1f, 1f, 1f, 0.18f);
    private static final Color VALID_TILE_COLOR = new Color(1f, 1f, 1f, 0.38f);
    private static final Color INVALID_TILE_COLOR = new Color(1f, 0.18f, 0.12f, 0.30f);
    private static final Color SUCCESS_TILE_COLOR = new Color(0.72f, 1f, 0.45f, 0.52f);

    private final GameplaySeedBankDataSource dataSource;
    private final GameplaySeedBank seedBank;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final PamAnimationCatalog animationCatalog;
    private final Image rowHighlight;
    private final Image columnHighlight;
    private final Image tileHighlight;
    private final Image feedbackHighlight;

    private GameplayInteractionMode mode = GameplayInteractionMode.NONE;
    private String selectedPlantName;
    private Actor cursorActor;
    private boolean cursorGroundAnchored;
    private float cursorCenterOffsetX;
    private float cursorGroundOffset;
    private int hoverColumn = -1;
    private int hoverRow = -1;
    private boolean hoverValid;
    private String lastAppliedPlantName;
    private boolean lastAppliedPlantBoosted;
    private InteractionActionListener actionListener;

    public GameplayBoardInteractionLayer(
            GameplaySeedBankDataSource dataSource,
            GameplaySeedBank seedBank
    ) {
        this(dataSource, seedBank, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayBoardInteractionLayer(
            GameplaySeedBankDataSource dataSource,
            GameplaySeedBank seedBank,
            GameAssetManager assets
    ) {
        if (dataSource == null || seedBank == null || assets == null) {
            throw new IllegalArgumentException("Gameplay interaction dependencies are required");
        }
        this.dataSource = dataSource;
        this.seedBank = seedBank;
        this.assets = assets;
        this.animationCatalog = new PamAnimationCatalog();
        this.rowHighlight = overlay(AXIS_COLOR);
        this.columnHighlight = overlay(AXIS_COLOR);
        this.tileHighlight = overlay(VALID_TILE_COLOR);
        this.feedbackHighlight = overlay(SUCCESS_TILE_COLOR);
        addActor(this.rowHighlight);
        addActor(this.columnHighlight);
        addActor(this.tileHighlight);
        addActor(this.feedbackHighlight);
        this.feedbackHighlight.setVisible(false);
        hideHighlights();
        setTouchable(Touchable.enabled);
        addPointerListener();
    }

    public GameplayInteractionMode getMode() {
        return this.mode;
    }

    public int getHoverColumn() {
        return this.hoverColumn;
    }

    public int getHoverRow() {
        return this.hoverRow;
    }

    public boolean isHoverValid() {
        return this.hoverValid;
    }

    public String getSelectedPlantName() {
        return this.selectedPlantName;
    }

    public String getLastAppliedPlantName() {
        return this.lastAppliedPlantName;
    }

    public boolean wasLastAppliedPlantBoosted() {
        return this.lastAppliedPlantBoosted;
    }

    public void setActionListener(InteractionActionListener listener) {
        this.actionListener = listener;
    }

    public void selectPlant(String plantName) {
        if (plantName == null || plantName.trim().isEmpty()) {
            clearMode();
            return;
        }
        this.mode = GameplayInteractionMode.PLANT;
        this.selectedPlantName = plantName;
        this.cursorGroundAnchored = false;
        this.cursorCenterOffsetX = 0f;
        this.cursorGroundOffset = 0f;
        replaceCursor(createPlantCursor(plantName));
        refreshHoverState();
    }

    /** Keeps Imitater selected logically while previewing the plant it will copy. */
    public void updatePlantPreview(String visualPlantName) {
        if (this.mode != GameplayInteractionMode.PLANT
                || this.selectedPlantName == null
                || visualPlantName == null
                || visualPlantName.isBlank()) {
            return;
        }
        this.cursorGroundAnchored = false;
        this.cursorCenterOffsetX = 0f;
        this.cursorGroundOffset = 0f;
        replaceCursor(createPlantCursor(visualPlantName));
        refreshHoverState();
    }

    public boolean activateShovel() {
        if (this.mode == GameplayInteractionMode.SHOVEL) {
            clearMode();
            return false;
        }
        this.mode = GameplayInteractionMode.SHOVEL;
        this.selectedPlantName = null;
        this.cursorGroundAnchored = false;
        this.cursorCenterOffsetX = 0f;
        this.cursorGroundOffset = 0f;
        replaceCursor(createResourceCursor(SHOVEL_ICON, 122f, 82f));
        refreshHoverState();
        return true;
    }

    public boolean activatePlantFood() {
        if (this.mode == GameplayInteractionMode.PLANT_FOOD) {
            clearMode();
            return false;
        }
        if (this.dataSource.getPlantFoodCount() <= 0) {
            return false;
        }
        this.mode = GameplayInteractionMode.PLANT_FOOD;
        this.selectedPlantName = null;
        this.cursorGroundAnchored = false;
        this.cursorCenterOffsetX = 0f;
        this.cursorGroundOffset = 0f;
        replaceCursor(createResourceCursor(PLANT_FOOD_ICON, 76f, 80f));
        refreshHoverState();
        return true;
    }

    public void clearMode() {
        this.mode = GameplayInteractionMode.NONE;
        this.selectedPlantName = null;
        this.hoverColumn = -1;
        this.hoverRow = -1;
        this.hoverValid = false;
        this.cursorGroundAnchored = false;
        this.cursorCenterOffsetX = 0f;
        this.cursorGroundOffset = 0f;
        hideHighlights();
        replaceCursor(null);
    }

    /** Runtime-test helper that also mirrors mouse hover at the center of a tile. */
    public void hoverCell(int column, int row) {
        if (!insideCell(column, row)) {
            clearHoverOnly();
            return;
        }
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;
        float x = (column + 0.5f) * cellWidth;
        float y = (ROW_COUNT - row - 0.5f) * cellHeight;
        updateHover(column, row, x, y);
    }

    /** Runtime-test helper; user clicks use the same execution path. */
    public boolean applyAtCell(int column, int row) {
        if (!insideCell(column, row) || this.mode == GameplayInteractionMode.NONE) {
            return false;
        }
        this.hoverColumn = column;
        this.hoverRow = row;
        this.hoverValid = isTargetValid(column, row);
        if (!this.hoverValid) {
            refreshHoverVisuals();
            playTileFeedback(column, row, false);
            if (this.actionListener != null) {
                this.actionListener.onActionRejected(this.mode, column, row);
            }
            return false;
        }

        GameplayInteractionMode appliedMode = this.mode;
        this.lastAppliedPlantName = appliedMode == GameplayInteractionMode.PLANT
                ? this.selectedPlantName
                : null;
        this.lastAppliedPlantBoosted = this.lastAppliedPlantName != null
                && this.dataSource.isBoosted(this.lastAppliedPlantName);
        boolean applied = executeAction(column, row);
        if (applied) {
            playTileFeedback(column, row, true);
            if (this.actionListener != null) {
                this.actionListener.onActionApplied(appliedMode, column, row);
            }
            clearMode();
        } else if (this.actionListener != null) {
            this.actionListener.onActionRejected(appliedMode, column, row);
        }
        this.seedBank.refresh();
        return applied;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        if (this.mode == GameplayInteractionMode.PLANT_FOOD
                && this.dataSource.getPlantFoodCount() <= 0) {
            clearMode();
        }
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void addPointerListener() {
        addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateFromPointer(x, y);
                return true;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                updateFromPointer(x, y);
                return button == Input.Buttons.LEFT
                        && applyAtCell(hoverColumn, hoverRow);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (toActor == null || !isAscendantOf(toActor)) {
                    clearHoverOnly();
                }
            }
        });
    }

    private void updateFromPointer(float x, float y) {
        if (this.mode == GameplayInteractionMode.NONE || !insideLocal(x, y)) {
            clearHoverOnly();
            return;
        }
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;
        int column = Math.min(COLUMN_COUNT - 1, (int) (x / cellWidth));
        int visualRow = Math.min(ROW_COUNT - 1, (int) (y / cellHeight));
        int row = ROW_COUNT - 1 - visualRow;
        updateHover(column, row, x, y);
    }

    private void updateHover(int column, int row, float pointerX, float pointerY) {
        this.hoverColumn = column;
        this.hoverRow = row;
        this.hoverValid = isTargetValid(column, row);
        refreshHoverVisuals();
        positionCursor(pointerX, pointerY);
        updateCursorColor();
    }

    private void refreshHoverState() {
        if (insideCell(this.hoverColumn, this.hoverRow)) {
            hoverCell(this.hoverColumn, this.hoverRow);
        } else {
            clearHoverOnly();
        }
    }

    private void refreshHoverVisuals() {
        if (this.mode == GameplayInteractionMode.NONE
                || !insideCell(this.hoverColumn, this.hoverRow)) {
            hideHighlights();
            return;
        }
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;
        float tileX = this.hoverColumn * cellWidth;
        float tileY = (ROW_COUNT - 1 - this.hoverRow) * cellHeight;

        this.rowHighlight.setBounds(0f, tileY, getWidth(), cellHeight);
        this.columnHighlight.setBounds(tileX, 0f, cellWidth, getHeight());
        this.tileHighlight.setBounds(tileX, tileY, cellWidth, cellHeight);
        this.tileHighlight.setColor(this.hoverValid ? VALID_TILE_COLOR : INVALID_TILE_COLOR);
        this.rowHighlight.setVisible(true);
        this.columnHighlight.setVisible(true);
        this.tileHighlight.setVisible(true);
    }

    private boolean isTargetValid(int column, int row) {
        return switch (this.mode) {
            case PLANT -> this.selectedPlantName != null
                    && this.dataSource.canPlant(this.selectedPlantName, column, row);
            case SHOVEL -> this.dataSource.hasPlantAt(column, row);
            case PLANT_FOOD -> this.dataSource.canFeedPlantAt(column, row);
            case NONE -> false;
        };
    }

    private boolean executeAction(int column, int row) {
        return switch (this.mode) {
            case PLANT -> this.seedBank.plantActiveAt(column, row);
            case SHOVEL -> this.dataSource.pluckPlant(column, row);
            case PLANT_FOOD -> this.dataSource.feedPlant(column, row);
            case NONE -> false;
        };
    }

    private Actor createPlantCursor(String plantName) {
        PamAnimationCatalog.AnimationInfo animation = this.animationCatalog.find(plantName);
        if (canUseAnimation(animation)) {
            PamAnimationActor actor = new PamAnimationActor(
                    this.assets.getPamPlayer(),
                    animation.getPath(),
                    animation.getPreviewClip(),
                    animation.getCanvasWidth(),
                    animation.getCanvasHeight()
            );
            actor.setSize(
                    GameplayPamScale.actorWidth(animation.getCanvasWidth()),
                    GameplayPamScale.actorHeight(animation.getCanvasHeight())
            );
            this.cursorGroundAnchored = true;
            this.cursorCenterOffsetX = plantCenterOffset(animation);
            this.cursorGroundOffset = plantGroundOffset(animation);
            actor.setColor(1f, 1f, 1f, 0.84f);
            actor.setTouchable(Touchable.disabled);
            return actor;
        }

        PlantPacketCatalog.PacketVisual packet = PlantPacketCatalog.findPacket(plantName);
        if (packet == null) {
            return null;
        }
        Image image = createImage(packet.getResourceId());
        if (image != null) {
            image.setSize(112f, 112f);
            image.setColor(1f, 1f, 1f, 0.82f);
        }
        return image;
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

    private Actor createResourceCursor(String resourceId, float width, float height) {
        Image image = createImage(resourceId);
        if (image != null) {
            image.setSize(width, height);
            image.setColor(1f, 1f, 1f, 0.92f);
        }
        return image;
    }

    private Image createImage(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // The graphical fallback below keeps partial asset packs usable.
        }
        String normalized = resourceId.toLowerCase(Locale.ROOT);
        if (PvzSkin.get().has(normalized, Drawable.class)) {
            return PvzSkin.get().getDrawable(normalized);
        }
        return null;
    }

    private void positionCursor(float x, float y) {
        if (this.cursorActor == null) {
            return;
        }
        if (this.cursorGroundAnchored && insideCell(this.hoverColumn, this.hoverRow)) {
            // Match GameplayPlantLayer's PAM body layout exactly so the ghost
            // does not jump when the click turns it into a real planted actor.
            float cellWidth = getWidth() / COLUMN_COUNT;
            float cellHeight = getHeight() / ROW_COUNT;
            float tileCenterX = (this.hoverColumn + 0.5f) * cellWidth;
            float tileBottom = (ROW_COUNT - 1 - this.hoverRow) * cellHeight;
            this.cursorActor.setPosition(
                    tileCenterX + this.cursorCenterOffsetX - this.cursorActor.getWidth() / 2f,
                    tileBottom - cellHeight * 0.01f
                            + this.cursorGroundOffset - this.cursorActor.getHeight() / 2f
            );
        } else {
            this.cursorActor.setPosition(
                    x - this.cursorActor.getWidth() / 2f,
                    y - this.cursorActor.getHeight() * 0.34f
            );
        }
        this.cursorActor.setVisible(true);
    }

    private void updateCursorColor() {
        if (this.cursorActor == null) {
            return;
        }
        if (this.hoverValid) {
            this.cursorActor.setColor(1f, 1f, 1f, 0.88f);
        } else {
            this.cursorActor.setColor(1f, 0.56f, 0.52f, 0.78f);
        }
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
            // Keep the preview centered if clip bounds are unavailable.
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
            // Keep cursor centered if clip bounds are unavailable.
        }
        return 0f;
    }

    private void replaceCursor(Actor actor) {
        if (this.cursorActor != null) {
            this.cursorActor.remove();
        }
        this.cursorActor = actor;
        if (actor != null) {
            actor.setVisible(false);
            actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
            actor.getColor().a = 0f;
            actor.setScale(0.90f);
            actor.addAction(Actions.parallel(
                    Actions.fadeIn(0.10f),
                    Actions.scaleTo(1f, 1f, 0.12f)
            ));
            addActor(actor);
        }
    }

    private void playTileFeedback(int column, int row, boolean success) {
        if (!insideCell(column, row)) {
            return;
        }
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;
        float tileX = column * cellWidth;
        float tileY = (ROW_COUNT - 1 - row) * cellHeight;
        this.feedbackHighlight.clearActions();
        this.feedbackHighlight.setBounds(tileX, tileY, cellWidth, cellHeight);
        this.feedbackHighlight.setColor(success ? SUCCESS_TILE_COLOR : INVALID_TILE_COLOR);
        this.feedbackHighlight.getColor().a = success ? 0.60f : 0.72f;
        this.feedbackHighlight.setVisible(true);
        this.feedbackHighlight.addAction(Actions.sequence(
                Actions.alpha(success ? 0.22f : 0.28f, 0.18f),
                Actions.visible(false)
        ));
    }

    private Image overlay(Color color) {
        Image image = new Image(PvzSkin.get().newDrawable(WHITE_PIXEL, color));
        image.setColor(color);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void hideHighlights() {
        this.rowHighlight.setVisible(false);
        this.columnHighlight.setVisible(false);
        this.tileHighlight.setVisible(false);
    }

    private void clearHoverOnly() {
        this.hoverColumn = -1;
        this.hoverRow = -1;
        this.hoverValid = false;
        hideHighlights();
        if (this.cursorActor != null) {
            this.cursorActor.setVisible(false);
        }
    }

    private boolean insideLocal(float x, float y) {
        return x >= 0f && y >= 0f && x < getWidth() && y < getHeight();
    }

    private boolean insideCell(int column, int row) {
        return column >= 0 && column < COLUMN_COUNT && row >= 0 && row < ROW_COUNT;
    }

    public interface InteractionActionListener {
        void onActionApplied(GameplayInteractionMode mode, int column, int row);

        default void onActionRejected(GameplayInteractionMode mode, int column, int row) {
        }
    }
}
