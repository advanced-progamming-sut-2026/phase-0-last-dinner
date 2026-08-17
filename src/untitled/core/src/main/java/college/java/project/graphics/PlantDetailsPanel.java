package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.collection.PlantCollectionState;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import pvz.libpvz.textures.TextureBank;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.stream.Collectors;

/** Full-screen PvZ2 Almanac-style plant detail view. */
public final class PlantDetailsPanel extends Group {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private static final String BACK_UP = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_DOWN = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String NEXT_UP = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_NEXT";
    private static final String NEXT_DOWN = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_NEXT_DOWN";
    private static final String PREVIOUS_UP = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_PREVIOUS";
    private static final String PREVIOUS_DOWN = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_PREVIOUS_DOWN";
    private static final String SUN_BACKGROUND = "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SUNCOST_LAYER_0";
    private static final String SUN_GLYPH = "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SUNCOST_LAYER_1";
    private static final String TOUGHNESS_ICON = "IMAGE_UI_ALMANAC_PLANTS_TOUGHNESS_ICON";
    private static final String DAMAGE_ICON = "IMAGE_UI_ALMANAC_PLANTS_DAMAGE_ICON";
    private static final String RECHARGE_ICON = "IMAGE_UI_ALMANAC_PLANTS_RECHARGE_ICON";
    private static final String VARIABLE_ICON = "IMAGE_UI_ALMANAC_PLANTS_VARIABLE_ICON";
    private static final String SPECIAL_ICON = "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SPECIAL";
    private static final String PLANT_FOOD_ICON = "IMAGE_UI_ALMANAC_PLANT_FOOD_STAT_ICON";
    private static final String FAMILY_BANNER = "IMAGE_UI_PACKETS_MINTFAM_BANNER";
    private static final String XP_ICON = "IMAGE_UI_GENERIC_XP_PROGRESS_ICON_YELLOW";
    private static final String PREVIEW_BACKGROUND = "IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_MODERN";
    private static final String GRADIENT_TOP = "IMAGE_UI_ALMANAC_GRADIENT_TOP";
    private static final String GRADIENT_BOTTOM = "IMAGE_UI_ALMANAC_GRADIENT_BOTTOM";
    private static final String STAT_BACKGROUND = "IMAGE_UI_ALMANAC_ALMANAC_STAT_BACKGROUND";
    private static final String EDGE_GRADIENT = "IMAGE_UI_ALMANAC_EDGE_GRADIENT";

    private static final float PREVIEW_WIDTH = 524f;
    private static final float PREVIEW_HEIGHT = 462f;
    private static final float PREVIEW_CENTER_X = PREVIEW_WIDTH / 2f;
    private static final float PREVIEW_CENTER_Y = PREVIEW_HEIGHT / 2f;
    private static final float MAX_STATIC_PLANT_SCALE = 2.15f;

    private static final Color TOP_BLUE = new Color(0.006f, 0.020f, 0.075f, 1f);
    private static final Color MID_BLUE = new Color(0.055f, 0.285f, 0.79f, 1f);
    private static final Color BOTTOM_BLUE = new Color(0.004f, 0.025f, 0.10f, 1f);
    private static final Color EDGE_SHADE = new Color(0f, 0f, 0f, 0.27f);
    private static final Color PREVIEW_BORDER = new Color(0.13f, 0.10f, 0.075f, 1f);
    private static final Color PREVIEW_FALLBACK = new Color(0.24f, 0.43f, 0.18f, 1f);
    private static final Color LOCKED_COLOR = new Color(0.52f, 0.52f, 0.52f, 1f);
    private static final Color INFO_YELLOW = new Color(0.73f, 0.43f, 0.03f, 1f);
    private static final Color DETAIL_BASE = new Color(0.018f, 0.055f, 0.145f, 1f);
    private static final Color STAT_TEXT = new Color(0.18f, 0.11f, 0.045f, 1f);
    private static final Color STAT_HEADING = new Color(0.34f, 0.23f, 0.10f, 1f);
    private static final Color LOCKED_TEXT = new Color(1f, 0.60f, 0.30f, 1f);
    private static final int UNLOCK_PRICE = 2000;

