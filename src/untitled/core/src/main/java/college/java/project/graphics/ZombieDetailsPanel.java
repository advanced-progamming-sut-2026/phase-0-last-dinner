package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.collection.ZombieCollectionState;
import model.zombie.ConditionResistance;
import model.zombie.ZombieArmorDefinition;
import pvz.libpvz.textures.TextureBank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Full-screen PvZ2 Almanac-style zombie detail view with safe PAM fallback. */
public final class ZombieDetailsPanel extends Group {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private static final String BACK_UP = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_DOWN = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String NEXT_UP = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_NEXT";
    private static final String NEXT_DOWN = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_NEXT_DOWN";
    private static final String PREVIOUS_UP = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_PREVIOUS";
    private static final String PREVIOUS_DOWN = "IMAGE_UI_ALMANAC_STATS_SCREEN_NAV_ARROW_PREVIOUS_DOWN";
    private static final String TOUGHNESS_ICON = "IMAGE_UI_ALMANAC_ZOMBIES_ZOMBIETOUGHNESS_ICON";
    private static final String SPEED_ICON = "IMAGE_UI_ALMANAC_ZOMBIES_ZOMBIESPEED_ICON";
    private static final String PREVIEW_BACKGROUND = "IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_MODERN";

    private static final Color TOP_BLUE = new Color(0.006f, 0.020f, 0.075f, 1f);
    private static final Color MID_BLUE = new Color(0.055f, 0.285f, 0.79f, 1f);
    private static final Color BOTTOM_BLUE = new Color(0.004f, 0.025f, 0.10f, 1f);
    private static final Color EDGE_SHADE = new Color(0f, 0f, 0f, 0.27f);
    private static final Color PREVIEW_BORDER = new Color(0.10f, 0.10f, 0.085f, 1f);
    private static final Color LAWN = new Color(0.24f, 0.43f, 0.18f, 1f);
    private static final Color INFO_YELLOW = new Color(1f, 0.80f, 0.12f, 1f);

    private static final float PREVIEW_WIDTH = 455f;
    private static final float PREVIEW_HEIGHT = 500f;
    private static final float PREVIEW_CENTER_X = PREVIEW_WIDTH / 2f;
    private static final float PREVIEW_CENTER_Y = PREVIEW_HEIGHT / 2f;

    private final Skin skin;
    private final GameAssetManager assets;
    private final TextureBank textureBank;
    private final ZombieAnimationCatalog animationCatalog;
    private final Group previewVisual;
    private final Label nameLabel;
    private final Label toughnessValue;
    private final Label speedValue;
    private final Label descriptionLabel;
    private final Label featureLabel;
    private final Label identityLabel;
    private final Label combatLabel;
    private final Label armorLabel;
    private final Label resistanceLabel;
    private final AlmanacResourceStrip resourceStrip;

    private Runnable backAction;
    private Runnable previousAction;
    private Runnable nextAction;

    public ZombieDetailsPanel(Skin skin, GameAssetManager assets, ZombieAnimationCatalog animationCatalog) {
        if (skin == null || assets == null) {
            throw new IllegalArgumentException("Zombie detail resources are required");
        }
        this.skin = skin;
        this.assets = assets;
        this.textureBank = assets.getTextureBank();
        this.animationCatalog = animationCatalog;
        this.previewVisual = new Group();
        this.nameLabel = label("", 1.34f, Align.center);
        this.toughnessValue = label("", 0.68f, Align.left);
        this.speedValue = label("", 0.68f, Align.left);
        this.descriptionLabel = label("", 0.56f, Align.topLeft);
        this.featureLabel = label("", 0.52f, Align.topLeft);
        this.identityLabel = label("", 0.46f, Align.topLeft);
        this.combatLabel = label("", 0.46f, Align.topLeft);
        this.armorLabel = label("", 0.44f, Align.topLeft);
        this.resistanceLabel = label("", 0.44f, Align.topLeft);
        this.resourceStrip = new AlmanacResourceStrip(this.skin);

        setSize(WIDTH, HEIGHT);
        setOrigin(WIDTH / 2f, HEIGHT / 2f);
        addGradientBackground();
        addPreview();
        addStats();
        addResourceStrip();
        addNavigation();
        setVisible(false);
    }

    public void setActions(Runnable backAction, Runnable previousAction, Runnable nextAction) {
        this.backAction = backAction;
        this.previousAction = previousAction;
        this.nextAction = nextAction;
    }

    public void setResources(int mints, int gems, int coins) {
        this.resourceStrip.setCounts(mints, gems, coins);
    }

