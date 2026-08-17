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

/** Mandatory win/loss dialog with Exit and loss-only Retry. */
public final class GameplayOutcomeOverlay extends Group {
    private final Label title;
    private final Label subtitle;
    private final TextButton retryButton;
    private Runnable exitAction;
    private Runnable retryAction;
    private boolean loss;

    public GameplayOutcomeOverlay() {
        setSize(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        setTouchable(Touchable.enabled);
        Image dim = new Image(PvzSkin.get().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.68f)));
        dim.setBounds(0f, 0f, getWidth(), getHeight());
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        Table panel = new Table();
        panel.setBounds(580f, 260f, 760f, 560f);
        panel.setBackground(PvzSkin.get().getDrawable("image_ui_generic_popup_9slice"));
        panel.pad(46f, 70f, 48f, 70f);
        this.title = new Label("VICTORY!", PvzSkin.get(), "medium_outline");
        this.title.setAlignment(Align.center);
        this.title.setFontScale(1.08f);
        this.subtitle = new Label("The lawn is safe.", PvzSkin.get(), "medium_outline");
        this.subtitle.setAlignment(Align.center);
        this.subtitle.setFontScale(0.66f);
        this.retryButton = button("TRY AGAIN", "green", () -> run(this.retryAction));
        TextButton exit = button("EXIT", "purple", () -> run(this.exitAction));
        panel.add(this.title).growX().height(100f).row();
        panel.add(this.subtitle).growX().height(76f).padBottom(26f).row();
        panel.add(this.retryButton).growX().height(88f).padBottom(18f).row();
        panel.add(exit).growX().height(88f);
        addActor(panel);
        setVisible(false);
    }

    public void setActions(Runnable exit, Runnable retry) {
        this.exitAction = exit;
        this.retryAction = retry;
    }

    public void showResult(boolean lost) {
        this.loss = lost;
        this.title.setText(lost ? "LEVEL FAILED" : "VICTORY!");
        this.subtitle.setText(lost ? "The zombies broke through." : "The lawn is safe.");
        this.retryButton.setVisible(lost);
        setVisible(true);
    }

    public boolean isLoss() {
        return this.loss;
    }

    private TextButton button(String text, String style, Runnable action) {
        TextButton button = new TextButton(text, PvzSkin.get(), style);
        button.getLabel().setFontScale(0.80f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