    private final Skin skin;
    private final TextureBank textureBank;
    private final GameAssetManager assets;
    private final PamAnimationCatalog animationCatalog;
    private final Group previewVisual;
    private final Group familyBadge;
    private final Label nameLabel;
    private final Label levelLabel;
    private final Label seedLabel;
    private final Label lockLabel;
    private final Label sunCostValue;
    private final Label healthValue;
    private final Label actionValue;
    private final Label rechargeValue;
    private final Label damageValue;
    private final Label specialValue;
    private final Label familyValue;
    private final Label plantFoodValue;
    private final Label abilityValue;
    private final Label levelEffectsValue;
    private final ProgressBar seedBar;
    private final AlmanacResourceStrip resourceStrip;
    private final Button collectionActionButton;
    private final Label collectionActionLabel;

    private Runnable backAction;
    private Runnable previousAction;
    private Runnable nextAction;
    private Runnable purchaseAction;
    private Runnable upgradeAction;
    private PlantCollectionState currentState;
    private int currentCoins;

    public PlantDetailsPanel(Skin skin, GameAssetManager assets, PamAnimationCatalog animationCatalog) {
        if (skin == null || assets == null || animationCatalog == null) {
            throw new IllegalArgumentException("Plant details resources are required");
        }
        this.skin = skin;
        this.assets = assets;
        this.textureBank = assets.getTextureBank();
        this.animationCatalog = animationCatalog;
        this.previewVisual = new Group();
        this.familyBadge = new Group();
        this.nameLabel = label("", 1.34f, Align.center);
        this.levelLabel = label("", 0.72f, Align.center);
        this.seedLabel = label("", 0.54f, Align.center);
        this.lockLabel = label("", 0.64f, Align.center);
        this.sunCostValue = label("", 0.72f, Align.left);
        this.healthValue = label("", 0.72f, Align.left);
        this.actionValue = label("", 0.68f, Align.left);
        this.rechargeValue = label("", 0.72f, Align.left);
        this.damageValue = label("", 0.68f, Align.left);
        this.specialValue = label("", 0.54f, Align.left);
        this.familyValue = label("", 0.66f, Align.left);
        this.plantFoodValue = label("", 0.50f, Align.topLeft);
        this.abilityValue = label("", 0.56f, Align.topLeft);
        this.levelEffectsValue = label("", 0.46f, Align.topLeft);
        this.seedBar = createSeedBar();
        this.resourceStrip = new AlmanacResourceStrip(this.skin);
        this.collectionActionLabel = new Label("FIND MORE", this.skin, "medium_outline");
        this.collectionActionButton = createCollectionActionButton();

        setSize(WIDTH, HEIGHT);
        setOrigin(WIDTH / 2f, HEIGHT / 2f);
        addGradientBackground();
        addPreviewPanel();
        addStats();
        addResourceStrip();
        addCollectionActionButton();
        addNavigation();
        setVisible(false);
    }

    public void setActions(Runnable backAction, Runnable previousAction, Runnable nextAction) {
        this.backAction = backAction;
        this.previousAction = previousAction;
        this.nextAction = nextAction;
    }

    public void setResources(int mints, int gems, int coins) {
        this.currentCoins = Math.max(0, coins);
        this.resourceStrip.setCounts(mints, gems, coins);
        if (this.currentState != null) {
            updateCollectionAction(this.currentState);
        }
    }

    public void setDebugCurrencyControls(
            boolean visible,
            Runnable addGemAction,
            Runnable addCoinAction
    ) {
        this.resourceStrip.setDebugControls(visible, addGemAction, addCoinAction);
    }

    public void setCollectionActions(Runnable purchaseAction, Runnable upgradeAction) {
        this.purchaseAction = purchaseAction;
        this.upgradeAction = upgradeAction;
    }

