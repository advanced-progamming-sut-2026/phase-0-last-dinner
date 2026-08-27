package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import college.java.project.Main;
import college.java.project.graphics.GameplayWorldLayout;
import network.izombie.client.IZombieClientCommandResult;
import network.izombie.client.IZombieClientController;
import network.izombie.client.IZombieClientMatchState;
import network.izombie.client.IZombieClientPhase;
import pvz.skin.PvzSkin;

public final class IZombieMatchmakingScreen implements Screen {

    private final Main application;
    private final IZombieClientController controller;
    private final IZombieClientMatchState state;
    private final Runnable onBack;

    private final Stage stage;
    private final Skin skin;

    private final TextField usernameField;
    private final TextButton inviteButton;
    private final TextButton randomButton;
    private final TextButton[] stageButtons = new TextButton[3];

    private final Label connectionLabel;
    private final Label statusLabel;

    private final Group invitationOverlay;
    private final Label invitationMessage;

    private int selectedStage = 1;
    private String displayedInvitationId;
    private String openedMatchId;
    private String lastStatusMessage;
    private boolean disposed;

    public IZombieMatchmakingScreen(Main application, IZombieClientController controller, Runnable onBack) {
        if (application == null || controller == null) {
            throw new IllegalArgumentException("Application and I, Zombie client controller are required.");
        }

        this.application = application;
        this.controller = controller;
        this.state = controller.getState();
        this.onBack = onBack;
        this.skin = PvzSkin.get();

        stage = new Stage(new FitViewport(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT));

        usernameField = new TextField("", skin, "default");

        usernameField.setMessageText("Opponent username");

        inviteButton = new TextButton("SEND INVITATION", skin, "green");

        randomButton = new TextButton("FIND RANDOM PLAYER", skin, "purple");

        connectionLabel = new Label("", skin, "secondary");

        statusLabel = new Label("", skin, "secondary");

        invitationMessage = new Label("", skin, "medium_outline");

        invitationOverlay = createInvitationOverlay();

        createMainUi();
        refreshStageButtons();
    }

    private void createMainUi() {
        Table root = new Table();
        root.setFillParent(true);

        root.setBackground(skin.newDrawable("white_pixel", new Color(0.05f, 0.12f, 0.07f, 1f)));

        Table panel = new Table();

        panel.setBackground(skin.newDrawable("white_pixel", new Color(0.16f, 0.12f, 0.08f, 0.97f)));

        panel.pad(44f);

        Label title = new Label("I, ZOMBIE MULTIPLAYER", skin, "medium_outline");

        title.setAlignment(Align.center);

        Label description = new Label("Challenge a specific player or find a random opponent.", skin, "secondary");

        description.setAlignment(Align.center);
        description.setWrap(true);

        connectionLabel.setAlignment(Align.center);

        panel.add(title).width(760f).height(62f).colspan(3);

        panel.row();

        panel.add(description).width(760f).height(56f).padBottom(18f).colspan(3);

        panel.row();

        panel.add(connectionLabel).width(760f).height(38f).padBottom(20f).colspan(3);

        panel.row();

        Label stageTitle = new Label("SELECT STAGE", skin, "secondary");

        stageTitle.setAlignment(Align.center);

        panel.add(stageTitle).width(760f).height(36f).padBottom(10f).colspan(3);

        panel.row();

        for (int index = 0; index < 3; index++) {
            int stageNumber = index + 1;

            TextButton button = new TextButton("STAGE " + stageNumber, skin, "brown");

            button.addListener(new ClickListener() {

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectStage(stageNumber);
                }
            });

            stageButtons[index] = button;

            panel.add(button).width(238f).height(56f).pad(6f);
        }

        panel.row();

        panel.add(usernameField).width(510f).height(58f).padTop(26f).padRight(12f).colspan(2);

        panel.add(inviteButton).width(238f).height(58f).padTop(26f);

        panel.row();

        panel.add(randomButton).width(760f).height(64f).padTop(18f).colspan(3);

        panel.row();

        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        panel.add(statusLabel).width(760f).height(70f).padTop(16f).colspan(3);

