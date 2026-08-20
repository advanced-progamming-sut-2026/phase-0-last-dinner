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
import controller.WallnutBowlingController;
import model.User.User;
import model.minigame.MiniGameType;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;

public final class WallnutBowlingScreen implements Screen {
    private static final float TICKS_PER_SECOND = 10f;

    private final Main application;
    private final WallnutBowlingMiniGame game;
    private final WallnutBowlingController controller;
    private final MiniGameGameplayDataSource dataSource;
    private final GameplayWorldScene worldScene;
    private final WallnutBowlingLayer bowlingLayer;
    private final Stage stage;
    private final int stageNumber;
    private final Runnable onBack;

    private float tickRemainder;
    private boolean disposed;

    public WallnutBowlingScreen(
        Main application,
        WallnutBowlingMiniGame game,
        int stageNumber,
        Runnable onBack
    ) {
        if (application == null || game == null) {
            throw new IllegalArgumentException(
                "Application and Wallnut Bowling game are required."
            );
        }

        this.application = application;
        this.game = game;
        this.stageNumber = Math.max(1, Math.min(3, stageNumber));
        this.onBack = onBack;
        this.controller = new WallnutBowlingController(game);

        this.controller.onStartWallnutBowlingRequested(
            this.stageNumber
        );

        User user = application
            .getApplicationController()
            .getCurrentUser();

        if (user != null) {
            user.initializeMissingFields();
        }

        this.dataSource = new MiniGameGameplayDataSource(
            game,
            user,
            game::getCurrentTick
        );

        this.worldScene = new GameplayWorldScene(
            this.dataSource,
            this.dataSource
        );

        this.stage = new Stage(new FitViewport(
            GameplayWorldLayout.STAGE_WIDTH,
            GameplayWorldLayout.STAGE_HEIGHT
        ));

        this.stage.addActor(this.worldScene);

        this.bowlingLayer = new WallnutBowlingLayer(
            this.controller,
            this.worldScene.getAssets()
        );

        this.bowlingLayer.setBounds(
            0f,
            0f,
            GameplayWorldLayout.STAGE_WIDTH,
            GameplayWorldLayout.STAGE_HEIGHT
        );

        this.worldScene.addActorBefore(
            this.worldScene.getInteractionLayer(),
            this.bowlingLayer
        );

        configureWorldScene();
    }

    private void configureWorldScene() {
        this.worldScene.getSeedBank().remove();
        this.worldScene.getConveyorBelt().remove();
        this.worldScene.getSunLayer().remove();
        this.worldScene.getWaveProgressBar().remove();

        this.worldScene
            .getInteractionHud()
            .setVisible(false);

        this.worldScene
            .getInteractionLayer()
            .setTouchable(Touchable.disabled);

        this.worldScene.setSessionActions(
            () -> queueSessionAction(this::restartStage),
            () -> queueSessionAction(this::exitScreen),
            () -> queueSessionAction(this::exitScreen),
            () -> queueSessionAction(this::restartStage)
        );

        this.worldScene.setOutcomeAction(
            () -> this.application
                .getApplicationController()
                .save()
        );
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(
            Math.max(delta, 0f),
            0.1f
        );

        if (this.worldScene.shouldAdvanceModel()
            && this.game.isStarted()
            && !this.game.isCompleted()
            && !this.game.isLoseConditionMet()) {
            advanceModel(safeDelta);
        }

        ScreenUtils.clear(new Color(
            0f,
            0f,
            0f,
            1f
        ));

        this.stage.act(safeDelta);
        this.stage.draw();
    }

    private void advanceModel(float delta) {
        this.tickRemainder += delta * TICKS_PER_SECOND;

        int ticks = (int) this.tickRemainder;

        if (ticks <= 0) {
            return;
        }

        this.tickRemainder -= ticks;
        this.controller.onAdvanceTicksRequested(ticks);
    }

    private void restartStage() {
        this.application
            .getApplicationController()
            .save();

        WallnutBowlingMiniGame restartedGame =
            (WallnutBowlingMiniGame) this.application
                .createMiniGameFactory()
                .create(MiniGameType.WALLNUT_BOWLING);

        restartedGame.restoreHighestUnlockedStage(
            this.game.getHighestUnlockedStage()
        );

        switchScreen(new WallnutBowlingScreen(
            this.application,
            restartedGame,
            this.stageNumber,
            this.onBack
        ));
    }

    private void exitScreen() {
        this.application
            .getApplicationController()
            .save();

        if (this.onBack != null) {
            this.onBack.run();
        }
    }

    private void switchScreen(Screen nextScreen) {
        if (nextScreen == null) {
            return;
        }

        Screen previousScreen = this.application.getScreen();
        this.application.setScreen(nextScreen);

        if (previousScreen != null
            && previousScreen != nextScreen) {
            previousScreen.dispose();
        }
    }

    private void queueSessionAction(Runnable action) {
        if (action == null) {
            return;
        }

        if (Gdx.app == null) {
            action.run();
            return;
        }

        Gdx.app.postRunnable(action);
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            this.stage
                .getViewport()
                .update(width, height, true);
        }
    }

    @Override
    public void pause() {
        this.worldScene.openPauseMenu();

        this.application
            .getApplicationController()
            .save();
    }

    @Override
    public void resume() {
        if (!this.disposed) {
            Gdx.input.setInputProcessor(this.stage);
        }
    }

    @Override
    public void hide() {
        InputProcessor processor =
            Gdx.input.getInputProcessor();

        if (processor == this.stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (this.disposed) {
            return;
        }

        this.disposed = true;
        this.worldScene.dispose();
        this.stage.dispose();
    }
}
