package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.collection.PlantCollectionState;
import model.mechanism.PlantStatus;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reusable normal-stage seed bank. It intentionally reuses PlantCard, matching
 * the Phase 2 requirement shared by Collection, Plant Pick and gameplay.
 */
public final class GameplaySeedBank extends Table {
    private static final float CARD_SCALE = 0.74f;
    private static final String WHITE_PIXEL = "white_pixel";
    private static final String SUN_ICON = "IMAGE_UI_HUD_INGAME_SUN";
    private static final String HUD_BACKGROUND = "IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE";

    private static final Color RESOURCE_BG = new Color(0.05f, 0.08f, 0.06f, 0.92f);

    private final Skin skin;
    private final GameplaySeedBankDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final PamAnimationCatalog animationCatalog;
    private final PlantCardVisualCatalog visualCatalog;
    private final Table cardRow;
    private final Label sunLabel;
    private final Label statusLabel;
    private final List<PlantCard> cards = new ArrayList<>();
    private final Map<String, PlantCard> cardsByName = new HashMap<>();
    private String activePlantName;
    private String imitaterCopyTarget;
    private PlantSelectionListener selectionListener;
    private int lastSunAmount = Integer.MIN_VALUE;

    public GameplaySeedBank(GameplaySeedBankDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplaySeedBank(GameplaySeedBankDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null || assets == null) {
            throw new IllegalArgumentException("Gameplay seed-bank dependencies are required");
        }
        this.skin = PvzSkin.get();
        this.dataSource = dataSource;
        this.assets = assets;
        this.animationCatalog = new PamAnimationCatalog();
        this.visualCatalog = new PlantCardVisualCatalog();
        this.cardRow = new Table();
        this.sunLabel = valueLabel("0");
        this.statusLabel = valueLabel("");
        build();
        refresh();
    }

    public void setPlantSelectionListener(PlantSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public String getActivePlantName() {
        return this.activePlantName;
    }

    public String getImitaterCopyTarget() {
        return this.imitaterCopyTarget;
    }

    public int getCardCount() {
        return this.cards.size();
    }

    public String getStatusText() {
        return this.statusLabel.getText().toString();
    }

    public void showInteractionStatus(String text) {
        showStatus(text);
    }

    public void clearSelection() {
        clearSelectionInternal(true);
    }

    /** Clears the cursor selection without adding a transient gameplay message. */
    public void clearSelectionSilently() {
        clearSelectionInternal(false);
    }

    private void clearSelectionInternal(boolean showMessage) {
        this.imitaterCopyTarget = null;
        this.dataSource.setImitaterCopyTarget(null);
        setActivePlant(null);
        if (showMessage) {
            showStatus("Selection cleared.");
        } else {
            showStatus("");
        }
        if (this.selectionListener != null) {
            this.selectionListener.onPlantSelectionCleared();
        }
    }

    /** Selects a packet delivered by an external source such as Conveyor Belt. */
    public void selectExternalPlant(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            clearSelection();
            return;
        }
        setActivePlant(plantName);
        showStatus(plantName + " selected.");
        if (this.selectionListener != null) {
            this.selectionListener.onPlantSelected(plantName);
        }
    }

    public boolean selectPlant(String plantName) {
        PlantCard card = this.cardsByName.get(normalize(plantName));
        if (card == null) {
            showStatus("Plant is not in the active seed bank.");
            return false;
        }
        this.imitaterCopyTarget = null;
        this.dataSource.setImitaterCopyTarget(null);
        PlantStatus status = statusFor(card.getPlantName());
        if (status == null || !status.isAvailable()) {
            showUnavailableStatus(status);
            return false;
        }
        setActivePlant(card.getPlantName());
        if ("imitater".equals(normalize(card.getPlantName()))) {
            showStatus("Imitater selected. Choose another seed packet to copy.");
        } else {
            showStatus(card.getPlantName() + " selected.");
        }
        if (this.selectionListener != null) {
            this.selectionListener.onPlantSelected(card.getPlantName());
        }
        return true;
    }

    public void updateAssets() {
        this.assets.update();
    }

    public void refresh() {
        int sunAmount = this.dataSource.getSunAmount();
        if (sunAmount != this.lastSunAmount) {
            if (this.lastSunAmount != Integer.MIN_VALUE && sunAmount > this.lastSunAmount) {
                pulseValue(this.sunLabel);
            }
            this.lastSunAmount = sunAmount;
            this.sunLabel.setText(Integer.toString(sunAmount));
        }
        List<PlantCollectionState> selected = this.dataSource.getSelectedPlants();
        if (!matchesCurrentCards(selected)) {
            rebuildCards(selected);
        }
        refreshLiveStatuses();
    }

