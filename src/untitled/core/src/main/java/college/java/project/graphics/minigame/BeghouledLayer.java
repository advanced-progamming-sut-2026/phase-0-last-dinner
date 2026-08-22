package college.java.project.graphics.minigame;

import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.GameplayPlantLayer;
import college.java.project.graphics.PlantPacketCatalog;
import com.badlogic.gdx.graphics.Color;
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
import college.java.project.graphics.GameplayWorldLayout;
import com.badlogic.gdx.utils.Scaling;
import controller.BeghouledController;
import model.mechanism.Position;
import model.minigame.beghouledminigame.BeghouledActionResult;
import model.minigame.beghouledminigame.BeghouledStateResult;
import model.minigame.beghouledminigame.PlantUpgradeOption;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.*;

public final class BeghouledLayer extends Group {
    private static final int COLUMN_COUNT = BeghouledStateResult.COLUMN_COUNT;
    private static final int ROW_COUNT = BeghouledStateResult.ROW_COUNT;

    private static final float PANEL_X = 175f;
    private static final float PANEL_WIDTH = 390f;
    private static final float CARD_HEIGHT = 78f;
    private static final float CARD_GAP = 8f;
    private static final float CARD_TOP = 790f;

    private static final Color SELECTED_COLOR = new Color(1f, 0.82f, 0.12f, 0.36f);

    private static final Color SUCCESS_COLOR = new Color(0.18f, 1f, 0.24f, 0.42f);

    private static final Color INVALID_COLOR = new Color(1f, 0.12f, 0.08f, 0.44f);

    private static final Color CRATER_COLOR = new Color(0.08f, 0.05f, 0.03f, 0.58f);

    private static final Color CARD_NORMAL = new Color(0.20f, 0.14f, 0.09f, 0.95f);

    private static final Color CARD_AVAILABLE = new Color(0.22f, 0.48f, 0.16f, 0.97f);

    private static final Color CARD_DISABLED = new Color(0.12f, 0.12f, 0.12f, 0.90f);

    private final BeghouledController controller;
    private final Skin skin;

    private final Table[][] boardCells = new Table[ROW_COUNT][COLUMN_COUNT];

    private final List<UpgradeCard> upgradeCards = new ArrayList<>();

    private final List<String> upgradeSignature = new ArrayList<>();

    private final Drawable selectedDrawable;
    private final Drawable successDrawable;
    private final Drawable invalidDrawable;
    private final Drawable craterDrawable;
    private final Drawable cardNormalDrawable;
    private final Drawable cardAvailableDrawable;
    private final Drawable cardDisabledDrawable;

    private final Label stageLabel;
    private final MiniGameSunCounter sunCounter;
    private final Label progressLabel;
    private final Label upgradesLabel;
    private final Label statusLabel;

    private BeghouledStateResult currentState;
    private Position selectedPosition;
    private Position firstFeedbackPosition;
    private Position secondFeedbackPosition;

    private float feedbackTime;
    private boolean successfulFeedback;
    private String lastMessage = "Swap adjacent plants to create a match.";

    private final GameplayPlantLayer plantLayer;

    private final GameAssetManager assets;

    private final Set<String> usedUpgradeSources = new HashSet<>();

    private static final Color MATCH_COLOR = new Color(1f, 0.84f, 0.12f, 0.42f);

    private final Set<Integer> changedMatchCells = new HashSet<>();
    private final Drawable matchDrawable;
    private float matchFeedbackTime;

