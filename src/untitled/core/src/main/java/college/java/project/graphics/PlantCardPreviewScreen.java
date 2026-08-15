package college.java.project.graphics;

import com.badlogic.gdx.Screen;
import controller.CollectionController;
import model.collection.PlantCollectionState;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantUpgradeService;

import java.util.ArrayList;
import java.util.List;

public final class PlantCardPreviewScreen implements Screen {
    private static final int PREVIEW_UNLOCKED_COUNT = 24;
    private final PlantCollectionScreen collectionScreen;

    public PlantCardPreviewScreen(
            PlantDefinitionRepository plantDefinitions,
            PlantUpgradeService plantUpgrades
    ) {
        if (plantDefinitions == null || plantUpgrades == null) {
            throw new IllegalArgumentException("Plant data is required");
        }

        this.collectionScreen = new PlantCollectionScreen(
                new CollectionController(plantDefinitions, plantUpgrades)
        );
    }

    private static List<PlantCollectionState> createPreviewStates(
            PlantDefinitionRepository plantDefinitions,
            PlantUpgradeService plantUpgrades
    ) {
        List<PlantCollectionState> states = new ArrayList<>();
        List<PlantDefinition> definitions = plantDefinitions.findAll();

        for (int index = 0; index < definitions.size(); index++) {
            PlantDefinition definition = definitions.get(index);
            if (definition == null) {
                continue;
            }
            boolean unlocked = index < PREVIEW_UNLOCKED_COUNT;
            states.add(PlantCollectionState.from(definition, plantUpgrades, unlocked));
        }
        return states;
    }

    @Override
    public void show() {
        this.collectionScreen.show();
    }

    @Override
    public void render(float delta) {
        this.collectionScreen.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        this.collectionScreen.resize(width, height);
    }

    @Override
    public void pause() {
        this.collectionScreen.pause();
    }

    @Override
    public void resume() {
        this.collectionScreen.resume();
    }

    @Override
    public void hide() {
        this.collectionScreen.hide();
    }

    @Override
    public void dispose() {
        this.collectionScreen.dispose();
    }
}
