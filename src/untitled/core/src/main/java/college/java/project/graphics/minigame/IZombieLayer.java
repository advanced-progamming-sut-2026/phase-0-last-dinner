package college.java.project.graphics.minigame;

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
import college.java.project.graphics.GameplayWorldLayout;
import college.java.project.graphics.ZombiePacketCatalog;
import controller.IZombieController;
import model.mechanism.Position;
import model.minigame.izombieminigame.IZombieActionResult;
import model.minigame.izombieminigame.IZombieStateResult;
import model.zombie.ZombieDefinition;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import java.util.ArrayList;
import java.util.List;

public final class IZombieLayer extends Group {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;
    private static final float PANEL_X = 170f;
    private static final float CARD_TOP = 960f;
    private static final float CARD_WIDTH = 290f;
    private static final float CARD_HEIGHT = 104f;
    private static final float CARD_GAP = 8f;
    private static final Color CARD_NORMAL = new Color(0.17f, 0.12f, 0.09f, 0.94f);
    private static final Color CARD_SELECTED = new Color(0.55f, 0.18f, 0.12f, 0.98f);
    private static final Color CARD_DISABLED = new Color(0.12f, 0.12f, 0.12f, 0.88f);
    private static final Color PLACEMENT = new Color(0.85f, 0.16f, 0.12f, 0.18f);
    private static final Color RED_LINE = new Color(0.96f, 0.08f, 0.05f, 0.88f);

    private final IZombieController controller;
    private final GameAssetManager assets;
    private final Skin skin;
    private final List<RenderedZombieCard> cards = new ArrayList<>();
    private final List<String> zombieSignature = new ArrayList<>();
    private final Table[][] placementCells = new Table[ROW_COUNT][COLUMN_COUNT];
    private final Image[] brainActors = new Image[ROW_COUNT];
    private final boolean[] brainEaten = new boolean[ROW_COUNT];
    private final Texture brainTexture;
    private final Image redLine;
    private final Label sunLabel;
    private final Label stageLabel;
    private final Label statusLabel;
    private IZombieStateResult currentState;
    private ZombieDefinition selectedZombie;

    public IZombieLayer(IZombieController controller, GameAssetManager assets) {
        if (controller == null || assets == null) {
            throw new IllegalArgumentException("I Zombie layer dependencies are required.");
        }

        this.controller = controller;
        this.assets = assets;
        this.skin = PvzSkin.get();
        this.redLine = new Image(this.skin.newDrawable("white_pixel", RED_LINE));
        this.sunLabel = new Label("SUN: 0", this.skin, "medium_outline");
        this.stageLabel = new Label("I, ZOMBIE", this.skin, "medium_outline");
        this.statusLabel = new Label("", this.skin, "medium_outline");

        this.brainTexture = new Texture(Gdx.files.internal("ui/izombie_brain.png"));
        this.brainTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        setTouchable(Touchable.childrenOnly);
        createPlacementCells();
        createRedLine();
        createBrains();
        createLabels();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();

        IZombieStateResult state = this.controller.onShowIZombieRequested();
        if (state == null)
            return;

        this.currentState = state;
        syncZombieCards(state.getAvailableZombies());
        refreshCards();
        refreshPlacementCells();
        refreshBrains();
        refreshLabels();
        refreshRedLine();
    }

