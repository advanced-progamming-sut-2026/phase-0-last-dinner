package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.GameplayWorldLayout;
import network.izombie.client.IZombieClientCommandResult;
import network.izombie.client.IZombieClientController;
import network.izombie.client.IZombieClientGameData;
import network.izombie.client.IZombieClientMatchState;
import network.izombie.client.IZombieReceivedReaction;
import network.izombie.protocol.IZombieReaction;
import network.izombie.protocol.IZombieReactionCatalog;
import network.izombie.protocol.IZombieRole;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;

public final class IZombieMultiplayerLayer extends Group {

    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    private static final float PANEL_X = 170f;
    private static final float CARD_TOP = 910f;
    private static final float CARD_GAP = 8f;

    private static final Color PLACEMENT_COLOUR = new Color(0.16f, 0.72f, 0.20f, 0.20f);

    private static final Color RED_LINE_COLOUR = new Color(0.96f, 0.08f, 0.05f, 0.88f);

    private final IZombieClientController controller;
    private final IZombieClientMatchState state;
    private final IZombieClientGameData data;
    private final GameAssetManager assets;
    private final Skin skin;

    private final List<IZombieMultiplayerUnitCard> unitCards = new ArrayList<>();

    private final List<String> unitSignature = new ArrayList<>();

    private final Table[][] placementCells = new Table[ROW_COUNT][COLUMN_COUNT];

    private final Image[] brainActors = new Image[ROW_COUNT];

    private final boolean[] brainEaten = new boolean[ROW_COUNT];

    private final Texture brainTexture;
    private final Image redLine;

    private final Label stageLabel;
    private final Label roleLabel;
    private final Label sunLabel;
    private final Label timerLabel;
    private final Label statusLabel;
    private final Label reactionLabel;

    private final Group reactionPanel;

    private String selectedUnitKey;
    private String lastServerStatusMessage;

    public IZombieMultiplayerLayer(IZombieClientController controller, IZombieClientGameData data, GameAssetManager assets) {
        if (controller == null || data == null || assets == null) {
            throw new IllegalArgumentException("Multiplayer I, Zombie layer dependencies are required.");
        }

        this.controller = controller;
        this.state = controller.getState();
        this.data = data;
        this.assets = assets;
        this.skin = PvzSkin.get();

        redLine = new Image(skin.newDrawable("white_pixel", RED_LINE_COLOUR));

        stageLabel = new Label("I, ZOMBIE", skin, "medium_outline");

        roleLabel = new Label("", skin, "secondary");

        sunLabel = new Label("SUN 0", skin, "medium_outline");

        timerLabel = new Label("02:00", skin, "medium_outline");

        statusLabel = new Label("", skin, "medium_outline");

        reactionLabel = new Label("", skin, "medium_outline");

        reactionPanel = new Group();

        brainTexture = new Texture(Gdx.files.internal("ui/izombie_brain.png"));

        brainTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        setTouchable(Touchable.childrenOnly);

        createPlacementCells();
        createRedLine();
        createBrains();
        createLabels();
        createReactionPanel();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        assets.update();

        if (!data.hasSnapshot()) {
            refreshWaitingState();
            return;
        }

        syncUnitCards();
        refreshCards();
        refreshPlacementCells();
        refreshBrains();
        refreshLabels();
        refreshRedLine();
        refreshServerMessages();
        refreshReactionPanel();
    }

