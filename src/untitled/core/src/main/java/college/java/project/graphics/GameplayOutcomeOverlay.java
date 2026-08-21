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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import model.chapters.ChapterType;

/** Mandatory win/loss dialog with Exit and loss-only Retry. */
public final class GameplayOutcomeOverlay extends Group {
    private static final String BRAIN = "IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY";
    private static final Color LOSS_GREEN = new Color(0.02f, 0.83f, 0.05f, 1f);

    private final GameAssetManager assets;
    private final String victoryTrophyResource;
    private final Group victoryContent;
    private final Group lossContent;
    private final Label title;
    private final Label subtitle;
    private final TextButton retryButton;
    private final Image victoryTrophy;
    private Runnable exitAction;
    private Runnable retryAction;
    private boolean loss;

    public GameplayOutcomeOverlay() {
        this(new GameAssetManager(), ChapterType.ANCIENT_EGYPT);
    }

    GameplayOutcomeOverlay(GameAssetManager assets) {
        this(assets, ChapterType.ANCIENT_EGYPT);
    }

    GameplayOutcomeOverlay(GameAssetManager assets, ChapterType chapterType) {
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.assets = assets;
        this.victoryTrophyResource = trophyResource(chapterType);
        setSize(GameplayWorldLayout.STAGE_WIDTH, GameplayWorldLayout.STAGE_HEIGHT);
        setTouchable(Touchable.enabled);
        Image dim = new Image(PvzSkin.get().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.68f)));
        dim.setBounds(0f, 0f, getWidth(), getHeight());
        dim.setTouchable(Touchable.enabled);
        addActor(dim);

        this.victoryContent = new Group();
        this.victoryContent.setSize(getWidth(), getHeight());

        Image footerShade = solid(new Color(0f, 0f, 0f, 0.72f));
        footerShade.setBounds(250f, 60f, 1420f, 165f);
        this.victoryContent.addActor(footerShade);

        Image shadow = solid(new Color(0f, 0f, 0f, 0.62f));
        shadow.setBounds(470f, 214f, 980f, 676f);
        this.victoryContent.addActor(shadow);

        Image frame = solid(new Color(0.26f, 0.27f, 0.20f, 1f));
        frame.setBounds(458f, 232f, 1004f, 666f);
        this.victoryContent.addActor(frame);

        Image body = solid(new Color(0.95f, 0.91f, 0.75f, 1f));
        body.setBounds(470f, 244f, 980f, 642f);
        this.victoryContent.addActor(body);

        Image header = solid(new Color(0.96f, 0.72f, 0.05f, 1f));
        header.setBounds(470f, 742f, 980f, 144f);
        this.victoryContent.addActor(header);

        Image headerHighlight = solid(new Color(1f, 0.94f, 0.36f, 1f));
        headerHighlight.setBounds(470f, 862f, 980f, 24f);
        this.victoryContent.addActor(headerHighlight);

        Image headerShade = solid(new Color(0.78f, 0.53f, 0.02f, 1f));
        headerShade.setBounds(470f, 742f, 980f, 16f);
        this.victoryContent.addActor(headerShade);

        this.title = new Label("LEVEL COMPLETE!", PvzSkin.get(), "medium_outline");
        this.title.setAlignment(Align.center);
        this.title.setFontScale(2.52f, 2.22f);
        this.title.setBounds(520f, 756f, 880f, 110f);
        this.victoryContent.addActor(this.title);

        this.victoryTrophy = new Image();
        Drawable trophyDrawable = resourceDrawable(this.victoryTrophyResource);
        if (trophyDrawable != null) {
            this.victoryTrophy.setDrawable(trophyDrawable);
        }
        this.victoryTrophy.setScaling(Scaling.fit);
        this.victoryTrophy.setBounds(745f, 340f, 430f, 390f);
        this.victoryTrophy.setTouchable(Touchable.disabled);
        this.victoryContent.addActor(this.victoryTrophy);

        this.subtitle = new Label("The lawn is safe.", PvzSkin.get(), "medium_outline");
        this.subtitle.setAlignment(Align.center);
        this.subtitle.setFontScale(1.42f, 1.24f);
        this.subtitle.setColor(new Color(0.28f, 0.23f, 0.03f, 1f));
        this.subtitle.setBounds(560f, 252f, 800f, 80f);
        this.victoryContent.addActor(this.subtitle);

        this.retryButton = button("TRY AGAIN", "green", () -> run(this.retryAction));
        this.retryButton.setBounds(710f, 92f, 240f, 122f);
        this.victoryContent.addActor(this.retryButton);

        TextButton exit = button("CONTINUE", "brown", () -> run(this.exitAction));
        exit.setBounds(760f, 92f, 400f, 122f);
        exit.getLabel().setFontScale(1.58f);
        this.victoryContent.addActor(exit);
        addActor(this.victoryContent);

        this.lossContent = buildLossContent();
        this.lossContent.setVisible(false);
        addActor(this.lossContent);
        setVisible(false);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        if (this.victoryTrophy.getDrawable() == null) {
            Drawable trophyDrawable = resourceDrawable(this.victoryTrophyResource);
            if (trophyDrawable != null) {
                this.victoryTrophy.setDrawable(trophyDrawable);
            }
        }
    }

    public void setActions(Runnable exit, Runnable retry) {
        this.exitAction = exit;
        this.retryAction = retry;
    }

    public void showResult(boolean lost) {
        this.loss = lost;
        this.title.setText(lost ? "LEVEL FAILED" : "LEVEL COMPLETE!");
        this.subtitle.setText(lost ? "The zombies broke through." : "The lawn is safe.");
        this.retryButton.setVisible(lost);
        this.victoryContent.setVisible(!lost);
        this.lossContent.setVisible(lost);
        setVisible(true);
    }

    public boolean isLoss() {
        return this.loss;
    }

    private Group buildLossContent() {
        Group content = new Group();
        content.setSize(getWidth(), getHeight());

        Image black = new Image(PvzSkin.get().newDrawable("white_pixel", Color.BLACK));
        black.setBounds(0f, 0f, getWidth(), getHeight());
        black.setTouchable(Touchable.enabled);
        content.addActor(black);

        Label messageOutline = new Label(
                "THE ZOMBIES\nATE YOUR\nBRAINS!",
                PvzSkin.get(),
                "medium_outline"
        );
        messageOutline.setAlignment(Align.center);
        messageOutline.setFontScale(5.42f, 4.08f);
        messageOutline.setColor(Color.LIGHT_GRAY);
        messageOutline.setBounds(380f, 635f, 1160f, 390f);
        messageOutline.setTouchable(Touchable.disabled);
        content.addActor(messageOutline);

        Label message = new Label("THE ZOMBIES\nATE YOUR\nBRAINS!", PvzSkin.get(), "medium_outline");
        message.setAlignment(Align.center);
        message.setFontScale(5.28f, 3.94f);
        message.setColor(LOSS_GREEN);
        message.setBounds(380f, 635f, 1160f, 390f);
        message.setTouchable(Touchable.disabled);
        content.addActor(message);

        Drawable brainDrawable = resourceDrawable(BRAIN);
        if (brainDrawable != null) {
            Image brain = new Image(brainDrawable);
            brain.setScaling(Scaling.stretch);
            brain.setBounds(565f, 120f, 790f, 450f);
            brain.setTouchable(Touchable.disabled);
            content.addActor(brain);
        }

        TextButton exitToMap = button("EXIT TO MAP", "brown", () -> run(this.exitAction));
        TextButton retry = button("RETRY", "purple", () -> run(this.retryAction));
        exitToMap.getLabel().setFontScale(2.0f);
        retry.getLabel().setFontScale(2.0f);
        Table actions = new Table();
        actions.setBounds(655f, 75f, 610f, 90f);
        actions.add(exitToMap).width(255f).height(82f).padRight(100f);
        actions.add(retry).width(255f).height(82f);
        content.addActor(actions);
        return content;
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

    private Image solid(Color color) {
        return new Image(PvzSkin.get().newDrawable("white_pixel", color));
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static String trophyResource(ChapterType chapterType) {
        if (chapterType == null) {
            return "IMAGE_ENDLEVEL_EGYPT_TROPHY";
        }
        return switch (chapterType) {
            case ICE_CAVES -> "IMAGE_ENDLEVEL_ICEAGE_TROPHY";
            case BIG_WAVE_BEACH -> "IMAGE_ENDLEVEL_BEACH_TROPHY";
            case MEDIEVAL -> "IMAGE_ENDLEVEL_DARK_TROPHY";
            case ANCIENT_EGYPT -> "IMAGE_ENDLEVEL_EGYPT_TROPHY";
        };
    }
}
