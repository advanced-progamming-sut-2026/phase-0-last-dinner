package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import pvz.skin.PvzSkin;

/** Mandatory stage-start objective display. */
public final class GameplayMissionOverlay extends Group {
    private final Label title;
    private final Label description;
    private Runnable continueAction;

    public GameplayMissionOverlay() {
        setSize(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        setTouchable(Touchable.enabled);
        Image dim = new Image(PvzSkin.get().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.58f)));
        dim.setBounds(0f, 0f, getWidth(), getHeight());
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        Table panel = new Table();
        panel.setBounds(470f, 290f, 980f, 500f);
        panel.setBackground(PvzSkin.get().getDrawable("image_ui_generic_popup_9slice"));
        panel.pad(44f, 70f, 44f, 70f);
        this.title = new Label("LEVEL OBJECTIVE", PvzSkin.get(), "medium_outline");
        this.title.setFontScale(0.92f);
        this.title.setAlignment(Align.center);
        this.description = new Label("", PvzSkin.get(), "medium_outline");
        this.description.setFontScale(0.66f);
        this.description.setAlignment(Align.center);
        this.description.setWrap(true);
        panel.add(this.title).growX().height(72f).row();
        panel.add(this.description).grow().padTop(20f).padBottom(24f).row();
        TextButton start = new TextButton("CONTINUE", PvzSkin.get(), "green");
        start.getLabel().setFontScale(0.78f);
        start.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (continueAction != null) {
                    continueAction.run();
                }
            }
        });
        CollectionUiAnimator.installHoverScale(start);
        panel.add(start).width(430f).height(86f);
        addActor(panel);
        setVisible(false);
    }

    public void showMission(String titleText, String descriptionText, Runnable onContinue) {
        this.title.setText(safe(titleText, "LEVEL OBJECTIVE"));
        this.description.setText(safe(descriptionText, "Don't let the zombies reach your house!"));
        this.continueAction = onContinue;
        setVisible(true);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
