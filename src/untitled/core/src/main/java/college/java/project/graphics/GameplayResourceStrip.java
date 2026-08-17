package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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

import java.util.Locale;

/** Mandatory in-game coin/gem display with debug-only currency controls. */
public final class GameplayResourceStrip extends Table {
    private static final String COIN_ICON = "IMAGE_UI_HUD_INGAME_COIN";
    private static final String GEM_ICON = "IMAGE_UI_HUD_INGAME_GEM";
    private static final String CURRENCY_BG = "IMAGE_UI_GENERIC_BUTTON_GENERIC_CURRENCY_NORMAL";
    private static final int DEBUG_COIN_INCREMENT = 1000;
    private static final int DEBUG_GEM_INCREMENT = 10;

    private final GameplaySeedBankDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final Label coinLabel;
    private final Label gemLabel;
    private final TextButton coinPlus;
    private final TextButton gemPlus;
    private int lastCoins = Integer.MIN_VALUE;
    private int lastGems = Integer.MIN_VALUE;
    private boolean lastDebug;

    public GameplayResourceStrip(GameplaySeedBankDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayResourceStrip(GameplaySeedBankDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay resource data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        this.coinLabel = valueLabel();
        this.gemLabel = valueLabel();
        this.coinPlus = plusButton(() -> {
            this.dataSource.cheatAddCoins(DEBUG_COIN_INCREMENT);
            refresh();
        });
        this.gemPlus = plusButton(() -> {
            this.dataSource.cheatAddGems(DEBUG_GEM_INCREMENT);
            refresh();
        });
        build();
        refresh();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        refresh();
    }

    public void refresh() {
        int coins = Math.max(0, this.dataSource.getCoinCount());
        int gems = Math.max(0, this.dataSource.getGemCount());
        boolean debug = this.dataSource.isDebugModeEnabled()
                && this.dataSource.supportsCurrencyCheats();
        if (coins != this.lastCoins) {
            if (this.lastCoins != Integer.MIN_VALUE && coins > this.lastCoins) {
                pulse(this.coinLabel);
            }
            this.lastCoins = coins;
            this.coinLabel.setText(format(coins));
        }
        if (gems != this.lastGems) {
            if (this.lastGems != Integer.MIN_VALUE && gems > this.lastGems) {
                pulse(this.gemLabel);
            }
            this.lastGems = gems;
            this.gemLabel.setText(format(gems));
        }
        this.lastDebug = debug;
        this.coinPlus.setVisible(debug);
        this.gemPlus.setVisible(debug);
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void build() {
        pad(3f, 3f, 3f, 3f);
        add(currency(COIN_ICON, this.coinLabel, this.coinPlus)).size(178f, 54f).padRight(8f);
        add(currency(GEM_ICON, this.gemLabel, this.gemPlus)).size(166f, 54f);
    }

    private Table currency(String resourceId, Label label, TextButton plus) {
        Table table = new Table();
        Drawable background = resourceDrawable(CURRENCY_BG);
        if (background != null) {
            table.setBackground(background);
        }
        Image icon = icon(resourceId);
        if (icon != null) {
            table.add(icon).size(43f).padLeft(5f).padRight(4f);
        }
        table.add(label).width(90f).left();
        table.add(plus).size(39f, 36f).padRight(4f);
        return table;
    }

    private Image icon(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private TextButton plusButton(Runnable action) {
        TextButton button = new TextButton("+", PvzSkin.get(), "green");
        button.getLabel().setFontScale(0.64f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private Label valueLabel() {
        Label label = new Label("0", PvzSkin.get(), "medium_outline");
        label.setFontScale(0.61f);
        label.setColor(PvzVisualTheme.TEXT_CREAM);
        label.setAlignment(Align.left);
        return label;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // Counter text remains usable even when an optional icon atlas is absent.
        }
        return null;
    }

    private void pulse(Label label) {
        label.clearActions();
        label.setOrigin(label.getWidth() / 2f, label.getHeight() / 2f);
        label.addAction(Actions.sequence(
                Actions.scaleTo(1.14f, 1.14f, 0.06f),
                Actions.scaleTo(1f, 1f, 0.10f)
        ));
    }

    private String format(int value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }
}
