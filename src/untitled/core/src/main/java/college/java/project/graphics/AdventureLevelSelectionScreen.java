package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import college.java.project.Main;
import controller.ApplicationController;
import controller.CollectionController;
import controller.PlantPickController;
import model.Menu.MenuType;
import model.User.User;
import model.chapters.ChapterType;
import model.level.LevelType;
import pvz.skin.PvzSkin;

public final class AdventureLevelSelectionScreen implements Screen {
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;

    private final Main game;
    private final ApplicationController applicationController;
    private final Stage stage;
    private final Label statusLabel;

    public AdventureLevelSelectionScreen(Main game) {
        if (game == null) {
            throw new IllegalArgumentException("Game is required");
        }
        this.game = game;
        this.applicationController = game.getApplicationController();
        this.stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        this.statusLabel = new Label("", PvzSkin.get());
        build();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(new Color(0.045f, 0.095f, 0.055f, 1f));
        this.stage.act(Math.min(Math.max(delta, 0f), 1f / 20f));
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
        this.applicationController.save();
    }

    @Override
    public void resume() {
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
        this.stage.dispose();
    }

    public boolean openLevel(ChapterType chapterType, LevelType levelType) {
        User user = this.applicationController.getCurrentUser();
        if (user == null) {
            showStatus("Login is required.");
            return false;
        }
        if (chapterType == null || levelType == null) {
            showStatus("Choose a valid chapter and level.");
            return false;
        }
        if (!user.isChapterUnlocked(chapterType)) {
            showStatus("Chapter is locked.");
            return false;
        }

        try {
            this.applicationController.getChapterController().enterChapterMenu(chapterType);
        } catch (RuntimeException exception) {
            showStatus("Could not open chapter.");
            return false;
        }

        String result = this.applicationController.execute(
                "select level -t " + levelType.name()
        );

        if (levelType == LevelType.CONVEYOR_BELT) {
            if (this.applicationController.getCurrentGame() == null) {
                showStatus(result == null || result.isBlank()
                        ? "Could not start conveyor level."
                        : result);
                return false;
            }
            showGameplay(this.applicationController.getCurrentGame());
            return true;
        }

        if (this.applicationController.getCurrentMenu() != MenuType.PLANT_PICK_MENU) {
            showStatus(result == null || result.isBlank()
                    ? "Level is not available."
                    : result);
            return false;
        }

        this.applicationController.execute("show available plants");
        PlantPickController plantPickController =
                this.applicationController.getPlantPickController();
        if (plantPickController == null) {
            showStatus("Plant picker could not be prepared.");
            return false;
        }

        CollectionController collectionController = new CollectionController(
                user,
                this.game.getPlantDefinitions(),
                this.game.getZombieDefinitions()
        );
        ControllerPlantPickDataSource dataSource = new ControllerPlantPickDataSource(
                plantPickController,
                collectionController,
                user,
                this.applicationController
        );
        PlantPickScreen plantPickScreen = new PlantPickScreen(dataSource, chapterType);
        plantPickScreen.setOnClose(this::returnFromPlantPick);
        plantPickScreen.setOnStart(() -> startSelectedGame(plantPickScreen));
        switchScreen(plantPickScreen);
        return true;
    }

    private void startSelectedGame(PlantPickScreen plantPickScreen) {
        this.applicationController.execute("start game");
        if (this.applicationController.getCurrentGame() == null) {
            CollectionUiAnimator.enterScreen(plantPickScreen.getStage());
            return;
        }
        this.applicationController.save();
        showGameplay(this.applicationController.getCurrentGame());
    }

    private void returnFromPlantPick() {
        this.applicationController.execute("menu exit");
        showAdventureLevelSelection();
    }

    private void build() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(60f);
        this.stage.addActor(root);

        Label title = new Label("ADVENTURE", PvzSkin.get());
        title.setFontScale(1.7f);
        title.setAlignment(Align.center);
        root.add(title).growX().height(90f).colspan(2);
        root.row();

        for (ChapterType chapterType : ChapterType.values()) {
            if (!isAdventureChapter(chapterType)) {
                continue;
            }
            root.add(chapterButton(chapterType, LevelType.NORMAL))
                    .size(500f, 100f).pad(14f);
            LevelType specialLevel = specialLevel(chapterType);
            TextButton special = chapterButton(chapterType, specialLevel);
            root.add(special).size(500f, 100f).pad(14f);
            root.row();
        }

        this.statusLabel.setAlignment(Align.center);
        this.statusLabel.setWrap(true);
        root.add(this.statusLabel).growX().height(90f).padTop(30f).colspan(2);
    }

    private TextButton chapterButton(ChapterType chapterType, LevelType levelType) {
        User user = this.applicationController.getCurrentUser();
        boolean unlocked = user != null
                && user.isAdventureLevelUnlocked(chapterType, levelType);
        String label = chapterLabel(chapterType) + "  -  " + levelLabel(levelType);
        if (!unlocked) {
            label += "  [LOCKED]";
        }
        TextButton button = new TextButton(label, PvzSkin.get());
        button.setDisabled(!unlocked);
        button.setTouchable(unlocked ? Touchable.enabled : Touchable.disabled);
        if (unlocked) {
            button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(
                        com.badlogic.gdx.scenes.scene2d.InputEvent event,
                        float x,
                        float y
                ) {
                    openLevel(chapterType, levelType);
                }
            });
        }
        return button;
    }

    private boolean isAdventureChapter(ChapterType chapterType) {
        return chapterType == ChapterType.ANCIENT_EGYPT
                || chapterType == ChapterType.ICE_CAVES
                || chapterType == ChapterType.BIG_WAVE_BEACH
                || chapterType == ChapterType.MEDIEVAL;
    }

    private LevelType specialLevel(ChapterType chapterType) {
        switch (chapterType) {
            case ANCIENT_EGYPT:
                return LevelType.CONVEYOR_BELT;
            case ICE_CAVES:
                return LevelType.DEADLINE;
            case BIG_WAVE_BEACH:
                return LevelType.NIGHT_OPS;
            case MEDIEVAL:
                return LevelType.LOVE_YOUR_PLANTS;
            default:
                return LevelType.NORMAL;
        }
    }

    private String chapterLabel(ChapterType chapterType) {
        return chapterType.name().replace('_', ' ');
    }

    private String levelLabel(LevelType levelType) {
        return levelType.name().replace('_', ' ');
    }

    private void showGameplay(model.mechanism.PlantZombieGame gameModel) {
        if (gameModel == null) {
            return;
        }
        switchScreen(new GameplayScreen(this.game, gameModel));
    }

    private void showAdventureLevelSelection() {
        switchScreen(new AdventureLevelSelectionScreen(this.game));
    }

    private void switchScreen(Screen nextScreen) {
        if (nextScreen == null) {
            return;
        }
        Screen previousScreen = this.game.getScreen();
        this.game.setScreen(nextScreen);
        if (previousScreen != null && previousScreen != nextScreen) {
            previousScreen.dispose();
        }
    }

    private void showStatus(String message) {
        this.statusLabel.setText(message == null ? "" : message);
    }
}
