package college.java.project.graphics.minigame;

import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.GameplayPamScale;
import college.java.project.graphics.GameplayWorldLayout;
import college.java.project.graphics.PamAnimationActor;
import college.java.project.graphics.PamAnimationCatalog;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.WallnutBowlingController;
import model.mechanism.Position;
import model.minigame.wallnutbowlingminigame.BowlingWallnutType;
import model.minigame.wallnutbowlingminigame.RollingWallnut;
import model.minigame.wallnutbowlingminigame.WallnutBowlingActionResult;
import model.minigame.wallnutbowlingminigame.WallnutBowlingActionStatus;
import model.minigame.wallnutbowlingminigame.WallnutBowlingStateResult;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WallnutBowlingLayer extends Group {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;
    private static final String BELT = "IMAGE_UI_CONVEYOR_CONVEYOR_BELT";
    private static final String SIDE = "IMAGE_UI_CONVEYOR_CONVEYOR_SIDE";
    private static final String TOP = "IMAGE_UI_CONVEYOR_CONVEYOR_TOP";
    private static final float BELT_X = 276f;
    private static final float BELT_Y = 0f;
    private static final float BELT_WIDTH = 146f;
    private static final float BELT_HEIGHT = GameplayWorldLayout.STAGE_HEIGHT;
    private static final float BELT_SEGMENT_HEIGHT = 150f;
    private static final float BELT_SPEED = 54f;
    private static final float CARD_X = 290f;
    private static final float CARD_TOP = 950f;
    private static final float CARD_WIDTH = 118f;
    private static final float CARD_HEIGHT = 108f;
    private static final float CARD_GAP = 10f;
    private static final float MOVE_SECONDS = 0.14f;
    private static final Color CARD_NORMAL = new Color(0.16f, 0.24f, 0.14f, 0.92f);
    private static final Color CARD_SELECTED = new Color(0.55f, 0.82f, 0.18f, 0.98f);
    private static final Color BELT_FALLBACK = new Color(0.20f, 0.16f, 0.11f, 0.97f);
    private static final Color PLANTING_HIGHLIGHT = new Color(0.30f, 0.85f, 0.18f, 0.18f);

    private final WallnutBowlingController controller;
    private final GameAssetManager assets;
    private final PamAnimationCatalog animationCatalog;
    private final Skin skin;
    private final Table conveyorChrome = new Table();
    private final List<Image> beltSegments = new ArrayList<>();
    private final List<RenderedCard> conveyorCards = new ArrayList<>();
    private final List<BowlingWallnutType> conveyorSnapshot = new ArrayList<>();
    private final Map<RollingWallnut, RenderedWallnut> rollingActors = new IdentityHashMap<>();
    private final Table[][] plantingCells = new Table[ROW_COUNT][COLUMN_COUNT];
    private final Label statusLabel;
    private int selectedConveyorIndex = -1;
    private int plantingBoundaryColumn;
    private float beltOffset;

    public WallnutBowlingLayer(WallnutBowlingController controller, GameAssetManager assets) {
        if (controller == null || assets == null) {
            throw new IllegalArgumentException("Wallnut Bowling layer dependencies are required");
        }

        this.controller = controller;
        this.assets = assets;
        this.animationCatalog = new PamAnimationCatalog();
        this.skin = PvzSkin.get();
        this.statusLabel = new Label("", this.skin, "medium_outline");

        setTouchable(Touchable.childrenOnly);
        createPlantingCells();
        createConveyorChrome();
        configureStatusLabel();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        advanceBelt(Math.max(0f, delta));

        WallnutBowlingStateResult state = this.controller.onShowWallnutBowlingRequested();
        if (state == null) {
            return;
        }

        this.plantingBoundaryColumn = state.getPlantingBoundaryColumn();
        syncConveyor(state.getConveyorBelt());
        syncRollingWallnuts(state.getRollingWallnuts());
        refreshPlantingCells();
    }

    private void createPlantingCells() {
        float cellWidth = GameplayWorldLayout.cellWidth();
        float cellHeight = GameplayWorldLayout.cellHeight();

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Table cell = new Table();
                float x = GameplayWorldLayout.LAWN_X + column * cellWidth;
                float y = GameplayWorldLayout.LAWN_Y + (ROW_COUNT - row - 1) * cellHeight;

                cell.setBounds(x, y, cellWidth, cellHeight);
                cell.setTouchable(Touchable.disabled);

                int targetColumn = column;
                int targetRow = row;
                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        placeSelectedWallnut(targetColumn, targetRow);
                    }
                });

                this.plantingCells[row][column] = cell;
                addActor(cell);
            }
        }
    }

    private void createConveyorChrome() {
        this.conveyorChrome.setBounds(BELT_X, BELT_Y, BELT_WIDTH, BELT_HEIGHT);
        this.conveyorChrome.setTouchable(Touchable.disabled);
        this.conveyorChrome.setClip(true);

        Drawable beltDrawable = resourceDrawable(BELT);
        if (beltDrawable == null) {
            Table fallback = new Table();
            fallback.setBounds(0f, 0f, BELT_WIDTH, BELT_HEIGHT);
            fallback.setBackground(this.skin.newDrawable("white_pixel", BELT_FALLBACK));
            this.conveyorChrome.addActor(fallback);
        } else {
            int segmentCount = Math.max(2, (int) Math.ceil(BELT_HEIGHT / BELT_SEGMENT_HEIGHT) + 2);
            for (int index = 0; index < segmentCount; index++) {
                Image segment = new Image(beltDrawable);
                segment.setScaling(Scaling.stretch);
                segment.setBounds(
                    0f,
                    index * BELT_SEGMENT_HEIGHT - BELT_SEGMENT_HEIGHT,
                    BELT_WIDTH,
                    BELT_SEGMENT_HEIGHT
                );
                segment.setTouchable(Touchable.disabled);
                this.conveyorChrome.addActor(segment);
                this.beltSegments.add(segment);
            }
        }

        addChromeImage(SIDE, 0f, 0f, 30f, BELT_HEIGHT);
        addChromeImage(TOP, 0f, BELT_HEIGHT - 28f, BELT_WIDTH, 28f);
        addActor(this.conveyorChrome);
    }

    private void advanceBelt(float delta) {
        if (this.beltSegments.isEmpty() || delta <= 0f) {
            return;
        }

        this.beltOffset = (this.beltOffset + delta * BELT_SPEED) % BELT_SEGMENT_HEIGHT;
        for (int index = 0; index < this.beltSegments.size(); index++) {
            this.beltSegments.get(index).setY(
                index * BELT_SEGMENT_HEIGHT - BELT_SEGMENT_HEIGHT - this.beltOffset
            );
        }
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
        this.conveyorChrome.addActor(image);
    }

    private void configureStatusLabel() {
        this.statusLabel.setAlignment(Align.center);
        this.statusLabel.setColor(Color.WHITE);
        this.statusLabel.setTouchable(Touchable.disabled);
        this.statusLabel.setBounds(610f, 24f, 760f, 48f);
        addActor(this.statusLabel);
    }

    private void syncConveyor(List<BowlingWallnutType> conveyor) {
        List<BowlingWallnutType> safeConveyor = conveyor == null
            ? Collections.emptyList()
            : conveyor;

        if (this.conveyorSnapshot.equals(safeConveyor)) {
            return;
        }

        List<RenderedCard> previousCards = new ArrayList<>(this.conveyorCards);
        boolean[] previousUsed = new boolean[previousCards.size()];
        List<RenderedCard> nextCards = new ArrayList<>();

        for (int index = 0; index < safeConveyor.size(); index++) {
            BowlingWallnutType type = safeConveyor.get(index);
            int previousIndex = findPreviousCard(type, previousCards, previousUsed);
            RenderedCard card;

            if (previousIndex >= 0) {
                card = previousCards.get(previousIndex);
                previousUsed[previousIndex] = true;
                moveExistingCard(card, index, previousIndex);
            } else {
                card = createConveyorCard(type, index);
                animateNewCard(card, index);
            }

            card.index = index;
            nextCards.add(card);
        }

        for (int index = 0; index < previousCards.size(); index++) {
            if (!previousUsed[index]) {
                removeConveyorCard(previousCards.get(index));
            }
        }

        this.conveyorCards.clear();
        this.conveyorCards.addAll(nextCards);
        this.conveyorSnapshot.clear();
        this.conveyorSnapshot.addAll(safeConveyor);

        if (this.selectedConveyorIndex >= safeConveyor.size()) {
            this.selectedConveyorIndex = -1;
        }

        refreshCardSelection();
    }

    private int findPreviousCard(
        BowlingWallnutType type,
        List<RenderedCard> previousCards,
        boolean[] previousUsed
    ) {
        for (int index = 0; index < previousCards.size(); index++) {
            if (!previousUsed[index] && previousCards.get(index).type == type) {
                return index;
            }
        }
        return -1;
    }

    private RenderedCard createConveyorCard(BowlingWallnutType type, int index) {
        Group root = new Group();
        root.setBounds(CARD_X, targetCardY(index), CARD_WIDTH, CARD_HEIGHT);
        root.setTouchable(Touchable.enabled);

        Group visual = new Group();
        visual.setTransform(true);
        visual.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);
        visual.setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
        visual.setTouchable(Touchable.disabled);

        Table background = new Table();
        background.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);
        background.setBackground(this.skin.newDrawable("white_pixel", CARD_NORMAL));
        background.setTouchable(Touchable.disabled);
        visual.addActor(background);

        Actor body = createWallnutBody(type);
        centreBody(body, CARD_WIDTH, CARD_HEIGHT);
        visual.addActor(body);
        root.addActor(visual);

        RenderedCard card = new RenderedCard(type, root, visual, background, index);
        installCardInteraction(card);
        addActor(root);
        return card;
    }

    private void installCardInteraction(RenderedCard card) {
        card.root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectConveyorIndex(card.index);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                card.hovered = true;
                refreshCardVisual(card);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                card.hovered = false;
                refreshCardVisual(card);
            }
        });
    }

    private void animateNewCard(RenderedCard card, int index) {
        float targetY = targetCardY(index);
        float startY = BELT_Y - CARD_HEIGHT - 30f - index * 14f;
        float duration = 0.30f + Math.min(index, 5) * 0.045f;

        card.root.setPosition(CARD_X, startY);
        card.root.getColor().a = 0f;
        card.visual.setScale(0.82f);
        card.root.addAction(Actions.parallel(
            Actions.moveTo(CARD_X, targetY, duration, Interpolation.smooth),
            Actions.fadeIn(Math.min(0.22f, duration))
        ));
    }

    private void moveExistingCard(RenderedCard card, int newIndex, int oldIndex) {
        float targetY = targetCardY(newIndex);
        float duration = 0.20f + Math.min(Math.abs(oldIndex - newIndex), 4) * 0.035f;
        card.root.clearActions();
        card.root.addAction(Actions.moveTo(CARD_X, targetY, duration, Interpolation.smooth));
    }

    private void removeConveyorCard(RenderedCard card) {
        card.root.clearActions();
        card.root.setTouchable(Touchable.disabled);
        card.root.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeOut(0.16f),
                Actions.moveBy(0f, -28f, 0.16f, Interpolation.smooth)
            ),
            Actions.removeActor()
        ));
    }

    private float targetCardY(int index) {
        return CARD_TOP - CARD_HEIGHT - index * (CARD_HEIGHT + CARD_GAP);
    }

    private void selectConveyorIndex(int index) {
        this.selectedConveyorIndex = this.selectedConveyorIndex == index ? -1 : index;
        refreshCardSelection();
        refreshPlantingCells();
    }

    private void refreshCardSelection() {
        for (RenderedCard card : this.conveyorCards) {
            refreshCardVisual(card);
        }
    }

    private void refreshCardVisual(RenderedCard card) {
        boolean selected = card.index == this.selectedConveyorIndex;
        card.background.setBackground(this.skin.newDrawable(
            "white_pixel",
            selected ? CARD_SELECTED : CARD_NORMAL
        ));

        float targetScale = selected ? 1.08f : card.hovered ? 1.04f : 1f;
        card.visual.clearActions();
        card.visual.addAction(Actions.scaleTo(
            targetScale,
            targetScale,
            0.10f,
            Interpolation.smooth
        ));
    }

    private void refreshPlantingCells() {
        boolean selecting = this.selectedConveyorIndex >= 0;

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Table cell = this.plantingCells[row][column];
                boolean available = selecting && column < this.plantingBoundaryColumn;

                cell.setTouchable(available ? Touchable.enabled : Touchable.disabled);
                cell.setBackground(available
                    ? this.skin.newDrawable("white_pixel", PLANTING_HIGHLIGHT)
                    : null);
            }
        }
    }

    private void placeSelectedWallnut(int column, int row) {
        if (this.selectedConveyorIndex < 0) {
            return;
        }

        WallnutBowlingActionResult result = this.controller.onPlaceWallnutRequested(
            this.selectedConveyorIndex + 1,
            new Position(column + 1, row + 1)
        );

        if (result != null && result.getStatus() == WallnutBowlingActionStatus.WALLNUT_PLACED) {
            this.selectedConveyorIndex = -1;
            showStatus("Wallnut launched.");
            return;
        }

        showStatus(messageFor(result));
    }

    private void syncRollingWallnuts(List<RollingWallnut> wallnuts) {
        Set<RollingWallnut> active = Collections.newSetFromMap(new IdentityHashMap<>());
        if (wallnuts == null) {
            wallnuts = Collections.emptyList();
        }

        for (RollingWallnut wallnut : wallnuts) {
            if (wallnut == null || !wallnut.isMoving() || wallnut.getPosition() == null) {
                continue;
            }

            active.add(wallnut);
            RenderedWallnut rendered = this.rollingActors.get(wallnut);
            if (rendered == null) {
                rendered = createRollingActor(wallnut);
            }

            moveRollingActor(rendered, wallnut.getPosition());
        }

        Iterator<Map.Entry<RollingWallnut, RenderedWallnut>> iterator = this.rollingActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RollingWallnut, RenderedWallnut> entry = iterator.next();
            if (!active.contains(entry.getKey())) {
                removeRollingActor(entry.getValue());
                iterator.remove();
            }
        }
    }

    private RenderedWallnut createRollingActor(RollingWallnut wallnut) {
        float cellWidth = GameplayWorldLayout.cellWidth();
        float cellHeight = GameplayWorldLayout.cellHeight();

        Group root = new Group();
        root.setTransform(true);
        root.setSize(cellWidth, cellHeight);
        root.setOrigin(cellWidth / 2f, cellHeight / 2f);
        root.setTouchable(Touchable.disabled);

        Actor body = createWallnutBody(wallnut.getType());
        centreBody(body, cellWidth, cellHeight);
        root.addActor(body);

        RenderedWallnut rendered = new RenderedWallnut(root);
        this.rollingActors.put(wallnut, rendered);
        addActor(root);
        return rendered;
    }

    private void moveRollingActor(RenderedWallnut rendered, Position position) {
        int column = position.getX() - 1;
        int row = position.getY() - 1;
        if (column == rendered.column && row == rendered.row) {
            return;
        }

        float x = GameplayWorldLayout.cellCenterX(column) - rendered.root.getWidth() / 2f;
        float y = GameplayWorldLayout.cellCenterY(row) - rendered.root.getHeight() / 2f;
        rendered.root.clearActions();

        if (rendered.column < 0) {
            rendered.root.setPosition(x, y);
        } else {
            rendered.root.addAction(Actions.moveTo(x, y, MOVE_SECONDS));
        }

        rendered.column = column;
        rendered.row = row;
    }

    private void removeRollingActor(RenderedWallnut rendered) {
        rendered.root.clearActions();
        rendered.root.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeOut(0.14f),
                Actions.scaleTo(0.70f, 0.70f, 0.14f)
            ),
            Actions.removeActor()
        ));
    }

    private Actor createWallnutBody(BowlingWallnutType type) {
        String animationName = type == BowlingWallnutType.EXPLODE_O_NUT
            ? "EXPLODEONUT"
            : "WALLNUT";
        PamAnimationCatalog.AnimationInfo animation = this.animationCatalog.find(animationName);

        if (animation != null && Gdx.files.internal("IMAGES/" + animation.getPath()).exists()) {
            PamAnimationActor body = new PamAnimationActor(
                this.assets.getPamPlayer(),
                animation.getPath(),
                animation.getPreviewClip(),
                animation.getCanvasWidth(),
                animation.getCanvasHeight()
            );

            body.setSize(
                GameplayPamScale.actorWidth(animation.getCanvasWidth()),
                GameplayPamScale.actorHeight(animation.getCanvasHeight())
            );
            body.setTouchable(Touchable.disabled);

            if (type == BowlingWallnutType.GIANT_WALLNUT) {
                body.setScale(1.38f);
                body.setColor(new Color(0.82f, 1f, 0.58f, 1f));
            }

            return body;
        }

        Label fallback = new Label(displayName(type), this.skin, "secondary");
        fallback.setAlignment(Align.center);
        fallback.setWrap(true);
        fallback.setTouchable(Touchable.disabled);
        return fallback;
    }

    private void centreBody(Actor body, float width, float height) {
        if (body.getWidth() <= 0f || body.getHeight() <= 0f) {
            body.setBounds(0f, 0f, width, height);
            return;
        }

        body.setPosition(
            (width - body.getWidth()) / 2f,
            (height - body.getHeight()) / 2f
        );
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private String displayName(BowlingWallnutType type) {
        if (type == BowlingWallnutType.EXPLODE_O_NUT) {
            return "Explode-o-nut";
        }
        if (type == BowlingWallnutType.GIANT_WALLNUT) {
            return "Giant Wall-nut";
        }
        return "Wall-nut";
    }

    private String messageFor(WallnutBowlingActionResult result) {
        if (result == null) {
            return "Action failed.";
        }
        if (result.getStatus() == WallnutBowlingActionStatus.OUTSIDE_PLANTING_AREA) {
            return "Choose a highlighted tile.";
        }
        if (result.getStatus() == WallnutBowlingActionStatus.INVALID_CONVEYOR_INDEX) {
            return "That wallnut is no longer available.";
        }
        return "Action failed: " + result.getStatus().name();
    }

    private void showStatus(String message) {
        this.statusLabel.setText(message == null ? "" : message);
        this.statusLabel.clearActions();
        this.statusLabel.getColor().a = 1f;
        this.statusLabel.addAction(Actions.sequence(
            Actions.delay(1.6f),
            Actions.fadeOut(0.25f)
        ));
    }

    private static final class RenderedCard {
        private final BowlingWallnutType type;
        private final Group root;
        private final Group visual;
        private final Table background;
        private int index;
        private boolean hovered;

        private RenderedCard(
            BowlingWallnutType type,
            Group root,
            Group visual,
            Table background,
            int index
        ) {
            this.type = type;
            this.root = root;
            this.visual = visual;
            this.background = background;
            this.index = index;
        }
    }

    private static final class RenderedWallnut {
        private final Group root;
        private int column = -1;
        private int row = -1;

        private RenderedWallnut(Group root) {
            this.root = root;
        }
    }
}
