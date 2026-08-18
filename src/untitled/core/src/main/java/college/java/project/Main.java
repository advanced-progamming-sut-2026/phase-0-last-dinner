package college.java.project;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import college.java.project.graphics.CollectionMenuCoordinator;
import college.java.project.graphics.ControllerPlantCollectionDataSource;
import college.java.project.graphics.ControllerZombieCollectionDataSource;
import controller.ApplicationController;
import controller.CollectionController;
import lombok.Getter;
import model.GameMenuRelated.TravelLog;
import model.Greenhouse.GreenhouseBoostService;
import model.User.User;
import model.User.UserRepository;
import model.mechanism.PlantZombieGame;
import model.minigame.MiniGameFactory;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantUpgradeService;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

import java.io.IOException;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import pvz.skin.PvzSkin;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

@Getter
public final class Main extends Game {
    private static final String PLANTS_RESOURCE = "data/plants.csv";
    private static final String ZOMBIES_RESOURCE = "data/zombies.json";
    private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";

    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final ZombieFactory zombieFactory;
    private final ApplicationController applicationController;
    private final PlantUpgradeService plantUpgradeService;
    private CollectionController collectionController;
    private CollectionMenuCoordinator collectionMenuCoordinator;
    private Screen collectionReturnScreen;

    private Skin skin;

    private Texture authBackground;

    public Main() {
        try {
            this.plantDefinitions = CsvPlantDefinitionRepository.fromClasspath(PLANTS_RESOURCE);

            this.zombieDefinitions = JsonZombieDefinitionRepository.fromClasspath(ZOMBIES_RESOURCE, ARMOR_RESOURCE);

            this.zombieFactory = new ZombieFactory(this.zombieDefinitions);
            this.plantUpgradeService = new PlantUpgradeService();
            this.collectionController = new CollectionController(this.plantDefinitions, this.plantUpgradeService);

            this.applicationController = new ApplicationController(new UserRepository(), this.plantDefinitions,
                this.zombieDefinitions);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled plant and zombie definitions", e);
        }
    }

    public static Main loadApplication() {
        return new Main();
    }

    @Override
    public void create() {
        this.skin = PvzSkin.get();
        this.authBackground = new Texture(Gdx.files.internal("ui/auth_background.png"));
        this.authBackground.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        if (this.applicationController.getCurrentUser() != null)
            showMainMenuScreen();
        else
            showLoginScreen();
    }

    public void showLoginScreen() {
        changeScreen(new LoginScreen(this));
    }

    public void showRegisterScreen() {
        changeScreen(new RegisterScreen(this));
    }

    private void changeScreen(Screen nextScreen) {
        Gdx.app.postRunnable(() -> {
            Screen previousScreen = getScreen();
            setScreen(nextScreen);

            if (previousScreen != null)
                previousScreen.dispose();
        });
    }

    public boolean showCollection() {
        User user = this.applicationController.getCurrentUser();
        if (user == null) {
            return false;
        }
        user.initializeMissingFields();
        Screen currentScreen = getScreen();
        if (this.collectionMenuCoordinator == null) {
            this.collectionReturnScreen = currentScreen;
        }
        if (this.collectionMenuCoordinator != null) {
            this.collectionMenuCoordinator.dispose();
        }
        this.collectionController = new CollectionController(
                user,
                this.plantDefinitions,
                this.zombieDefinitions
        );
        this.collectionMenuCoordinator = new CollectionMenuCoordinator(
                this,
                new ControllerPlantCollectionDataSource(
                        this.collectionController,
                        this.applicationController.getGameController(),
                        false,
                        this.applicationController::save
                ),
                new ControllerZombieCollectionDataSource(
                        this.collectionController,
                        this.applicationController.getGameController(),
                        false,
                        this.applicationController::save
                )
        );
        this.collectionMenuCoordinator.setOnClose(this::closeCollection);
        this.collectionMenuCoordinator.showPlants();
        return true;
    }

    public MiniGameFactory createMiniGameFactory() {
        return new MiniGameFactory(
            this.plantDefinitions,
            this.zombieDefinitions,
            this.zombieFactory
        );
    }

    public TravelLog createTravelLog() {
        return new TravelLog(createMiniGameFactory());
    }

