package college.java.project.graphics.minigame;

import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.GameplayWorldLayout;
import college.java.project.graphics.PlantPacketCatalog;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.VasebreakerController;
import model.mechanism.Position;
import model.minigame.vasebreakerminigame.DroppedSeedPacket;
import model.minigame.vasebreakerminigame.VasebreakerActionResult;
import model.minigame.vasebreakerminigame.VasebreakerActionStatus;
import model.minigame.vasebreakerminigame.VasebreakerStateResult;
import pvz.skin.PvzSkin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class VasebreakerSeedPacketLayer extends Group {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;
    private static final float DROP_WIDTH = 96f;
    private static final float DROP_HEIGHT = 76f;
    private static final float INVENTORY_WIDTH = 118f;
    private static final float INVENTORY_HEIGHT = 90f;

    private static final float INVENTORY_BANK_X = 276f;
    private static final float INVENTORY_BANK_WIDTH = 146f;
    private static final float INVENTORY_X = INVENTORY_BANK_X + (INVENTORY_BANK_WIDTH - INVENTORY_WIDTH) / 2f;

    private static final float INVENTORY_TOP = 954f;
    private static final float INVENTORY_GAP = 8f;
    private static final String PACKET_FRAME = "IMAGE_UI_PACKETS_READY";

    private static final int INVENTORY_ROWS_PER_COLUMN = 7;
    private static final float INVENTORY_COLUMN_GAP = 8f;

    private final VasebreakerController controller;
    private final GameAssetManager assets;
    private final Skin skin;
    private final Map<DroppedSeedPacket, RenderedPacket> droppedActors = new IdentityHashMap<>();
    private final Map<DroppedSeedPacket, RenderedPacket> collectedActors = new IdentityHashMap<>();
    private final Actor[][] plantCells = new Actor[ROW_COUNT][COLUMN_COUNT];
    private final Label statusLabel;
    private DroppedSeedPacket selectedPacket;

    public VasebreakerSeedPacketLayer(VasebreakerController controller, GameAssetManager assets) {
        if (controller == null || assets == null) {
            throw new IllegalArgumentException("Seed-packet layer dependencies are required");
        }

        this.controller = controller;
        this.assets = assets;
        this.skin = PvzSkin.get();
        this.statusLabel = new Label("", this.skin, "medium_outline");

        setTouchable(Touchable.childrenOnly);
        createPlantCells();
        configureStatusLabel();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        syncPackets();
    }

    private void createPlantCells() {
        float cellWidth = GameplayWorldLayout.cellWidth();
        float cellHeight = GameplayWorldLayout.cellHeight();

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Actor cell = new Actor();
                float x = GameplayWorldLayout.LAWN_X + column * cellWidth;
                float y = GameplayWorldLayout.LAWN_Y + (ROW_COUNT - row - 1) * cellHeight;

                cell.setBounds(x, y, cellWidth, cellHeight);
                cell.setTouchable(Touchable.disabled);

                int targetColumn = column;
                int targetRow = row;

                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        plantSelectedPacket(targetColumn, targetRow);
                    }
                });

                this.plantCells[row][column] = cell;
                addActor(cell);
            }
        }
    }

    private void configureStatusLabel() {
        this.statusLabel.setAlignment(Align.center);
        this.statusLabel.setColor(Color.WHITE);
        this.statusLabel.setTouchable(Touchable.disabled);
        this.statusLabel.setBounds(610f, 24f, 760f, 48f);
        addActor(this.statusLabel);
    }

    private void syncPackets() {
        VasebreakerStateResult state = this.controller.onShowVasebreakerRequested();
        if (state == null) {
            return;
        }

        syncDroppedPackets(state);
        syncCollectedPackets(state.getCollectedSeedPackets());
        updateDroppedExpiry(state.getCurrentTick());
        updatePlantCells();
    }

    private void syncDroppedPackets(VasebreakerStateResult state) {
        Set<DroppedSeedPacket> active = identitySet();

        for (DroppedSeedPacket packet : state.getDroppedSeedPackets()) {
            if (packet == null || !packet.isAvailable(state.getCurrentTick())) {
                continue;
            }

            active.add(packet);
            if (!this.droppedActors.containsKey(packet)) {
                createDroppedActor(packet);
            }
        }

        removeMissing(this.droppedActors, active);
    }

    private void syncCollectedPackets(List<DroppedSeedPacket> packets) {
        Set<DroppedSeedPacket> active = identitySet();

        for (DroppedSeedPacket packet : packets) {
            if (packet == null || !packet.isPlantable()) {
                continue;
            }

            active.add(packet);
            if (!this.collectedActors.containsKey(packet)) {
                createCollectedActor(packet);
            }
        }

        removeMissing(this.collectedActors, active);

        if (this.selectedPacket != null && !active.contains(this.selectedPacket)) {
            this.selectedPacket = null;
        }

        layoutCollectedPackets(packets);
        refreshSelection();
    }

    private void createDroppedActor(DroppedSeedPacket packet) {
        Stack root = createPacketVisual(packet, DROP_WIDTH, DROP_HEIGHT);

        Position position = packet.getPosition();
        int column = position.getX() - 1;
        int row = position.getY() - 1;

        root.setPosition(
            GameplayWorldLayout.cellCenterX(column) - DROP_WIDTH / 2f,
            GameplayWorldLayout.cellCenterY(row) - DROP_HEIGHT / 2f
        );
        root.setOrigin(DROP_WIDTH / 2f, DROP_HEIGHT / 2f);
        root.addAction(Actions.forever(Actions.sequence(
            Actions.scaleTo(1.06f, 1.06f, 0.38f),
            Actions.scaleTo(1f, 1f, 0.38f)
        )));

        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                collectPacket(packet);
            }
        });

        this.droppedActors.put(packet, new RenderedPacket(root));
        addActor(root);
    }

    private void createCollectedActor(DroppedSeedPacket packet) {
        Stack root = createPacketVisual(packet, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        root.setOrigin(INVENTORY_WIDTH / 2f, INVENTORY_HEIGHT / 2f);

        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectPacket(packet);
            }
        });

        this.collectedActors.put(packet, new RenderedPacket(root));
        addActor(root);
    }

    private Stack createPacketVisual(DroppedSeedPacket packet, float width, float height) {
        Stack root = new Stack();
        root.setSize(width, height);
        root.setTransform(true);
        root.setTouchable(Touchable.enabled);

        Drawable frameDrawable = resourceDrawable(PACKET_FRAME);

        if (frameDrawable != null) {
            Image frame = new Image(frameDrawable);
            frame.setScaling(Scaling.stretch);
            frame.setTouchable(Touchable.disabled);
            root.add(frame);
        }

        Table artworkLayer = new Table();
        artworkLayer.setTouchable(Touchable.disabled);

        Drawable artwork = packetDrawable(packet == null ? null : packet.getPlantName());

        if (artwork != null) {
            Image image = new Image(artwork);
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            artworkLayer.add(image).grow().pad(7f, 9f, 5f, 9f);
        } else {
            String plantName = packet == null || packet.getPlantName() == null ? "Plant" : packet.getPlantName();
            Label fallback = new Label(plantName, this.skin, "secondary");
            fallback.setAlignment(Align.center);
            fallback.setWrap(true);
            fallback.setTouchable(Touchable.disabled);
            artworkLayer.add(fallback).grow().pad(8f);
        }

        root.add(artworkLayer);
        return root;
    }

    private Drawable packetDrawable(String plantName) {
        PlantPacketCatalog.PacketVisual visual = PlantPacketCatalog.findPacket(plantName);
        return visual == null ? null : resourceDrawable(visual.getResourceId());
    }

    private Drawable resourceDrawable(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }

        try {
            TextureRegion region = this.assets.getTextureBank().region(resourceId);
            if (region != null) {
                return new TextureRegionDrawable(region);
            }
        } catch (RuntimeException ignored) {
        }

        if (this.skin.has(resourceId, Drawable.class)) {
            return this.skin.getDrawable(resourceId);
        }

        String normalised = resourceId.toLowerCase(Locale.ROOT);
        return this.skin.has(normalised, Drawable.class) ? this.skin.getDrawable(normalised) : null;
    }

    private void collectPacket(DroppedSeedPacket packet) {
        VasebreakerActionResult result = this.controller.onCollectSeedPacketRequested(packet.getPosition());

        if (result != null && result.getStatus() == VasebreakerActionStatus.SEED_PACKET_COLLECTED) {
            showStatus(packet.getPlantName() + " collected.");
            syncPackets();
            return;
        }

        showStatus(messageFor(result));
    }

    private void selectPacket(DroppedSeedPacket packet) {
        this.selectedPacket = this.selectedPacket == packet ? null : packet;
        refreshSelection();
        updatePlantCells();

        String message = this.selectedPacket == null ? "Selection cleared." : packet.getPlantName() + " selected.";
        showStatus(message);
    }

    private void plantSelectedPacket(int column, int row) {
        if (this.selectedPacket == null) {
            return;
        }

        VasebreakerActionResult result = this.controller.onPlantSeedPacketRequested(
            this.selectedPacket.getPlantName(),
            new Position(column + 1, row + 1)
        );

        if (result != null && result.getStatus() == VasebreakerActionStatus.PLANT_FROM_PACKET) {
            showStatus(this.selectedPacket.getPlantName() + " planted.");
            this.selectedPacket = null;
            syncPackets();
            return;
        }

        showStatus(messageFor(result));
    }

    private void layoutCollectedPackets(List<DroppedSeedPacket> packets) {
        int index = 0;

        for (DroppedSeedPacket packet : packets) {
            RenderedPacket rendered = this.collectedActors.get(packet);

            if (rendered == null)
                continue;

            int column = index / INVENTORY_ROWS_PER_COLUMN;
            int row = index % INVENTORY_ROWS_PER_COLUMN;

            float x = INVENTORY_X - column * (INVENTORY_WIDTH + INVENTORY_COLUMN_GAP);
            float y = INVENTORY_TOP - INVENTORY_HEIGHT - row * (INVENTORY_HEIGHT + INVENTORY_GAP);

            rendered.root.setPosition(x, y);
            index++;
        }
    }

    private void refreshSelection() {
        for (Map.Entry<DroppedSeedPacket, RenderedPacket> entry : this.collectedActors.entrySet()) {
            boolean selected = entry.getKey() == this.selectedPacket;
            entry.getValue().root.setScale(selected ? 1.10f : 1f);
            entry.getValue().root.setColor(selected ? Color.LIME : Color.WHITE);
        }
    }

    private void updatePlantCells() {
        Touchable touchable = this.selectedPacket == null ? Touchable.disabled : Touchable.enabled;

        for (Actor[] row : this.plantCells) {
            for (Actor cell : row) {
                cell.setTouchable(touchable);
            }
        }
    }

    private void updateDroppedExpiry(long currentTick) {
        for (Map.Entry<DroppedSeedPacket, RenderedPacket> entry : this.droppedActors.entrySet()) {
            long remaining = entry.getKey().getRemainingTicks(currentTick);
            entry.getValue().root.getColor().a = remaining >= 60 ? 1f : 0.45f + 0.55f * remaining / 60f;
        }
    }

    private void removeMissing(Map<DroppedSeedPacket, RenderedPacket> actors, Set<DroppedSeedPacket> active) {
        Iterator<Map.Entry<DroppedSeedPacket, RenderedPacket>> iterator = actors.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<DroppedSeedPacket, RenderedPacket> entry = iterator.next();

            if (!active.contains(entry.getKey())) {
                entry.getValue().root.remove();
                iterator.remove();
            }
        }
    }

    private Set<DroppedSeedPacket> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private String messageFor(VasebreakerActionResult result) {
        if (result == null) {
            return "Action failed.";
        }

        if (result.getStatus() == VasebreakerActionStatus.TILE_HAS_UNBROKEN_VASE) {
            return "Break the vase first.";
        }

        if (result.getStatus() == VasebreakerActionStatus.TILE_OCCUPIED) {
            return "That tile is occupied.";
        }

        if (result.getStatus() == VasebreakerActionStatus.SEED_PACKET_NOT_AVAILABLE) {
            return "That seed packet is no longer available.";
        }

        return "Action failed: " + result.getStatus().name();
    }

    private void showStatus(String message) {
        this.statusLabel.setText(message == null ? "" : message);
        this.statusLabel.clearActions();
        this.statusLabel.getColor().a = 1f;
        this.statusLabel.addAction(Actions.sequence(Actions.delay(1.6f), Actions.fadeOut(0.25f)));
    }

    private static final class RenderedPacket {
        private final Stack root;

        private RenderedPacket(Stack root) {
            this.root = root;
        }
    }
}
