package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.CollectionController;
import model.collection.CollectionActionResult;
import model.collection.PlantCollectionState;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


@SuppressWarnings("PMD.ExcessiveClassLength")
public final class PlantCollectionScreen implements Screen {
    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;
    private static final int GRID_COLUMNS = 10;
    private static final int DEBUG_COIN_INCREMENT = 1000;
    private static final int DEBUG_GEM_INCREMENT = 10;
    private static final String WHITE_PIXEL = "white_pixel";
    private static final String PLANTS_TAB = "image_ui_almanac_tabs_plants_active";
    private static final String ZOMBIES_TAB = "image_ui_almanac_tabs_zombies_down";
    private static final String CLOSE_TAB = "image_ui_almanac_tabs_close_tab";
    private static final String FILTER_UP = "image_ui_almanac_filter_button_up";
    private static final String FILTER_DOWN = "image_ui_almanac_filter_button_down";
    private static final String SORT_UP = "image_ui_almanac_sort_button_up";
    private static final String SORT_DOWN = "image_ui_almanac_sort_button_down";
    private static final String CURRENCY_BG = "image_ui_generic_button_generic_currency_normal";
    private static final String MINT_ICON = "image_ui_generic_mint_icon_small";
    private static final String GEM_ICON = "image_ui_generic_gem_icon_small";
    private static final String COIN_ICON = "image_ui_generic_coin_icon_small";
    private static final String SCROLL_BG = "image_ui_almanac_card_plant_scrollbar";
    private static final String SCROLL_KNOB = "image_ui_almanac_scroll_slider";
    private static final String PLUS_UP = "image_ui_generic_greenbutton";
    private static final String PLUS_DOWN = "image_ui_generic_greenbutton_down";
    private static final Color HEADER = new Color(0.015f, 0.020f, 0.018f, 1f);
    private static final Color OUTER = new Color(0.23f, 0.105f, 0.028f, 1f);
    private static final Color FRAME = new Color(0.63f, 0.31f, 0.085f, 1f);
    private static final Color INNER = new Color(0.34f, 0.14f, 0.038f, 1f);
    private static final Color GLOW = new Color(0.80f, 0.46f, 0.16f, 1f);
    private static final Color FOOTER = new Color(0.91f, 0.86f, 0.65f, 1f);
    private static final Color FOOTER_BORDER = new Color(0.51f, 0.33f, 0.13f, 1f);
    private static final Color POPUP = new Color(0.28f, 0.12f, 0.03f, 0.98f);
    private static final Color POPUP_ROW = new Color(0.39f, 0.20f, 0.055f, 1f);
    private static final Color SHADE = new Color(0f, 0f, 0f, 0.72f);

    private final Stage stage;
    private final Skin skin;
    private final GameAssetManager assets;
    private final PlantCollectionDataSource dataSource;
    private final PamAnimationCatalog animationCatalog;
    private final PlantCardVisualCatalog visualCatalog;
    private final Table grid;
    private final List<PlantCollectionState> visibleStates = new ArrayList<>();
    private final List<PlantCard> renderedCards = new ArrayList<>();
    private final Label filterLabel;
    private final Label sortLabel;
    private final Label collectedLabel;
    private final Label mintLabel;
    private final Label gemLabel;
    private final Label coinLabel;
    private final Label statusLabel;
    private final Button gemPlus;
    private final Button coinPlus;
    private final Table filterPopup;
    private final Table sortPopup;
    private final Image detailsShade;
    private final PlantDetailsPanel detailsPanel;
    private PlantCollectionFilter filter = PlantCollectionFilter.ALL;
    private PlantCollectionSort sort = PlantCollectionSort.DEFAULT;
    private int detailIndex = -1;
    private String selectedPlantName;
    private String loadErrorMessage = "";
    private Runnable onClose;
    private Runnable onZombiesTab;