    public void setDebugCurrencyControls(
            boolean visible,
            Runnable addGemAction,
            Runnable addCoinAction
    ) {
        this.resourceStrip.setDebugControls(visible, addGemAction, addCoinAction);
    }

    public void showZombie(ZombieCollectionState state) {
        if (state == null || !state.isEncountered()) {
            return;
        }
        this.nameLabel.setText(canonicalName(state));
        this.toughnessValue.setText(toughnessText(state));
        this.speedValue.setText(speedText(state));
        this.descriptionLabel.setText(descriptionText(state));
        this.featureLabel.setText(
                "WORLD  " + friendly(state.getChapter()) + "    •    TYPE  " + friendly(state.getType())
        );
        this.identityLabel.setText(
                "HP  " + state.getHitpoints()
                        + "    •    BITE DPS  " + state.getEatDamagePerSecond()
                        + "    •    WAVE COST  " + state.getWavePointCost()
        );
        this.combatLabel.setText("PLANT FOOD  " + (state.isCanSpawnPlantFood() ? "Yes" : "No"));
        this.armorLabel.setText(compactArmor(state.getArmorDefinitions()));
        this.resistanceLabel.setText(compactResistances(state.getConditionResistances()));
        updateVisual(state);
        setVisible(true);
    }

    private void addGradientBackground() {
        int strips = 108;
        float stripHeight = HEIGHT / strips;
        for (int index = 0; index < strips; index++) {
            float position = (index + 0.5f) / strips;
            Image strip = colorImage(gradient(position));
            strip.setBounds(0f, index * stripHeight, WIDTH, stripHeight + 1f);
            addActor(strip);
        }
        Image topShade = colorImage(EDGE_SHADE);
        topShade.setBounds(0f, 955f, WIDTH, 125f);
        addActor(topShade);
        Image bottomShade = colorImage(EDGE_SHADE);
        bottomShade.setBounds(0f, 0f, WIDTH, 105f);
        addActor(bottomShade);
    }

    private Color gradient(float y) {
        if (y < 0.60f) {
            return lerp(BOTTOM_BLUE, MID_BLUE, y / 0.60f);
        }
        return lerp(MID_BLUE, TOP_BLUE, (y - 0.60f) / 0.40f);
    }

    private Color lerp(Color from, Color to, float amount) {
        float value = Math.max(0f, Math.min(1f, amount));
        return new Color(
                from.r + (to.r - from.r) * value,
                from.g + (to.g - from.g) * value,
                from.b + (to.b - from.b) * value,
                1f
        );
    }

