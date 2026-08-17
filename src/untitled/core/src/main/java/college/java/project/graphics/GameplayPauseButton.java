package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

/** Original PvZ2 pause button used by the gameplay HUD. */
public final class GameplayPauseButton extends ImageButton {
    private static final String UP = "IMAGE_UI_HUD_INGAME_PAUSE_BUTTON";
    private static final String DOWN = "IMAGE_UI_HUD_INGAME_PAUSE_BUTTON_DOWN";

    private final GameAssetManager assets;
    private boolean ownsAssets;

    public GameplayPauseButton(Runnable action) {
        this(new GameAssetManager(), action);
        this.ownsAssets = true;
    }

    GameplayPauseButton(GameAssetManager assets, Runnable action) {
        super(style(requireAssets(assets)));
        this.assets = assets;
        getImage().setScaling(Scaling.fit);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
        CollectionUiAnimator.installHoverScale(this);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private static GameAssetManager requireAssets(GameAssetManager assets) {
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        return assets;
    }

    private static ImageButtonStyle style(GameAssetManager assets) {
        Drawable up = drawable(assets, UP);
        Drawable down = drawable(assets, DOWN);
        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = up;
        style.imageDown = down == null ? up : down;
        return style;
    }

    private static Drawable drawable(GameAssetManager assets, String resourceId) {
        try {
            TextureBank bank = assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // Fall through to skin fallback.
        }
        return PvzSkin.get().getDrawable("white_pixel");
    }
}
