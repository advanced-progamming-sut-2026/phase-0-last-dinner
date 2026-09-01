package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import view.GameSettings;

import java.util.function.Consumer;

/** Mandatory Pause menu arranged to closely mirror the original PvZ2 pause card. */
public final class GameplayPauseOverlay extends Group {
    private static final String WINDOW_TOPPER = "IMAGE_UI_PAUSEMENU_WINDOWTOPPER";
    private static final String WINDOW_TOPPER_BACK = "IMAGE_UI_PAUSEMENU_WINDOWTOPPER_BACK_HALF";
    private static final String SUNFLOWER_TOPPER = "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER";
    private static final String BLANK_CARD = "IMAGE_UI_PAUSEMENU_BLANK_CARD";
    private static final String SLIDER_BOLT = "IMAGE_UI_PAUSEMENU_SLIDER_BOLT";

    private static final Color PANEL_BORDER = new Color(0.16f, 0.070f, 0.022f, 0.99f);
    private static final Color PANEL_BROWN = new Color(0.714f, 0.467f, 0.269f, 0.995f);
    private static final Color PANEL_BROWN_LIGHT = new Color(0.49f, 0.235f, 0.075f, 0.98f);
    private static final Color SLIDER_TRACK = new Color(0.13f, 0.080f, 0.055f, 0.94f);
    private static final Color SLIDER_FILL = new Color(0.03f, 0.82f, 0.03f, 0.99f);

    private final GameAssetManager assets;
    private boolean ownsAssets;
    private Runnable resumeAction;
    private Runnable restartAction;
    private Runnable saveAndExitAction;
    private float musicVolume = GameSettings.getMusicVolume();
    private float soundFxVolume = GameSettings.getSoundFxVolume();
    private VolumeSlider musicSlider;
    private VolumeSlider soundFxSlider;
    private VolumeListener volumeListener;

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

    public float getMusicVolume() {
        return this.musicVolume;
    }

    public float getSoundFxVolume() {
        return this.soundFxVolume;
    }

    public void setMusicVolume(float volume) {
        if (this.musicSlider != null) {
            this.musicSlider.setValue(volume);
        }
    }

    public void setSoundFxVolume(float volume) {
        if (this.soundFxSlider != null) {
            this.soundFxSlider.setValue(volume);
        }
    }

    public void setVolumeListener(VolumeListener listener) {
        this.volumeListener = listener;
        notifyVolumeChanged();
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void build() {
        Image dim = new Image(PvzSkin.get().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.54f)));
        dim.setBounds(0f, 0f, getWidth(), getHeight());
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        // Geometry is normalized from the supplied original PvZ2 pause screenshot.
        final float panelX = 380f;
        final float panelY = 220f;
        final float panelW = 1160f;
        final float panelH = 455f;

        Label title = new Label("GAME PAUSED", PvzSkin.get(), "medium_outline");
        title.setAlignment(Align.center);
        title.setFontScale(2.0f);
        title.setColor(Color.WHITE);
        title.setBounds(panelX, 814f, panelW, 78f);
        title.setTouchable(Touchable.disabled);
        addActor(title);

        // A narrow dark outer rim plus a warm inner rim reproduces the original card edge.
        Image outerFrame = new Image(roundedPanelDrawable(PANEL_BORDER));
        outerFrame.setBounds(panelX - 13f, panelY - 13f, panelW + 26f, panelH + 26f);
        outerFrame.setTouchable(Touchable.disabled);
        addActor(outerFrame);

        Image innerFrame = new Image(roundedPanelDrawable(PANEL_BROWN_LIGHT));
        innerFrame.setBounds(panelX - 6f, panelY - 6f, panelW + 12f, panelH + 12f);
        innerFrame.setTouchable(Touchable.disabled);
        addActor(innerFrame);

        Stack panelStack = new Stack();
        panelStack.setBounds(panelX, panelY, panelW, panelH);
        Image panelBase = new Image(roundedPanelDrawable(PANEL_BROWN));
        panelStack.add(panelBase);
        Image cardTexture = resourceImage(BLANK_CARD);
        if (cardTexture != null) {
            cardTexture.setColor(1f, 0.93f, 0.82f, 0.08f);
            cardTexture.setScaling(Scaling.stretch);
            panelStack.add(cardTexture);
        }
        addActor(panelStack);

        Label music = settingLabel("Music");
        music.setBounds(685f, 470f, 145f, 48f);
        addActor(music);
        this.musicSlider = new VolumeSlider(this.musicVolume, value -> {
            this.musicVolume = value;
            notifyVolumeChanged();
        });
        this.musicSlider.setBounds(860f, 480f, 400f, 36f);
        addActor(this.musicSlider);

        Label soundFx = settingLabel("Sound FX");
        soundFx.setBounds(650f, 390f, 180f, 48f);
        addActor(soundFx);
        this.soundFxSlider = new VolumeSlider(this.soundFxVolume, value -> {
            this.soundFxVolume = value;
            notifyVolumeChanged();
        });
        this.soundFxSlider.setBounds(860f, 400f, 400f, 36f);
        addActor(this.soundFxSlider);

