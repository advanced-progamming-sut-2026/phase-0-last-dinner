package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import college.java.project.Main;
import college.java.project.graphics.GameplayWorldLayout;
import college.java.project.graphics.GameplayWorldScene;
import network.izombie.client.IZombieClientCommandResult;
import network.izombie.client.IZombieClientController;
import network.izombie.client.IZombieClientGameData;
import network.izombie.client.IZombieClientMatchState;

public final class IZombieMultiplayerScreen implements Screen {

    private final Main application;
    private final IZombieClientController controller;
    private final IZombieClientMatchState state;
    private final IZombieClientGameData gameData;

    private final IZombieMultiplayerWorldDataSource worldDataSource;
    private final GameplayWorldScene worldScene;
    private final IZombieMultiplayerEntityLayer entityLayer;
    private final IZombieMultiplayerLayer hudLayer;
    private final Stage stage;
    private final Runnable onBack;

    private boolean leaveRequestSent;
    private boolean disposed;

    public IZombieMultiplayerScreen(Main application, IZombieClientController controller, Runnable onBack) {
        if (application == null || controller == null) {
            throw new IllegalArgumentException("Application and multiplayer controller are required.");
        }

        this.application = application;
        this.controller = controller;
        this.state = controller.getState();
        this.onBack = onBack;

        if (!state.isInMatch() && state.getSnapshot() == null) {
            throw new IllegalStateException("A multiplayer match must be started before opening its screen.");
        }

        gameData = new IZombieClientGameData(state);

        worldDataSource = new IZombieMultiplayerWorldDataSource(gameData);

        worldScene = new GameplayWorldScene(worldDataSource, worldDataSource);

        entityLayer = new IZombieMultiplayerEntityLayer(gameData, worldScene.getAssets());

        hudLayer = new IZombieMultiplayerLayer(controller, gameData, worldScene.getAssets());

        entityLayer.setBounds(0f, 0f, GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);

        hudLayer.setBounds(0f, 0f, GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);

        stage = new Stage(new FitViewport(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT));

        stage.addActor(worldScene);

        configureWorldScene();
        insertMultiplayerLayers();
    }

    private void configureWorldScene() {
        worldScene.getSeedBank().remove();
        worldScene.getConveyorBelt().remove();
        worldScene.getSunLayer().remove();
        worldScene.getWaveProgressBar().remove();
        worldScene.getResourceStrip().remove();
        worldScene.getMowerLayer().remove();

        worldScene.getPlantLayer().remove();
        worldScene.getZombieLayer().remove();
        worldScene.getProjectileLayer().remove();
        worldScene.getBoardEntityLayer().remove();

        worldScene.getInteractionHud().setVisible(false);

        worldScene.getInteractionLayer().setTouchable(Touchable.disabled);

        worldScene.getInteractionLayer().remove();

        worldScene.setSessionActions(this::leaveAndExit, this::leaveAndExit, this::leaveAndExit, this::exitScreen);
    }

    private void insertMultiplayerLayers() {
        worldScene.addActorAfter(worldScene.getLevelRulesLayer(), entityLayer);

        worldScene.addActorAfter(entityLayer, hudLayer);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(Math.max(delta, 0f), 0.1f);

        ScreenUtils.clear(new Color(0f, 0f, 0f, 1f));

        stage.act(safeDelta);
        stage.draw();
    }

    private void leaveAndExit() {
        sendLeaveRequestIfNeeded();
        exitScreen();
    }

    private void sendLeaveRequestIfNeeded() {
        if (leaveRequestSent || !state.isInMatch()) {
            return;
        }

        IZombieClientCommandResult result = controller.leaveMatch();

        if (result.sent()) {
            leaveRequestSent = true;
        }
    }

    private void exitScreen() {
        application.getApplicationController().save();

        if (onBack != null) {
            onBack.run();
        }
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

        sendLeaveRequestIfNeeded();

        hudLayer.dispose();
        entityLayer.clearEntities();
        worldScene.dispose();
        stage.dispose();
    }
}