    public BeghouledLayer(BeghouledController controller, GameplayPlantLayer plantLayer, GameAssetManager assets) {
        if (controller == null || plantLayer == null || assets == null)
            throw new IllegalArgumentException("Beghouled layer dependencies are required.");

        this.controller = controller;
        this.plantLayer = plantLayer;
        this.assets = assets;
        this.skin = PvzSkin.get();

        this.selectedDrawable = this.skin.newDrawable("white_pixel", SELECTED_COLOR);

        this.successDrawable = this.skin.newDrawable("white_pixel", SUCCESS_COLOR);

        this.invalidDrawable = this.skin.newDrawable("white_pixel", INVALID_COLOR);

        this.craterDrawable = this.skin.newDrawable("white_pixel", CRATER_COLOR);

        this.cardNormalDrawable = this.skin.newDrawable("white_pixel", CARD_NORMAL);

        this.cardAvailableDrawable = this.skin.newDrawable("white_pixel", CARD_AVAILABLE);

        this.cardDisabledDrawable = this.skin.newDrawable("white_pixel", CARD_DISABLED);

        this.stageLabel = new Label("BEGHOULed", this.skin, "medium_outline");

        this.sunCounter = new MiniGameSunCounter(
            assets, () -> this.currentState == null ? 0 : this.currentState.getSunAmount());

        this.progressLabel = new Label("MATCHES: 0 / 0", this.skin, "secondary");

        this.upgradesLabel = new Label("UPGRADES", this.skin, "medium_outline");

        this.statusLabel = new Label(this.lastMessage, this.skin, "secondary");

        this.matchDrawable = this.skin.newDrawable("white_pixel", MATCH_COLOR);

        setTouchable(Touchable.childrenOnly);

        createBoardCells();
        createLabels();
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        updateFeedback(delta);

        BeghouledStateResult state = this.controller.onShowBeghouledRequested();

        if (state == null)
            return;

        this.currentState = state;

        syncUpgradeCards(state.getUpgradeOptions());
        refreshBoardCells();
        refreshUpgradeCards();
        refreshLabels();
    }

