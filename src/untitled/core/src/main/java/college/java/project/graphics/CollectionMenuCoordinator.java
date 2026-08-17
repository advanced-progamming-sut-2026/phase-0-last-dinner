package college.java.project.graphics;

import com.badlogic.gdx.Game;
import controller.CollectionController;
import controller.GameController;


public final class CollectionMenuCoordinator {
    private final Game game;
    private final PlantCollectionDataSource plants;
    private final ZombieCollectionDataSource zombies;
    private final PlantCollectionScreen plantsScreen;
    private final ZombieCollectionScreen zombiesScreen;
    private Runnable onClose;

    public CollectionMenuCoordinator(
            Game game,
            PlantCollectionDataSource plants,
            ZombieCollectionDataSource zombies
    ) {
        if (game == null || plants == null || zombies == null) {
            throw new IllegalArgumentException("Collection menu dependencies are required");
        }
        this.game = game;
        this.plants = plants;
        this.zombies = zombies;
        this.plantsScreen = new PlantCollectionScreen(plants);
        this.zombiesScreen = new ZombieCollectionScreen(zombies);
        this.plantsScreen.setOnZombiesTab(this::showZombies);
        this.zombiesScreen.setOnPlantsTab(this::showPlants);
        this.plantsScreen.setOnClose(this::close);
        this.zombiesScreen.setOnClose(this::close);
    }

    public CollectionMenuCoordinator(Game game, CollectionController controller) {
        this(
                game,
                new ControllerPlantCollectionDataSource(controller),
                new ControllerZombieCollectionDataSource(controller)
        );
    }

    
    public CollectionMenuCoordinator(
            Game game,
            CollectionController collectionController,
            GameController gameController
    ) {
        this(
                game,
                new ControllerPlantCollectionDataSource(collectionController, gameController),
                new ControllerZombieCollectionDataSource(collectionController, gameController)
        );
    }

    public void showPlants() {
        this.plantsScreen.refresh();
        this.game.setScreen(this.plantsScreen);
    }

    public void showZombies() {
        this.zombiesScreen.refresh();
        this.game.setScreen(this.zombiesScreen);
    }

    
    public void setDebugModeEnabled(boolean enabled) {
        this.plants.setDebugModeEnabled(enabled);
        this.zombies.setDebugModeEnabled(enabled);
        this.plantsScreen.refresh();
        this.zombiesScreen.refresh();
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public PlantCollectionScreen getPlantsScreen() {
        return this.plantsScreen;
    }

    public ZombieCollectionScreen getZombiesScreen() {
        return this.zombiesScreen;
    }

    public void dispose() {
        this.plantsScreen.dispose();
        this.zombiesScreen.dispose();
    }

    private void close() {
        if (this.onClose != null) {
            this.onClose.run();
        }
    }
}
