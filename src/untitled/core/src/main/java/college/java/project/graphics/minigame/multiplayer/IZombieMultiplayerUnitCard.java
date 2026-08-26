package college.java.project.graphics.minigame.multiplayer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.PlantPacketCatalog;
import college.java.project.graphics.ZombiePacketCatalog;
import lombok.Getter;
import network.izombie.client.IZombieClientGameData;
import network.izombie.protocol.IZombieRole;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.function.Consumer;

public final class IZombieMultiplayerUnitCard extends Group {

    public static final float CARD_WIDTH = 290f;
    public static final float CARD_HEIGHT = 104f;

    private static final Color CARD_NORMAL = new Color(0.17f, 0.12f, 0.09f, 0.94f);

    private static final Color CARD_SELECTED = new Color(0.55f, 0.18f, 0.12f, 0.98f);

    private static final Color CARD_DISABLED = new Color(0.12f, 0.12f, 0.12f, 0.88f);

    @Getter
    private final String unitKey;
    private final IZombieRole role;
    private final IZombieClientGameData data;
    private final Skin skin;

    private final Table background;
    private final Label detailsLabel;
    private final Consumer<String> selectionListener;

    @Getter
    private boolean selected;

    public IZombieMultiplayerUnitCard(String unitKey, String displayName, IZombieRole role, IZombieClientGameData data,
                                      GameAssetManager assets, Consumer<String> selectionListener) {

        if (unitKey == null || unitKey.isBlank() || role == null || data == null || assets == null)
            throw new IllegalArgumentException("Multiplayer unit card dependencies are required.");

        this.unitKey = unitKey;
        this.role = role;
        this.data = data;
        this.selectionListener = selectionListener;
        this.skin = PvzSkin.get();

        setSize(CARD_WIDTH, CARD_HEIGHT);
        setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
        setTransform(true);
        setTouchable(Touchable.enabled);

        background = createBackground();
        addActor(background);

        Image packetImage = createPacketImage(assets);

        if (packetImage != null) {
            packetImage.setBounds(6f, 4f, 98f, 96f);
            addActor(packetImage);
        }

        Label nameLabel = createNameLabel(resolveDisplayName(displayName));

        addActor(nameLabel);

        detailsLabel = createDetailsLabel();
        addActor(detailsLabel);

        addSelectionListener();
        refresh();
    }

    public void setSelected(boolean selected) {
        if (this.selected == selected) {
            return;
        }

        this.selected = selected;

        clearActions();
        addAction(Actions.scaleTo(selected ? 1.06f : 1f, selected ? 1.06f : 1f, 0.10f));

        refresh();
    }

    public void refresh() {
        boolean affordable = data.canAfford(unitKey);
        boolean ready = data.isUnitReady(unitKey);
        boolean available = affordable && ready;

        Color backgroundColour;

        if (selected) {
            backgroundColour = CARD_SELECTED;
        } else if (available) {
            backgroundColour = CARD_NORMAL;
        } else {
            backgroundColour = CARD_DISABLED;
        }

        background.setBackground(skin.newDrawable("white_pixel", backgroundColour));

        int cost = data.getUnitCost(unitKey);
        int cooldown = data.getCooldownTicks(unitKey);

        String stateText = ready ? "READY" : cooldownText(cooldown);

        detailsLabel.setText("SUN " + cost + "   " + stateText);

        getColor().a = available ? 1f : 0.68f;
    }

    private Table createBackground() {
        Table table = new Table();

        table.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);

        table.setTouchable(Touchable.disabled);
        return table;
    }

    private Label createNameLabel(String displayName) {
        Label label = new Label(displayName, skin, "secondary");

        label.setAlignment(Align.left);
        label.setEllipsis(true);
        label.setFontScale(0.62f);

        label.setBounds(108f, 50f, 174f, 42f);

        label.setTouchable(Touchable.disabled);
        return label;
    }

    private Label createDetailsLabel() {
        Label label = new Label("", skin, "secondary");

        label.setAlignment(Align.left);
        label.setFontScale(0.54f);

        label.setBounds(108f, 12f, 174f, 34f);

        label.setTouchable(Touchable.disabled);
        return label;
    }

    private Image createPacketImage(GameAssetManager assets) {
        Drawable drawable;

        if (role == IZombieRole.PLANTS) {
            PlantPacketCatalog.PacketVisual visual = PlantPacketCatalog.findPacket(unitKey);

            drawable = visual == null ? null : resourceDrawable(assets, visual.getResourceId());
        } else {
            ZombiePacketCatalog.PacketVisual visual = ZombiePacketCatalog.findPacket(unitKey);

            drawable = visual == null ? null : resourceDrawable(assets, visual.getResourceId());
        }

        if (drawable == null) {
            return null;
        }

        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);

        return image;
    }

    private Drawable resourceDrawable(GameAssetManager assets, String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }

        try {
            TextureBank bank = assets.getTextureBank();

            if (bank == null || bank.region(resourceId) == null) {
                return null;
            }

            return new TextureRegionDrawable(bank.region(resourceId));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void addSelectionListener() {
        addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectionListener != null) {
                    selectionListener.accept(unitKey);
                }
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (!selected) {
                    clearActions();

                    addAction(Actions.scaleTo(1.04f, 1.04f, 0.10f));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (!selected) {
                    clearActions();

                    addAction(Actions.scaleTo(1f, 1f, 0.10f));
                }
            }
        });
    }

    private String resolveDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }

        return unitKey;
    }

    private String cooldownText(int cooldownTicks) {
        int ticksPerSecond = Math.max(1, data.getTicksPerSecond());

        int safeTicks = Math.max(0, cooldownTicks);
        int wholeSeconds = safeTicks / ticksPerSecond;

        int tenths = (safeTicks % ticksPerSecond) * 10 / ticksPerSecond;

        return wholeSeconds + "." + tenths + "s";
    }
}
