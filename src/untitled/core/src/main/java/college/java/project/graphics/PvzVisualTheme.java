package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;

final class PvzVisualTheme {
    static final float WORLD_WIDTH = 1920f;
    static final float WORLD_HEIGHT = 1080f;
    static final Color ALMANAC_DARK = new Color(0.055f, 0.035f, 0.020f, 1f);
    static final Color ALMANAC_WOOD = new Color(0.29f, 0.105f, 0.026f, 1f);
    static final Color ALMANAC_INNER = new Color(0.20f, 0.067f, 0.018f, 0.96f);
    static final Color ALMANAC_GOLD = new Color(0.83f, 0.52f, 0.18f, 1f);
    static final Color TEXT_CREAM = new Color(0.98f, 0.92f, 0.73f, 1f);
    static final Color TEXT_MUTED = new Color(0.82f, 0.78f, 0.66f, 1f);
    static final Color CHOOSER_NIGHT = new Color(0.035f, 0.065f, 0.105f, 1f);
    static final Color CHOOSER_WOOD = new Color(0.31f, 0.105f, 0.022f, 0.98f);
    static final Color CHOOSER_INNER = new Color(0.19f, 0.055f, 0.014f, 0.96f);
    static final Color DISABLED_TINT = new Color(0.54f, 0.56f, 0.56f, 0.82f);
    static final Color SELECT_GLOW = new Color(1f, 0.92f, 0.30f, 1f);

    private PvzVisualTheme() {
    }

    static Drawable resourceDrawable(GameAssetManager assets, Skin skin, String resourceId) {
        if (resourceId == null) {
            return null;
        }
        if (assets != null) {
            try {
                TextureBank bank = assets.getTextureBank();
                TextureRegion region = bank == null ? null : bank.region(resourceId);
                if (region != null) {
                    return new TextureRegionDrawable(region);
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (skin != null && skin.has(resourceId, Drawable.class)) {
            return skin.getDrawable(resourceId);
        }
        String normalized = resourceId.toLowerCase(java.util.Locale.ROOT);
        if (skin != null && skin.has(normalized, Drawable.class)) {
            return skin.getDrawable(normalized);
        }
        return null;
    }

    static Image resourceImage(GameAssetManager assets, Skin skin, String resourceId, Scaling scaling) {
        Drawable drawable = resourceDrawable(assets, skin, resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(scaling == null ? Scaling.stretch : scaling);
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
