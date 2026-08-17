package college.java.project.graphics;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/** Shared top-right PvZ2 Almanac currency strip for full-screen detail views. */
final class AlmanacResourceStrip extends Table {
    private static final String CURRENCY_BG = "image_ui_generic_button_generic_currency_normal";
    private static final String MINT_ICON = "image_ui_generic_mint_icon_small";
    private static final String GEM_ICON = "image_ui_generic_gem_icon_small";
    private static final String COIN_ICON = "image_ui_generic_coin_icon_small";
    private static final String PLUS_UP = "image_ui_generic_greenbutton";
    private static final String PLUS_DOWN = "image_ui_generic_greenbutton_down";

    private final Skin skin;
    private final Label mintLabel;
    private final Label gemLabel;
    private final Label coinLabel;
    private final Button gemPlus;
    private final Button coinPlus;
    private Runnable addGemAction;
    private Runnable addCoinAction;

    AlmanacResourceStrip(Skin skin) {
        if (skin == null) {
            throw new IllegalArgumentException("Skin is required");
        }
        this.skin = skin;
        this.mintLabel = resourceLabel();
        this.gemLabel = resourceLabel();
        this.coinLabel = resourceLabel();
        this.gemPlus = plusButton(() -> run(this.addGemAction));
        this.coinPlus = plusButton(() -> run(this.addCoinAction));
        this.gemPlus.setVisible(false);
        this.coinPlus.setVisible(false);
        left();
        add(counter(MINT_ICON, this.mintLabel, null)).size(188f, 54f).padRight(12f);
        add(counter(GEM_ICON, this.gemLabel, this.gemPlus)).size(188f, 54f).padRight(12f);
        add(counter(COIN_ICON, this.coinLabel, this.coinPlus)).size(236f, 54f);
        setCounts(0, 0, 0);
    }

    void setCounts(int mints, int gems, int coins) {
        this.mintLabel.setText(formatNumber(mints));
        this.gemLabel.setText(formatNumber(gems));
        this.coinLabel.setText(formatNumber(coins));
    }

    void setDebugControls(
            boolean visible,
            Runnable addGemAction,
            Runnable addCoinAction
    ) {
        this.addGemAction = addGemAction;
        this.addCoinAction = addCoinAction;
        this.gemPlus.setVisible(visible && addGemAction != null);
        this.coinPlus.setVisible(visible && addCoinAction != null);
    }

    private Stack counter(String icon, Label label, Actor debugButton) {
        Stack stack = new Stack();
        stack.add(new Image(this.skin.getDrawable(CURRENCY_BG)));
        Table content = new Table();
        content.left();
        content.add(new Image(this.skin.getDrawable(icon))).size(49f).padLeft(4f).padRight(8f);
        content.add(label).growX().left().padRight(debugButton == null ? 35f : 50f);
        stack.add(content);
        if (debugButton != null) {
            Table plusLayer = new Table();
            plusLayer.right();
            plusLayer.add(debugButton).size(42f, 38f).padRight(2f);
            stack.add(plusLayer);
        }
        return stack;
    }

    private Button plusButton(Runnable action) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = this.skin.getDrawable(PLUS_UP);
        style.down = this.skin.getDrawable(PLUS_DOWN);
        Button button = new Button(style);
        Label plus = new Label("+", this.skin, "medium_outline");
        plus.setFontScale(0.72f);
        plus.setAlignment(Align.center);
        plus.setTouchable(Touchable.disabled);
        button.add(plus).grow();
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private Label resourceLabel() {
        Label label = new Label("0", this.skin, "medium_outline");
        label.setFontScale(0.82f);
        label.setAlignment(Align.left);
        return label;
    }

    private String formatNumber(int value) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0, value));
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