    public void showPlant(PlantCollectionState state) {
        if (state == null) {
            return;
        }
        this.currentState = state;
        this.nameLabel.setText(state.getName());
        this.levelLabel.setText("Level " + state.getCurrentLevel());
        this.lockLabel.setText(state.isUnlocked() ? "" : "LOCKED");
        this.sunCostValue.setText(Integer.toString(state.getSunCost()));
        this.healthValue.setText(Integer.toString(state.getBaseHealth()));
        this.actionValue.setText(formatSeconds(state.getActionIntervalSeconds()));
        this.rechargeValue.setText(formatSeconds(state.getRechargeSeconds()));
        this.damageValue.setText(safeText(state.getDamageExpression(), "0"));
        this.specialValue.setText(formatTags(state));
        this.familyValue.setText(familyName(state));
        this.plantFoodValue.setText(plantFoodText(state));
        this.abilityValue.setText(abilityText(state));
        this.levelEffectsValue.setText(formatLevelEffects(state));
        updateSeedProgress(state);
        updateFamilyBadge(state);
        updatePlantVisual(state);
        updateCollectionAction(state);
        playOpenAnimation();
    }

    private void playOpenAnimation() {
        clearActions();
        setVisible(true);
        getColor().a = 0f;
        setScale(0.992f);
        addAction(Actions.parallel(Actions.fadeIn(0.15f), Actions.scaleTo(1f, 1f, 0.18f)));
    }

    private void addGradientBackground() {
        Image base = colorImage(DETAIL_BASE);
        base.setBounds(0f, 0f, WIDTH, HEIGHT);
        addActor(base);

        TextureRegion topRegion = this.textureBank.region(GRADIENT_TOP);
        TextureRegion bottomRegion = this.textureBank.region(GRADIENT_BOTTOM);
        if (topRegion != null && bottomRegion != null) {
            Image bottom = new Image(new TextureRegionDrawable(bottomRegion));
            bottom.setScaling(Scaling.stretch);
            bottom.setColor(0.38f, 0.68f, 1f, 0.88f);
            bottom.setBounds(0f, 0f, WIDTH, HEIGHT * 0.60f);
            addActor(bottom);
            Image top = new Image(new TextureRegionDrawable(topRegion));
            top.setScaling(Scaling.stretch);
            top.setColor(0.28f, 0.50f, 0.92f, 0.88f);
            top.setBounds(0f, HEIGHT * 0.40f, WIDTH, HEIGHT * 0.60f);
            addActor(top);
        } else {
            addFallbackGradient();
        }

        Image leftEdge = image(EDGE_GRADIENT);
        leftEdge.setBounds(0f, 0f, 48f, HEIGHT);
        leftEdge.setColor(1f, 1f, 1f, 0.70f);
        addActor(leftEdge);
        Image rightEdge = image(EDGE_GRADIENT);
        rightEdge.setBounds(WIDTH - 48f, 0f, 48f, HEIGHT);
        rightEdge.setOrigin(24f, HEIGHT / 2f);
        rightEdge.setScaleX(-1f);
        rightEdge.setColor(1f, 1f, 1f, 0.70f);
        addActor(rightEdge);

        Image topShade = colorImage(EDGE_SHADE);
        topShade.setBounds(0f, 955f, WIDTH, 125f);
        addActor(topShade);
        Image bottomShade = colorImage(EDGE_SHADE);
        bottomShade.setBounds(0f, 0f, WIDTH, 100f);
        addActor(bottomShade);
    }

    private void addFallbackGradient() {
        int strips = 72;
        float stripHeight = HEIGHT / strips;
        for (int index = 0; index < strips; index++) {
            float center = (index + 0.5f) / strips;
            Image strip = colorImage(gradientColor(center));
            strip.setBounds(0f, index * stripHeight, WIDTH, stripHeight + 1f);
            addActor(strip);
        }
    }

    private Color gradientColor(float y) {
        if (y < 0.60f) {
            return lerp(BOTTOM_BLUE, MID_BLUE, y / 0.60f);
        }
        return lerp(MID_BLUE, TOP_BLUE, (y - 0.60f) / 0.40f);
    }

    private Color lerp(Color from, Color to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        return new Color(
                from.r + (to.r - from.r) * clamped,
                from.g + (to.g - from.g) * clamped,
                from.b + (to.b - from.b) * clamped,
                1f
        );
    }

