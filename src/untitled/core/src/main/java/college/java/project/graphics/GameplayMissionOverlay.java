package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import pvz.skin.PvzSkin;

/** Mandatory stage-start objective display. */
public final class GameplayMissionOverlay extends Group {
    private final Label description;
    private Runnable continueAction;

    public GameplayMissionOverlay() {
        setSize(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        setTouchable(Touchable.enabled);
        Image dim = solid(new Color(0f, 0f, 0f, 0.54f));
        dim.setBounds(0f, 0f, getWidth(), getHeight());
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        Image footerShade = solid(new Color(0f, 0f, 0f, 0.72f));
        footerShade.setBounds(105f, 70f, 1710f, 160f);
        addActor(footerShade);

        Image shadow = solid(new Color(0f, 0f, 0f, 0.62f));
        shadow.setBounds(206f, 274f, 1518f, 618f);
        addActor(shadow);

        Image frame = solid(new Color(0.26f, 0.27f, 0.20f, 1f));
        frame.setBounds(192f, 292f, 1536f, 616f);
        addActor(frame);

        Image body = solid(new Color(0.95f, 0.91f, 0.75f, 1f));
        body.setBounds(204f, 304f, 1512f, 592f);
        addActor(body);

        Image header = solid(new Color(0.96f, 0.72f, 0.05f, 1f));
        header.setBounds(204f, 732f, 1512f, 164f);
        addActor(header);

        Image headerHighlight = solid(new Color(1f, 0.94f, 0.36f, 1f));
        headerHighlight.setBounds(204f, 872f, 1512f, 24f);
        addActor(headerHighlight);

        Image headerShade = solid(new Color(0.78f, 0.53f, 0.02f, 1f));
        headerShade.setBounds(204f, 732f, 1512f, 16f);
        addActor(headerShade);

        Label title = new Label("Level Objectives", PvzSkin.get(), "medium_outline");
        title.setFontScale(2.82f, 2.48f);
        title.setAlignment(Align.center);
        title.setBounds(310f, 748f, 1300f, 126f);
        addActor(title);

        Image bullet = new Image(PvzSkin.get().getDrawable("image_ui_generic_radio_up"));
        bullet.setBounds(268f, 466f, 72f, 72f);
        bullet.setTouchable(Touchable.disabled);
        addActor(bullet);

        this.description = new Label("", PvzSkin.get(), "medium");
        this.description.setColor(new Color(0.20f, 0.18f, 0.02f, 1f));
        this.description.setFontScale(2.25f, 1.92f);
        this.description.setAlignment(Align.left);
        this.description.setWrap(true);
        this.description.setBounds(370f, 382f, 1260f, 250f);
        addActor(this.description);

        TextButton start = new TextButton("CONTINUE", PvzSkin.get(), "brown");
        start.getLabel().setFontScale(1.68f);
        start.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (continueAction != null) {
                    continueAction.run();
                }
            }
        });
        CollectionUiAnimator.installHoverScale(start);
        start.setBounds(765f, 92f, 390f, 126f);
        addActor(start);
        setVisible(false);
    }

    public void showMission(String titleText, String descriptionText, Runnable onContinue) {
        this.description.setText(safe(descriptionText, safe(titleText, "Don't let the zombies reach your house!")));
        this.continueAction = onContinue;
        setVisible(true);
    }

    private Image solid(Color color) {
        return new Image(PvzSkin.get().newDrawable("white_pixel", color));
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
