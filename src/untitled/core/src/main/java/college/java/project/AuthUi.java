package college.java.project;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;

final class AuthUi {
    private static final float WORLD_WIDTH = 1280f;
    private static final float WORLD_HEIGHT = 720f;

    private AuthUi() {
    }

    static void addBackground(Stage stage, Texture texture) {
        Image background = new Image(texture);
        background.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        background.setScaling(Scaling.fill);
        background.setTouchable(Touchable.disabled);
        stage.addActor(background);
    }

    static Table createContent(Skin skin, float padding) {
        Table content = new Table();
        content.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(padding);
        return content;
    }

    static Table createFrame(Skin skin, Table content) {
        Table frame = new Table();
        frame.setBackground(skin.getDrawable("image_ui_dialog_asset_dialogborder_10"));
        frame.pad(16f);
        frame.add(content).grow();
        return frame;
    }

    static void submitOnEnter(TextField field, Runnable action) {
        field.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode != Input.Keys.ENTER)
                    return false;

                action.run();
                return true;
            }
        });
    }

    static TextButton createPasswordToggle(Skin skin, TextField... fields) {
        TextButton button = new TextButton("SHOW", skin, "green_small");

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean visible = button.isChecked();

                for (TextField field : fields)
                    field.setPasswordMode(!visible);

                button.setText(visible ? "HIDE" : "SHOW");
            }
        });

        return button;
    }
}