    private void addPreviewPanel() {
        this.nameLabel.setBounds(520f, 928f, 880f, 92f);
        this.nameLabel.setColor(PvzVisualTheme.TEXT_CREAM);
        addActor(this.nameLabel);

        Group panel = new Group();
        panel.setBounds(150f, 205f, 620f, 650f);
        addAt(panel, colorImage(PREVIEW_BORDER), 0f, 0f, 620f, 650f);
        addPreviewBackground(panel);

        Table previewClip = new Table();
        previewClip.setClip(true);
        previewClip.setBounds(48f, 125f, 524f, 462f);
        this.previewVisual.setBounds(0f, 0f, 524f, 462f);
        previewClip.addActor(this.previewVisual);
        panel.addActor(previewClip);

        Label seedHeading = label("SEED PACKETS", 0.50f, Align.left);
        seedHeading.setColor(PvzVisualTheme.TEXT_CREAM);
        seedHeading.setBounds(65f, 82f, 220f, 34f);
        panel.addActor(seedHeading);
        this.levelLabel.setBounds(315f, 80f, 190f, 38f);
        this.levelLabel.setColor(PvzVisualTheme.TEXT_CREAM);
        panel.addActor(this.levelLabel);
        this.lockLabel.setBounds(485f, 80f, 110f, 38f);
        this.lockLabel.setColor(LOCKED_TEXT);
        panel.addActor(this.lockLabel);

        Image xpIcon = image(XP_ICON);
        xpIcon.setBounds(62f, 37f, 38f, 38f);
        panel.addActor(xpIcon);
        this.seedBar.setBounds(108f, 43f, 445f, 27f);
        panel.addActor(this.seedBar);
        this.seedLabel.setBounds(108f, 41f, 445f, 30f);
        panel.addActor(this.seedLabel);
        addActor(panel);
    }

    private void addPreviewBackground(Group panel) {
        TextureRegion region = this.textureBank.region(PREVIEW_BACKGROUND);
        if (region != null) {
            Image background = new Image(new TextureRegionDrawable(region));
            background.setScaling(Scaling.fill);
            addAt(panel, background, 15f, 112f, 590f, 520f);
            return;
        }
        addAt(panel, colorImage(PREVIEW_FALLBACK), 15f, 112f, 590f, 520f);
    }

    private void addStats() {
        addStat(SUN_BACKGROUND, "SUN COST", this.sunCostValue, 835f, 735f, true);
        addStat(RECHARGE_ICON, "RECHARGE", this.rechargeValue, 1305f, 735f, false);
        addStat(TOUGHNESS_ICON, "TOUGHNESS", this.healthValue, 835f, 615f, false);
        addStat(DAMAGE_ICON, "DAMAGE", this.damageValue, 1305f, 615f, false);
        addStat(VARIABLE_ICON, "ACTION", this.actionValue, 835f, 495f, false);
        addStat(SPECIAL_ICON, "SPECIAL", this.specialValue, 1305f, 495f, false);

        addInfoCard(835f, 375f, 430f, 100f);
        this.familyBadge.setBounds(852f, 389f, 72f, 72f);
        addActor(this.familyBadge);
        Label familyHeading = darkLabel("FAMILY", 0.92f, Align.left);
        familyHeading.setBounds(940f, 428f, 260f, 30f);
        addActor(familyHeading);
        this.familyValue.setFontScale(0.78f);
        this.familyValue.setBounds(940f, 390f, 290f, 44f);
        this.familyValue.setColor(STAT_TEXT);
        addActor(this.familyValue);

        addInfoCard(1305f, 375f, 475f, 100f);
        Image plantFood = image(PLANT_FOOD_ICON);
        plantFood.setBounds(1323f, 389f, 72f, 72f);
        addActor(plantFood);
        Label foodHeading = darkLabel("PLANT FOOD", 0.92f, Align.left);
        foodHeading.setBounds(1405f, 428f, 305f, 30f);
        addActor(foodHeading);
        this.plantFoodValue.setFontScale(0.60f);
        this.plantFoodValue.setWrap(true);
        this.plantFoodValue.setAlignment(Align.topLeft);
        this.plantFoodValue.setBounds(1405f, 384f, 345f, 50f);
        this.plantFoodValue.setColor(STAT_TEXT);
        addActor(this.plantFoodValue);

        addInfoCard(835f, 112f, 945f, 240f);
        Label abilityHeading = darkLabel("ABILITY", 1.02f, Align.left);
        abilityHeading.setBounds(865f, 305f, 250f, 34f);
        addActor(abilityHeading);
        this.abilityValue.setWrap(true);
        this.abilityValue.setAlignment(Align.topLeft);
        this.abilityValue.setFontScale(0.70f);
        this.abilityValue.setBounds(865f, 178f, 885f, 122f);
        this.abilityValue.setColor(STAT_TEXT);
        addActor(this.abilityValue);

        this.levelEffectsValue.setWrap(true);
        this.levelEffectsValue.setAlignment(Align.topLeft);
        this.levelEffectsValue.setFontScale(0.54f);
        this.levelEffectsValue.setBounds(865f, 126f, 885f, 48f);
        this.levelEffectsValue.setColor(INFO_YELLOW);
        addActor(this.levelEffectsValue);
    }

