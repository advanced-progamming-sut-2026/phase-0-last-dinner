package college.java.project.graphics.minigame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import college.java.project.Main;
import college.java.project.graphics.GameplayWorldLayout;
import college.java.project.graphics.GameplayWorldScene;
import controller.ZombotanyController;
import model.User.User;
import model.chapters.ChapterType;
import model.minigame.MiniGameType;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;

public final class ZombotanyScreen implements Screen {
    private static final float TICKS_PER_SECOND = 10f;
    private final ZombotanyTraitLayer traitLayer;
    private final Main application;
    private final ZombotanyMiniGame game;
    private final ZombotanyController controller;
    private final ZombotanyGameplayDataSource dataSource;
    private final GameplayWorldScene worldScene;
    private final Stage stage;
    private final Runnable onBack;
    private final int stageNumber;

    private float tickRemainder;
    private long currentTick;
    private boolean disposed;

    public ZombotanyScreen(Main application, ZombotanyMiniGame game, Runnable onBack) {
        this(application, game, 1, null, onBack);
    }

    public ZombotanyScreen(Main application, ZombotanyMiniGame game, int stageNumber, Runnable onBack) {
        this(application, game, stageNumber, null, onBack);
    }

    public ZombotanyScreen(Main application, ZombotanyMiniGame game, int stageNumber, ChapterType chapterType, Runnable onBack) {
        if (application == null || game == null)
            throw new IllegalArgumentException("Application and Zombotany game are required.");

        if (stageNumber < 1 || stageNumber > 3)
            throw new IllegalArgumentException("Zombotany stage must be between 1 and 3.");

        if (!game.isStarted())
            throw new IllegalStateException("Zombotany must be opened through " + "ZombotanyCoordinator.");

        this.application = application;
        this.game = game;
        this.stageNumber = stageNumber;
        this.onBack = onBack;
        this.controller = new ZombotanyController(game);

        User user = application.getApplicationController().getCurrentUser();

        if (user != null) user.initializeMissingFields();

        this.dataSource = new ZombotanyGameplayDataSource(game, user, () -> this.currentTick, chapterType);

        this.worldScene = new GameplayWorldScene(this.dataSource, this.dataSource);

        this.stage = new Stage(new FitViewport(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT));

        this.stage.addActor(this.worldScene);

        this.traitLayer = new ZombotanyTraitLayer(this.game, this.worldScene.getZombieLayer());

        this.traitLayer.setBounds(GameplayWorldLayout.LAWN_X, GameplayWorldLayout.LAWN_Y, GameplayWorldLayout.LAWN_WIDTH,
            GameplayWorldLayout.LAWN_HEIGHT);

        this.worldScene.addActorBefore(this.worldScene.getSeedBank(), this.traitLayer);

        configureWorldScene();
    }


    private void configureWorldScene() {
        this.worldScene.getConveyorBelt().remove();

        this.worldScene.setSessionActions(() -> queueSessionAction(this::restartGame), () -> queueSessionAction(this::exitScreen),
            () -> queueSessionAction(this::exitScreen), () -> queueSessionAction(this::restartGame));

        this.worldScene.setOutcomeAction(() -> this.application.recordMiniGameOutcome(this.game));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(Math.max(delta, 0f), 0.1f);

        if (this.worldScene.shouldAdvanceModel() && this.game.isStarted() && !this.game.isCompleted()) {
            advanceModel(safeDelta);
        }

        ScreenUtils.clear(new Color(0f, 0f, 0f, 1f));
        this.stage.act(safeDelta);
        this.stage.draw();
    }

    private void advanceModel(float delta) {
        this.tickRemainder += delta * TICKS_PER_SECOND;
        int ticks = (int) this.tickRemainder;

        if (ticks <= 0) return;

        this.tickRemainder -= ticks;
        this.controller.onAdvanceTicksRequested(ticks);
        this.currentTick += ticks;
    }

    private void restartGame() {
        this.application.getApplicationController().save();

        ZombotanyCoordinator.open(this.application, this.game, this.stageNumber, this.onBack);
    }

    private void exitScreen() {
        this.application.getApplicationController().save();

        if (this.onBack != null) this.onBack.run();
    }

    private void switchScreen(Screen nextScreen) {
        if (nextScreen == null) return;

        Screen previousScreen = this.application.getScreen();
        this.application.setScreen(nextScreen);

        if (previousScreen != null && previousScreen != nextScreen) {
            previousScreen.dispose();
        }
    }

    private void queueSessionAction(Runnable action) {
        if (action == null) return;

        if (Gdx.app == null) {
            action.run();
            return;
        }

        Gdx.app.postRunnable(action);
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        this.worldScene.openPauseMenu();

        this.application.getApplicationController().save();
    }

    @Override
    public void resume() {
        if (!this.disposed) Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void hide() {
        InputProcessor processor = Gdx.input.getInputProcessor();

        if (processor == this.stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (this.disposed) return;

        this.disposed = true;
        this.traitLayer.dispose();
        this.worldScene.dispose();
        this.stage.dispose();
    }
}