    private void createBoardCells() {
        float cellWidth = GameplayWorldLayout.cellWidth();
        float cellHeight = GameplayWorldLayout.cellHeight();

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Table cell = new Table();

                float x = GameplayWorldLayout.LAWN_X + column * cellWidth;

                float y = GameplayWorldLayout.LAWN_Y + (ROW_COUNT - row - 1) * cellHeight;

                cell.setBounds(x, y, cellWidth, cellHeight);
                cell.setTouchable(Touchable.enabled);

                int selectedColumn = column;
                int selectedRow = row;

                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectCell(selectedColumn, selectedRow);
                    }
                });

                this.boardCells[row][column] = cell;
                addActor(cell);
            }
        }
    }

    private void createLabels() {
        this.stageLabel.setAlignment(Align.center);
        this.stageLabel.setBounds(PANEL_X, 1018f, PANEL_WIDTH, 42f);

        this.sunCounter.setBounds(PANEL_X + 45f, 935f, PANEL_WIDTH - 90f, 76f);

        this.progressLabel.setAlignment(Align.center);
        this.progressLabel.setBounds(PANEL_X, 888f, PANEL_WIDTH, 38f);

        this.upgradesLabel.setAlignment(Align.center);
        this.upgradesLabel.setBounds(PANEL_X, 835f, PANEL_WIDTH, 42f);

        this.statusLabel.setAlignment(Align.center);
        this.statusLabel.setWrap(true);
        this.statusLabel.setTouchable(Touchable.disabled);
        this.statusLabel.setBounds(650f, 18f, 760f, 58f);

        addActor(this.stageLabel);
        addActor(this.sunCounter);
        addActor(this.progressLabel);
        addActor(this.upgradesLabel);
        addActor(this.statusLabel);
    }

    private void selectCell(int column, int row) {
        if (!canInteract())
            return;

        Position position = new Position(column + 1, row + 1);

        if (this.currentState.isCrater(position)) {
            this.lastMessage = "Crater cells cannot be swapped.";
            startFeedback(position, null, false);
            return;
        }

        if (this.currentState.getPlantNameAt(position).isEmpty())
            return;

        if (this.selectedPosition == null) {
            this.selectedPosition = position;
            this.lastMessage = "Select an adjacent plant.";
            refreshBoardCells();
            return;
        }

        if (samePosition(this.selectedPosition, position)) {
            this.selectedPosition = null;
            this.lastMessage = "Selection cleared.";
            refreshBoardCells();
            return;
        }

        if (!areAdjacent(this.selectedPosition, position)) {
            this.selectedPosition = position;
            this.lastMessage = "Now select an adjacent plant.";
            refreshBoardCells();
            return;
        }

        Position first = this.selectedPosition;
        this.selectedPosition = null;

        BeghouledStateResult beforeState = this.currentState;
        BeghouledActionResult result = this.controller.onSwapRequested(first, position);

        if (result.isSuccessful()) {
            BeghouledStateResult afterState = this.controller.onShowBeghouledRequested();
            rememberChangedCells(beforeState, afterState);
            this.currentState = afterState;
            this.plantLayer.animateNextBoardTransition();
        }

        this.lastMessage = result.getMessage();
        startFeedback(first, position, result.isSuccessful());
    }

    private void startFeedback(Position first, Position second, boolean successful) {
        this.firstFeedbackPosition = first;
        this.secondFeedbackPosition = second;
        this.successfulFeedback = successful;
        this.feedbackTime = 0.38f;
        refreshBoardCells();
    }

    private void updateFeedback(float delta) {
        boolean refreshRequired = false;

        if (this.feedbackTime > 0f) {
            this.feedbackTime = Math.max(0f, this.feedbackTime - delta);

            if (this.feedbackTime <= 0f) {
                this.firstFeedbackPosition = null;
                this.secondFeedbackPosition = null;
                refreshRequired = true;
            }
        }

        if (this.matchFeedbackTime > 0f) {
            this.matchFeedbackTime = Math.max(0f, this.matchFeedbackTime - delta);

            if (this.matchFeedbackTime <= 0f) {
                this.changedMatchCells.clear();
                refreshRequired = true;
            }
        }

        if (refreshRequired)
            refreshBoardCells();
    }

    private void refreshBoardCells() {
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Position position = new Position(column + 1, row + 1);

                Table cell = this.boardCells[row][column];

                if (this.currentState != null
                    && this.currentState.isCrater(position)) {
                    cell.setBackground(this.craterDrawable);
                    cell.setTouchable(Touchable.disabled);
                    continue;
                }

                cell.setTouchable(canInteract() ? Touchable.enabled : Touchable.disabled);

                if (isFeedbackPosition(position)) {
                    cell.setBackground(this.successfulFeedback ? this.successDrawable : this.invalidDrawable);
                    continue;
                }

                if (this.matchFeedbackTime > 0f && this.changedMatchCells.contains(boardCellKey(position))) {
                    cell.setBackground(this.matchDrawable);
                    continue;
                }

                if (samePosition(this.selectedPosition, position)) {
                    cell.setBackground(this.selectedDrawable);
                    continue;
                }

                cell.setBackground((Drawable) null);
            }
        }
    }

    private void rememberChangedCells(BeghouledStateResult beforeState, BeghouledStateResult afterState) {
        this.changedMatchCells.clear();

        if (beforeState == null || afterState == null)
            return;

        for (int row = 1; row <= ROW_COUNT; row++) {
            for (int column = 1; column <= COLUMN_COUNT; column++) {
                Position position = new Position(column, row);
                String beforeName = beforeState.getPlantNameAt(position);
                String afterName = afterState.getPlantNameAt(position);

                if (!samePlantName(beforeName, afterName))
                    this.changedMatchCells.add(boardCellKey(position));
            }
        }

        this.matchFeedbackTime = this.changedMatchCells.isEmpty() ? 0f : 0.48f;
    }

    private int boardCellKey(Position position) {
        return (position.getY() - 1) * COLUMN_COUNT + position.getX() - 1;
    }

    private boolean samePlantName(String first, String second) {
        if (first == null)
            return second == null;

        return second != null && first.equalsIgnoreCase(second);
    }

    private boolean isFeedbackPosition(Position position) {
        if (this.feedbackTime <= 0f)
            return false;

        return samePosition(position, this.firstFeedbackPosition) || samePosition(position, this.secondFeedbackPosition);
    }

    private void syncUpgradeCards(List<PlantUpgradeOption> options) {
        List<String> signature = createUpgradeSignature(options);

        if (this.upgradeSignature.equals(signature))
            return;

        for (UpgradeCard card : this.upgradeCards)
            card.root.remove();

        this.upgradeCards.clear();
        this.upgradeSignature.clear();
        this.upgradeSignature.addAll(signature);

        if (options == null)
            return;

        for (int index = 0; index < options.size(); index++)
            createUpgradeCard(options.get(index), index);
    }

    private List<String> createUpgradeSignature(List<PlantUpgradeOption> options) {
        List<String> signature = new ArrayList<>();

        if (options == null)
            return signature;

        for (PlantUpgradeOption option : options) {
            if (option == null) {
                signature.add("");
                continue;
            }

            signature.add(option.getSourcePlant().getName() + ":" + option.getTargetPlant().getName() + ":"
                + option.getSunCost());
        }

        return signature;
    }

    private void createUpgradeCard(PlantUpgradeOption option, int index) {
        Group root = new Group();

        float y = CARD_TOP - CARD_HEIGHT - index * (CARD_HEIGHT + CARD_GAP);

        root.setBounds(PANEL_X, y, PANEL_WIDTH, CARD_HEIGHT);

        root.setOrigin(PANEL_WIDTH / 2f, CARD_HEIGHT / 2f);

        root.setTransform(true);
        root.setTouchable(Touchable.enabled);

        Table background = new Table();
        background.setBounds(0f, 0f, PANEL_WIDTH, CARD_HEIGHT);
        background.setTouchable(Touchable.disabled);
        root.addActor(background);

        Image sourceImage = createPlantPacketImage(option.getSourcePlant().getName());
        if (sourceImage != null) {
            sourceImage.setBounds(5f, 7f, 64f, 64f);
            root.addActor(sourceImage);
        }

        Label arrowLabel = new Label(">", this.skin, "medium_outline");
        arrowLabel.setAlignment(Align.center);
        arrowLabel.setFontScale(0.65f);
        arrowLabel.setBounds(68f, 18f, 28f, 40f);
        arrowLabel.setTouchable(Touchable.disabled);
        root.addActor(arrowLabel);

        Image targetImage = createPlantPacketImage(option.getTargetPlant().getName());
        if (targetImage != null) {
            targetImage.setBounds(96f, 7f, 64f, 64f);
            root.addActor(targetImage);
        }

        Label sourceLabel = new Label(option.getSourcePlant().getName(), this.skin, "medium_outline");
        sourceLabel.setAlignment(Align.left);
        sourceLabel.setEllipsis(true);
        sourceLabel.setFontScale(0.52f);
        sourceLabel.setBounds(166f, 40f, 132f, 32f);
        sourceLabel.setTouchable(Touchable.disabled);
        root.addActor(sourceLabel);

        Label targetLabel = new Label(option.getTargetPlant().getName(), this.skin, "medium_outline");
        targetLabel.setAlignment(Align.left);
        targetLabel.setEllipsis(true);
        targetLabel.setFontScale(0.48f);
        targetLabel.setBounds(166f, 7f, 132f, 31f);
        targetLabel.setTouchable(Touchable.disabled);
        root.addActor(targetLabel);

        Label costLabel = new Label(option.getSunCost() + " SUN", this.skin, "medium_outline");
        costLabel.setAlignment(Align.center);
        costLabel.setColor(Color.YELLOW);
        costLabel.setFontScale(0.50f);
        costLabel.setBounds(300f, 17f, 84f, 44f);
        costLabel.setTouchable(Touchable.disabled);
        root.addActor(costLabel);

        UpgradeCard card = new UpgradeCard(option, root, background, costLabel);

        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                requestUpgrade(card);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from
            ) {
                card.root.addAction(Actions.scaleTo(1.03f, 1.03f, 0.10f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
                card.root.addAction(Actions.scaleTo(1f, 1f, 0.10f));
            }
        });

        this.upgradeCards.add(card);
        addActor(root);
    }

    private Image createPlantPacketImage(String plantName) {
        PlantPacketCatalog.PacketVisual visual = PlantPacketCatalog.findPacket(plantName);

        if (visual == null || visual.getResourceId() == null)
            return null;

        if ("IMAGE_UI_PACKETS_EMPTY_PACKET".equals(visual.getResourceId()))
            return null;

        Drawable drawable = resourceDrawable(visual.getResourceId());

        if (drawable == null)
            return null;

        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();

            if (bank != null && bank.region(resourceId) != null)
                return new TextureRegionDrawable(bank.region(resourceId));
        } catch (RuntimeException ignored) {
            return null;
        }

        return null;
    }

    private void requestUpgrade(UpgradeCard card) {
        if (!canInteract() || card == null)
            return;

        String sourceName = card.option.getSourcePlant().getName();

        BeghouledActionResult result = this.controller.onUpgradeRequested(sourceName);
        this.lastMessage = result.getMessage();

        if (result.isSuccessful()) {
            this.usedUpgradeSources.add(normalize(sourceName));

            card.root.addAction(Actions.sequence(
                Actions.scaleTo(1.08f, 1.08f, 0.10f),
                Actions.scaleTo(1f, 1f, 0.14f)
            ));
        } else {
            card.root.addAction(Actions.sequence(
                Actions.moveBy(-8f, 0f, 0.05f),
                Actions.moveBy(16f, 0f, 0.08f),
                Actions.moveBy(-8f, 0f, 0.05f)
            ));
        }
    }

    private void refreshUpgradeCards() {
        if (this.currentState == null)
            return;

        int sunAmount = this.currentState.getSunAmount();
        boolean running = canInteract();

        for (UpgradeCard card : this.upgradeCards) {
            String sourceName = card.option.getSourcePlant().getName();
            boolean used = this.usedUpgradeSources.contains(normalize(sourceName));
            boolean affordable = card.option.canUpgrade(sunAmount);
            boolean enabled = running && !used;

            if (!enabled) {
                card.background.setBackground(this.cardDisabledDrawable);
            } else if (affordable) {
                card.background.setBackground(this.cardAvailableDrawable);
            } else {
                card.background.setBackground(this.cardNormalDrawable);
            }

            if (used) {
                card.costLabel.setText("DONE");
                card.costLabel.setColor(Color.LIGHT_GRAY);
            } else {
                card.costLabel.setText(card.option.getSunCost() + " SUN");
                card.costLabel.setColor(affordable ? Color.YELLOW : Color.LIGHT_GRAY);
            }

            card.root.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void refreshLabels() {
        if (this.currentState == null)
            return;

        this.stageLabel.setText("BEGHOULed - STAGE " + this.currentState.getStageNumber());

        this.progressLabel.setText("MATCHES: " + this.currentState.getCompletedMatchCount() + " / "
                + this.currentState.getTargetMatchCount());

        if (this.currentState.isWon()) {
            this.statusLabel.setText("BEGHOULed complete!");
            return;
        }

        if (this.currentState.isLost()) {
            this.statusLabel.setText("The zombies reached the house.");
            return;
        }

        if (!this.currentState.isPossibleMove() && this.currentState.isStarted()) {
            this.statusLabel.setText("No possible move. Board is resetting.");
            return;
        }

        this.statusLabel.setText(this.lastMessage);
    }

    private boolean canInteract() {
        return this.currentState != null && this.currentState.isStarted() && !this.currentState.isCompleted();
    }

    private boolean areAdjacent(Position first, Position second) {
        if (first == null || second == null)
            return false;

        int distance = Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY());

        return distance == 1;
    }

    private boolean samePosition(Position first, Position second) {
        return first != null && second != null && first.getX() == second.getX() && first.getY() == second.getY();
    }

    private record UpgradeCard(PlantUpgradeOption option, Group root, Table background, Label costLabel) {
    }
}