        panel.row();

        TextButton backButton = new TextButton("BACK", skin, "brown");

        panel.add(backButton).width(240f).height(56f).padTop(10f).colspan(3);

        inviteButton.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendInvitation();
            }
        });

        randomButton.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleRandomQueue();
            }
        });

        backButton.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                goBack();
            }
        });

        root.add(panel).width(860f).height(820f);

        stage.addActor(root);
        stage.addActor(invitationOverlay);
    }

    private Group createInvitationOverlay() {
        Group overlay = new Group();

        overlay.setBounds(0f, 0f, GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);

        Image darkness = new Image(skin.newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.72f)));

        darkness.setBounds(0f, 0f, GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);

        darkness.setTouchable(Touchable.enabled);
        overlay.addActor(darkness);

        Table popup = new Table();

        popup.setBackground(skin.newDrawable("white_pixel", new Color(0.20f, 0.13f, 0.08f, 0.99f)));

        popup.setBounds(560f, 350f, 800f, 380f);
        popup.pad(38f);

        Label title = new Label("MATCH INVITATION", skin, "medium_outline");

        title.setAlignment(Align.center);

        invitationMessage.setAlignment(Align.center);
        invitationMessage.setWrap(true);

        TextButton acceptButton = new TextButton("ACCEPT", skin, "green");

        TextButton rejectButton = new TextButton("REJECT", skin, "brown");

        popup.add(title).width(700f).height(60f).colspan(2);

        popup.row();

        popup.add(invitationMessage).width(700f).height(130f).padTop(16f).padBottom(22f).colspan(2);

        popup.row();

        popup.add(acceptButton).width(310f).height(60f).padRight(15f);

        popup.add(rejectButton).width(310f).height(60f).padLeft(15f);

        acceptButton.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                respondToInvitation(true);
            }
        });

        rejectButton.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                respondToInvitation(false);
            }
        });

        overlay.addActor(popup);
        overlay.setVisible(false);

        return overlay;
    }

    private void selectStage(int stageNumber) {
        if (state.getPhase() == IZombieClientPhase.SEARCHING_RANDOM_MATCH) {
            showStatus("Cancel random matchmaking before changing the stage.");
            return;
        }

        selectedStage = Math.max(1, Math.min(3, stageNumber));

        refreshStageButtons();
    }

    private void refreshStageButtons() {
        for (int index = 0; index < 3; index++) {
            boolean selected = selectedStage == index + 1;

            stageButtons[index].setStyle(skin.get(selected ? "green" : "brown", TextButton.TextButtonStyle.class));
        }
    }

    private void sendInvitation() {
        if (state.getPhase() == IZombieClientPhase.SEARCHING_RANDOM_MATCH) {
            showStatus("Cancel random matchmaking before sending an invitation.");
            return;
        }

        IZombieClientCommandResult result = controller.invitePlayer(usernameField.getText(), selectedStage);

        showCommandResult(result, "Invitation sent.");
    }

    private void toggleRandomQueue() {
        IZombieClientCommandResult result;

        if (state.getPhase() == IZombieClientPhase.SEARCHING_RANDOM_MATCH) {
            result = controller.leaveRandomQueue();
        } else {
            result = controller.joinRandomQueue(selectedStage);
        }

        showCommandResult(result, state.getPhase() == IZombieClientPhase.SEARCHING_RANDOM_MATCH
            ? "Leaving random queue..." : "Joining random queue...");
    }

    private void respondToInvitation(boolean accepted) {
        IZombieClientCommandResult result = controller.respondToPendingInvitation(accepted);

        if (!result.sent()) {
            showStatus(result.message());
            displayedInvitationId = null;
            return;
        }

        invitationOverlay.setVisible(false);
        displayedInvitationId = null;

        showStatus(accepted ? "Invitation accepted." : "Invitation rejected.");
    }

    private void refreshClientState() {
        connectionLabel.setText(controller.isConnected() ? "SERVER: CONNECTED" : "SERVER: DISCONNECTED");

        connectionLabel.setColor(controller.isConnected() ? Color.GREEN : Color.SCARLET);

        IZombieClientPhase phase = state.getPhase();

        boolean searching = phase == IZombieClientPhase.SEARCHING_RANDOM_MATCH;

        randomButton.setText(searching ? "CANCEL RANDOM SEARCH" : "FIND RANDOM PLAYER");

        randomButton.setStyle(skin.get(searching ? "brown" : "purple", TextButton.TextButtonStyle.class));

        usernameField.setDisabled(searching);
        inviteButton.setDisabled(searching);

        for (TextButton stageButton : stageButtons) {
            stageButton.setDisabled(searching);
        }

        showInvitationIfNeeded();
        refreshMessages();
        openMatchIfReady();
        resetCompletedMatchIfNeeded();
    }

    private void showInvitationIfNeeded() {
        if (state.getPhase() != IZombieClientPhase.INVITATION_RECEIVED) {
            invitationOverlay.setVisible(false);
            return;
        }

        String invitationId = state.getPendingInvitationId();

        if (invitationId == null || invitationId.equals(displayedInvitationId)) {
            return;
        }

        displayedInvitationId = invitationId;

        String challenger = state.getPendingChallengerUsername();

        invitationMessage.setText((challenger == null ? "A player" : challenger) + " invited you to Stage " + state.getPendingInvitationStage() + ".");

        invitationOverlay.setVisible(true);
        invitationOverlay.toFront();
    }

    private void refreshMessages() {
        String error = state.consumeErrorMessage();

        if (error != null && !error.isBlank()) {
            showStatus(error);
        }

        String message = state.getStatusMessage();

        if (message != null && !message.isBlank() && !message.equals(lastStatusMessage)) {
            lastStatusMessage = message;
            showStatus(message);
        }
    }

    private void openMatchIfReady() {
        if (state.getPhase() != IZombieClientPhase.IN_MATCH) {
            return;
        }

        String matchId = state.getMatchId();

        if (matchId == null || matchId.equals(openedMatchId)) {
            return;
        }

        openedMatchId = matchId;

        switchScreen(new IZombieMultiplayerScreen(application, controller, this::returnFromMatch));
    }

    private void returnFromMatch() {
        if (state.getPhase() == IZombieClientPhase.MATCH_FINISHED) {
            controller.resetFinishedMatch();
        }

        switchScreen(new IZombieMatchmakingScreen(application, controller, onBack));
    }

    private void resetCompletedMatchIfNeeded() {
        if (state.getPhase() != IZombieClientPhase.MATCH_FINISHED) {
            return;
        }

        String message = state.getStatusMessage();

        controller.resetFinishedMatch();

        if (message != null && !message.isBlank()) {
            showStatus(message);
        }
    }

    private void showCommandResult(IZombieClientCommandResult result, String successMessage) {
        if (result == null) {
            showStatus("Could not perform the request.");
            return;
        }

        showStatus(result.sent() ? successMessage : result.message());
    }

    private void showStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    private void goBack() {
        if (state.getPhase() == IZombieClientPhase.SEARCHING_RANDOM_MATCH) {
            controller.leaveRandomQueue();
        }

        application.getApplicationController().save();

        if (onBack != null) {
            onBack.run();
        }
    }

    private void switchScreen(Screen nextScreen) {
        if (nextScreen == null) {
            return;
        }

        Screen previousScreen = application.getScreen();

        application.setScreen(nextScreen);

        if (previousScreen != null && previousScreen != nextScreen) {
            previousScreen.dispose();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.setKeyboardFocus(usernameField);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(Math.max(delta, 0f), 0.1f);

        refreshClientState();

        ScreenUtils.clear(new Color(0.03f, 0.07f, 0.04f, 1f));

        stage.act(safeDelta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
        if (!disposed) {
            Gdx.input.setInputProcessor(stage);
        }
    }

    @Override
    public void hide() {
        InputProcessor processor = Gdx.input.getInputProcessor();

        if (processor == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (disposed)
            return;

        disposed = true;
        stage.dispose();
    }
}