    private void addPreview() {
        this.nameLabel.setBounds(575f, 930f, 770f, 90f);
        addActor(this.nameLabel);

        Group panel = new Group();
        panel.setBounds(260f, 260f, 530f, 590f);
        addAt(panel, colorImage(PREVIEW_BORDER), 0f, 0f, 530f, 590f);
        TextureRegion lawn = this.textureBank.region(PREVIEW_BACKGROUND);
        Actor background = lawn == null
                ? colorImage(LAWN)
                : new Image(new TextureRegionDrawable(lawn));
        addAt(panel, background, 13f, 13f, 504f, 564f);
        this.previewVisual.setBounds(38f, 42f, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        panel.addActor(this.previewVisual);
        addActor(panel);
    }

    private void addStats() {
        addIconStat(TOUGHNESS_ICON, "TOUGHNESS", this.toughnessValue, 910f, 700f);
        addIconStat(SPEED_ICON, "SPEED", this.speedValue, 1335f, 700f);

        this.descriptionLabel.setWrap(true);
        this.descriptionLabel.setBounds(910f, 525f, 805f, 125f);
        addActor(this.descriptionLabel);

        this.featureLabel.setWrap(true);
        this.featureLabel.setColor(INFO_YELLOW);
        this.featureLabel.setBounds(910f, 430f, 805f, 72f);
        addActor(this.featureLabel);

        this.identityLabel.setBounds(910f, 365f, 805f, 48f);
        addActor(this.identityLabel);
        this.combatLabel.setBounds(910f, 320f, 390f, 42f);
        addActor(this.combatLabel);
        this.armorLabel.setBounds(910f, 275f, 805f, 42f);
        addActor(this.armorLabel);
        this.resistanceLabel.setBounds(910f, 230f, 805f, 42f);
        addActor(this.resistanceLabel);
    }

    private void addIconStat(String iconId, String heading, Label value, float x, float y) {
        Image icon = image(iconId);
        icon.setBounds(x, y, 72f, 72f);
        addActor(icon);
        Label title = label(heading, 0.48f, Align.left);
        title.setBounds(x + 88f, y + 42f, 240f, 32f);
        addActor(title);
        value.setBounds(x + 88f, y - 2f, 320f, 52f);
        addActor(value);
    }

    private void addResourceStrip() {
        this.resourceStrip.setBounds(1065f, 995f, 690f, 63f);
        addActor(this.resourceStrip);
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

    private void updateVisual(ZombieCollectionState state) {
        this.previewVisual.clearChildren();
        ZombieAnimationCatalog.AnimationInfo animation = this.animationCatalog == null
                ? null
                : this.animationCatalog.find(state.getAlias());
        Actor visual = hasVisibleArmor(state) ? createStaticPacket(state) : createAnimation(animation);
        if (visual == null) {
            visual = createStaticPacket(state);
        }
        if (visual != null) {
            this.previewVisual.addActor(visual);
        }
    }

    private boolean hasVisibleArmor(ZombieCollectionState state) {
        return state.getArmorDefinitions() != null && !state.getArmorDefinitions().isEmpty();
    }

    private Actor createAnimation(ZombieAnimationCatalog.AnimationInfo animation) {
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
        float centerX = visualOriginX(bounds, scale);
        float centerY = visualOriginY(bounds, scale);
        actor.setBounds(
                centerX - animation.getCanvasWidth() / 2f,
                centerY - animation.getCanvasHeight() / 2f,
                animation.getCanvasWidth(),
                animation.getCanvasHeight()
        );
        actor.setScale(scale);
        return actor;
    }

    private boolean canUseAnimation(ZombieAnimationCatalog.AnimationInfo animation) {
        if (animation == null || animation.getPreviewClip() == null) {
            return false;
        }
        com.badlogic.gdx.files.FileHandle pamFile = Gdx.files.internal("IMAGES/" + animation.getPath());
        return pamFile.exists() && PamTextureAvailability.allTexturesAvailable(this.textureBank, pamFile);
    }

    private Rectangle readBounds(ZombieAnimationCatalog.AnimationInfo animation) {
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
        float fit = Math.min((PREVIEW_WIDTH - 30f) / bounds.width, (PREVIEW_HEIGHT - 30f) / bounds.height);
        return Math.max(0.08f, Math.min(1.25f, fit));
    }

    private float visualOriginX(Rectangle bounds, float scale) {
        if (!validBounds(bounds)) {
            return PREVIEW_CENTER_X;
        }
        return PREVIEW_CENTER_X - (bounds.x + bounds.width / 2f) * scale;
    }

    private float visualOriginY(Rectangle bounds, float scale) {
        if (!validBounds(bounds)) {
            return PREVIEW_CENTER_Y;
        }
        return PREVIEW_CENTER_Y - (bounds.y + bounds.height / 2f) * scale;
    }

    private boolean validBounds(Rectangle bounds) {
        return bounds != null && bounds.width > 0f && bounds.height > 0f;
    }

    private Actor createStaticPacket(ZombieCollectionState state) {
        ZombiePacketCatalog.PacketVisual packet = ZombiePacketCatalog.findPacket(state.getAlias());
        TextureRegion region = packet == null ? null : this.textureBank.region(packet.getResourceId());
        if (region == null) {
            return null;
        }
        float fit = Math.min((PREVIEW_WIDTH * 0.58f) / region.getRegionWidth(),
                (PREVIEW_HEIGHT * 0.64f) / region.getRegionHeight());
        float width = region.getRegionWidth() * fit;
        float height = region.getRegionHeight() * fit;
        Image image = new Image(new TextureRegionDrawable(region));
        image.setBounds(PREVIEW_CENTER_X - width / 2f, PREVIEW_CENTER_Y - height / 2f, width, height);
        return image;
    }

    private String canonicalName(ZombieCollectionState state) {
        String alias = normalize(state.getAlias());
        switch (alias) {
            case "zombiedefault": return "Basic Zombie";
            case "zombiearmor1": return "Conehead Zombie";
            case "zombiearmor2": return "Buckethead Zombie";
            case "zombiearmor4": return "Brickhead Zombie";
            case "zombiegargantuar": return "Gargantuar";
            case "zombieimp": return "Imp";
            case "zombiera": return "Ra Zombie";
            case "zombieexplorer": return "Explorer Zombie";
            case "zombietombraiser": return "Tomb Raiser Zombie";
            case "zombieiceagedodo": return "Dodo Rider Zombie";
            case "zombieiceagehunter": return "Hunter Zombie";
            case "zombieiceagetroglobite": return "Troglobite Zombie";
            case "zombiebeachfisherman": return "Fisherman Zombie";
            case "zombiebeachoctopus": return "Octo Zombie";
            case "zombiebeachsnorkel": return "Snorkel Zombie";
            case "zombiedarkjuggler": return "Jester Zombie";
            case "zombiewizard": return "Wizard Zombie";
            case "zombiedarkking": return "King Zombie";
            case "zombiedarkimpdragon": return "Imp Dragon Zombie";
            case "zombiemodernallstar": return "All-Star Zombie";
            case "zombielostcityjane": return "Parasol Zombie";
            case "zombiecrystalskull": return "Turquoise Skull Zombie";
            case "zombieprospector": return "Prospector Zombie";
            case "zombiepiano": return "Pianist Zombie";
            case "zombienewspaper": return "Newspaper Zombie";
            case "zombiearcade": return "Arcade Zombie";
            case "zombiebarrelroller": return "Barrel Roller Zombie";
            case "zombiedarkarmor3": return "Knight Zombie";
            default:
                String display = safe(state.getDisplayName(), "");
                return display.isEmpty() ? friendly(state.getAlias()) : display;
        }
    }

    private String toughnessText(ZombieCollectionState state) {
        int armorHealth = 0;
        if (state.getArmorDefinitions() != null) {
            for (ZombieArmorDefinition armor : state.getArmorDefinitions()) {
                if (armor != null) {
                    armorHealth += Math.max(0, armor.getBaseHealth());
                }
            }
        }
        if (armorHealth > 0) {
            return "Protected  •  " + state.getHitpoints() + "+" + armorHealth + " HP";
        }
        String className = state.getHitpoints() >= 1000 ? "Dense" : state.getHitpoints() >= 350 ? "Solid" : "Basic";
        return className + "  •  " + state.getHitpoints() + " HP";
    }

    private String speedText(ZombieCollectionState state) {
        double speed = state.getSpeed();
        String className = speed < 0.16 ? "Slow" : speed <= 0.22 ? "Basic" : "Fast";
        return className + "  •  " + formatSpeed(speed);
    }

    private String descriptionText(ZombieCollectionState state) {
        String description = safe(state.getDescription(), "");
        if (!description.isEmpty()
                && !normalize(description).equals(normalize(state.getAlias()))
                && !normalize(description).equals(normalize(state.getDisplayName()))) {
            return description;
        }
        if (hasVisibleArmor(state)) {
            int armorHealth = state.getArmorDefinitions().stream()
                    .mapToInt(ZombieArmorDefinition::getBaseHealth)
                    .sum();
            return "Protected by armor that absorbs " + armorHealth
                    + " damage before its base health is affected.";
        }
        return "Base health " + state.getHitpoints()
                + ". Moves at speed " + formatSpeed(state.getSpeed())
                + " and bites for " + state.getEatDamagePerSecond() + " damage per second.";
    }

    private String compactArmor(List<ZombieArmorDefinition> armorDefinitions) {
        if (armorDefinitions == null || armorDefinitions.isEmpty()) {
            return "ARMOR  None";
        }
        return "ARMOR  " + armorDefinitions.stream()
                .map(armor -> friendly(armor.getType()) + " " + armor.getBaseHealth() + " HP")
                .collect(Collectors.joining("  •  "));
    }

    private String compactResistances(List<ConditionResistance> resistances) {
        if (resistances == null || resistances.isEmpty()) {
            return "RESISTANCE  None";
        }
        return "RESISTANCE  " + resistances.stream().map(resistance -> {
            String amount = resistance.isImmune() ? "Immune" : resistance.getResistancePercent() + "%";
            return friendly(resistance.getCondition()) + " " + amount;
        }).collect(Collectors.joining("  •  "));
    }

    private String formatSpeed(double speed) {
        return BigDecimal.valueOf(speed).stripTrailingZeros().toPlainString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String friendly(Object value) {
        if (value == null) {
            return "Unknown";
        }
        String text = value.toString().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder(text.length());
        boolean upper = true;
        for (char character : text.toCharArray()) {
            if (upper && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                upper = false;
            } else {
                result.append(character);
            }
            if (character == ' ') {
                upper = true;
            }
        }
        return result.toString();
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
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        };
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    private Label label(String text, float scale, int alignment) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(scale);
        label.setAlignment(alignment);
        return label;
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

    private void addAt(Group group, Actor actor, float x, float y, float width, float height) {
        actor.setBounds(x, y, width, height);
        group.addActor(actor);
    }
}
