package college.java.project.graphics.minigame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import college.java.project.graphics.GameAssetManager;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.Locale;
import java.util.function.IntSupplier;

public final class MiniGameSunCounter extends Table {
    private static final String SUN_ICON = "IMAGE_UI_HUD_INGAME_SUN";
    private static final String HUD_BACKGROUND = "IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE";
    private static final Color FALLBACK_BACKGROUND = new Color(0.08f, 0.06f, 0.03f, 0.92f);

    private final GameAssetManager assets;
    private final IntSupplier amountSupplier;
    private final Label amountLabel;

    private int lastAmount = Integer.MIN_VALUE;

    public MiniGameSunCounter(GameAssetManager assets, IntSupplier amountSupplier) {
        if (assets == null || amountSupplier == null)
            throw new IllegalArgumentException("Sun counter dependencies are required.");

        this.assets = assets;
        this.amountSupplier = amountSupplier;

        Skin skin = PvzSkin.get();
        Drawable background = resourceDrawable(HUD_BACKGROUND);

        if (background == null)
            background = skin.newDrawable("white_pixel", FALLBACK_BACKGROUND);

        setBackground(background);
        setTouchable(Touchable.disabled);
        pad(4f, 12f, 4f, 5f);

        Image sunImage = createSunImage();
        if (sunImage != null)
            add(sunImage).size(78f).padRight(3f);

        this.amountLabel = new Label("0", skin, "medium_outline");
        this.amountLabel.setAlignment(Align.center);
        this.amountLabel.setFontScale(1.05f);
        this.amountLabel.setColor(Color.WHITE);

        add(this.amountLabel).grow();
        refresh();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        refresh();
    }

    public void refresh() {
        int amount = Math.max(0, this.amountSupplier.getAsInt());

        if (amount == this.lastAmount)
            return;

        if (this.lastAmount != Integer.MIN_VALUE)
            pulse();

        this.lastAmount = amount;
        this.amountLabel.setText(String.format(Locale.ROOT, "%,d", amount));
    }

    private Image createSunImage() {
        Drawable drawable = resourceDrawable(SUN_ICON);

        if (drawable == null)
            return null;

        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();

            if (bank != null && bank.region(resourceId) != null)
                return new TextureRegionDrawable(bank.region(resourceId));
        } catch (RuntimeException ignored) {
            return null;
        }

        return null;
    }

    private void pulse() {
        this.amountLabel.clearActions();
        this.amountLabel.setOrigin(this.amountLabel.getWidth() / 2f, this.amountLabel.getHeight() / 2f);

        this.amountLabel.addAction(Actions.sequence(
            Actions.scaleTo(1.18f, 1.18f, 0.07f),
            Actions.scaleTo(1f, 1f, 0.11f)
        ));
    }
}
