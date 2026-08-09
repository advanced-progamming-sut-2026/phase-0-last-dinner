package college.java.project;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
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
    private final CollectionController collectionController;

    public Main() {
        try {
            this.plantDefinitions =
                CsvPlantDefinitionRepository.fromClasspath(PLANTS_RESOURCE);

            this.zombieDefinitions =
                JsonZombieDefinitionRepository.fromClasspath(
                    ZOMBIES_RESOURCE,
                    ARMOR_RESOURCE
                );

            this.zombieFactory = new ZombieFactory(this.zombieDefinitions);
            this.plantUpgradeService = new PlantUpgradeService();

            this.collectionController = new CollectionController(
                this.plantDefinitions,
                this.plantUpgradeService
            );

            this.applicationController = new ApplicationController(
                new UserRepository(),
                this.plantDefinitions,
                this.zombieDefinitions
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                "Could not load bundled plant and zombie definitions",
                e
            );
        }
    }

    public static Main loadApplication() {
        return new Main();
    }

    @Override
    public void create() {
        setScreen(new LoginScreen());
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

        GreenhouseBoostService boostService =
            new GreenhouseBoostService(user.getGreenhouse());

        PlantUpgradeService userUpgradeService =
            user.getPlantUpgradeService();

        PlantZombieGame game = new PlantZombieGame(
            this.plantDefinitions,
            this.zombieDefinitions,
            this.zombieFactory,
            userUpgradeService,
            boostService,
            user
        );

        game.getBoard().setZombieEncounterListener(definition -> {
            if (definition != null
                && user.recordEncounteredZombie(definition.getAlias())) {
                this.applicationController.save();
            }
        });

        transferStoredPlantFood(user, game);
        this.applicationController.save();

        return game;
    }

    private void transferStoredPlantFood(
        User user,
        PlantZombieGame game
    ) {
        int storedPlantFood = Math.max(
            0,
            Math.min(3, user.getNextLevelPlantFood())
        );

        int transferredPlantFood = 0;

        for (int i = 0; i < storedPlantFood; i++) {
            if (!game.getPlantFoodSystem().addPlantFood()) {
                break;
            }

            transferredPlantFood++;
        }

        user.setNextLevelPlantFood(
            storedPlantFood - transferredPlantFood
        );
    }

    @Override
    public void pause() {
        if (this.applicationController != null) {
            this.applicationController.save();
        }

        super.pause();
    }

    @Override
    public void dispose() {
        Screen currentScreen = getScreen();

        if (this.applicationController != null) {
            this.applicationController.close();
        }

        super.dispose();

        if (currentScreen != null) {
            currentScreen.dispose();
        }
    }
}