    private void addStat(String iconId, String heading, Label value, float x, float y, boolean sunIcon) {
        addInfoCard(x, y, 430f, 100f);
        if (sunIcon) {
            Group sun = new Group();
            sun.setBounds(x + 16f, y + 14f, 72f, 72f);
            addAt(sun, image(iconId), 6f, 6f, 60f, 60f);
            addAt(sun, image(SUN_GLYPH), 15f, 14f, 43f, 44f);
            addActor(sun);
        } else {
            Image icon = image(iconId);
            icon.setBounds(x + 22f, y + 20f, 60f, 60f);
            addActor(icon);
        }
        Label title = darkLabel(heading, 0.92f, Align.left);
        title.setBounds(x + 102f, y + 57f, 285f, 28f);
        addActor(title);
        value.setFontScale(value == this.specialValue ? 0.56f : 0.78f);
        value.setBounds(x + 102f, y + 15f, 300f, 44f);
        value.setColor(STAT_TEXT);
        value.setAlignment(Align.left);
        if (value == this.specialValue) {
            value.setWrap(true);
            value.setFontScale(0.58f);
            value.setBounds(x + 102f, y + 8f, 300f, 54f);
        }
        addActor(value);
    }

    private void addInfoCard(float x, float y, float width, float height) {
        TextureRegion region = this.textureBank.region(STAT_BACKGROUND);
        if (region != null) {
            NinePatch patch = new NinePatch(region, 18, 18, 18, 18);
            Image background = new Image(new NinePatchDrawable(patch));
            background.setBounds(x, y, width, height);
            addActor(background);
            return;
        }
        Image fallback = colorImage(new Color(0.94f, 0.88f, 0.68f, 0.96f));
        fallback.setBounds(x, y, width, height);
        addActor(fallback);
    }

    private Label darkLabel(String text, float scale, int alignment) {
        Label label = new Label(text, this.skin, "secondary");
        label.setFontScale(scale);
        label.setAlignment(alignment);
        label.setColor(STAT_HEADING);
        return label;
    }

    private void addResourceStrip() {
        this.resourceStrip.setBounds(1065f, 995f, 690f, 63f);
        addActor(this.resourceStrip);
    }