    public boolean plantActiveAt(int column, int row) {
        if (this.activePlantName == null) {
            showStatus("Select a plant first.");
            return false;
        }
        if ("imitater".equals(normalize(this.activePlantName)) && this.imitaterCopyTarget == null) {
            showStatus("Choose a seed packet for Imitater to copy first.");
            return false;
        }
        boolean planted = this.dataSource.plant(this.activePlantName, column, row);
        if (planted) {
            String plantedName = this.activePlantName;
            showStatus(plantedName + " planted.");
            this.imitaterCopyTarget = null;
            this.dataSource.setImitaterCopyTarget(null);
            setActivePlant(null);
            if (this.selectionListener != null) {
                this.selectionListener.onPlantSelectionCleared();
            }
        } else {
            showStatus(this.dataSource.getPlantingFailureMessage(
                    this.activePlantName,
                    column,
                    row
            ));
        }
        refresh();
        return planted;
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void build() {
        // The original PvZ2 gameplay HUD uses a compact vertical seed bank on
        // the left edge of the lawn instead of a full-width top toolbar.
        setBackground((Drawable) null);
        top().left();
        pad(0f);

        Table sunBox = originalSunCounter();
        add(sunBox).width(142f).height(76f).left().padBottom(0f);
        row();

        Drawable bankBackground = resourceDrawable(HUD_BACKGROUND);
        if (bankBackground != null) {
            this.cardRow.setBackground(bankBackground);
            this.cardRow.pad(3f, 2f, 2f, 2f);
        }
        add(this.cardRow).width(138f).left();
        row();

        this.statusLabel.setAlignment(Align.center);
        this.statusLabel.setColor(PvzVisualTheme.TEXT_CREAM);
        this.statusLabel.setWrap(true);
        this.statusLabel.setFontScale(0.48f);
        this.statusLabel.setVisible(false);
        add(this.statusLabel).width(138f).height(32f).padTop(2f);
    }


    /**
     * Recreates the original PvZ2 in-level sun meter: the full-size sun
     * overlaps the left edge of the dark rounded counter instead of sitting
     * inside a generic resource box.  The source sprites keep their native
     * 768p proportions (70x71 sun, 84x42 three-slice background).
     */
    private Table originalSunCounter() {
        Table root = new Table();
        root.setTouchable(Touchable.disabled);

        Drawable counterBackground = resourceDrawable(HUD_BACKGROUND);
        Table counter = new Table();
        counter.setBackground(counterBackground == null
                ? this.skin.newDrawable(WHITE_PIXEL, RESOURCE_BG)
                : counterBackground);
        counter.add(this.sunLabel).grow().center().padLeft(22f).padRight(7f);

        Stack stack = new Stack();
        Table backgroundLayer = new Table();
        backgroundLayer.left().add().width(31f);
        backgroundLayer.add(counter).width(105f).height(48f);
        stack.add(backgroundLayer);

        Drawable sunDrawable = resourceDrawable(SUN_ICON);
        if (sunDrawable != null) {
            Table sunLayer = new Table();
            sunLayer.left();
            Image sun = new Image(sunDrawable);
            sun.setScaling(Scaling.fit);
            sun.setTouchable(Touchable.disabled);
            sunLayer.add(sun).size(70f, 71f);
            stack.add(sunLayer);
        }

        root.add(stack).width(142f).height(74f);
        return root;
    }

    private void rebuildCards(List<PlantCollectionState> selected) {
        this.cardRow.clearChildren();
        this.cards.clear();
        this.cardsByName.clear();
        PamPlayer pamPlayer = this.assets.getPamPlayer();
        TextureBank textureBank = this.assets.getTextureBank();
        for (PlantCollectionState state : selected) {
            PlantCard card = new PlantCard(
                    this.skin,
                    textureBank,
                    pamPlayer,
                    this.animationCatalog.find(state.getName()),
                    state,
                    this.visualCatalog.find(state.getName())
            );
            card.setSeedProgressVisible(false);
            card.setSunCostVisible(true);
            card.setGameplayMode(true);
            card.setBoosted(this.dataSource.isBoosted(state.getName()));
            card.setSelected(normalize(state.getName()).equals(normalize(this.activePlantName)));
            card.setActionListener(new PlantCard.PlantCardActionListener() {
                @Override
                public void onPlantCardClicked(PlantCard clicked) {
                    selectCard(clicked);
                }
            });
            this.cards.add(card);
            this.cardsByName.put(normalize(state.getName()), card);
            this.cardRow.add(new ScaledPlantCard(card, CARD_SCALE))
                    .width(132f)
                    .height(78f)
                    .padBottom(1f);
            this.cardRow.row();
        }
    }

    private void refreshLiveStatuses() {
        Map<String, PlantStatus> statuses = new HashMap<>();
        for (PlantStatus status : this.dataSource.getPlantStatuses()) {
            if (status != null && status.getPlant() != null) {
                statuses.put(normalize(status.getPlant().getName()), status);
            }
        }
        for (PlantCard card : this.cards) {
            PlantStatus status = statuses.get(normalize(card.getPlantName()));
            card.updateGameplayStatus(status);
            card.setBoosted(this.dataSource.isBoosted(card.getPlantName()));
            card.setSelected(normalize(card.getPlantName()).equals(normalize(this.activePlantName)));
        }
    }

    private void selectCard(PlantCard card) {
        if (card == null) {
            return;
        }
        String name = card.getPlantName();
        if ("imitater".equals(normalize(this.activePlantName))
                && !"imitater".equals(normalize(name))) {
            this.imitaterCopyTarget = name;
            this.dataSource.setImitaterCopyTarget(name);
            showStatus("Imitater will copy " + name + ".");
            if (this.selectionListener != null) {
                this.selectionListener.onImitaterCopyTargetSelected(name);
            }
            return;
        }
        PlantStatus status = statusFor(name);
        if (status == null || !status.isAvailable()) {
            showUnavailableStatus(status);
            return;
        }
        if (normalize(name).equals(normalize(this.activePlantName))) {
            clearSelection();
            return;
        }
        selectPlant(name);
    }

    private void setActivePlant(String name) {
        this.activePlantName = name;
        for (PlantCard card : this.cards) {
            card.setSelected(normalize(card.getPlantName()).equals(normalize(name)));
        }
    }

    private PlantStatus statusFor(String plantName) {
        String normalizedName = normalize(plantName);
        for (PlantStatus status : this.dataSource.getPlantStatuses()) {
            if (status == null || status.getPlant() == null) {
                continue;
            }
            if (normalize(status.getPlant().getName()).equals(normalizedName)) {
                return status;
            }
        }
        return null;
    }

    private void showUnavailableStatus(PlantStatus status) {
        if (status != null && status.getRemainingSeconds() > 0) {
            showStatus("Cooldown: " + formatSeconds(status.getRemainingSeconds()));
        } else {
            showStatus("Not enough sun.");
        }
    }

    private void pulseValue(Label label) {
        label.clearActions();
        label.setOrigin(label.getWidth() / 2f, label.getHeight() / 2f);
        label.addAction(Actions.sequence(
                Actions.scaleTo(1.18f, 1.18f, 0.07f),
                Actions.scaleTo(1f, 1f, 0.10f)
        ));
    }

    private void showStatus(String text) {
        String value = text == null ? "" : text.trim();
        this.statusLabel.setText(value);
        this.statusLabel.clearActions();
        if (value.isEmpty()) {
            this.statusLabel.setVisible(false);
            return;
        }
        this.statusLabel.setVisible(true);
        this.statusLabel.getColor().a = 1f;
        this.statusLabel.addAction(Actions.sequence(
                Actions.delay(1.55f),
                Actions.fadeOut(0.28f),
                Actions.visible(false)
        ));
    }

    private boolean matchesCurrentCards(List<PlantCollectionState> selected) {
        if (selected == null || selected.size() != this.cards.size()) {
            return false;
        }
        for (int index = 0; index < selected.size(); index++) {
            PlantCollectionState state = selected.get(index);
            if (state == null || !normalize(state.getName()).equals(
                    normalize(this.cards.get(index).getPlantName()))) {
                return false;
            }
        }
        return true;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // Skin fallback below is intentional for partial asset packs.
        }
        String normalized = resourceId.toLowerCase(Locale.ROOT);
        if (this.skin.has(normalized, Drawable.class)) {
            return this.skin.getDrawable(normalized);
        }
        return null;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(0.86f);
        label.setAlignment(Align.center);
        label.setColor(Color.WHITE);
        return label;
    }

    private String formatSeconds(double seconds) {
        if (seconds >= 10d) {
            return Integer.toString((int) Math.ceil(seconds)) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", Math.max(0d, seconds));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public interface PlantSelectionListener {
        void onPlantSelected(String plantName);

        default void onImitaterCopyTargetSelected(String plantName) {
        }

        default void onPlantSelectionCleared() {
        }
    }
}
