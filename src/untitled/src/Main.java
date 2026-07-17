import controller.ApplicationController;
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
import view.ConsoleApplication;

import java.io.IOException;

@Getter
public final class Main {
    private static final String PLANTS_RESOURCE = "data/plants.csv";
    private static final String ZOMBIES_RESOURCE = "data/zombies.json";
    private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";

    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final ZombieFactory zombieFactory;
    private final ApplicationController applicationController;

    private Main(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.zombieFactory = zombieFactory;
        this.applicationController = new ApplicationController(
                new UserRepository(),
                this.plantDefinitions,
                this.zombieDefinitions
        );
    }

    public MiniGameFactory createMiniGameFactory() {
        return new MiniGameFactory(
                this.plantDefinitions,
                this.zombieDefinitions,
                this.zombieFactory
        );
    }

    public TravelLog createTravelLog() {
        return new TravelLog(
                createMiniGameFactory()
        );
    }

    public static void main(String[] args) {
        Main application = loadApplication();
        new ConsoleApplication(
                application.getApplicationController(),
                System.in,
                System.out
        ).run();
    }

    public static Main loadApplication() {
        try {
            PlantDefinitionRepository plantDefinitions =
                    CsvPlantDefinitionRepository.fromClasspath(PLANTS_RESOURCE);
            JsonZombieDefinitionRepository zombieDefinitions =
                    JsonZombieDefinitionRepository.fromClasspath(ZOMBIES_RESOURCE, ARMOR_RESOURCE);
            ZombieFactory zombieFactory = new ZombieFactory(zombieDefinitions);

            return new Main(
                    plantDefinitions,
                    zombieDefinitions,
                    zombieFactory
            );
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled plant and zombie definitions", e);
        }
    }

    public PlantZombieGame createGame() {
        User user = applicationController.getCurrentUser();

        if (user == null) {
            return new PlantZombieGame(
                    this.plantDefinitions,
                    this.zombieDefinitions,
                    this.zombieFactory,
                    new PlantUpgradeService()
            );
        }

        user.initializeMissingFields();

        GreenhouseBoostService boostService = new GreenhouseBoostService(user.getGreenhouse());
        PlantUpgradeService userUpgradeService = user.getPlantUpgradeService();

        PlantZombieGame game =
                new PlantZombieGame(
                        this.plantDefinitions,
                        this.zombieDefinitions,
                        this.zombieFactory,
                        userUpgradeService,
                        boostService
                );

        game.getBoard().setZombieEncounterListener(definition -> {
            if (definition != null && user.recordEncounteredZombie(definition.getAlias()))
                applicationController.save();
        });

        int storedPlantFood = Math.max(0, Math.min(3, user.getNextLevelPlantFood()));
        int transferredPlantFood = 0;

        for (int i = 0; i < storedPlantFood; i++) {
            boolean added = game.getPlantFoodSystem().addPlantFood();

            if (!added)
                break;

            transferredPlantFood++;
        }

        user.setNextLevelPlantFood(storedPlantFood - transferredPlantFood);
        applicationController.save();

        return game;
    }
}