package college.java.project.graphics.minigame;

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
import controller.IZombieController;
import model.User.User;
import model.minigame.MiniGameType;
import model.minigame.izombieminigame.IZombieMiniGame;

public final class IZombieScreen implements Screen {
    private static final float TICKS_PER_SECOND = 10f;

    private final Main application;
    private final IZombieMiniGame game;
    private final IZombieController controller;
    private final MiniGameGameplayDataSource dataSource;
    private final GameplayWorldScene worldScene;
    private final IZombieLayer iZombieLayer;
    private final Stage stage;
    private final Runnable onBack;
    private float tickRemainder;
    private long currentTick;
    private boolean disposed;

    public IZombieScreen(Main application, IZombieMiniGame game, Runnable onBack) {
        if (application == null || game == null)
            throw new IllegalArgumentException("Application and I Zombie game are required.");

        this.application = application;
        this.game = game;
        this.onBack = onBack;
        this.controller = new IZombieController(game);
        this.controller.onStartIZombieRequested();

        User user = application.getApplicationController().getCurrentUser();
        if (user != null)
            user.initializeMissingFields();

        this.dataSource = new MiniGameGameplayDataSource(game, user, () -> this.currentTick);
        this.worldScene = new GameplayWorldScene(this.dataSource, this.dataSource);
        this.stage = new Stage(new FitViewport(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT));
        this.stage.addActor(this.worldScene);

        this.iZombieLayer = new IZombieLayer(this.controller, this.worldScene.getAssets());
        this.iZombieLayer.setBounds(0f, 0f, GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        this.worldScene.addActorBefore(this.worldScene.getInteractionLayer(), this.iZombieLayer);
        configureWorldScene();
    }

    private void configureWorldScene() {
        this.worldScene.getSeedBank().remove();
        this.worldScene.getConveyorBelt().remove();
        this.worldScene.getSunLayer().remove();
        this.worldScene.getWaveProgressBar().remove();
        this.worldScene.getResourceStrip().remove();
        this.worldScene.getInteractionHud().setVisible(false);
        this.worldScene.getInteractionLayer().setTouchable(Touchable.disabled);

        this.worldScene.setSessionActions(
            () -> queueSessionAction(this::restartGame),
            () -> queueSessionAction(this::exitScreen),
            () -> queueSessionAction(this::exitScreen),
            () -> queueSessionAction(this::restartGame)
        );

        this.worldScene.setOutcomeAction(() -> this.application.getApplicationController().save());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(Math.max(delta, 0f), 0.1f);

        if (this.worldScene.shouldAdvanceModel() && this.game.isStarted() && !this.game.isCompleted())
            advanceModel(safeDelta);

        ScreenUtils.clear(new Color(0f, 0f, 0f, 1f));
        this.stage.act(safeDelta);
        this.stage.draw();
    }

    private void advanceModel(float delta) {
        this.tickRemainder += delta * TICKS_PER_SECOND;
        int ticks = (int) this.tickRemainder;

        if (ticks <= 0)
            return;

        this.tickRemainder -= ticks;
        this.controller.onAdvanceTicksRequested(ticks);
        this.currentTick += ticks;
    }

    private void restartGame() {
        this.application.getApplicationController().save();

        IZombieMiniGame restartedGame = (IZombieMiniGame) this.application.createMiniGameFactory().create(MiniGameType.I_ZOMBIE);

        restartedGame.restoreHighestUnlockedStage(this.game.getHighestUnlockedStage());
        switchScreen(new IZombieScreen(this.application, restartedGame, this.onBack));
    }

    private void exitScreen() {
        this.application.getApplicationController().save();

        if (this.onBack != null)
            this.onBack.run();
    }

    private void switchScreen(Screen nextScreen) {
        if (nextScreen == null)
            return;

        Screen previousScreen = this.application.getScreen();
        this.application.setScreen(nextScreen);

        if (previousScreen != null && previousScreen != nextScreen)
            previousScreen.dispose();
    }

    private void queueSessionAction(Runnable action) {
        if (action == null)
            return;

        if (Gdx.app == null) {
            action.run();
            return;
        }

        Gdx.app.postRunnable(action);
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0)
            this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        this.worldScene.openPauseMenu();
        this.application.getApplicationController().save();
    }

    @Override
    public void resume() {
        if (!this.disposed)
            Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void hide() {
        InputProcessor processor = Gdx.input.getInputProcessor();

        if (processor == this.stage)
            Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (this.disposed)
            return;

        this.disposed = true;
        this.iZombieLayer.dispose();
        this.worldScene.dispose();
        this.stage.dispose();
    }
}
