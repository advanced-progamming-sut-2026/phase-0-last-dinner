package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.collection.ZombieCollectionState;
import pvz.libpvz.textures.TextureBank;


public final class ZombieCard extends Group {
    public static final float CARD_WIDTH = 132f;
    public static final float CARD_HEIGHT = 185f;

    private static final String READY_FRAME = "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_READY";
    private static final String SELECTED_FRAME = "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_SELECTED";
    private static final float SOURCE_FRAME_WIDTH = 112f;
    private static final float SOURCE_FRAME_HEIGHT = 157f;
    private static final float ART_BOTTOM = 9f;
    private static final float ART_TOP = 8f;
    private static final float ART_SIDE = 8f;
    private static final Color FALLBACK_FRAME = new Color(0.11f, 0.17f, 0.23f, 1f);

    private final ZombieCollectionState zombieState;
    private final Skin skin;
    private final TextureBank textureBank;
    private final Group selectionLayer;
    private ZombieCardActionListener actionListener;

    public ZombieCard(
            ZombieCollectionState zombieState,
            Skin skin,
            TextureBank textureBank,
            ZombieCardActionListener actionListener
    ) {
        if (zombieState == null || skin == null || textureBank == null) {
            throw new IllegalArgumentException("Zombie card resources are required");
        }
        this.zombieState = zombieState;
        this.skin = skin;
        this.textureBank = textureBank;
        this.actionListener = actionListener;
        this.selectionLayer = new Group();
        this.selectionLayer.setTouchable(Touchable.disabled);

        setSize(CARD_WIDTH, CARD_HEIGHT);
        setTouchable(Touchable.enabled);
        buildCard();
        installClickHandler();
        CollectionUiAnimator.installHoverScale(this);
    }

    public ZombieCollectionState getZombieState() {
        return this.zombieState;
    }

    public void setSelected(boolean selected) {
        this.selectionLayer.setVisible(selected);
    }

    public void setActionListener(ZombieCardActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private void buildCard() {
        addActor(createFrame(READY_FRAME, FALLBACK_FRAME));
        addSelectionLayer();
        if (this.zombieState.isEncountered()) {
            addEncounteredArtwork();
        }
    }

    private Image createFrame(String resourceId, Color fallbackColor) {
        TextureRegion region = this.textureBank.region(resourceId);
        Image image;
        if (region != null) {
            image = new Image(new TextureRegionDrawable(region));
        } else {
            image = colorImage(fallbackColor);
        }
        image.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void addEncounteredArtwork() {
        ZombiePacketCatalog.PacketVisual packet = ZombiePacketCatalog.findPacket(this.zombieState.getAlias());
        TextureRegion region = packet == null ? null : this.textureBank.region(packet.getResourceId());
        if (region == null) {
            addFallbackName();
            return;
        }

        float sourceScale = CARD_WIDTH / SOURCE_FRAME_WIDTH;
        float width = region.getRegionWidth() * sourceScale;
        float height = region.getRegionHeight() * sourceScale;
        float maxWidth = CARD_WIDTH - 2f * ART_SIDE;
        float maxHeight = CARD_HEIGHT - ART_BOTTOM - ART_TOP;
        float clampScale = Math.min(1f, Math.min(maxWidth / width, maxHeight / height));
        width *= clampScale;
        height *= clampScale;

        Image artwork = new Image(new TextureRegionDrawable(region));
        artwork.setBounds((CARD_WIDTH - width) / 2f, ART_BOTTOM, width, height);
        artwork.setTouchable(Touchable.disabled);
        addActor(artwork);
    }

    private void addFallbackName() {
        Label label = new Label(this.zombieState.getDisplayName(), this.skin, "medium_outline");
        label.setFontScale(0.31f);
        label.setWrap(true);
        label.setAlignment(Align.center);
        label.setBounds(11f, 40f, CARD_WIDTH - 22f, 92f);
        label.setTouchable(Touchable.disabled);
        addActor(label);
    }

    private void addSelectionLayer() {
        TextureRegion selected = this.textureBank.region(SELECTED_FRAME);
        if (selected != null) {
            float scale = CARD_WIDTH / SOURCE_FRAME_WIDTH;
            float width = selected.getRegionWidth() * scale;
            float height = selected.getRegionHeight() * scale;
            Image selection = new Image(new TextureRegionDrawable(selected));
            selection.setBounds((CARD_WIDTH - width) / 2f, (CARD_HEIGHT - height) / 2f, width, height);
            selection.setTouchable(Touchable.disabled);
            this.selectionLayer.addActor(selection);
        } else {
            Image border = colorImage(new Color(1f, 0.84f, 0.16f, 0.18f));
            border.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);
            this.selectionLayer.addActor(border);
        }
        this.selectionLayer.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);
        this.selectionLayer.setVisible(false);
        addActor(this.selectionLayer);
    }

    private Image colorImage(Color color) {
        Image image = new Image(this.skin.getDrawable("white_pixel"));
        image.setColor(color);
        return image;
    }

    private void installClickHandler() {
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onZombieCardClicked(ZombieCard.this);
                }
            }
        });
    }

    public interface ZombieCardActionListener {
        void onZombieCardClicked(ZombieCard card);
    }
}