    public PlantCollectionScreen(PlantCollectionDataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("Plant Collection data source is required");
        this.dataSource = dataSource;
        this.stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        this.skin = PvzSkin.get();
        this.assets = new GameAssetManager();
        this.animationCatalog = new PamAnimationCatalog();
        this.visualCatalog = new PlantCardVisualCatalog();
        this.grid = new Table();
        this.grid.top().left();
        this.filterLabel = footerLabel(this.filter.getDisplayName());
        this.sortLabel = footerLabel(this.sort.getDisplayName());
        this.collectedLabel = footerLabel("");
        this.mintLabel = resourceLabel();
        this.gemLabel = resourceLabel();
        this.coinLabel = resourceLabel();
        this.statusLabel = statusLabel();
        this.gemPlus = debugPlusButton(this::debugAddGems);
        this.coinPlus = debugPlusButton(this::debugAddCoins);
        this.gemPlus.setVisible(false);
        this.coinPlus.setVisible(false);
        this.filterPopup = filterPopup();
        this.sortPopup = sortPopup();
        this.detailsShade = colorImage(SHADE);
        this.detailsShade.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        this.detailsShade.setVisible(false);
        this.detailsShade.setTouchable(Touchable.enabled);
        this.detailsPanel = new PlantDetailsPanel(this.skin, this.assets, this.animationCatalog);
        this.detailsPanel.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        this.detailsPanel.setActions(this::hideDetails, this::previousDetail, this::nextDetail);
        this.detailsPanel.setCollectionActions(this::purchaseCurrentDetail, this::upgradeCurrentDetail);
        this.detailsShade.addListener(click(this::hideDetails));
        build();
        refresh();
    }

    public PlantCollectionScreen(CollectionController controller) {
        this(new ControllerPlantCollectionDataSource(controller));
    }

    public Stage getStage() { return this.stage; }
    public int getRenderedCardCount() { return this.renderedCards.size(); }
    public String getSelectedPlantName() { return this.selectedPlantName; }
    public void setOnClose(Runnable action) { this.onClose = action; }
    public void setOnZombiesTab(Runnable action) { this.onZombiesTab = action; }

    public void refresh() {
        List<PlantCollectionState> states = safePlants();
        this.mintLabel.setText(formatNumber(this.dataSource.getMints()));
        this.gemLabel.setText(formatNumber(this.dataSource.getGems()));
        this.coinLabel.setText(formatNumber(this.dataSource.getGold()));
        boolean debug = this.dataSource.isDebugModeEnabled() && this.dataSource.supportsCurrencyCheats();
        this.gemPlus.setVisible(debug);
        this.coinPlus.setVisible(debug);
        int unlocked = 0;
        for (PlantCollectionState state : states) {
            if (state != null && state.isUnlocked()) {
                unlocked++;
            }
        }
        this.collectedLabel.setText("Plants Collected: " + unlocked + " of " + states.size());
        rebuildGrid(states);
        updateSelectionFrames();
        if (this.detailsPanel.isVisible()) {
            syncOpenDetailAfterRefresh();
        }
    }

    @Override public void show() {
        Gdx.input.setInputProcessor(this.stage);
        CollectionUiAnimator.enterScreen(this.stage);
    }
    @Override public void render(float delta) {
        ScreenUtils.clear(HEADER);
        this.assets.update();
        this.stage.act(Math.min(Math.max(delta, 0f), 1f / 20f));
        this.stage.draw();
    }
    @Override public void resize(int width, int height) {
        if (width > 0 && height > 0) this.stage.getViewport().update(width, height, true);
    }
    @Override public void pause() { this.dataSource.save(); }
    @Override public void resume() { }
    @Override public void hide() {
        InputProcessor current = Gdx.input.getInputProcessor();
        if (current == this.stage) Gdx.input.setInputProcessor(null);
    }
    @Override public void dispose() {
        this.dataSource.save(); this.stage.dispose(); this.assets.dispose();
    }