    private void createPlacementCells() {
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
                        placeSelectedUnit(targetColumn, targetRow);
                    }
                });

                placementCells[row][column] = cell;
                addActor(cell);
            }
        }
    }

    private void createRedLine() {
        redLine.setTouchable(Touchable.disabled);
        addActor(redLine);
    }

    private void createBrains() {
        for (int row = 0; row < ROW_COUNT; row++) {
            Image brain = new Image(brainTexture);

            brain.setScaling(Scaling.fit);
            brain.setTouchable(Touchable.disabled);

            float x = GameplayWorldLayout.LAWN_X - 96f;

            float y = GameplayWorldLayout.cellCenterY(row) - 43f;

            brain.setBounds(x, y, 90f, 86f);

            brainActors[row] = brain;
            addActor(brain);
        }
    }

    private void createLabels() {
        stageLabel.setAlignment(Align.center);
        stageLabel.setBounds(PANEL_X, 1034f, IZombieMultiplayerUnitCard.CARD_WIDTH, 38f);

        roleLabel.setAlignment(Align.center);
        roleLabel.setBounds(PANEL_X, 990f, IZombieMultiplayerUnitCard.CARD_WIDTH, 34f);

        sunLabel.setAlignment(Align.center);
        sunLabel.setBounds(PANEL_X, 930f, 140f, 48f);

        timerLabel.setAlignment(Align.center);
        timerLabel.setBounds(PANEL_X + 150f, 930f, 140f, 48f);

        statusLabel.setAlignment(Align.center);
        statusLabel.setColor(Color.WHITE);
        statusLabel.setTouchable(Touchable.disabled);
        statusLabel.setBounds(620f, 22f, 800f, 46f);

        reactionLabel.setAlignment(Align.center);
        reactionLabel.setTouchable(Touchable.disabled);
        reactionLabel.setBounds(710f, 945f, 620f, 54f);
        reactionLabel.getColor().a = 0f;

        addActor(stageLabel);
        addActor(roleLabel);
        addActor(sunLabel);
        addActor(timerLabel);
        addActor(statusLabel);
        addActor(reactionLabel);
    }

    private void createReactionPanel() {
        reactionPanel.setBounds(1650f, 540f, 250f, 430f);

        Label title = new Label("REACTIONS", skin, "medium_outline");

        title.setAlignment(Align.center);
        title.setBounds(0f, 370f, 250f, 42f);
        reactionPanel.addActor(title);

        List<IZombieReaction> reactions = IZombieReactionCatalog.getAll();

        for (int index = 0; index < reactions.size(); index++) {
            IZombieReaction reaction = reactions.get(index);

            TextButton button = new TextButton(reaction.displayValue(), skin, "brown");

            float y = 310f - index * 55f;

            button.setBounds(5f, y, 240f, 48f);

            button.addListener(new ClickListener() {

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    sendReaction(reaction.id());
                }
            });

            reactionPanel.addActor(button);
        }

        addActor(reactionPanel);
    }

    private void syncUnitCards() {
        List<String> availableUnits = data.getAvailableUnits();

        List<String> currentSignature = new ArrayList<>();

        currentSignature.add(String.valueOf(data.getRole()));

        currentSignature.addAll(availableUnits);

        if (unitSignature.equals(currentSignature)) {
            return;
        }

        for (IZombieMultiplayerUnitCard card : unitCards) {
            card.remove();
        }

        unitCards.clear();
        unitSignature.clear();
        unitSignature.addAll(currentSignature);
        selectedUnitKey = null;

        for (int index = 0; index < availableUnits.size(); index++) {
            String unitKey = availableUnits.get(index);

            IZombieMultiplayerUnitCard card = new IZombieMultiplayerUnitCard(unitKey, displayName(unitKey), data.getRole(), data, assets, this::selectUnit);

            float y = CARD_TOP - IZombieMultiplayerUnitCard.CARD_HEIGHT - index * (IZombieMultiplayerUnitCard.CARD_HEIGHT + CARD_GAP);

            card.setPosition(PANEL_X, y);

            unitCards.add(card);
            addActor(card);
        }
    }

    private void selectUnit(String unitKey) {
        if (!state.isInMatch()) {
            showStatus("The match is not running.");
            return;
        }

        if (!data.canAfford(unitKey)) {
            showStatus("Not enough sun for " + displayName(unitKey) + ".");
            return;
        }

        if (!data.isUnitReady(unitKey)) {
            showStatus(displayName(unitKey) + " is ready in " + cooldownText(data.getCooldownTicks(unitKey)) + ".");
            return;
        }

        if (unitKey.equals(selectedUnitKey)) {
            selectedUnitKey = null;
        } else {
            selectedUnitKey = unitKey;
        }

        refreshCards();
        refreshPlacementCells();
    }

    private void refreshCards() {
        for (IZombieMultiplayerUnitCard card : unitCards) {
            card.setSelected(card.getUnitKey().equals(selectedUnitKey));

            card.refresh();
        }
    }

    private void refreshPlacementCells() {
        boolean selecting = selectedUnitKey != null && data.canUseUnit(selectedUnitKey) && state.isInMatch();

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Table cell = placementCells[row][column];

                boolean available = selecting && data.canPlaceAt(column, row);

                cell.setTouchable(available ? Touchable.enabled : Touchable.disabled);

                cell.setBackground(available ? skin.newDrawable("white_pixel", PLACEMENT_COLOUR) : null);
            }
        }
    }

    private void placeSelectedUnit(int column, int row) {
        if (selectedUnitKey == null) {
            return;
        }

        if (!data.canPlaceAt(column, row)) {
            showStatus("You cannot place a unit on this side.");
            return;
        }

        IZombieClientCommandResult result = controller.placeUnit(selectedUnitKey, column, row);

        if (!result.sent()) {
            showStatus(result.message());
            return;
        }

        showStatus("Placement request sent.");

        selectedUnitKey = null;
        refreshCards();
        refreshPlacementCells();
    }

    private void sendReaction(String reactionId) {
        IZombieClientCommandResult result = controller.sendReaction(reactionId);

        if (!result.sent()) {
            showStatus(result.message());
        }
    }

    private void refreshBrains() {
        for (int row = 0; row < ROW_COUNT; row++) {
            boolean eaten = data.isBrainEaten(row);

            if (eaten == brainEaten[row]) {
                continue;
            }

            Image brain = brainActors[row];
            brain.clearActions();

            if (eaten) {
                brain.addAction(Actions.sequence(Actions.parallel(Actions.fadeOut(0.25f), Actions.scaleTo(0.65f, 0.65f, 0.25f)), Actions.visible(false)));
            } else {
                brain.setVisible(true);
                brain.setScale(0.65f);
                brain.getColor().a = 0f;

                brain.addAction(Actions.parallel(Actions.fadeIn(0.25f), Actions.scaleTo(1f, 1f, 0.25f)));
            }

            brainEaten[row] = eaten;
        }
    }

    private void refreshLabels() {
        stageLabel.setText("I, ZOMBIE  " + data.getStageNumber() + "/3");

        IZombieRole role = data.getRole();

        String roleText = role == IZombieRole.PLANTS ? "PLANTS" : "ZOMBIES";

        String opponent = state.getOpponentUsername();

        roleLabel.setText(roleText + (opponent == null ? "" : "  VS  " + opponent));

        sunLabel.setText("SUN " + data.getSunAmount());

        timerLabel.setText(formatTime(data.getRemainingSeconds()));
    }

    private void refreshRedLine() {
        float x = GameplayWorldLayout.LAWN_X + data.getRedLineColumn() * GameplayWorldLayout.cellWidth();

        redLine.setBounds(x - 5f, GameplayWorldLayout.LAWN_Y, 10f, GameplayWorldLayout.LAWN_HEIGHT);
    }

    private void refreshServerMessages() {
        String error = state.consumeErrorMessage();

        if (error != null && !error.isBlank()) {
            showStatus(error);
        }

        String serverMessage = state.getStatusMessage();

        if (serverMessage != null && !serverMessage.isBlank() && !serverMessage.equals(lastServerStatusMessage)) {
            lastServerStatusMessage = serverMessage;
            showStatus(serverMessage);
        }

        IZombieReceivedReaction received = state.consumeReceivedReaction();

        if (received != null) {
            showReaction(received);
        }
    }

    private void refreshReactionPanel() {
        boolean enabled = state.isInMatch();

        reactionPanel.setVisible(enabled);
        reactionPanel.setTouchable(enabled ? Touchable.childrenOnly : Touchable.disabled);
    }

    private void refreshWaitingState() {
        stageLabel.setText("I, ZOMBIE");
        roleLabel.setText("WAITING FOR SERVER");
        sunLabel.setText("SUN 0");
        timerLabel.setText("--:--");

        reactionPanel.setVisible(false);

        String error = state.consumeErrorMessage();

        if (error != null && !error.isBlank()) {
            showStatus(error);
        }
    }

    private void showReaction(IZombieReceivedReaction received) {
        IZombieReaction reaction = received.reaction();

        if (reaction == null) {
            return;
        }

        String sender = received.senderUsername();

        reactionLabel.setText((sender == null ? "Opponent" : sender) + ": " + reaction.displayValue());

        reactionLabel.clearActions();
        reactionLabel.setScale(0.75f);
        reactionLabel.getColor().a = 0f;

        reactionLabel.addAction(Actions.sequence(Actions.parallel(Actions.fadeIn(0.15f), Actions.scaleTo(1.10f, 1.10f, 0.15f)), Actions.scaleTo(1f, 1f, 0.10f), Actions.delay(2.1f), Actions.fadeOut(0.25f)));
    }

    private void showStatus(String message) {
        statusLabel.setText(message == null ? "" : message);

        statusLabel.clearActions();
        statusLabel.getColor().a = 1f;

        statusLabel.addAction(Actions.sequence(Actions.delay(2.2f), Actions.fadeOut(0.25f)));
    }

    private String displayName(String unitKey) {
        if (unitKey == null || unitKey.isBlank()) {
            return "Unit";
        }

        String displayName = unitKey.replaceAll("([a-z])([A-Z])", "$1 $2");

        displayName = displayName.replace("Default", "").replace("Zombie ", "").trim();

        return displayName.isBlank() ? unitKey : displayName;
    }

    private String cooldownText(int ticks) {
        int ticksPerSecond = Math.max(1, data.getTicksPerSecond());

        int safeTicks = Math.max(0, ticks);
        int wholeSeconds = safeTicks / ticksPerSecond;

        int tenths = (safeTicks % ticksPerSecond) * 10 / ticksPerSecond;

        return wholeSeconds + "." + tenths + "s";
    }

    private String formatTime(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public void dispose() {
        brainTexture.dispose();
    }
}