    private Button createCollectionActionButton() {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = this.skin.getDrawable("image_ui_generic_purplebutton");
        style.down = this.skin.getDrawable("image_ui_generic_purplebutton_down");
        Button button = new Button(style);
        this.collectionActionLabel.setFontScale(0.72f);
        this.collectionActionLabel.setAlignment(Align.center);
        this.collectionActionLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        button.add(this.collectionActionLabel).grow();
        button.addListener(click(this::runCollectionAction));
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private void addCollectionActionButton() {
        this.collectionActionButton.setBounds(300f, 108f, 320f, 80f);
        addActor(this.collectionActionButton);
    }

    private void updateCollectionAction(PlantCollectionState state) {
        this.collectionActionButton.setVisible(true);
        if (!state.isUnlocked()) {
            boolean affordable = this.currentCoins >= UNLOCK_PRICE;
            this.collectionActionLabel.setText(affordable ? "UNLOCK  2,000" : "NEED 2,000 COINS");
            setCollectionActionEnabled(affordable);
            return;
        }
        if (state.getCurrentLevel() >= state.getMaximumLevel()) {
            this.collectionActionLabel.setText("MAX LEVEL");
            setCollectionActionEnabled(false);
            return;
        }
        int requiredSeeds = Math.max(0, state.getRequiredSeedPackets());
        if (requiredSeeds > 0 && state.getSeedPackets() < requiredSeeds) {
            int missing = requiredSeeds - state.getSeedPackets();
            this.collectionActionLabel.setText("NEED " + missing + " SEEDS");
            setCollectionActionEnabled(false);
            return;
        }
        int requiredCoins = Math.max(0, state.getRequiredCoins());
        if (this.currentCoins < requiredCoins) {
            this.collectionActionLabel.setText("NEED " + formatCoins(requiredCoins) + " COINS");
            setCollectionActionEnabled(false);
            return;
        }
        this.collectionActionLabel.setText("UPGRADE  " + formatCoins(requiredCoins));
        setCollectionActionEnabled(true);
    }

    private void setCollectionActionEnabled(boolean enabled) {
        this.collectionActionButton.setDisabled(!enabled);
        this.collectionActionButton.setColor(enabled ? Color.WHITE : PvzVisualTheme.DISABLED_TINT);
    }

    private void runCollectionAction() {
        if (this.currentState == null || this.collectionActionButton.isDisabled()) {
            return;
        }
        if (!this.currentState.isUnlocked()) {
            run(this.purchaseAction);
        } else if (isUpgradeAvailable(this.currentState)) {
            run(this.upgradeAction);
        }
    }

    private boolean isUpgradeAvailable(PlantCollectionState state) {
        return state != null
                && state.isUnlocked()
                && state.getCurrentLevel() < state.getMaximumLevel()
                && state.getRequiredSeedPackets() > 0
                && state.getSeedPackets() >= state.getRequiredSeedPackets();
    }

    private String formatCoins(int coins) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, coins));
    }

    private void addNavigation() {
        ImageButton back = button(BACK_UP, BACK_DOWN);
        back.setBounds(35f, 935f, 96f, 92f);
        back.addListener(click(() -> run(this.backAction)));
        CollectionUiAnimator.installHoverScale(back);
        addActor(back);

        ImageButton previous = button(PREVIOUS_UP, PREVIOUS_DOWN);
        previous.setBounds(32f, 38f, 82f, 82f);
        previous.addListener(click(() -> run(this.previousAction)));
        CollectionUiAnimator.installHoverScale(previous);
        addActor(previous);

        ImageButton next = button(NEXT_UP, NEXT_DOWN);
        next.setBounds(1806f, 38f, 82f, 82f);
        next.addListener(click(() -> run(this.nextAction)));
        CollectionUiAnimator.installHoverScale(next);
        addActor(next);
    }

    private void updateSeedProgress(PlantCollectionState state) {
        int required = state.getRequiredSeedPackets();
        float maximum = required > 0 ? required : 1f;
        this.seedBar.setRange(0f, maximum);
        this.seedBar.setValue(required > 0 ? Math.min(state.getSeedPackets(), required) : maximum);
        this.seedLabel.setText(state.getCurrentLevel() >= state.getMaximumLevel()
                ? "MAX"
                : state.getSeedPackets() + "/" + required);
    }

    private void updateFamilyBadge(PlantCollectionState state) {
        this.familyBadge.clearChildren();
        PlantPacketCatalog.FamilyVisual family = PlantPacketCatalog.findFamily(state);
        Image banner = image(FAMILY_BANNER);
        banner.setColor(family.getColor());
        addAt(this.familyBadge, banner, 0f, 0f, 72f, 72f);
        addAt(this.familyBadge, image(family.getGlyphResourceId()), 11f, 12f, 50f, 48f);
    }

    private void updatePlantVisual(PlantCollectionState state) {
        this.previewVisual.clearChildren();
        PamAnimationCatalog.AnimationInfo animation = this.animationCatalog.find(state.getName());
        Actor visual = createAnimation(animation);
        if (visual == null) {
            visual = createStaticPlant(state);
        }
        if (visual == null) {
            return;
        }
        if (!state.isUnlocked()) {
            visual.setColor(LOCKED_COLOR);
        }
        this.previewVisual.addActor(visual);
    }

    private Actor createAnimation(PamAnimationCatalog.AnimationInfo animation) {
        if (!canUseAnimation(animation)) {
            return null;
        }
        Rectangle bounds = readBounds(animation);
        PamAnimationActor actor = new PamAnimationActor(
                this.assets.getPamPlayer(),
                animation.getPath(),
                animation.getPreviewClip(),
                animation.getCanvasWidth(),
                animation.getCanvasHeight()
        );
        float scale = animationScale(bounds);
        float originX = previewOriginX(bounds, scale);
        float originY = previewOriginY(bounds, scale);
        actor.setBounds(
                originX - animation.getCanvasWidth() / 2f,
                originY - animation.getCanvasHeight() / 2f,
                animation.getCanvasWidth(),
                animation.getCanvasHeight()
        );
        actor.setScale(scale);
        return actor;
    }

    private boolean canUseAnimation(PamAnimationCatalog.AnimationInfo animation) {
        if (animation == null || animation.getPreviewClip() == null) {
            return false;
        }
        com.badlogic.gdx.files.FileHandle pamFile = Gdx.files.internal("IMAGES/" + animation.getPath());
        return pamFile.exists() && PamTextureAvailability.allTexturesAvailable(this.textureBank, pamFile);
    }

    private Rectangle readBounds(PamAnimationCatalog.AnimationInfo animation) {
        try {
            return this.assets.getPamPlayer().bounds(animation.getPath(), animation.getPreviewClip());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private float animationScale(Rectangle bounds) {
        if (!validBounds(bounds)) {
            return 1f;
        }
        float fit = Math.min((PREVIEW_WIDTH - 35f) / bounds.width, (PREVIEW_HEIGHT - 35f) / bounds.height);
        return Math.max(0.1f, Math.min(1.35f, fit));
    }

    private float previewOriginX(Rectangle bounds, float scale) {
        if (!validBounds(bounds)) {
            return PREVIEW_CENTER_X;
        }
        return PREVIEW_CENTER_X - (bounds.x + bounds.width / 2f) * scale;
    }

    private float previewOriginY(Rectangle bounds, float scale) {
        if (!validBounds(bounds)) {
            return PREVIEW_CENTER_Y;
        }
        return PREVIEW_CENTER_Y - (bounds.y + bounds.height / 2f) * scale;
    }

    private boolean validBounds(Rectangle bounds) {
        return bounds != null && bounds.width > 0f && bounds.height > 0f;
    }

    private Actor createStaticPlant(PlantCollectionState state) {
        PlantPacketCatalog.PacketVisual packet = PlantPacketCatalog.findPacket(state.getName());
        if (packet == null) {
            return null;
        }
        TextureRegion region = this.textureBank.region(packet.getResourceId());
        if (region == null) {
            return null;
        }
        float fitScale = Math.min(420f / region.getRegionWidth(), 390f / region.getRegionHeight());
        float scale = Math.min(MAX_STATIC_PLANT_SCALE, fitScale);
        float width = region.getRegionWidth() * scale;
        float height = region.getRegionHeight() * scale;
        Image image = new Image(new TextureRegionDrawable(region));
        image.setBounds(PREVIEW_CENTER_X - width / 2f, PREVIEW_CENTER_Y - height / 2f, width, height);
        return image;
    }

    private ProgressBar createSeedBar() {
        String style = this.skin.has("xp_yellow", ProgressBar.ProgressBarStyle.class) ? "xp_yellow" : "xp_green";
        ProgressBar bar = new ProgressBar(0f, 1f, 1f, false, this.skin, style);
        bar.setAnimateDuration(0f);
        return bar;
    }

    private ImageButton button(String upId, String downId) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = drawable(upId);
        style.imageDown = drawable(downId);
        return new ImageButton(style);
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                action.run();
            }
        };
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    private Image image(String resourceId) {
        TextureRegion region = this.textureBank.region(resourceId);
        return region == null ? new Image() : new Image(new TextureRegionDrawable(region));
    }

    private TextureRegionDrawable drawable(String resourceId) {
        TextureRegion region = this.textureBank.region(resourceId);
        return region == null ? new TextureRegionDrawable() : new TextureRegionDrawable(region);
    }

    private Image colorImage(Color color) {
        Image image = new Image(this.skin.getDrawable("white_pixel"));
        image.setColor(color);
        return image;
    }

    private Label label(String text, float scale, int alignment) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(scale);
        label.setAlignment(alignment);
        return label;
    }

    private void addAt(Group group, Actor actor, float x, float y, float width, float height) {
        actor.setBounds(x, y, width, height);
        group.addActor(actor);
    }

    private String formatSeconds(double seconds) {
        return BigDecimal.valueOf(seconds).stripTrailingZeros().toPlainString() + "s";
    }

    private String formatTags(PlantCollectionState state) {
        if (state.getTags() == null || state.getTags().isEmpty()) {
            return "None";
        }
        return state.getTags().stream()
                .map(PlantTag::name)
                .map(this::friendlyEnum)
                .collect(Collectors.joining(", "));
    }

    private String formatLevelEffects(PlantCollectionState state) {
        if (state.getLevelUpEffects() == null || state.getLevelUpEffects().isEmpty()) {
            return "";
        }
        return "LEVEL UP: " + state.getLevelUpEffects().stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .limit(2)
                .collect(Collectors.joining("  •  "));
    }

    private String plantFoodText(PlantCollectionState state) {
        String description = state.getPlantFoodEffectDescription();
        if (description == null || description.trim().isEmpty() || description.contains("ندارد")) {
            return "No Plant Food effect";
        }
        if (containsPersian(description)) {
            return "Plant Food effect available";
        }
        return description.trim();
    }

    private String abilityText(PlantCollectionState state) {
        String description = state.getBaseAbilityDescription();
        if (description != null && !description.trim().isEmpty() && !containsPersian(description)) {
            return description.trim();
        }
        String categories = state.getCategories() == null
                ? ""
                : state.getCategories().stream()
                .map(PlantCategory::name)
                .map(this::friendlyEnum)
                .collect(Collectors.joining(" • "));
        String tags = formatTags(state);
        if (categories.isEmpty()) {
            return tags.equals("None") ? "No additional ability description" : tags;
        }
        return tags.equals("None") ? categories : categories + " • " + tags;
    }

    private boolean containsPersian(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character >= '\u0600' && character <= '\u06FF')
                    || (character >= '\u0750' && character <= '\u077F')) {
                return true;
            }
        }
        return false;
    }

    private String friendlyEnum(String value) {
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder(text.length());
        boolean capitalize = true;
        for (char character : text.toCharArray()) {
            if (capitalize && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
            if (character == ' ') {
                capitalize = true;
            }
        }
        return result.toString();
    }

    private String familyName(PlantCollectionState state) {
        String id = PlantPacketCatalog.findFamily(state).getGlyphResourceId();
        if (id.endsWith("_SUN")) return "Enlighten-mint";
        if (id.endsWith("_PEASHOOTER")) return "Appease-mint";
        if (id.endsWith("_LOBBER")) return "Arma-mint";
        if (id.endsWith("_EXPLOSIVE")) return "Bombard-mint";
        if (id.endsWith("_MELEE")) return "Enforce-mint";
        if (id.endsWith("_DEFENSE")) return "Reinforce-mint";
        if (id.endsWith("_SHARP")) return "Spear-mint";
        if (id.endsWith("_TRAP")) return "Contain-mint";
        if (id.endsWith("_FIRE")) return "Pepper-mint";
        if (id.endsWith("_COLD")) return "Winter-mint";
        if (id.endsWith("_POISON")) return "Ail-mint";
        return "Enchant-mint";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
