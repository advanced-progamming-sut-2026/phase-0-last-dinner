package college.java.project.graphics.minigame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import controller.VasebreakerController;
import lombok.Getter;
import model.User.User;
import model.minigame.vasebreakerminigame.PlantZombieVasebreakerIntegration;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerActionStatus;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;

@Getter
public final class VasebreakerScreen implements Screen {
    private static final float TICKS_PER_SECOND = 10f;

    private final Main application;
    private final Runnable onBack;
    private final int stageNumber;
    private final VasebreakerMiniGame game;
    private final VasebreakerController controller;
    private final MiniGameGameplayDataSource dataSource;
    private final GameplayWorldScene worldScene;
    private final Stage stage;

    private float tickRemainder;
    private boolean disposed;

    public VasebreakerScreen(Main application, int stageNumber, Runnable onBack) {
        this(application, createGame(application), stageNumber, onBack);
    }

    public VasebreakerScreen(Main application, VasebreakerMiniGame game, int stageNumber, Runnable onBack) {
        if (application == null || game == null || onBack == null)
            throw new IllegalArgumentException("Application, game and back action are required");

        this.application = application;
        this.game = game;
        this.stageNumber = stageNumber;
        this.onBack = onBack;
        this.controller = new VasebreakerController(game);

        VasebreakerActionResult startResult = this.controller.onStartVasebreakerRequested(stageNumber);
        if (startResult.getStatus() != VasebreakerActionStatus.STARTED)
            throw new IllegalStateException("Could not start Vasebreaker stage: " + startResult.getStatus());

        User user = application.getApplicationController().getCurrentUser();
        this.dataSource = new MiniGameGameplayDataSource(game, user, game::getCurrentTick);
        this.worldScene = new GameplayWorldScene(this.dataSource, this.dataSource);
        this.stage = new Stage(new FitViewport(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT));
        this.stage.addActor(this.worldScene);

        VaseLayer vaseLayer = new VaseLayer(this.controller, this.worldScene.getAssets());
        vaseLayer.setBounds(GameplayWorldLayout.LAWN_X, GameplayWorldLayout.LAWN_Y, GameplayWorldLayout.LAWN_WIDTH,
            GameplayWorldLayout.LAWN_HEIGHT);
        this.worldScene.addActorBefore(this.worldScene.getInteractionLayer(), vaseLayer);

        VasebreakerSeedPacketLayer seedPacketLayer = new VasebreakerSeedPacketLayer(this.controller, this.worldScene.getAssets());
        seedPacketLayer.setBounds(0f, 0f, GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        this.worldScene.addActorBefore(this.worldScene.getInteractionLayer(), seedPacketLayer);

        configureWorldScene();
    }

    private static VasebreakerMiniGame createGame(Main application) {
        if (application == null)
            throw new IllegalArgumentException("Application is required");

        PlantZombieVasebreakerIntegration integration = new PlantZombieVasebreakerIntegration(
            application.getPlantDefinitions(),
            application.getZombieDefinitions(),
            application.getZombieFactory()
        );

        return new VasebreakerMiniGame(integration);
    }

    private void configureWorldScene() {
        this.worldScene.getSeedBank().remove();
        this.worldScene.getConveyorBelt().remove();
        this.worldScene.getSunLayer().remove();
        this.worldScene.getMowerLayer().remove();

        this.worldScene.getWaveProgressBar().setVisible(false);
        this.worldScene.getInteractionHud().setVisible(false);
        this.worldScene.getInteractionLayer().setTouchable(Touchable.disabled);

        this.worldScene.setSessionActions(this::restartStage, this::exitScreen, this::exitScreen, this::restartStage);

        this.worldScene.setOutcomeAction(() -> this.application.recordMiniGameOutcome(this.game));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(Math.max(delta, 0f), 0.1f);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (this.worldScene.isPaused())
                this.worldScene.resumeGame();
            else
                this.worldScene.openPauseMenu();
        }

        if (this.worldScene.shouldAdvanceModel() && this.game.isStarted()
            && !this.game.isWinConditionMet() && !this.game.isLoseConditionMet())
            advanceModel(safeDelta);

        ScreenUtils.clear(Color.BLACK);
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
    }

    private void restartStage() {
        Gdx.app.postRunnable(() -> {
            Screen previousScreen = this.application.getScreen();
            this.application.setScreen(new VasebreakerScreen(
                this.application,
                this.game,
                this.stageNumber,
                this.onBack
            ));

            if (previousScreen != null)
                previousScreen.dispose();
        });
    }

    private void exitScreen() {
        this.application.getApplicationController().save();
        Gdx.app.postRunnable(this.onBack);
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
        this.worldScene.dispose();
        this.stage.dispose();
    }
}
