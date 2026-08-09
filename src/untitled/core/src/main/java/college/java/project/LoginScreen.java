package college.java.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;

public final class LoginScreen implements Screen {
    private static final float WORLD_WIDTH = 1280f;
    private static final float WORLD_HEIGHT = 720f;

    private final Stage stage;

    public LoginScreen() {
        this.stage = new Stage(
            new FitViewport(WORLD_WIDTH, WORLD_HEIGHT)
        );
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

        this.stage.act(Math.min(delta, 1f / 30f));
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        this.stage.getViewport().update(
            width,
            height,
            true
        );
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        InputProcessor currentProcessor =
            Gdx.input.getInputProcessor();

        if (currentProcessor == this.stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        this.stage.dispose();
    }
}
