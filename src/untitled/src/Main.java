import controller.CollectionController;
import controller.ApplicationController;
import model.GameMenuRelated.TravelLog;
import model.minigame.MiniGameFactory;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantUpgradeService;
import model.mechanism.PlantZombieGame;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import view.ConsoleApplication;

import java.io.IOException;

public final class Main {
    private static final String PLANTS_RESOURCE = "data/plants.csv";
    private static final String ZOMBIES_RESOURCE = "data/zombies.json";
    private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";

    private final PlantDefinitionRepository plantDefinitions;
    private final ZombieDefinitionRepository zombieDefinitions;
    private final ZombieFactory zombieFactory;
    // upgrade haye daemi plant ro beyn game haye jadid moshtarak negah midare
    private final PlantUpgradeService plantUpgradeService;
    private final CollectionController collectionController;
    private final ApplicationController applicationController;

    private Main(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory,
            PlantUpgradeService plantUpgradeService
    ) {
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.zombieFactory = zombieFactory;
        this.plantUpgradeService = plantUpgradeService;
        this.collectionController = new CollectionController(
                plantDefinitions,
                plantUpgradeService
        );
        this.applicationController = new ApplicationController();
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
            PlantUpgradeService plantUpgradeService = new PlantUpgradeService();

            return new Main(
                    plantDefinitions,
                    zombieDefinitions,
                    zombieFactory,
                    plantUpgradeService
            );
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled plant and zombie definitions", e);
        }
    }

    public PlantDefinitionRepository getPlantDefinitions() {
        return this.plantDefinitions;
    }

    public ZombieDefinitionRepository getZombieDefinitions() {
        return this.zombieDefinitions;
    }

    public ZombieFactory getZombieFactory() {
        return this.zombieFactory;
    }

    public PlantUpgradeService getPlantUpgradeService() {
        return this.plantUpgradeService;
    }

    public CollectionController getCollectionController() {
        return this.collectionController;
    }

    public ApplicationController getApplicationController() {
        return this.applicationController;
    }

    public PlantZombieGame createGame() {
        return new PlantZombieGame(
                this.plantDefinitions,
                this.zombieDefinitions,
                this.zombieFactory,
                this.plantUpgradeService
        );
    }
}