    private void createPlacementCells() {
        float cellWidth = GameplayWorldLayout.cellWidth();
        float cellHeight = GameplayWorldLayout.cellHeight();

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Table cell = new Table();
                float x = GameplayWorldLayout.LAWN_X + column * cellWidth;
                float y = GameplayWorldLayout.LAWN_Y + (ROW_COUNT - row - 1) * cellHeight;

                cell.setBounds(x, y, cellWidth, cellHeight);
                cell.setTouchable(Touchable.disabled);

                int targetColumn = column;
                int targetRow = row;

                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        placeSelectedZombie(targetColumn, targetRow);
                    }
                });

                this.placementCells[row][column] = cell;
                addActor(cell);
            }
        }
    }

    private void createRedLine() {
        this.redLine.setTouchable(Touchable.disabled);
        addActor(this.redLine);
    }

    private void createBrains() {
        for (int row = 0; row < ROW_COUNT; row++) {
            Image brain = new Image(this.brainTexture);
            brain.setScaling(Scaling.fit);
            brain.setTouchable(Touchable.disabled);

            float x = GameplayWorldLayout.LAWN_X - 96f;
            float y = GameplayWorldLayout.cellCenterY(row) - 43f;
            brain.setBounds(x, y, 90f, 86f);

            this.brainActors[row] = brain;
            addActor(brain);
        }
    }

    private void createLabels() {
        this.stageLabel.setAlignment(Align.center);
        this.stageLabel.setBounds(PANEL_X, 1034f, CARD_WIDTH, 38f);

        this.sunLabel.setAlignment(Align.center);
        this.sunLabel.setBounds(PANEL_X, 990f, CARD_WIDTH, 38f);

        this.statusLabel.setAlignment(Align.center);
        this.statusLabel.setColor(Color.WHITE);
        this.statusLabel.setTouchable(Touchable.disabled);
        this.statusLabel.setBounds(650f, 22f, 760f, 46f);

        addActor(this.stageLabel);
        addActor(this.sunLabel);
        addActor(this.statusLabel);
    }

    private void syncZombieCards(List<ZombieDefinition> zombies) {
        List<String> signature = createZombieSignature(zombies);
        if (this.zombieSignature.equals(signature))
            return;

        for (RenderedZombieCard card : this.cards) {
            card.root.remove();
        }

        this.cards.clear();
        this.zombieSignature.clear();
        this.zombieSignature.addAll(signature);
        this.selectedZombie = null;

        if (zombies == null)
            return;

        for (int index = 0; index < zombies.size(); index++) {
            createZombieCard(zombies.get(index), index);
        }
    }

    private List<String> createZombieSignature(List<ZombieDefinition> zombies) {
        List<String> signature = new ArrayList<>();
        if (zombies == null)
            return signature;

        for (ZombieDefinition zombie : zombies) {
            signature.add(zombie == null || zombie.getAlias() == null ? "" : zombie.getAlias());
        }

        return signature;
    }

    private void createZombieCard(ZombieDefinition definition, int index) {
        Group root = new Group();
        float y = CARD_TOP - CARD_HEIGHT - index * (CARD_HEIGHT + CARD_GAP);

        root.setBounds(PANEL_X, y, CARD_WIDTH, CARD_HEIGHT);
        root.setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
        root.setTransform(true);
        root.setTouchable(Touchable.enabled);

        Table background = new Table();
        background.setBounds(0f, 0f, CARD_WIDTH, CARD_HEIGHT);
        background.setTouchable(Touchable.disabled);
        root.addActor(background);

        Image packet = createPacketImage(definition);
        if (packet != null) {
            packet.setBounds(6f, 4f, 98f, 96f);
            root.addActor(packet);
        }

        Label name = new Label(displayName(definition), this.skin, "secondary");
        name.setAlignment(Align.left);
        name.setEllipsis(true);
        name.setFontScale(0.62f);
        name.setBounds(108f, 50f, 174f, 42f);
        name.setTouchable(Touchable.disabled);
        root.addActor(name);

        Label details = new Label("", this.skin, "secondary");
        details.setAlignment(Align.left);
        details.setFontScale(0.54f);
        details.setBounds(108f, 12f, 174f, 34f);
        details.setTouchable(Touchable.disabled);
        root.addActor(details);

        RenderedZombieCard card = new RenderedZombieCard(definition, root, background, details);

        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectZombie(card);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                if (card.definition != selectedZombie)
                    card.root.addAction(Actions.scaleTo(1.04f, 1.04f, 0.10f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
                if (card.definition != selectedZombie)
                    card.root.addAction(Actions.scaleTo(1f, 1f, 0.10f));
            }
        });

        this.cards.add(card);
        addActor(root);
    }

    private Image createPacketImage(ZombieDefinition definition) {
        if (definition == null)
            return null;

        ZombiePacketCatalog.PacketVisual visual = ZombiePacketCatalog.findPacket(definition.getAlias());
        Drawable drawable = visual == null ? null : resourceDrawable(visual.getResourceId());

        if (drawable == null)
            return null;

        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void selectZombie(RenderedZombieCard card) {
        if (card == null || this.currentState == null)
            return;

        if (!this.currentState.canAfford(card.definition)) {
            showStatus("Not enough sun for " + displayName(card.definition) + ".");
            return;
        }

        if (!this.currentState.isZombieReady(card.definition)) {
            int ticks = this.currentState.getZombieCooldownTicks(card.definition);
            showStatus(displayName(card.definition) + " is ready in " + cooldownText(ticks) + ".");
            return;
        }

        this.selectedZombie = this.selectedZombie == card.definition ? null : card.definition;
        refreshCards();
        refreshPlacementCells();
    }

    private void refreshCards() {
        if (this.currentState == null)
            return;

        for (RenderedZombieCard card : this.cards) {
            boolean selected = card.definition == this.selectedZombie;
            boolean affordable = this.currentState.canAfford(card.definition);
            boolean ready = this.currentState.isZombieReady(card.definition);
            boolean available = affordable && ready;
            Color colour = selected ? CARD_SELECTED : available ? CARD_NORMAL : CARD_DISABLED;

            card.background.setBackground(this.skin.newDrawable("white_pixel", colour));

            int cost = this.currentState.getZombieCost(card.definition);
            int cooldown = this.currentState.getZombieCooldownTicks(card.definition);
            String stateText = ready ? "READY" : cooldownText(cooldown);

            card.details.setText("SUN " + cost + "   " + stateText);
            card.root.getColor().a = available ? 1f : 0.68f;
            card.root.setScale(selected ? 1.06f : 1f);
        }
    }

    private void refreshPlacementCells() {
        boolean selecting = this.selectedZombie != null && this.currentState != null;
        int redLineColumn = this.currentState == null ? COLUMN_COUNT : this.currentState.getRedLineColumn();

        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                Table cell = this.placementCells[row][column];
                boolean available = selecting && column + 1 > redLineColumn;

                cell.setTouchable(available ? Touchable.enabled : Touchable.disabled);
                cell.setBackground(available ? this.skin.newDrawable("white_pixel", PLACEMENT) : null);
            }
        }
    }

    private void placeSelectedZombie(int column, int row) {
        if (this.selectedZombie == null || this.currentState == null)
            return;

        IZombieActionResult result = this.controller.onPlaceZombieRequested(
            this.selectedZombie.getAlias(), new Position(column + 1, row + 1));

        showStatus(result == null ? "Zombie placement failed." : result.getMessage());

        if (result == null || !result.isSuccessful() || !result.hasPlacementInformation())
            return;

        this.selectedZombie = null;
        refreshCards();
        refreshPlacementCells();
    }

    private String cooldownText(int ticks) {
        int safeTicks = Math.max(0, ticks);
        int wholeSeconds = safeTicks / 10;
        int tenths = safeTicks % 10;
        return wholeSeconds + "." + tenths + "s";
    }

    private void refreshBrains() {
        if (this.currentState == null)
            return;

        for (int row = 0; row < ROW_COUNT; row++) {
            boolean eaten = this.currentState.isBrainEaten(row + 1);
            if (eaten == this.brainEaten[row])
                continue;

            Image brain = this.brainActors[row];
            brain.clearActions();

            if (eaten) {
                brain.addAction(Actions.sequence(
                    Actions.parallel(
                        Actions.fadeOut(0.25f),
                        Actions.scaleTo(0.65f, 0.65f, 0.25f)
                    ),
                    Actions.visible(false)
                ));
            } else {
                brain.setVisible(true);
                brain.setScale(0.65f);
                brain.getColor().a = 0f;
                brain.addAction(Actions.parallel(
                    Actions.fadeIn(0.25f),
                    Actions.scaleTo(1f, 1f, 0.25f)
                ));
            }

            this.brainEaten[row] = eaten;
        }
    }

    private void refreshLabels() {
        if (this.currentState == null)
            return;

        this.sunLabel.setText("SUN: " + this.currentState.getSunAmount());
        this.stageLabel.setText("I, ZOMBIE  " + this.currentState.getStageNumber() + "/" + this.currentState.getStageCount());
    }

    private void refreshRedLine() {
        if (this.currentState == null)
            return;

        float x = GameplayWorldLayout.LAWN_X + this.currentState.getRedLineColumn() * GameplayWorldLayout.cellWidth();

        this.redLine.setBounds(x - 5f, GameplayWorldLayout.LAWN_Y, 10f, GameplayWorldLayout.LAWN_HEIGHT);
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

    private String displayName(ZombieDefinition definition) {
        if (definition == null)
            return "Zombie";

        String name = definition.getDisplayName();
        return name == null || name.isBlank() ? definition.getAlias() : name;
    }

    private void showStatus(String message) {
        this.statusLabel.setText(message == null ? "" : message);
        this.statusLabel.clearActions();
        this.statusLabel.getColor().a = 1f;
        this.statusLabel.addAction(Actions.sequence(Actions.delay(1.8f), Actions.fadeOut(0.25f)));
    }

    public void dispose() {
        this.brainTexture.dispose();
    }

    private record RenderedZombieCard(ZombieDefinition definition, Group root, Table background, Label details) {
    }
}
