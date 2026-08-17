package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import model.collection.PlantCollectionState;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantUpgradeService;
import pvz.skin.PvzSkin;

public final class PlantCardPreviewScreen implements Screen {
    private static final float WORLD_WIDTH = 1280f;
    private static final float WORLD_HEIGHT = 720f;
    private static final String[] PREVIEW_PLANTS = {
            "Peashooter",
            "Sunflower",
            "Wall-nut"
    };

    private final Stage stage;
    private final GameAssetManager assets;

    public PlantCardPreviewScreen(
            PlantDefinitionRepository plantDefinitions,
            PlantUpgradeService plantUpgrades
    ) {
        if (plantDefinitions == null || plantUpgrades == null) {
            throw new IllegalArgumentException(
                    "Plant data is required"
            );
        }

        this.stage = new Stage(new FitViewport(
                WORLD_WIDTH,
                WORLD_HEIGHT
        ));
        this.assets = new GameAssetManager();

        Skin skin = PvzSkin.get();
        PamAnimationCatalog animations = new PamAnimationCatalog();

        Table root = new Table();
        root.setFillParent(true);

        for (int index = 0; index < PREVIEW_PLANTS.length; index++) {
            String plantName = PREVIEW_PLANTS[index];
            PlantDefinition definition = this.findPlant(
                    plantDefinitions,
                    plantName
            );

            if (definition == null) {
                continue;
            }

            PlantCollectionState plantState = PlantCollectionState.from(
                    definition,
                    plantUpgrades,
                    index != PREVIEW_PLANTS.length - 1
            );
            PlantCard card = new PlantCard(
                    skin,
                    this.assets.getPamPlayer(),
                    animations.find(plantName),
                    plantState
            );
            root.add(card).pad(12f);
        }
        this.stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(new Color(
                0.08f,
                0.18f,
                0.10f,
                1f
        ));
        this.assets.update();
        this.stage.act(Math.min(delta, 1f / 30f));
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
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        InputProcessor currentProcessor = Gdx.input.getInputProcessor();

        if (currentProcessor == this.stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        this.assets.dispose();
    }

    private PlantDefinition findPlant(
            PlantDefinitionRepository plantDefinitions,
            String plantName
    ) {
        for (PlantDefinition definition : plantDefinitions.findAll()) {
            if (definition != null && definition.getName() != null
                    && definition.getName().equalsIgnoreCase(plantName)) {
                return definition;
            }
        }

        return null;
    }
}
