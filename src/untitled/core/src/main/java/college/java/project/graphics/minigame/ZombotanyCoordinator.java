package college.java.project.graphics.minigame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import college.java.project.Main;
import college.java.project.graphics.PlantPickScreen;
import controller.ZombotanyController;
import model.User.User;
import model.chapters.ChapterType;
import model.minigame.zombotanyminigame.ZombotanyActionResult;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;

import java.util.concurrent.ThreadLocalRandom;

public final class ZombotanyCoordinator {
    private final Main application;
    private final ZombotanyMiniGame game;
    private final ZombotanyController controller;
    private final Runnable onBack;
    private final int stageNumber;
    private final ChapterType chapterType;

    private ZombotanyCoordinator(Main application, ZombotanyMiniGame game, int stageNumber, Runnable onBack) {
        if (application == null || game == null)
            throw new IllegalArgumentException("Application and Zombotany game are required.");

        if (stageNumber < 1 || stageNumber > 3)
            throw new IllegalArgumentException("Zombotany stage must be between 1 and 3.");

        this.application = application;
        this.game = game;
        this.stageNumber = stageNumber;
        this.onBack = onBack;
        this.controller = new ZombotanyController(game);
        this.chapterType = randomChapter();
    }

    public static void open(Main application, ZombotanyMiniGame game, int stageNumber, Runnable onBack) {
        ZombotanyCoordinator coordinator = new ZombotanyCoordinator(application, game, stageNumber, onBack);

        coordinator.showPlantPicker();
    }

    private void showPlantPicker() {
        ZombotanyActionResult result = this.controller.onStartRequested(this.stageNumber);

        if (result == null || !result.isSuccessful())
            throw new IllegalStateException(result == null ? "Zombotany plant selection failed." : result.getMessage());

        User user = this.application.getApplicationController().getCurrentUser();

        if (user == null) throw new IllegalStateException("A user is required to start Zombotany.");

        user.initializeMissingFields();

        ZombotanyPlantPickDataSource dataSource = new ZombotanyPlantPickDataSource(this.game, this.controller, user,
            this.application.getApplicationController()::save);

        PlantPickScreen plantPickScreen = new PlantPickScreen(dataSource, this.chapterType);

        plantPickScreen.setOnStart(this::showGameplay);

        plantPickScreen.setOnClose(this::close);

        queueScreen(plantPickScreen);
    }

    private void showGameplay() {
        if (!this.game.isStarted()) return;

        ZombotanyScreen gameplayScreen = new ZombotanyScreen(this.application, this.game, this.stageNumber,
            this.chapterType, this.onBack);

        queueScreen(gameplayScreen);
    }

    private void close() {
        this.application.getApplicationController().save();

        if (this.onBack != null) this.onBack.run();
    }

    private void queueScreen(Screen nextScreen) {
        if (nextScreen == null) return;

        if (Gdx.app == null) {
            switchScreen(nextScreen);
            return;
        }

        Gdx.app.postRunnable(() -> switchScreen(nextScreen));
    }

    private void switchScreen(Screen nextScreen) {
        Screen previousScreen = this.application.getScreen();

        this.application.setScreen(nextScreen);

        if (previousScreen != null && previousScreen != nextScreen) {
            previousScreen.dispose();
        }
    }

    private static ChapterType randomChapter() {
        ChapterType[] chapters = ChapterType.values();

        return chapters[ThreadLocalRandom.current().nextInt(chapters.length)];
    }
}
