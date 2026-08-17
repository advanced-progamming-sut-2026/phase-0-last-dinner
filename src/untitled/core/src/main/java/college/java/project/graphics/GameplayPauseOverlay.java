package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

/** Mandatory Pause menu arranged to closely mirror the original PvZ2 pause card. */
public final class GameplayPauseOverlay extends Group {
    private static final String WINDOW_TOPPER = "IMAGE_UI_PAUSEMENU_WINDOWTOPPER";
    private static final String WINDOW_TOPPER_BACK = "IMAGE_UI_PAUSEMENU_WINDOWTOPPER_BACK_HALF";
    private static final String SUNFLOWER_TOPPER = "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER";
    private static final String BLANK_CARD = "IMAGE_UI_PAUSEMENU_BLANK_CARD";
    private static final String SLIDER_BOLT = "IMAGE_UI_PAUSEMENU_SLIDER_BOLT";

    private static final Color PANEL_BORDER = new Color(0.11f, 0.045f, 0.015f, 0.99f);
    private static final Color PANEL_BROWN = new Color(0.35f, 0.145f, 0.043f, 0.995f);
    private static final Color PANEL_BROWN_LIGHT = new Color(0.49f, 0.235f, 0.075f, 0.98f);
    private static final Color SLIDER_TRACK = new Color(0.16f, 0.060f, 0.018f, 0.92f);
    private static final Color SLIDER_FILL = new Color(0.38f, 0.68f, 0.10f, 0.98f);

    private final GameAssetManager assets;
    private boolean ownsAssets;
    private Runnable resumeAction;
    private Runnable restartAction;
    private Runnable saveAndExitAction;

    public GameplayPauseOverlay() {
        this(new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayPauseOverlay(GameAssetManager assets) {
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.assets = assets;
        setSize(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        setTouchable(Touchable.enabled);
        build();
        setVisible(false);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
    }

    public void setActions(Runnable resume, Runnable restart, Runnable saveAndExit) {
        this.resumeAction = resume;
        this.restartAction = restart;
        this.saveAndExitAction = saveAndExit;
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void build() {
        Image dim = new Image(PvzSkin.get().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.64f)));
        dim.setBounds(0f, 0f, getWidth(), getHeight());
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        final float panelX = 505f;
        final float panelY = 255f;
        final float panelW = 910f;
        final float panelH = 495f;

        // Original game puts the title above the grass/sunflower topper instead of inside the card.
        Label title = new Label("GAME PAUSED", PvzSkin.get(), "medium_outline");
        title.setAlignment(Align.center);
        title.setFontScale(1.08f);
        title.setColor(PvzVisualTheme.TEXT_CREAM);
        title.setBounds(panelX, panelY + panelH + 152f, panelW, 72f);
        title.setTouchable(Touchable.disabled);
        addActor(title);

        // A dark wooden edge makes the panel read as the same floating card used by PvZ2.
        Image frame = new Image(PvzSkin.get().newDrawable("white_pixel", PANEL_BORDER));
        frame.setBounds(panelX - 15f, panelY - 15f, panelW + 30f, panelH + 30f);
        frame.setTouchable(Touchable.disabled);
        addActor(frame);

        Stack panelStack = new Stack();
        panelStack.setBounds(panelX, panelY, panelW, panelH);
        Image panelBase = new Image(PvzSkin.get().newDrawable("white_pixel", PANEL_BROWN));
        panelStack.add(panelBase);
        Image cardTexture = resourceImage(BLANK_CARD);
        if (cardTexture != null) {
            cardTexture.setColor(1f, 0.88f, 0.66f, 0.10f);
            cardTexture.setScaling(Scaling.stretch);
            panelStack.add(cardTexture);
        }
        addActor(panelStack);

        // Audio rows are part of the recognizable original pause composition. They are display-only here;
        // phase two does not require changing the audio model, so they intentionally do not mutate game state.
        Table settings = new Table();
        settings.setBounds(panelX + 145f, panelY + 205f, panelW - 290f, 175f);
        settings.top();
        settings.add(settingLabel("MUSIC")).width(150f).right().padRight(24f);
        settings.add(displaySlider(0.78f)).width(400f).height(44f).left().row();
        settings.add(settingLabel("SOUND FX")).width(150f).right().padRight(24f).padTop(26f);
        settings.add(displaySlider(0.88f)).width(400f).height(44f).left().padTop(26f);
        addActor(settings);

        TextButton exit = button("SAVE & EXIT", "brown", () -> run(this.saveAndExitAction));
        TextButton restart = button("RESTART", "brown", () -> run(this.restartAction));
        TextButton resume = button("RESUME", "purple", () -> run(this.resumeAction));
        Table buttons = new Table();
        buttons.setBounds(panelX + 62f, panelY + 48f, panelW - 124f, 102f);
        buttons.add(exit).width(242f).height(82f).padRight(22f);
        buttons.add(restart).width(242f).height(82f).padRight(22f);
        buttons.add(resume).width(242f).height(82f);
        addActor(buttons);

        Image backTopper = resourceImage(WINDOW_TOPPER_BACK);
        if (backTopper != null) {
            backTopper.setBounds(panelX + 86f, panelY + panelH - 34f, panelW - 172f, 104f);
            addActor(backTopper);
        }
        Image topper = resourceImage(WINDOW_TOPPER);
        if (topper != null) {
            topper.setBounds(panelX + 82f, panelY + panelH - 52f, panelW - 164f, 148f);
            addActor(topper);
        }
        Image sunflower = resourceImage(SUNFLOWER_TOPPER);
        if (sunflower != null) {
            sunflower.setBounds(panelX + panelW / 2f - 80f, panelY + panelH + 4f, 160f, 140f);
            addActor(sunflower);
        }
    }

    private Label settingLabel(String text) {
        Label label = new Label(text, PvzSkin.get(), "medium_outline");
        label.setFontScale(0.66f);
        label.setAlignment(Align.right);
        label.setColor(PvzVisualTheme.TEXT_CREAM);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private Stack displaySlider(float fillAmount) {
        Stack slider = new Stack();
        Table trackLayer = new Table();
        trackLayer.setBackground(PvzSkin.get().newDrawable("white_pixel", SLIDER_TRACK));
        trackLayer.pad(7f);
        Table fillRow = new Table();
        fillRow.left();
        Image fill = new Image(PvzSkin.get().newDrawable("white_pixel", SLIDER_FILL));
        fillRow.add(fill).width(350f * Math.max(0f, Math.min(1f, fillAmount))).growY();
        fillRow.add().growX();
        trackLayer.add(fillRow).grow();
        slider.add(trackLayer);

        Image bolt = resourceImage(SLIDER_BOLT);
        if (bolt != null) {
            Table boltLayer = new Table();
            boltLayer.left();
            boltLayer.add().width(350f * Math.max(0f, Math.min(1f, fillAmount)) - 18f);
            boltLayer.add(bolt).size(44f);
            boltLayer.add().growX();
            slider.add(boltLayer);
        }
        slider.setTouchable(Touchable.disabled);
        return slider;
    }

    private TextButton button(String text, String style, Runnable action) {
        TextButton button = new TextButton(text, PvzSkin.get(), style);
        button.getLabel().setFontScale(0.70f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private Image resourceImage(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // Keep the pause menu functional if an optional atlas is absent.
        }
        return null;
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