    private void build() {
        this.stage.addActor(header());
        this.stage.addActor(panel());
        this.stage.addActor(footer());
        this.stage.addActor(plantsTab());
        this.stage.addActor(zombiesTab());
        this.stage.addActor(resources());
        this.stage.addActor(closeButton());
        this.stage.addActor(this.filterPopup);
        this.stage.addActor(this.sortPopup);
        this.stage.addActor(this.statusLabel);
        this.stage.addActor(this.detailsShade);
        this.stage.addActor(this.detailsPanel);
    }

    private Actor header() {
        Table table = colored(HEADER, 0f); table.setBounds(0f, 968f, WORLD_WIDTH, 112f); return table;
    }

    private Actor panel() {
        Stack stack = new Stack(); stack.setBounds(10f, 102f, 1900f, 876f);
        Table outer = colored(OUTER, 4f); Table glow = colored(GLOW, 3f);
        Table frame = colored(FRAME, 5f); Table inner = colored(INNER, 0f);
        inner.pad(39f, 24f, 112f, 24f); inner.add(scroll()).grow();
        frame.add(inner).grow(); glow.add(frame).grow(); outer.add(glow).grow(); stack.add(outer); return stack;
    }

    private ScrollPane scroll() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.vScroll = drawable(SCROLL_BG); style.vScrollKnob = drawable(SCROLL_KNOB);
        ScrollPane pane = new ScrollPane(this.grid, style);
        pane.setFadeScrollBars(false); pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false); pane.setSmoothScrolling(true); pane.setScrollbarsOnTop(true); return pane;
    }

    private Actor plantsTab() {
        Stack stack = tab(38f, 939f, 100f, 133f, PLANTS_TAB);
        Table icon = new Table();
        icon.add(new Image(this.skin.getDrawable("image_ui_generic_leaf_backdrop")))
                .size(55f).padBottom(22f);
        stack.add(icon);
        CollectionUiAnimator.installHoverScale(stack);
        return stack;
    }

    private Actor zombiesTab() {
        Stack stack = tab(146f, 967f, 100f, 100f, ZOMBIES_TAB);
        Table icon = new Table(); icon.add(new Image(this.skin.getDrawable(
                "image_ui_almanac_zombies_zombietoughness_icon"))).size(54f); stack.add(icon);
        stack.addListener(click(() -> {
            hidePopups();
            CollectionUiAnimator.leaveScreen(this.stage, () -> {
                if (this.onZombiesTab != null) {
                    this.onZombiesTab.run();
                }
            });
        }));
        CollectionUiAnimator.installHoverScale(stack);
        return stack;
    }

    private Stack tab(float x, float y, float width, float height, String drawable) {
        Stack stack = new Stack(); stack.setBounds(x, y, width, height);
        Image image = new Image(this.skin.getDrawable(drawable));
        image.setScaling(Scaling.stretch);
        stack.add(image);
        return stack;
    }

    private Actor resources() {
        Table strip = new Table();
        strip.setBounds(1065f, 995f, 690f, 63f);
        strip.add(counter(MINT_ICON, this.mintLabel, null)).size(188f, 54f).padRight(12f);
        strip.add(counter(GEM_ICON, this.gemLabel, this.gemPlus)).size(188f, 54f).padRight(12f);
        strip.add(counter(COIN_ICON, this.coinLabel, this.coinPlus)).size(236f, 54f);
        return strip;
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

    private Actor closeButton() {
        Stack close = new Stack(); close.setBounds(1800f, 988f, 88f, 80f);
        close.add(new Image(this.skin.getDrawable(CLOSE_TAB)));
        close.addListener(click(() -> {
            hidePopups();
            this.dataSource.save();
            CollectionUiAnimator.leaveScreen(this.stage, () -> {
                if (this.onClose != null) {
                    this.onClose.run();
                }
            });
        }));
        CollectionUiAnimator.installHoverScale(close);
        return close;
    }

    private Actor footer() {
        Stack footer = new Stack(); footer.setBounds(132f, 103f, 1655f, 91f);
        Table border = colored(FOOTER_BORDER, 3f); Table body = colored(FOOTER, 0f); body.pad(0f, 55f, 0f, 62f);
        body.add(footerControl(
                FILTER_UP, FILTER_DOWN, this.filterLabel, () -> toggle(this.filterPopup)
        )).width(390f).left();
        body.add(footerControl(SORT_UP, SORT_DOWN, this.sortLabel, () -> toggle(this.sortPopup))).width(315f).left();
        body.add().growX();
        this.collectedLabel.setAlignment(Align.right);
        body.add(this.collectedLabel).right().width(520f);
        border.add(body).grow(); footer.add(border); return footer;
    }

    private Table footerControl(String up, String down, Label label, Runnable action) {
        Table control = new Table();
        control.left();
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = this.skin.getDrawable(up);
        style.imageDown = this.skin.getDrawable(down);
        ImageButton button = new ImageButton(style);
        button.addListener(click(action));
        CollectionUiAnimator.installHoverScale(button);
        control.add(button).size(47f).padRight(14f);
        control.add(label).left();
        label.addListener(click(action));
        CollectionUiAnimator.installHoverScale(label);
        return control;
    }

    private Table filterPopup() {
        Table popup = popupShell(); popup.setBounds(155f, 193f, 520f, 530f);
        popup.add(popupHeader("FILTER PLANTS")).height(48f).growX(); popup.row();
        Table rows = new Table(); rows.top();
        for (PlantCollectionFilter value : PlantCollectionFilter.values()) {
            addPopupRow(rows, value.getDisplayName(), () -> {
                this.filter = value;
                this.filterLabel.setText(value.getDisplayName());
                CollectionUiAnimator.hidePopup(this.filterPopup);
                refresh();
            });
        }
        ScrollPane pane = new ScrollPane(rows);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        popup.add(pane).grow();
        popup.setVisible(false); return popup;
    }

    private Table sortPopup() {
        Table popup = popupShell(); popup.setBounds(545f, 193f, 430f, 345f);
        popup.add(popupHeader("SORT PLANTS")).height(48f).growX(); popup.row();
        for (PlantCollectionSort value : PlantCollectionSort.values()) {
            addPopupRow(popup, value.getDisplayName(), () -> {
                this.sort = value;
                this.sortLabel.setText(value.getDisplayName());
                CollectionUiAnimator.hidePopup(this.sortPopup);
                refresh();
            });
        }
        popup.setVisible(false); return popup;
    }

    private Table popupShell() { Table table = colored(POPUP, 12f); table.top(); return table; }
    private Label popupHeader(String text) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(0.72f);
        label.setAlignment(Align.center);
        return label;
    }
    private void addPopupRow(Table parent, String text, Runnable action) {
        Table row = colored(POPUP_ROW, 0f);
        Label label = new Label(text, this.skin, "secondary");
        label.setFontScale(0.88f);
        row.left();
        row.add(label).growX().left().padLeft(18f);
        row.addListener(click(action));
        CollectionUiAnimator.installHoverScale(row);
        parent.add(row).height(46f).growX().padBottom(3f);
        parent.row();
    }

    private void rebuildGrid(List<PlantCollectionState> source) {
        this.grid.clearChildren(); this.renderedCards.clear(); this.visibleStates.clear();
        List<PlantCollectionState> filtered = new ArrayList<>();
        int availableGold = this.dataSource.getGold();
        for (PlantCollectionState state : source) {
            if (this.filter.matches(state, availableGold)) {
                filtered.add(state);
            }
        }
        if (this.sort != PlantCollectionSort.DEFAULT) filtered.sort(this.sort.comparator());
        this.visibleStates.addAll(filtered); int column = 0;
        for (PlantCollectionState state : filtered) {
            PlantCard card = card(state);
            this.renderedCards.add(card);
            this.grid.add(card).size(PlantCard.CARD_WIDTH, PlantCard.CARD_HEIGHT).pad(6f, 4f, 6f, 4f);
            if (++column >= GRID_COLUMNS) {
                this.grid.row();
                column = 0;
            }
        }
        if (filtered.isEmpty()) {
            String message = this.loadErrorMessage.isEmpty()
                    ? "No plants match this filter."
                    : this.loadErrorMessage;
            Label empty = new Label(message, this.skin, "medium_outline");
            empty.setFontScale(0.80f);
            this.grid.add(empty).colspan(GRID_COLUMNS).padTop(140f);
        }
        this.grid.invalidateHierarchy();
    }

    private PlantCard card(PlantCollectionState state) {
        PlantCard card = new PlantCard(this.skin, this.assets.getTextureBank(), this.assets.getPamPlayer(),
                this.animationCatalog.find(state.getName()), state, this.visualCatalog.find(state.getName()));
        card.setActionListener(new PlantCard.PlantCardActionListener() {
            @Override public void onPlantCardClicked(PlantCard ignored) { showDetails(state); }
            @Override public void onUpgradeRequested(PlantCard ignored) {
                handle(dataSource.upgradePlant(state.getName()));
            }
            @Override public void onPurchaseRequested(PlantCard ignored) {
                handle(dataSource.purchasePlant(state.getName()));
            }
        });
        card.setSelected(isSelected(state.getName()));
        return card;
    }

    private void handle(CollectionActionResult result) {
        boolean success = result != null && result.isSuccessful();
        String message = result == null ? "Collection action failed." : safe(result.getMessage());
        showStatus(message, success);
        if (success) {
            refresh();
        }
    }

    private void showDetails(PlantCollectionState state) {
        int index = this.visibleStates.indexOf(state);
        if (index < 0) {
            return;
        }
        this.detailIndex = index;
        this.selectedPlantName = state.getName();
        updateSelectionFrames();
        hidePopups();
        updateDetailResources();
        this.detailsShade.setVisible(true);
        this.detailsPanel.showPlant(state);
        this.detailsShade.toFront();
        this.detailsPanel.toFront();
    }
    private void hideDetails() {
        this.detailsShade.setVisible(false);
        this.detailsPanel.setVisible(false);
        this.detailIndex = -1;
    }
    private void previousDetail() { showDetailAt(this.detailIndex - 1); }
    private void nextDetail() { showDetailAt(this.detailIndex + 1); }
    private void showDetailAt(int requested) {
        if (this.visibleStates.isEmpty()) {
            return;
        }
        int size = this.visibleStates.size();
        int index = ((requested % size) + size) % size;
        this.detailIndex = index;
        PlantCollectionState state = this.visibleStates.get(index);
        this.selectedPlantName = state.getName();
        updateSelectionFrames();
        updateDetailResources();
        this.detailsPanel.showPlant(state);
    }

    private void updateDetailResources() {
        this.detailsPanel.setResources(
                this.dataSource.getMints(), this.dataSource.getGems(), this.dataSource.getGold());
        boolean debug = this.dataSource.isDebugModeEnabled() && this.dataSource.supportsCurrencyCheats();
        this.detailsPanel.setDebugCurrencyControls(
                debug,
                this::debugAddGemsFromDetails,
                this::debugAddCoinsFromDetails
        );
    }

    private void purchaseCurrentDetail() {
        if (this.selectedPlantName != null) {
            handle(this.dataSource.purchasePlant(this.selectedPlantName));
        }
    }

    private void upgradeCurrentDetail() {
        if (this.selectedPlantName != null) {
            handle(this.dataSource.upgradePlant(this.selectedPlantName));
        }
    }

    private void debugAddCoins() {
        this.dataSource.cheatAddGold(DEBUG_COIN_INCREMENT);
        refresh();
        showStatus("Debug: +" + formatNumber(DEBUG_COIN_INCREMENT) + " coins", true);
    }

    private void debugAddGems() {
        this.dataSource.cheatAddGems(DEBUG_GEM_INCREMENT);
        refresh();
        showStatus("Debug: +" + formatNumber(DEBUG_GEM_INCREMENT) + " gems", true);
    }

    private void debugAddCoinsFromDetails() {
        debugAddCoins();
        updateDetailResources();
    }

    private void debugAddGemsFromDetails() {
        debugAddGems();
        updateDetailResources();
    }

    private void updateSelectionFrames() {
        for (PlantCard card : this.renderedCards) {
            card.setSelected(isSelected(card.getPlantName()));
        }
    }

    private boolean isSelected(String plantName) {
        return this.selectedPlantName != null
                && plantName != null
                && this.selectedPlantName.equalsIgnoreCase(plantName);
    }

    private void syncOpenDetailAfterRefresh() {
        for (int i = 0; i < this.visibleStates.size(); i++) {
            PlantCollectionState state = this.visibleStates.get(i);
            if (state != null && isSelected(state.getName())) {
                this.detailIndex = i;
                updateDetailResources();
                this.detailsPanel.showPlant(state);
                return;
            }
        }
        hideDetails();
    }

    private List<PlantCollectionState> safePlants() {
        try {
            List<PlantCollectionState> plants = this.dataSource.getPlants();
            this.loadErrorMessage = safe(this.dataSource.getLoadErrorMessage()).trim();
            return plants == null ? Collections.emptyList() : plants;
        } catch (RuntimeException exception) {
            this.loadErrorMessage = "Unable to load plant collection.";
            showStatus(this.loadErrorMessage, false);
            return Collections.emptyList();
        }
    }
    private void toggle(Table popup) {
        boolean show = !popup.isVisible();
        hidePopups();
        if (show) {
            CollectionUiAnimator.showPopup(popup);
        }
    }

    private void hidePopups() {
        CollectionUiAnimator.hidePopup(this.filterPopup);
        CollectionUiAnimator.hidePopup(this.sortPopup);
    }
    private Table colored(Color color, float pad) {
        Table table = new Table();
        table.setBackground(this.skin.newDrawable(WHITE_PIXEL, color));
        table.pad(pad);
        return table;
    }
    private Image colorImage(Color color) {
        Image image = new Image(this.skin.getDrawable(WHITE_PIXEL));
        image.setColor(color);
        return image;
    }
    private Drawable drawable(String name) {
        try {
            return this.skin.has(name, Drawable.class) ? this.skin.getDrawable(name) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        };
    }
    private Label footerLabel(String text) {
        Label label = new Label(text, this.skin, "secondary");
        label.setFontScale(1.05f);
        label.setColor(new Color(0.25f, 0.16f, 0.055f, 1f));
        return label;
    }
    private Label resourceLabel() {
        Label label = new Label("0", this.skin, "medium_outline");
        label.setFontScale(0.82f);
        return label;
    }
    private Label statusLabel() {
        Label label = new Label("", this.skin, "medium_outline");
        label.setFontScale(0.63f);
        label.setAlignment(Align.center);
        label.setBounds(575f, 205f, 770f, 42f);
        label.setVisible(false);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private Button debugPlusButton(Runnable action) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = this.skin.getDrawable(PLUS_UP);
        style.down = this.skin.getDrawable(PLUS_DOWN);
        Button button = new Button(style);
        Label plus = new Label("+", this.skin, "medium_outline");
        plus.setFontScale(0.72f);
        plus.setAlignment(Align.center);
        plus.setTouchable(Touchable.disabled);
        button.add(plus).grow();
        button.addListener(click(action));
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private void showStatus(String message, boolean success) {
        this.statusLabel.setText(message == null || message.trim().isEmpty() ? "Done." : message.trim());
        this.statusLabel.setColor(success ? Color.WHITE : new Color(1f, 0.47f, 0.38f, 1f));
        this.statusLabel.setVisible(true);
        this.statusLabel.clearActions();
        this.statusLabel.addAction(Actions.sequence(
                Actions.alpha(1f),
                Actions.delay(2.7f),
                Actions.fadeOut(0.3f),
                Actions.visible(false)
        ));
    }

    private String formatNumber(int value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }

    private String safe(String text) { return text == null ? "" : text; }
}