    public PlantZombieGame createGame() {
        User user = this.applicationController.getCurrentUser();

        if (user == null) {
            return new PlantZombieGame(
                this.plantDefinitions,
                this.zombieDefinitions,
                this.zombieFactory,
                this.plantUpgradeService
            );
        }

        user.initializeMissingFields();
        user.setGamesPlayed(user.getGamesPlayed() + 1);

        GreenhouseBoostService boostService = new GreenhouseBoostService(user.getGreenhouse());

        PlantUpgradeService userUpgradeService = user.getPlantUpgradeService();

        PlantZombieGame game = new PlantZombieGame(this.plantDefinitions, this.zombieDefinitions, this.zombieFactory,
            userUpgradeService, boostService, user);

        game.getBoard().setZombieEncounterListener(definition -> {
            if (definition != null && user.recordEncounteredZombie(definition.getAlias()))
                this.applicationController.save();
        });

        transferStoredPlantFood(user, game);
        this.applicationController.save();

        return game;
    }
    public void showMainMenuScreen() {
        changeScreen(new view.MainMenuScreen(this.applicationController, new view.MainMenuScreen.Navigator() {
            @Override
            public void openGameMenu() {
                showGameMenuScreen();
            }

            @Override
            public void openSettingsMenu() {
                showSettingsMenuScreen();
            }

            @Override
            public void openNewsMenu() {
                // TODO: هنوز صفحه‌ی گرافیکی News ساخته نشده
            }

            @Override
            public void openProfileMenu() {
                showProfileMenuScreen();
            }

            @Override
            public void onLoggedOut() {
                showLoginScreen();
            }
        }));
    }

    public void showProfileMenuScreen() {
        changeScreen(new view.ProfileMenuScreen(this.applicationController, this::showMainMenuScreen));
    }

    public void showSettingsMenuScreen() {
        changeScreen(new view.SettingsMenuScreen(this.applicationController, this::showMainMenuScreen));
    }
    public void showGameMenuScreen() {
        changeScreen(new view.GameMenuScreen(this.applicationController, new view.GameMenuScreen.Navigator() {
            @Override
            public void openChapterMenu(model.chapters.ChapterType chapter) {
                // TODO: هنوز صفحه‌ی گرافیکی Chapter/Level ساخته نشده
            }

            @Override
            public void openCollectionMenu() {
                showCollection();
            }

            @Override
            public void openGreenhouse() {
                // TODO: هنوز صفحه‌ی گرافیکی Greenhouse ساخته نشده
            }

            @Override
            public void openTravelLog() {
                // TODO: هنوز صفحه‌ی گرافیکی Travel Log ساخته نشده
            }

            @Override
            public void openLeaderboard() {
                showLeaderBoardMenuScreen(Main.this::showGameMenuScreen);
            }

            @Override
            public void onBack() {
                showMainMenuScreen();
            }
        }));
    }

    private void showLeaderBoardMenuScreen(Runnable onBack) {
        changeScreen(new view.LeaderBoardMenuScreen(this.applicationController, onBack::run));
    }

    private void transferStoredPlantFood(User user, PlantZombieGame game) {
        int storedPlantFood = Math.max(0, Math.min(3, user.getNextLevelPlantFood()));

        int transferredPlantFood = 0;

        for (int i = 0; i < storedPlantFood; i++) {
            if (!game.getPlantFoodSystem().addPlantFood())
                break;

            transferredPlantFood++;
        }

        user.setNextLevelPlantFood(storedPlantFood - transferredPlantFood);
    }

    @Override
    public void pause() {
        if (this.applicationController != null)
            this.applicationController.save();

        super.pause();
    }

    @Override
    public void dispose() {
        Screen currentScreen = getScreen();

        if (this.applicationController != null)
            this.applicationController.close();

        super.dispose();

        if (this.collectionMenuCoordinator != null) {
            this.collectionMenuCoordinator.dispose();
            this.collectionMenuCoordinator = null;
        } else if (currentScreen != null) {
            currentScreen.dispose();
        }

        if (this.skin != null)
            this.skin.dispose();

        if (this.authBackground != null)
            this.authBackground.dispose();
    }

    private void closeCollection() {
        this.applicationController.save();
        CollectionMenuCoordinator coordinator = this.collectionMenuCoordinator;
        this.collectionMenuCoordinator = null;
        this.collectionController = new CollectionController(
                this.plantDefinitions,
                this.plantUpgradeService
        );
        Screen returnScreen = this.collectionReturnScreen;
        this.collectionReturnScreen = null;
        if (returnScreen == null) {
            showMainMenuScreen();
        } else {
            setScreen(returnScreen);
        }
        if (coordinator != null) {
            coordinator.dispose();
        }
    }
}