        TextButton exit = button("EXIT TO MAP", "brown", () -> run(this.saveAndExitAction));
        exit.setBounds(455f, 225f, 275f, 86f);
        addActor(exit);

        TextButton restart = button("RESTART", "brown", () -> run(this.restartAction));
        restart.setBounds(825f, 225f, 275f, 86f);
        addActor(restart);

        TextButton resume = button("RESUME", "purple", () -> run(this.resumeAction));
        resume.setBounds(1195f, 225f, 275f, 86f);
        addActor(resume);

        Image topper = resourceImage(WINDOW_TOPPER);
        if (topper != null) {
            topper.setBounds(571f, 585f, 778f, 154f);
            addActor(topper);
        }
        Image sunflower = resourceImage(SUNFLOWER_TOPPER);
        if (sunflower != null) {
            sunflower.setBounds(863f, 662f, 166f, 138f);
            addActor(sunflower);
        }
    }

    private Label settingLabel(String text) {
        Label label = new Label(text, PvzSkin.get(), "medium_outline");
        label.setFontScale(1.65f);
        label.setAlignment(Align.right);
        label.setColor(Color.WHITE);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private TextButton button(String text, String style, Runnable action) {
        TextButton button = new TextButton(text, PvzSkin.get(), style);
        button.getLabel().setFontScale(1.70f);
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


    private Drawable roundedPanelDrawable(Color tint) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            TextureRegion region = bank == null ? null : bank.region(BLANK_CARD);
            if (region != null) {
                NinePatch patch = new NinePatch(region, 16, 16, 16, 16);
                patch.scale(2f, 2f);
                return new NinePatchDrawable(patch).tint(tint);
            }
        } catch (RuntimeException ignored) {
            // Fall through to the local solid-color fallback.
        }
        return PvzSkin.get().newDrawable("white_pixel", tint);
    }

    private Drawable roundedCardDrawable(Color tint) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            TextureRegion region = bank == null ? null : bank.region(BLANK_CARD);
            if (region != null) {
                return new NinePatchDrawable(new NinePatch(region, 12, 12, 12, 12)).tint(tint);
            }
        } catch (RuntimeException ignored) {
            // Fall through to the same local solid-color fallback used by the pause menu.
        }
        return PvzSkin.get().newDrawable("white_pixel", tint);
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

    private void notifyVolumeChanged() {
        if (this.volumeListener != null) {
            this.volumeListener.onVolumeChanged(this.musicVolume, this.soundFxVolume);
        }
    }

    public interface VolumeListener {
        void onVolumeChanged(float musicVolume, float soundFxVolume);
    }

    private final class VolumeSlider extends Group {
        private static final float INSET = 5f;
        private static final float KNOB_SIZE = 62f;

        private final Image track;
        private final Image fill;
        private final Image knob;
        private final Consumer<Float> changeListener;
        private float value;

        private VolumeSlider(float initialValue, Consumer<Float> changeListener) {
            this.changeListener = changeListener;
            this.track = new Image(roundedCardDrawable(SLIDER_TRACK));
            this.track.setTouchable(Touchable.disabled);
            addActor(this.track);

            this.fill = new Image(roundedCardDrawable(SLIDER_FILL));
            this.fill.setTouchable(Touchable.disabled);
            addActor(this.fill);

            Image sliderKnob = resourceImage(SLIDER_BOLT);
            if (sliderKnob == null) {
                sliderKnob = new Image(PvzSkin.get().newDrawable("white_pixel", Color.LIGHT_GRAY));
                sliderKnob.setTouchable(Touchable.disabled);
            }
            this.knob = sliderKnob;
            addActor(this.knob);

            setTouchable(Touchable.enabled);
            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    updateFromPosition(x);
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    updateFromPosition(x);
                }
            });
            setValue(initialValue);
        }

        private void setValue(float newValue) {
            float clamped = Math.max(0f, Math.min(1f, newValue));
            if (Math.abs(this.value - clamped) < 0.0001f) {
                layoutSlider();
                return;
            }
            this.value = clamped;
            layoutSlider();
            this.changeListener.accept(this.value);
        }

        private void updateFromPosition(float x) {
            float usableWidth = Math.max(1f, getWidth() - INSET * 2f);
            setValue((x - INSET) / usableWidth);
        }

        @Override
        protected void sizeChanged() {
            super.sizeChanged();
            layoutSlider();
        }

        private void layoutSlider() {
            float width = getWidth();
            float height = getHeight();
            float usableWidth = Math.max(0f, width - INSET * 2f);
            this.track.setBounds(0f, 0f, width, height);
            this.fill.setBounds(INSET, INSET, usableWidth * this.value, Math.max(0f, height - INSET * 2f));
            float knobX = INSET + usableWidth * this.value - KNOB_SIZE / 2f;
            this.knob.setBounds(knobX, (height - KNOB_SIZE) / 2f, KNOB_SIZE, KNOB_SIZE);
        }
    }
}
