package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import controller.ApplicationController;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
public class SettingsMenuScreen implements Screen {

    public interface Navigator {
        void onBack();
    }

    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String PANEL_DRAWABLE = "image_ui_mainmenu_mm_settings_tab_10";

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;
    private static final int MIN_SPEED = 1;
    private static final int MAX_SPEED = 3;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();

    private Stage stage;
    private Label statusLabel;
    private Label difficultyValueLabel;
    private Label speedValueLabel;
    private TextButton gridToggle;
    private TextButton debugToggle;

    public SettingsMenuScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null) {
            throw new IllegalArgumentException("Controller and navigator are required");
        }
        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new Stage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        Skin skin = PvzSkin.get();

        this.stage.addActor(this.createImageFill(BACKGROUND_PATH));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        this.stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable(PANEL_DRAWABLE));
        panel.pad(32);

        Label title = new Label("Settings", skin, "big_outline");
        panel.add(title).padBottom(28).colspan(2).row();

        this.buildDifficultyRow(skin, panel);
        this.buildSpeedRow(skin, panel);
        this.buildTogglesRow(skin, panel);

        this.statusLabel = new Label("", skin, "secondary");
        panel.add(this.statusLabel).colspan(2).padTop(16).row();

        TextButton backButton = new TextButton("Back", skin, "brown");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.execute("menu exit");
                navigator.onBack();
            }
        });
        panel.add(backButton).size(140, 48).colspan(2).padTop(20);

        root.add(panel).center();

        this.difficultyValueLabel.setText(String.valueOf(this.controller.getOrCreateSettingView().getCurrentDifficulty()));
        this.speedValueLabel.setText(GameSettings.getGameSpeed() + "x");
        this.refreshToggleLabels();
    }

    private void buildDifficultyRow(Skin skin, Table panel) {
        panel.add(new Label("Difficulty", skin, "medium")).left().padRight(16);

        Slider slider = new Slider(MIN_DIFFICULTY, MAX_DIFFICULTY, 1, false, skin, "default-horizontal");
        slider.setValue(this.controller.getOrCreateSettingView().getCurrentDifficulty());

        this.difficultyValueLabel = new Label("", skin, "secondary");

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int level = (int) slider.getValue();
                String result = controller.execute("menu settings change-difficulty -l " + level);
                statusLabel.setText(result);
                difficultyValueLabel.setText(String.valueOf(level));
            }
        });

        Table row = new Table();
        row.add(slider).width(220);
        row.add(this.difficultyValueLabel).padLeft(12);
        panel.add(row).padBottom(20).row();
    }

    private void buildSpeedRow(Skin skin, Table panel) {
        panel.add(new Label("Game Speed", skin, "medium")).left().padRight(16);

        Slider slider = new Slider(MIN_SPEED, MAX_SPEED, 1, false, skin, "default-horizontal");
        slider.setValue(GameSettings.getGameSpeed());

        this.speedValueLabel = new Label("", skin, "secondary");

        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int speed = (int) slider.getValue();
                GameSettings.setGameSpeed(speed);
                speedValueLabel.setText(speed + "x");
            }
        });

        Table row = new Table();
        row.add(slider).width(220);
        row.add(this.speedValueLabel).padLeft(12);
        panel.add(row).padBottom(20).row();
    }

    private void buildTogglesRow(Skin skin, Table panel) {
        this.gridToggle = new TextButton("", skin, "brown");
        this.gridToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.setShowGrid(!GameSettings.isShowGrid());
                refreshToggleLabels();
            }
        });

        this.debugToggle = new TextButton("", skin, "brown");
        this.debugToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.setDebugMode(!GameSettings.isDebugMode());
                refreshToggleLabels();
            }
        });

        panel.add(this.gridToggle).width(220).height(48).padBottom(12).row();
        panel.add(this.debugToggle).width(220).height(48).padBottom(12).row();
    }

    private void refreshToggleLabels() {
        this.gridToggle.setText("Show Grid: " + (GameSettings.isShowGrid() ? "ON" : "OFF"));
        this.debugToggle.setText("Debug Mode: " + (GameSettings.isDebugMode() ? "ON" : "OFF"));
    }

    private Image createImageFill(String assetPath) {
        Texture texture = this.loadTexture(assetPath);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setScaling(Scaling.fill);
        image.setFillParent(true);
        return image;
    }

    private Texture loadTexture(String assetPath) {
        Texture texture = new Texture(Gdx.files.internal(assetPath));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.loadedTextures.add(texture);
        return texture;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.valueOf("2f4b2f"));
        this.stage.act(delta);
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        this.stage.dispose();
        for (Texture texture : this.loadedTextures) {
            texture.dispose();
        }
        this.loadedTextures.clear();
    }
}
