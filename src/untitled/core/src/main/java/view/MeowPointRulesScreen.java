package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import pvz.skin.PvzSkin;

public final class MeowPointRulesScreen implements Screen {
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private final Runnable onStart;
    private Stage stage;

    public MeowPointRulesScreen(Runnable onStart) {
        if (onStart == null) {
            throw new IllegalArgumentException("onStart is required");
        }
        this.onStart = onStart;
    }

    @Override
    public void show() {
        this.stage = new college.java.project.graphics.SfxStage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        Skin skin = PvzSkin.get();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(40f);
        this.stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(30f);

        Label title = new Label("Meow Point Scoring", skin, "big");
        title.setAlignment(Align.center);
        panel.add(title).colspan(2).padBottom(20f).row();

        addRule(panel, skin, "Quick Kill",
            "Kill a zombie within 2 seconds of it entering the lawn.", "+15");
        addRule(panel, skin, "Kill Streak",
            "Kill another zombie within 3 seconds of your last kill.", "+5 each");
        addRule(panel, skin, "Multi-Kill",
            "Kill 2+ zombies with a single projectile.", "+10 per extra zombie");
        addRule(panel, skin, "Simultaneous Kill",
            "Kill 2+ zombies on the same tick.", "+5 per zombie");
        addRule(panel, skin, "No Plant Lost",
            "Finish the level without losing a single plant.", "+100");

        TextButton startButton = new TextButton("Start", skin, "green");
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onStart.run();
            }
        });
        panel.add(startButton).colspan(2).width(220f).height(64f).padTop(30f);

        root.add(panel).width(900f);
    }

    private void addRule(Table panel, Skin skin, String name, String description, String points) {
        Label nameLabel = new Label(name, skin, "medium");
        Label descLabel = new Label(description, skin, "secondary");
        descLabel.setWrap(true);
        Label pointsLabel = new Label(points, skin, "medium_outline");
        pointsLabel.setAlignment(Align.right);

        Table row = new Table();
        row.add(nameLabel).left().row();
        row.add(descLabel).left().width(600f);

        panel.add(row).left().padBottom(14f);
        panel.add(pointsLabel).right().padBottom(14f).row();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(new Color(0.05f, 0.08f, 0.05f, 1f));
        this.stage.act(delta);
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
    }

    @Override
    public void dispose() {
        this.stage.dispose();
    }
}
