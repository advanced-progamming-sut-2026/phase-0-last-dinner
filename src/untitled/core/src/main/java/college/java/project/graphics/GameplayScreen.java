package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import college.java.project.Main;
import controller.ApplicationController;
import controller.CollectionController;
import controller.MidGameController;
import controller.PlantPickController;
import model.Menu.MenuType;
import model.User.User;
import model.chapters.ChapterType;
import model.level.Level;
import model.level.LevelType;
import model.mechanism.PlantZombieGame;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GameplayScreen implements Screen {
    private static final float TICKS_PER_SECOND = 10f;

    private final Main application;
    private final PlantZombieGame game;
    private final MidGameController midGameController;
    private final ControllerGameplaySeedBankDataSource dataSource;
    private final GameplayWorldScene worldScene;
    private final Stage stage;
    private final ChapterType chapterType;
    private final LevelType levelType;
    private final List<String> selectedPlantNames;
    private final Set<String> boostedPlantNames;
    private float tickRemainder;
    private boolean disposed;

    public GameplayScreen(Main application, PlantZombieGame game) {
        if (application == null || game == null) {
            throw new IllegalArgumentException("Application and game are required");
        }
        this.application = application;
        this.game = game;
        this.chapterType = resolveChapterType(game);
        this.levelType = resolveLevelType(game);
        this.selectedPlantNames = copySelectedPlantNames(game);
        this.boostedPlantNames = copyBoostedPlantNames(game);
        this.midGameController = new MidGameController(game);
        User user = application.getApplicationController().getCurrentUser();
        CollectionController collectionController = user == null
                ? new CollectionController(
                        application.getPlantDefinitions(),
                        application.getPlantUpgradeService()
                )
                : new CollectionController(
                        user,
                        application.getPlantDefinitions(),
                        application.getZombieDefinitions()
                );
        this.dataSource = new ControllerGameplaySeedBankDataSource(
                this.midGameController,
                collectionController,
                game,
                false
        );
        this.worldScene = new GameplayWorldScene(this.dataSource, this.dataSource);
        this.stage = new Stage(new FitViewport(
                GameplayWorldLayout.STAGE_WIDTH,
                GameplayWorldLayout.STAGE_HEIGHT
        ));
        this.stage.addActor(this.worldScene);
        this.worldScene.setSessionActions(
                () -> queueSessionAction(this::restartLevel),
                () -> queueSessionAction(this::saveAndExit),
                () -> queueSessionAction(this::saveAndExit),
                () -> queueSessionAction(this::restartLevel)
        );
        this.worldScene.setOutcomeAction(this::persistOutcome);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        float safeDelta = Math.min(Math.max(delta, 0f), 0.1f);
        if (this.worldScene.shouldAdvanceModel() && this.game.getEngine().isGameRunning()) {
            advanceModel(safeDelta);
        }
        ScreenUtils.clear(new Color(0f, 0f, 0f, 1f));
        this.stage.act(safeDelta);
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            this.stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {
        this.worldScene.openPauseMenu();
        this.application.getApplicationController().save();
    }

    @Override
    public void resume() {
        if (!this.disposed) {
            Gdx.input.setInputProcessor(this.stage);
        }
    }

    @Override
    public void hide() {
        InputProcessor processor = Gdx.input.getInputProcessor();
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

    public GameplayWorldScene getWorldScene() {
        return this.worldScene;
    }

    public PlantZombieGame getGameModel() {
        return this.game;
    }

    private void advanceModel(float delta) {
        this.tickRemainder += delta * TICKS_PER_SECOND;
        int ticks = (int) this.tickRemainder;
        if (ticks <= 0) {
            return;
        }
        this.tickRemainder -= ticks;
        this.midGameController.onAdvanceTimeRequested(ticks);
    }

    private void restartLevel() {
        ApplicationController controller = this.application.getApplicationController();
        controller.save();
        if (!returnToChapterMenu(controller)) {
            returnToAdventureSelection(controller);
            return;
        }

        controller.execute("select level -t " + this.levelType.name());
        if (this.levelType == LevelType.CONVEYOR_BELT) {
            PlantZombieGame restartedGame = controller.getCurrentGame();
            if (restartedGame != null) {
                controller.save();
                showGameplay(restartedGame);
                return;
            }
            returnToAdventureSelection(controller);
            return;
        }

        if (controller.getCurrentMenu() != MenuType.PLANT_PICK_MENU) {
            returnToAdventureSelection(controller);
            return;
        }
        controller.execute("show available plants");
        PlantPickController plantPickController = controller.getPlantPickController();
        if (!restorePlantSelection(plantPickController)) {
            returnToAdventureSelection(controller);
            return;
        }
        controller.execute("start game");
        PlantZombieGame restartedGame = controller.getCurrentGame();
        if (restartedGame == null) {
            returnToAdventureSelection(controller);
            return;
        }
        restartedGame.configurePlantSelection(
                plantPickController.getSelectedPlants(),
                this.boostedPlantNames
        );
        controller.save();
        showGameplay(restartedGame);
    }

    private boolean restorePlantSelection(PlantPickController plantPickController) {
        if (plantPickController == null || this.selectedPlantNames.isEmpty()) {
            return false;
        }
        for (String plantName : this.selectedPlantNames) {
            plantPickController.addPlant(plantName);
        }
        return !plantPickController.getSelectedPlants().isEmpty();
    }

    private boolean returnToChapterMenu(ApplicationController controller) {
        if (controller.getCurrentMenu() == MenuType.MID_GAME_MENU) {
            controller.execute("menu exit");
        }
        MenuType currentMenu = controller.getCurrentMenu();
        try {
            if (currentMenu == MenuType.MAIN_MENU) {
                controller.getMenuContext().enterMenu(MenuType.GAME_MENU);
                currentMenu = MenuType.GAME_MENU;
            }
            if (currentMenu == MenuType.GAME_MENU) {
                controller.getChapterController().enterChapterMenu(this.chapterType);
                return true;
            }
            if (currentMenu == MenuType.CHAPTER_MENU) {
                if (controller.getChapterController().getSelectedChapter() != this.chapterType) {
                    controller.getChapterController().enterChapterMenu(this.chapterType);
                }
                return true;
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    private void saveAndExit() {
        ApplicationController controller = this.application.getApplicationController();
        controller.save();
        if (controller.getCurrentMenu() == MenuType.MID_GAME_MENU) {
            controller.execute("menu exit");
        }
        showAdventureLevelSelection();
    }

    private void persistOutcome() {
        boolean won = !this.worldScene.getOutcomeOverlay().isLoss();
        this.application.getApplicationController().finishGraphicalGame(won);
    }

    private void returnToAdventureSelection(ApplicationController controller) {
        if (controller.getCurrentMenu() == MenuType.PLANT_PICK_MENU
                || controller.getCurrentMenu() == MenuType.MID_GAME_MENU) {
            controller.execute("menu exit");
        }
        showAdventureLevelSelection();
    }

    private void showGameplay(PlantZombieGame gameModel) {
        if (gameModel == null) {
            return;
        }
        switchScreen(new GameplayScreen(this.application, gameModel));
    }

    private void showAdventureLevelSelection() {
        ApplicationController controller = this.application.getApplicationController();
        if (controller.getCurrentMenu() == MenuType.MAIN_MENU) {
            controller.getMenuContext().enterMenu(MenuType.GAME_MENU);
        }
        switchScreen(new AdventureLevelSelectionScreen(this.application));
    }

    private void switchScreen(Screen nextScreen) {
        if (nextScreen == null) {
            return;
        }
        Screen previousScreen = this.application.getScreen();
        this.application.setScreen(nextScreen);
        if (previousScreen != null && previousScreen != nextScreen) {
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

    private static ChapterType resolveChapterType(PlantZombieGame game) {
        return game.getActiveChapter() == null || game.getActiveChapter().getChapter() == null
                ? ChapterType.ANCIENT_EGYPT
                : game.getActiveChapter().getChapter();
    }

    private static LevelType resolveLevelType(PlantZombieGame game) {
        Level level = game.getActiveLevel();
        return level == null || level.getLevelType() == null
                ? LevelType.NORMAL
                : level.getLevelType();
    }

    private static List<String> copySelectedPlantNames(PlantZombieGame game) {
        if (game.getSelectedPlantNames() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(game.getSelectedPlantNames());
    }

    private static Set<String> copyBoostedPlantNames(PlantZombieGame game) {
        if (game.getBoostedPlantNames() == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(game.getBoostedPlantNames());
    }
}
