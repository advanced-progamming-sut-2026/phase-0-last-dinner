package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.collection.PlantCollectionState;
import model.mechanism.PlantStatus;
import model.plant.PlantCategory;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public final class PlantCard extends Table {
    public static final float CARD_WIDTH = 176f;
    public static final float CARD_HEIGHT = 132f;
    public static final float PREVIEW_WIDTH = 166f;
    public static final float PREVIEW_HEIGHT = 104f;
    public static final float SEED_BAR_HEIGHT = 20f;

    private static final String WHITE_PIXEL = "white_pixel";
    private static final String LOCK_ICON = "image_ui_lock_small";
    private static final String BOOST_ICON = "image_ui_almanac_almanac_boost";
    private static final String UPGRADE_ICON =
            "image_ui_leveling_upgrade_badge_upgrade_badge_84x84";
    private static final String SELECTION_FRAME =
            "image_ui_almanac_plant_select_pkt";
    private static final String SEED_ICON_GREEN =
            "image_ui_generic_xp_progress_icon_green";
    private static final String SEED_ICON_YELLOW =
            "image_ui_generic_xp_progress_icon_yellow";
    private static final String SEED_BAR_BACKGROUND =
            "image_ui_generic_xp_progress_bar_10";
    private static final String SEED_BAR_GREEN =
            "image_ui_generic_xp_progress_bar_fill_green_10";
    private static final String SEED_BAR_YELLOW =
            "image_ui_generic_xp_progress_bar_fill_yellow_10";
    private static final String FAMILY_BADGE = "image_ui_generic_leaf_backdrop";
    private static final String PACKET_READY = "IMAGE_UI_PACKETS_READY";
    private static final String PACKET_SELECT = "IMAGE_UI_PACKETS_SELECT";
    private static final String PACKET_LOCK_SMALL = "IMAGE_UI_PACKETS_LOCK_SMALL";

    private static final Color OUTER_BORDER = new Color(0.15f, 0.09f, 0.04f, 1f);
    private static final Color WOOD_BORDER = new Color(0.48f, 0.25f, 0.08f, 1f);
    private static final Color WOOD_HIGHLIGHT = new Color(0.78f, 0.45f, 0.14f, 1f);
    private static final Color INNER_RIM = new Color(0.10f, 0.08f, 0.05f, 1f);
    private static final Color TEAL_BACKGROUND = new Color(0.10f, 0.40f, 0.38f, 1f);
    private static final Color BOOST_BACKGROUND = new Color(0.38f, 0.48f, 0.10f, 1f);
    private static final Color BOOST_OVERLAY = new Color(0.95f, 0.72f, 0.10f, 0.20f);
    private static final Color LOCK_DIM = new Color(0f, 0f, 0f, 0.30f);
    private static final Color LOCKED_ART = new Color(0.48f, 0.48f, 0.50f, 1f);

    private final Skin skin;
    private final TextureBank textureBank;
    private final PlantCollectionState plantState;
    private final String plantName;
    private final Image packetArtwork;
    private final PamAnimationActor animationActor;
    private final Label levelLabel;
    private final Label seedPacketLabel;
    private final Label sunCostLabel;
    private final Label availabilityLabel;
    private final ProgressBar seedPacketBar;
    private final Image selectionFrame;
    private final Image boostIcon;
    private final Image upgradeIcon;
    private final Image lockIcon;
    private final Table pictureSurface;
    private final Table boostOverlay;
    private final Drawable packetFrameDrawable;
    private final boolean upgradeAvailable;
    private boolean selected;
    private boolean boosted;
    private PlantCardActionListener actionListener;

    public PlantCard(
            Skin skin,
            PamPlayer pamPlayer,
            PamAnimationCatalog.AnimationInfo animationInfo,
            PlantCollectionState plantState
    ) {
        this(
                skin,
                null,
                pamPlayer,
                animationInfo,
                plantState,
                new PlantCardVisualCatalog().find(
                        plantState == null ? null : plantState.getName()
                )
        );
    }

    public PlantCard(
            Skin skin,
            PamPlayer pamPlayer,
            PamAnimationCatalog.AnimationInfo animationInfo,
            PlantCollectionState plantState,
            PlantCardVisualProfile visualProfile
    ) {
        this(
                skin,
                null,
                pamPlayer,
                animationInfo,
                plantState,
                visualProfile
        );
    }

    public PlantCard(
            Skin skin,
            TextureBank textureBank,
            PamPlayer pamPlayer,
            PamAnimationCatalog.AnimationInfo animationInfo,
            PlantCollectionState plantState,
            PlantCardVisualProfile visualProfile
    ) {
        super(skin);
        this.validateArguments(skin, plantState);

        this.skin = skin;
        this.setTouchable(Touchable.enabled);
        this.textureBank = textureBank;
        this.plantState = plantState;
        this.plantName = plantState.getName();
        this.upgradeAvailable = this.isUpgradeAvailable(plantState);
        this.packetArtwork = this.createPacketArtwork();
        this.animationActor = this.packetArtwork == null
                ? this.createAnimation(pamPlayer, animationInfo, visualProfile)
                : null;
        this.levelLabel = this.createOverlayLabel(
                "LVL " + plantState.getCurrentLevel(),
                0.56f
        );
        this.seedPacketLabel = this.createOverlayLabel(
                this.seedPacketText(plantState),
                0.55f
        );
        this.sunCostLabel = new Label(
                Integer.toString(plantState.getSunCost()),
                skin,
                "secondary"
        );
        this.availabilityLabel = new Label("", skin, "secondary");
        this.seedPacketBar = this.createSeedPacketBar(plantState);
        this.selectionFrame = this.createResourceImage(PACKET_SELECT, SELECTION_FRAME);
        this.selectionFrame.setScaling(Scaling.stretch);
        this.boostIcon = new Image(skin.getDrawable(BOOST_ICON));
        this.upgradeIcon = new Image(skin.getDrawable(UPGRADE_ICON));
        this.lockIcon = this.createResourceImage(PACKET_LOCK_SMALL, LOCK_ICON);
        this.pictureSurface = new Table();
        this.boostOverlay = new Table();
        this.packetFrameDrawable = this.packetDrawable(PACKET_READY);

        this.configureOverlayActors();
        this.buildLayout();
        this.installCardClickHandler();
        this.setSize(CARD_WIDTH, CARD_HEIGHT);
        CollectionUiAnimator.installHoverScale(this);
    }

    @Override
    public float getPrefWidth() {
        return CARD_WIDTH;
    }

    @Override
    public float getPrefHeight() {
        return CARD_HEIGHT;
    }

    public String getPlantName() {
        return this.plantName;
    }

    public PlantCollectionState getPlantState() {
        return this.plantState;
    }

    public PamAnimationActor getAnimationActor() {
        return this.animationActor;
    }

    public Label getLevelLabel() {
        return this.levelLabel;
    }

    public Label getSeedPacketLabel() {
        return this.seedPacketLabel;
    }

    public ProgressBar getSeedPacketBar() {
        return this.seedPacketBar;
    }

    public Label getSunCostLabel() {
        return this.sunCostLabel;
    }

    public Label getAvailabilityLabel() {
        return this.availabilityLabel;
    }

    public boolean isUpgradeAvailable() {
        return this.upgradeAvailable;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.selectionFrame.setVisible(selected);
    }

    public boolean isBoosted() {
        return this.boosted;
    }

    public void setBoosted(boolean boosted) {
        this.boosted = boosted;
        this.boostIcon.setVisible(boosted);
        this.updatePictureBackground();
    }

    public void updateGameplayStatus(PlantStatus status) {
        if (status == null) {
            this.availabilityLabel.setText("");
            return;
        }

        if (status.isAvailable()) {
            this.availabilityLabel.setText("Ready");
            return;
        }

        double remainingSeconds = status.getRemainingSeconds();
        if (remainingSeconds > 0) {
            this.availabilityLabel.setText(
                    "Cooldown " + this.formatSeconds(remainingSeconds)
            );
        } else {
            this.availabilityLabel.setText("Not enough sun");
        }
    }

    public void setActionListener(PlantCardActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private void validateArguments(Skin skin, PlantCollectionState plantState) {
        if (skin == null) {
            throw new IllegalArgumentException("Skin is required");
        }
        if (plantState == null || plantState.getName() == null) {
            throw new IllegalArgumentException("Plant state is required");
        }
        if (plantState.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Plant name is required");
        }
    }

    private void configureOverlayActors() {
        this.selectionFrame.setVisible(false);
        this.selectionFrame.setTouchable(Touchable.disabled);
        this.boostIcon.setVisible(false);
        this.boostOverlay.setVisible(false);
        this.boostOverlay.setTouchable(Touchable.disabled);
        this.boostOverlay.setBackground(this.skin.newDrawable(WHITE_PIXEL, BOOST_OVERLAY));
        this.upgradeIcon.setVisible(this.upgradeAvailable);
        this.lockIcon.setVisible(!this.plantState.isUnlocked());

        this.installUpgradeClickHandler();
        this.installPurchaseClickHandler();
    }

    private void buildLayout() {
        this.defaults().center();
        this.add(this.createPreview())
                .size(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        this.row();
        this.add(this.createSeedPacketProgress())
                .size(PREVIEW_WIDTH, SEED_BAR_HEIGHT)
                .padTop(2f);
    }

    private Stack createPreview() {
        Stack preview = new Stack();

        if (this.packetFrameDrawable != null) {
            Image packetFrame = new Image(this.packetFrameDrawable);
            packetFrame.setScaling(Scaling.stretch);
            packetFrame.setTouchable(Touchable.disabled);
            preview.add(packetFrame);

            this.pictureSurface.setClip(true);
            this.addPlantContent(this.pictureSurface);
            preview.add(this.pictureSurface);
            preview.add(this.boostOverlay);
        } else {
            preview.add(this.createLegacyPreviewSurface());
        }

        preview.add(this.createFamilyLayer());
        preview.add(this.createLevelLayer());
        preview.add(this.createBoostLayer());
        preview.add(this.createUpgradeLayer());

        if (!this.plantState.isUnlocked()) {
            preview.add(this.createLockDimLayer());
            preview.add(this.createLockLayer());
        }

        preview.add(this.selectionFrame);
        return preview;
    }

    private Table createLegacyPreviewSurface() {
        Table outerBorder = new Table();
        outerBorder.setBackground(this.skin.newDrawable(WHITE_PIXEL, OUTER_BORDER));
        outerBorder.pad(2f);

        Table woodBorder = new Table();
        woodBorder.setBackground(this.skin.newDrawable(WHITE_PIXEL, WOOD_BORDER));
        woodBorder.pad(2f);

        Table highlightRim = new Table();
        highlightRim.setBackground(this.skin.newDrawable(WHITE_PIXEL, WOOD_HIGHLIGHT));
        highlightRim.pad(1f);

        Table innerRim = new Table();
        innerRim.setBackground(this.skin.newDrawable(WHITE_PIXEL, INNER_RIM));
        innerRim.pad(2f);

        this.pictureSurface.setClip(true);
        this.updatePictureBackground();
        this.addPlantContent(this.pictureSurface);

        innerRim.add(this.pictureSurface).grow();
        highlightRim.add(innerRim).grow();
        woodBorder.add(highlightRim).grow();
        outerBorder.add(woodBorder).grow();
        return outerBorder;
    }

    private void addPlantContent(Table picture) {
        if (this.packetArtwork != null) {
            picture.add(this.packetArtwork).grow().pad(7f, 9f, 5f, 9f);
            return;
        }

        if (this.animationActor != null) {
            if (!this.plantState.isUnlocked()) {
                this.animationActor.setColor(LOCKED_ART);
            }
            picture.add(this.animationActor).grow();
            return;
        }

        Label fallback = new Label(this.plantName, this.skin, "secondary");
        fallback.setAlignment(Align.center);
        fallback.setWrap(true);
        fallback.setFontScale(0.72f);
        picture.add(fallback).grow().pad(8f);
    }

    private Image createPacketArtwork() {
        PlantPacketCatalog.PacketVisual packet = PlantPacketCatalog.findPacket(this.plantName);
        if (packet == null || packet.getResourceId() == null) {
            return null;
        }
        if ("IMAGE_UI_PACKETS_EMPTY_PACKET".equals(packet.getResourceId())) {
            return null;
        }

        Drawable drawable = this.packetDrawable(packet.getResourceId());
        if (drawable == null) {
            return null;
        }

        Image artwork = new Image(drawable);
        artwork.setScaling(Scaling.fit);
        artwork.setTouchable(Touchable.disabled);
        if (!this.plantState.isUnlocked()) {
            artwork.setColor(LOCKED_ART);
        }
        return artwork;
    }

    private Drawable packetDrawable(String resourceId) {
        if (this.textureBank != null) {
            try {
                TextureRegion region = this.textureBank.region(resourceId);
                if (region != null) {
                    return new TextureRegionDrawable(region);
                }
            } catch (RuntimeException ignored) {
            }
        }

        if (this.skin.has(resourceId, Drawable.class)) {
            return this.skin.getDrawable(resourceId);
        }

        String normalized = resourceId.toLowerCase(Locale.ROOT);
        if (this.skin.has(normalized, Drawable.class)) {
            return this.skin.getDrawable(normalized);
        }
        return null;
    }

    private Table createFamilyLayer() {
        Table layer = new Table();
        layer.top().left();

        Image originalFamily = this.createOriginalFamilyIcon();
        if (originalFamily != null) {
            layer.add(originalFamily)
                    .size(27f)
                    .padTop(2f)
                    .padLeft(3f);
            return layer;
        }

        Stack badge = new Stack();
        Image badgeBackground = new Image(this.skin.newDrawable(
                FAMILY_BADGE,
                this.categoryColor(this.primaryCategory())
        ));
        Label badgeText = this.createOverlayLabel(
                this.categoryLetter(this.primaryCategory()),
                0.48f
        );
        Table textLayer = new Table();
        textLayer.add(badgeText);
        badge.add(badgeBackground);
        badge.add(textLayer);

        layer.add(badge)
                .size(31f)
                .padTop(1f)
                .padLeft(1f);
        return layer;
    }

    private Image createOriginalFamilyIcon() {
        PlantPacketCatalog.FamilyVisual family = PlantPacketCatalog.findFamily(this.plantState);
        if (family == null || family.getGlyphResourceId() == null) {
            return null;
        }

        Drawable drawable = this.packetDrawable(family.getGlyphResourceId());
        if (drawable == null) {
            return null;
        }

        Image icon = new Image(drawable);
        icon.setScaling(Scaling.fit);
        icon.setTouchable(Touchable.disabled);
        return icon;
    }

    private Table createLevelLayer() {
        Table layer = new Table();
        layer.top().right();
        layer.add(this.levelLabel)
                .padTop(1f)
                .padRight(4f);
        return layer;
    }

    private Table createBoostLayer() {
        Table layer = new Table();
        layer.bottom().left();
        layer.add(this.boostIcon)
                .size(27f)
                .padBottom(4f)
                .padLeft(5f);
        return layer;
    }

    private Table createUpgradeLayer() {
        Table layer = new Table();
        layer.top().right();
        layer.add(this.upgradeIcon)
                .size(29f)
                .padTop(26f)
                .padRight(2f);
        return layer;
    }

    private Table createLockDimLayer() {
        Table dim = new Table();
        dim.setBackground(this.skin.newDrawable(WHITE_PIXEL, LOCK_DIM));
        dim.setTouchable(Touchable.disabled);
        return dim;
    }

    private Table createLockLayer() {
        Table layer = new Table();
        layer.right();
        layer.add(this.lockIcon)
                .size(30f, 40f)
                .padRight(23f)
                .padTop(4f);
        return layer;
    }

    private Stack createSeedPacketProgress() {
        Stack progress = new Stack();
        progress.add(this.seedPacketBar);

        Table iconLayer = new Table();
        iconLayer.left();
        String iconName = this.upgradeAvailable
                ? SEED_ICON_GREEN
                : SEED_ICON_YELLOW;
        iconLayer.add(new Image(this.skin.getDrawable(iconName)))
                .size(19f, 20f)
                .padLeft(1f);
        progress.add(iconLayer);

        Table textLayer = new Table();
        textLayer.add(this.seedPacketLabel).padLeft(16f);
        progress.add(textLayer);
        return progress;
    }

    private ProgressBar createSeedPacketBar(PlantCollectionState state) {
        boolean green = this.isUpgradeAvailable(state)
                || state.getCurrentLevel() >= state.getMaximumLevel();
        Drawable background = this.skin.getDrawable(SEED_BAR_BACKGROUND);
        Drawable fill = this.skin.getDrawable(green ? SEED_BAR_GREEN : SEED_BAR_YELLOW);

        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
        style.background = background;
        style.knobBefore = fill;

        int required = state.getRequiredSeedPackets();
        float maximum = required > 0 ? required : 1f;
        ProgressBar bar = new ProgressBar(0f, maximum, 1f, false, style);
        bar.setAnimateDuration(0f);
        bar.setValue(required > 0
                ? Math.min(state.getSeedPackets(), required)
                : maximum);
        bar.setTouchable(Touchable.disabled);
        return bar;
    }

    private PamAnimationActor createAnimation(
            PamPlayer pamPlayer,
            PamAnimationCatalog.AnimationInfo animationInfo,
            PlantCardVisualProfile visualProfile
    ) {
        if (pamPlayer == null || animationInfo == null) {
            return null;
        }
        if (animationInfo.getIdleClip() == null) {
            return null;
        }
        if (this.textureBank != null && !PamTextureAvailability.allTexturesAvailable(
                this.textureBank,
                Gdx.files.internal("IMAGES/" + animationInfo.getPath())
        )) {
            return null;
        }

        PamAnimationActor animation = new PamAnimationActor(
                pamPlayer,
                animationInfo.getPath(),
                animationInfo.getIdleClip(),
                animationInfo.getCanvasWidth(),
                animationInfo.getCanvasHeight()
        );
        animation.applyVisualProfile(visualProfile);
        return animation;
    }

    private Label createOverlayLabel(String text, float fontScale) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setAlignment(Align.center);
        label.setFontScale(fontScale);
        return label;
    }

    private Image createResourceImage(String resourceId, String skinFallback) {
        Drawable drawable = this.packetDrawable(resourceId);
        if (drawable == null) {
            drawable = this.skin.getDrawable(skinFallback);
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        return image;
    }

    private void updatePictureBackground() {
        if (this.boostOverlay != null) {
            this.boostOverlay.setVisible(this.boosted);
        }
        if (this.pictureSurface == null || this.packetFrameDrawable != null) {
            return;
        }

        Color background = this.boosted ? BOOST_BACKGROUND : TEAL_BACKGROUND;
        this.pictureSurface.setBackground(this.skin.newDrawable(WHITE_PIXEL, background));
    }

    private void installCardClickHandler() {
        this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onPlantCardClicked(PlantCard.this);
                }
            }
        });
    }

    private void installUpgradeClickHandler() {
        this.upgradeIcon.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (upgradeAvailable && actionListener != null) {
                    actionListener.onUpgradeRequested(PlantCard.this);
                }
            }
        });
    }

    private void installPurchaseClickHandler() {
        this.lockIcon.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (!plantState.isUnlocked() && actionListener != null) {
                    actionListener.onPurchaseRequested(PlantCard.this);
                }
            }
        });
    }

    private boolean isUpgradeAvailable(PlantCollectionState state) {
        if (state == null || !state.isUnlocked()) {
            return false;
        }
        if (state.getCurrentLevel() >= state.getMaximumLevel()) {
            return false;
        }
        return state.getRequiredSeedPackets() > 0
                && state.getSeedPackets() >= state.getRequiredSeedPackets();
    }

    private String seedPacketText(PlantCollectionState state) {
        if (state.getCurrentLevel() >= state.getMaximumLevel()) {
            return "MAX";
        }

        return state.getSeedPackets() + "/" + state.getRequiredSeedPackets();
    }

    private PlantCategory primaryCategory() {
        Set<PlantCategory> categories = this.plantState.getCategories();
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.iterator().next();
    }

    private Color categoryColor(PlantCategory category) {
        if (category == null) {
            return new Color(0.35f, 0.55f, 0.30f, 1f);
        }

        switch (category) {
            case SUN_PRODUCER:
                return new Color(0.96f, 0.70f, 0.12f, 1f);
            case SHOOTER:
                return new Color(0.18f, 0.67f, 0.28f, 1f);
            case LOBBER:
                return new Color(0.57f, 0.36f, 0.18f, 1f);
            case EXPLOSIVE:
                return new Color(0.93f, 0.28f, 0.14f, 1f);
            case MELEE_ATTACKER:
                return new Color(0.55f, 0.20f, 0.60f, 1f);
            case DEFENDER:
                return new Color(0.64f, 0.51f, 0.20f, 1f);
            case MODIFIER:
                return new Color(0.72f, 0.28f, 0.58f, 1f);
            case STRIKE_THROUGH:
                return new Color(0.16f, 0.63f, 0.58f, 1f);
            case HOMING:
                return new Color(0.37f, 0.25f, 0.68f, 1f);
            case MINT:
                return new Color(0.20f, 0.70f, 0.50f, 1f);
            default:
                return new Color(0.35f, 0.55f, 0.30f, 1f);
        }
    }

    private String categoryLetter(PlantCategory category) {
        if (category == null) {
            return "P";
        }

        switch (category) {
            case SUN_PRODUCER:
                return "S";
            case SHOOTER:
                return "P";
            case LOBBER:
                return "L";
            case EXPLOSIVE:
                return "B";
            case MELEE_ATTACKER:
                return "M";
            case DEFENDER:
                return "D";
            case MODIFIER:
                return "T";
            case STRIKE_THROUGH:
                return "X";
            case HOMING:
                return "H";
            case MINT:
                return "N";
            default:
                return "P";
        }
    }

    private String formatSeconds(double seconds) {
        return BigDecimal.valueOf(seconds)
                .stripTrailingZeros()
                .toPlainString() + "s";
    }

    public interface PlantCardActionListener {
        void onPlantCardClicked(PlantCard plantCard);

        default void onUpgradeRequested(PlantCard plantCard) {
        }

        default void onPurchaseRequested(PlantCard plantCard) {
        }
    }
}
