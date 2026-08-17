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
import model.collection.ZombieCollectionState;
import model.zombie.ZombieDefinitionRepository;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Complete zombie tab of the graphical PvZ2 Collection/Almanac. */
@SuppressWarnings("PMD.ExcessiveClassLength")
public final class ZombieCollectionScreen implements Screen {
    public static final float WORLD_WIDTH = 1920f;
    public static final float WORLD_HEIGHT = 1080f;

    private static final int GRID_COLUMNS = 10;
    private static final int DEBUG_COIN_INCREMENT = 1000;
    private static final int DEBUG_GEM_INCREMENT = 10;
    private static final float GRID_HORIZONTAL_PAD = 16f;
    private static final float GRID_VERTICAL_PAD = 10f;

    private static final String WHITE_PIXEL = "white_pixel";
    private static final String PLANTS_TAB = "image_ui_almanac_tabs_plants_down";
    private static final String ZOMBIES_TAB = "image_ui_almanac_tabs_zombies_active";
    private static final String CLOSE_TAB = "image_ui_almanac_tabs_close_tab";
    private static final String CLOSE_TAB_DOWN = "image_ui_almanac_tabs_close_tab_down";
    private static final String FILTER_ICON = "image_ui_almanac_filter_button_up";
    private static final String FILTER_ICON_DOWN = "image_ui_almanac_filter_button_down";
    private static final String SORT_ICON = "image_ui_almanac_sort_button_up";
    private static final String SORT_ICON_DOWN = "image_ui_almanac_sort_button_down";
    private static final String CURRENCY_BACKGROUND = "image_ui_generic_button_generic_currency_normal";
    private static final String MINT_ICON = "image_ui_generic_mint_icon_small";
    private static final String GEM_ICON = "image_ui_generic_gem_icon_small";
    private static final String COIN_ICON = "image_ui_generic_coin_icon_small";
    private static final String POPUP_BACKGROUND = "image_ui_generic_popup_9slice";
    private static final String SCROLL_BACKGROUND = "image_ui_almanac_card_zombie_scrollbar";
    private static final String SCROLL_KNOB = "image_ui_almanac_scroll_slider";
    private static final String PLUS_UP = "image_ui_generic_greenbutton";
    private static final String PLUS_DOWN = "image_ui_generic_greenbutton_down";
    private static final String ALMANAC_GRADIENT_TOP = "IMAGE_UI_ALMANAC_GRADIENT_TOP";
    private static final String ALMANAC_GRADIENT_BOTTOM = "IMAGE_UI_ALMANAC_GRADIENT_BOTTOM";
    private static final String ALMANAC_EDGE_GRADIENT = "IMAGE_UI_ALMANAC_EDGE_GRADIENT";

    private static final Color HEADER_COLOR = new Color(0.010f, 0.014f, 0.012f, 1f);
    private static final Color COLLECTION_BASE = new Color(0.16f, 0.055f, 0.012f, 1f);
    private static final Color PANEL_OUTER = new Color(0.23f, 0.105f, 0.028f, 1f);
    private static final Color PANEL_FRAME = new Color(0.63f, 0.31f, 0.085f, 1f);
    private static final Color PANEL_INNER = new Color(0.34f, 0.14f, 0.038f, 1f);
    private static final Color PANEL_GLOW = new Color(0.80f, 0.46f, 0.16f, 1f);
    private static final Color FOOTER_COLOR = new Color(0.91f, 0.86f, 0.65f, 1f);
    private static final Color FOOTER_BORDER = new Color(0.51f, 0.33f, 0.13f, 1f);
    private static final Color POPUP_ROW = new Color(0.33f, 0.18f, 0.065f, 0.96f);
    private static final Color POPUP_ROW_HOVER = new Color(0.47f, 0.30f, 0.10f, 0.98f);
    private static final Color DETAILS_SHADE = new Color(0f, 0f, 0f, 0.72f);

    private final Stage stage;
    private final Skin skin;
    private final GameAssetManager assets;
    private final ZombieCollectionDataSource dataSource;
    private final Table cardGrid;
    private final List<ZombieCard> renderedCards;
    private final List<ZombieCollectionState> visibleStates;
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
    private final ZombieDetailsPanel detailsPanel;

    private ZombieCollectionFilter collectionFilter;
    private ZombieCollectionSort collectionSort;
    private String selectedZombieAlias;
    private int detailIndex;
    private Runnable onClose;
    private Runnable onPlantsTab;

    public ZombieCollectionScreen(ZombieCollectionDataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Zombie Collection data source is required");
        }
        this.dataSource = dataSource;
        this.stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        this.skin = PvzSkin.get();
        this.assets = new GameAssetManager();
        this.cardGrid = new Table();
        this.cardGrid.top().left();
        this.renderedCards = new ArrayList<>();
        this.visibleStates = new ArrayList<>();
        this.collectionFilter = ZombieCollectionFilter.ALL;
        this.collectionSort = ZombieCollectionSort.DEFAULT;
        this.detailIndex = -1;

        this.filterLabel = createFooterLabel(this.collectionFilter.getDisplayName());
        this.sortLabel = createFooterLabel(this.collectionSort.getDisplayName());
        this.collectedLabel = createFooterLabel("");
        this.mintLabel = createResourceLabel("0");
        this.gemLabel = createResourceLabel("0");
        this.coinLabel = createResourceLabel("0");
        this.statusLabel = createStatusLabel();
        this.gemPlus = createDebugPlusButton(this::debugAddGems);
        this.coinPlus = createDebugPlusButton(this::debugAddCoins);
        this.gemPlus.setVisible(false);
        this.coinPlus.setVisible(false);
        this.filterPopup = createFilterPopup();
        this.sortPopup = createSortPopup();
        this.detailsShade = colorImage(DETAILS_SHADE);
        this.detailsShade.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        this.detailsShade.setVisible(false);
        this.detailsShade.setTouchable(Touchable.enabled);
        this.detailsPanel = new ZombieDetailsPanel(this.skin, this.assets, createAnimationCatalog());
        this.detailsPanel.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        this.detailsPanel.setOrigin(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        this.detailsPanel.setActions(this::hideDetails, this::showPreviousDetail, this::showNextDetail);
        this.detailsShade.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hideDetails();
            }
        });

        buildScreen();
        refresh();
    }

    public ZombieCollectionScreen(ZombieDefinitionRepository zombieDefinitions) {
        this(new PreviewZombieCollectionDataSource(zombieDefinitions));
    }

    public Stage getStage() {
        return this.stage;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setOnPlantsTab(Runnable onPlantsTab) {
        this.onPlantsTab = onPlantsTab;
    }

    public String getSelectedZombieAlias() {
        return this.selectedZombieAlias;
    }

    public int getRenderedCardCount() {
        return this.renderedCards.size();
    }

    public void refresh() {
        List<ZombieCollectionState> states = safeZombies();
        updateResourceLabels();
        boolean debug = this.dataSource.isDebugModeEnabled() && this.dataSource.supportsCurrencyCheats();
        this.gemPlus.setVisible(debug);
        this.coinPlus.setVisible(debug);
        updateCollectedLabel(states);
        rebuildGrid(states);
        updateSelectionFrames();
        if (this.detailsPanel.isVisible()) {
            syncOpenDetailAfterRefresh();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
        CollectionUiAnimator.enterScreen(this.stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(HEADER_COLOR);
        this.assets.update();
        this.stage.act(Math.min(Math.max(delta, 0f), 1f / 20f));
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            this.stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {
        this.dataSource.save();
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        InputProcessor current = Gdx.input.getInputProcessor();
        if (current == this.stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        this.dataSource.save();
        this.stage.dispose();
        this.assets.dispose();
    }

    private void buildScreen() {
        this.stage.addActor(createBackground());
        this.stage.addActor(createHeader());
        this.stage.addActor(createCollectionPanel());
        this.stage.addActor(createFooter());
        this.stage.addActor(createPlantsTab());
        this.stage.addActor(createZombiesTab());
        this.stage.addActor(createResourceStrip());
        this.stage.addActor(createCloseButton());
        this.stage.addActor(this.filterPopup);
        this.stage.addActor(this.sortPopup);
        this.stage.addActor(this.statusLabel);
        this.stage.addActor(this.detailsShade);
        this.stage.addActor(this.detailsPanel);
    }

    private Actor createBackground() {
        Stack stack = new Stack();
        stack.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        stack.add(colorImage(COLLECTION_BASE));
        Image top = resourceImage(ALMANAC_GRADIENT_TOP);
        if (top != null) {
            top.setColor(1f, 0.78f, 0.52f, 0.24f);
            stack.add(top);
        }
        Image bottom = resourceImage(ALMANAC_GRADIENT_BOTTOM);
        if (bottom != null) {
            bottom.setColor(0.82f, 0.52f, 0.25f, 0.18f);
            stack.add(bottom);
        }
        Image edge = resourceImage(ALMANAC_EDGE_GRADIENT);
        if (edge != null) {
            edge.setColor(1f, 1f, 1f, 0.42f);
            stack.add(edge);
        }
        return stack;
    }

    private Actor createHeader() {
        Stack stack = new Stack();
        stack.setBounds(0f, 968f, WORLD_WIDTH, 112f);
        stack.add(coloredTable(HEADER_COLOR, 0f));
        Image gradient = resourceImage(ALMANAC_GRADIENT_TOP);
        if (gradient != null) {
            gradient.setColor(1f, 1f, 1f, 0.58f);
            stack.add(gradient);
        }
        Table titleLayer = new Table();
        titleLayer.left();
        Label title = new Label("COLLECTION", this.skin, "medium_outline");
        title.setFontScale(1.16f);
        title.setColor(PvzVisualTheme.TEXT_CREAM);
        titleLayer.add(title).width(650f).padLeft(285f).left();
        stack.add(titleLayer);
        return stack;
    }

    private Actor createCollectionPanel() {
        Stack panel = new Stack();
        panel.setBounds(10f, 102f, 1900f, 876f);
        Table outer = coloredTable(PANEL_OUTER, 4f);
        Table frame = coloredTable(PANEL_GLOW, 3f);
        Table border = coloredTable(PANEL_FRAME, 5f);
        Table inner = coloredTable(PANEL_INNER, 0f);
        inner.padTop(44f);
        inner.padLeft(25f);
        inner.padRight(25f);
        inner.padBottom(112f);
        inner.add(createCardScrollPane()).grow();
        border.add(inner).grow();
        frame.add(border).grow();
        outer.add(frame).grow();
        panel.add(outer);
        Image top = resourceImage(ALMANAC_GRADIENT_TOP);
        if (top != null) {
            top.setColor(1f, 1f, 1f, 0.24f);
            panel.add(top);
        }
        Image bottom = resourceImage(ALMANAC_GRADIENT_BOTTOM);
        if (bottom != null) {
            bottom.setColor(1f, 1f, 1f, 0.19f);
            panel.add(bottom);
        }
        return panel;
    }

    private Table coloredTable(Color color, float padding) {
        Table table = new Table();
        table.setBackground(this.skin.newDrawable(WHITE_PIXEL, color));
        table.pad(padding);
        return table;
    }

    private ScrollPane createCardScrollPane() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.vScroll = this.skin.getDrawable(SCROLL_BACKGROUND);
        style.vScrollKnob = this.skin.getDrawable(SCROLL_KNOB);
        Table centeredGrid = new Table();
        centeredGrid.top().center();
        centeredGrid.add(this.cardGrid).top().center();
        ScrollPane scrollPane = new ScrollPane(centeredGrid, style);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setSmoothScrolling(true);
        scrollPane.setScrollbarsOnTop(true);
        return scrollPane;
    }

    private Actor createPlantsTab() {
        Stack stack = new Stack();
        stack.setBounds(38f, 967f, 100f, 100f);
        Image tab = new Image(this.skin.getDrawable(PLANTS_TAB));
        tab.setScaling(Scaling.stretch);
        stack.add(tab);
        Table icon = new Table();
        icon.add(new Image(this.skin.getDrawable("image_ui_generic_leaf_backdrop"))).size(54f);
        stack.add(icon);
        makeClickable(stack, () -> {
            hidePopups();
            CollectionUiAnimator.leaveScreen(this.stage, () -> {
                if (this.onPlantsTab != null) {
                    this.onPlantsTab.run();
                } else {
                    showStatus("Plants tab requested.");
                }
            });
        });
        CollectionUiAnimator.installHoverScale(stack);
        return stack;
    }

    private Actor createZombiesTab() {
        Stack stack = new Stack();
        stack.setBounds(146f, 939f, 100f, 133f);
        Image tab = new Image(this.skin.getDrawable(ZOMBIES_TAB));
        tab.setScaling(Scaling.stretch);
        stack.add(tab);
        Table icon = new Table();
        icon.add(new Image(this.skin.getDrawable("image_ui_almanac_zombies_zombietoughness_icon")))
                .size(55f).padBottom(22f);
        stack.add(icon);
        CollectionUiAnimator.installHoverScale(stack);
        return stack;
    }

    private Actor createResourceStrip() {
        Table strip = new Table();
        strip.setBounds(1065f, 995f, 690f, 63f);
        strip.add(createResourceCounter(MINT_ICON, this.mintLabel, null)).size(188f, 54f).padRight(12f);
        strip.add(createResourceCounter(GEM_ICON, this.gemLabel, this.gemPlus)).size(188f, 54f).padRight(12f);
        strip.add(createResourceCounter(COIN_ICON, this.coinLabel, this.coinPlus)).size(236f, 54f);
        return strip;
    }

    private Stack createResourceCounter(String iconName, Label valueLabel, Actor debugButton) {
        Stack counter = new Stack();
        counter.add(new Image(this.skin.getDrawable(CURRENCY_BACKGROUND)));
        Table content = new Table();
        content.left();
        content.add(new Image(this.skin.getDrawable(iconName))).size(49f).padLeft(4f).padRight(8f);
        content.add(valueLabel).growX().left().padRight(debugButton == null ? 35f : 50f);
        counter.add(content);
        if (debugButton != null) {
            Table plusLayer = new Table();
            plusLayer.right();
            plusLayer.add(debugButton).size(42f, 38f).padRight(2f);
            counter.add(plusLayer);
        }
        return counter;
    }

    private Actor createCloseButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = this.skin.getDrawable(CLOSE_TAB);
        style.imageDown = this.skin.getDrawable(CLOSE_TAB_DOWN);
        ImageButton close = new ImageButton(style);
        close.setBounds(1800f, 988f, 88f, 80f);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hidePopups();
                dataSource.save();
                CollectionUiAnimator.leaveScreen(stage, () -> {
                    if (onClose != null) {
                        onClose.run();
                    } else {
                        showStatus("Close requested.");
                    }
                });
            }
        });
        CollectionUiAnimator.installHoverScale(close);
        return close;
    }

    private Actor createFooter() {
        Stack footer = new Stack();
        footer.setBounds(132f, 103f, 1655f, 91f);
        Table border = coloredTable(FOOTER_BORDER, 3f);
        Table body = coloredTable(FOOTER_COLOR, 0f);
        body.padLeft(62f);
        body.padRight(55f);
        body.add(createFooterControl(
                FILTER_ICON,
                FILTER_ICON_DOWN,
                this.filterLabel,
                () -> togglePopup(this.filterPopup)
        )).width(345f).left();
        body.add(createFooterControl(
                SORT_ICON,
                SORT_ICON_DOWN,
                this.sortLabel,
                () -> togglePopup(this.sortPopup)
        )).width(305f).left();
        body.add().growX();
        this.collectedLabel.setAlignment(Align.right);
        body.add(this.collectedLabel).right().width(520f);
        border.add(body).grow();
        footer.add(border);
        Image gradient = resourceImage(ALMANAC_GRADIENT_BOTTOM);
        if (gradient != null) {
            gradient.setColor(1f, 1f, 1f, 0.33f);
            footer.add(gradient);
        }
        return footer;
    }

    private Table createFooterControl(
            String iconName,
            String pressedIconName,
            Label label,
            Runnable action
    ) {
        Table control = new Table();
        control.left();
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = this.skin.getDrawable(iconName);
        style.imageDown = this.skin.getDrawable(pressedIconName);
        ImageButton button = new ImageButton(style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
        CollectionUiAnimator.installHoverScale(button);
        control.add(button).size(47f).padRight(14f);
        control.add(label).left();
        makeClickable(label, action);
        CollectionUiAnimator.installHoverScale(label);
        return control;
    }

    private Table createFilterPopup() {
        Table popup = createPopupShell();
        popup.setBounds(155f, 193f, 440f, 235f);
        addPopupHeader(popup, "FILTER ZOMBIES");
        for (ZombieCollectionFilter filter : ZombieCollectionFilter.values()) {
            addPopupRow(popup, filter.getDisplayName(), () -> {
                this.collectionFilter = filter;
                this.filterLabel.setText(filter.getDisplayName());
                CollectionUiAnimator.hidePopup(this.filterPopup);
                refresh();
            });
        }
        popup.setVisible(false);
        return popup;
    }

    private Table createSortPopup() {
        Table popup = createPopupShell();
        popup.setBounds(500f, 193f, 390f, 345f);
        addPopupHeader(popup, "SORT ZOMBIES");
        for (ZombieCollectionSort sort : ZombieCollectionSort.values()) {
            addPopupRow(popup, sort.getDisplayName(), () -> {
                this.collectionSort = sort;
                this.sortLabel.setText(sort.getDisplayName());
                CollectionUiAnimator.hidePopup(this.sortPopup);
                refresh();
            });
        }
        popup.setVisible(false);
        return popup;
    }

    private Table createPopupShell() {
        Table popup = new Table();
        popup.setBackground(this.skin.getDrawable(POPUP_BACKGROUND));
        popup.top().left();
        popup.pad(18f);
        return popup;
    }

    private void addPopupHeader(Table popup, String text) {
        Label header = new Label(text, this.skin, "medium_outline");
        header.setFontScale(0.62f);
        header.setAlignment(Align.left);
        popup.add(header).growX().left().pad(6f, 8f, 8f, 8f);
        popup.row();
    }

    private void addPopupRow(Table popup, String text, Runnable action) {
        Table row = new Table();
        Drawable normal = this.skin.newDrawable(WHITE_PIXEL, POPUP_ROW);
        Drawable hover = this.skin.newDrawable(WHITE_PIXEL, POPUP_ROW_HOVER);
        row.setBackground(normal);
        row.left();
        Label label = new Label(text, this.skin, "secondary");
        label.setFontScale(0.90f);
        row.add(label).left().pad(7f, 12f, 7f, 12f).growX();
        row.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                row.setBackground(hover);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                row.setBackground(normal);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
        CollectionUiAnimator.installHoverScale(row);
        popup.add(row).height(36f).growX().padBottom(3f);
        popup.row();
    }

    private void rebuildGrid(List<ZombieCollectionState> allStates) {
        List<ZombieCollectionState> visible = filteredAndSorted(allStates);
        this.visibleStates.clear();
        this.visibleStates.addAll(visible);
        this.cardGrid.clearChildren();
        this.renderedCards.clear();

        int column = 0;
        for (ZombieCollectionState state : visible) {
            ZombieCard card = createCard(state);
            this.renderedCards.add(card);
            this.cardGrid.add(card)
                    .size(ZombieCard.CARD_WIDTH, ZombieCard.CARD_HEIGHT)
                    .pad(GRID_VERTICAL_PAD, GRID_HORIZONTAL_PAD, GRID_VERTICAL_PAD, GRID_HORIZONTAL_PAD);
            column++;
            if (column == GRID_COLUMNS) {
                this.cardGrid.row();
                column = 0;
            }
        }
        if (visible.isEmpty()) {
            Label empty = new Label("No zombies match this filter.", this.skin, "medium_outline");
            empty.setFontScale(0.80f);
            this.cardGrid.add(empty).colspan(GRID_COLUMNS).padTop(140f);
        }
        this.cardGrid.invalidateHierarchy();
    }

    private ZombieCard createCard(ZombieCollectionState state) {
        ZombieCard card = new ZombieCard(state, this.skin, this.assets.getTextureBank(), clicked -> {
            ZombieCollectionState clickedState = clicked.getZombieState();
            this.selectedZombieAlias = clickedState.getAlias();
            updateSelectionFrames();
            if (!clickedState.isEncountered()) {
                showStatus("Undiscovered zombie.");
                return;
            }
            showDetails(clickedState);
        });
        card.setSelected(isSelected(state.getAlias()));
        return card;
    }

    private List<ZombieCollectionState> filteredAndSorted(List<ZombieCollectionState> states) {
        List<ZombieCollectionState> result = new ArrayList<>();
        for (ZombieCollectionState state : states) {
            if (this.collectionFilter.matches(state)) {
                result.add(state);
            }
        }
        if (this.collectionSort.getComparator() != null) {
            result.sort(this.collectionSort.getComparator());
        }
        return result;
    }

    private List<ZombieCollectionState> safeZombies() {
        List<ZombieCollectionState> states = this.dataSource.loadZombies();
        return states == null ? Collections.emptyList() : states;
    }

    private void updateCollectedLabel(List<ZombieCollectionState> states) {
        int encountered = 0;
        for (ZombieCollectionState state : states) {
            if (state != null && state.isEncountered()) {
                encountered++;
            }
        }
        this.collectedLabel.setText("Zombies Discovered: " + encountered + " of " + states.size());
    }

    private void updateResourceLabels() {
        this.mintLabel.setText(formatNumber(this.dataSource.getMintCount()));
        this.gemLabel.setText(formatNumber(this.dataSource.getGemCount()));
        this.coinLabel.setText(formatNumber(this.dataSource.getCoinCount()));
    }

    private void updateSelectionFrames() {
        for (ZombieCard card : this.renderedCards) {
            card.setSelected(isSelected(card.getZombieState().getAlias()));
        }
    }

    private boolean isSelected(String alias) {
        return this.selectedZombieAlias != null
                && alias != null
                && this.selectedZombieAlias.equalsIgnoreCase(alias);
    }

    private void showDetails(ZombieCollectionState state) {
        this.detailIndex = this.visibleStates.indexOf(state);
        updateDetailResources();
        this.detailsPanel.showZombie(state);
        this.detailsShade.clearActions();
        this.detailsPanel.clearActions();
        this.detailsShade.setVisible(true);
        this.detailsShade.getColor().a = 0f;
        this.detailsPanel.setVisible(true);
        this.detailsPanel.getColor().a = 0f;
        this.detailsPanel.setScale(0.965f);
        this.detailsShade.toFront();
        this.detailsPanel.toFront();
        this.detailsShade.addAction(Actions.alpha(0.72f, 0.16f));
        this.detailsPanel.addAction(Actions.parallel(
                Actions.fadeIn(0.16f),
                Actions.scaleTo(1f, 1f, 0.18f)
        ));
    }

    private void hideDetails() {
        if (!this.detailsPanel.isVisible()) {
            return;
        }
        this.detailsShade.clearActions();
        this.detailsPanel.clearActions();
        this.detailsShade.addAction(Actions.sequence(
                Actions.fadeOut(0.12f),
                Actions.visible(false)
        ));
        this.detailsPanel.addAction(Actions.sequence(
                Actions.parallel(Actions.fadeOut(0.12f), Actions.scaleTo(0.98f, 0.98f, 0.12f)),
                Actions.visible(false)
        ));
        this.detailIndex = -1;
    }

    private void showPreviousDetail() {
        showRelativeDetail(-1);
    }

    private void showNextDetail() {
        showRelativeDetail(1);
    }

    private void showRelativeDetail(int direction) {
        if (this.visibleStates.isEmpty()) {
            return;
        }
        int size = this.visibleStates.size();
        int index = this.detailIndex < 0 ? 0 : this.detailIndex;
        for (int step = 1; step <= size; step++) {
            int candidate = Math.floorMod(index + direction * step, size);
            ZombieCollectionState state = this.visibleStates.get(candidate);
            if (state != null && state.isEncountered()) {
                this.detailIndex = candidate;
                this.selectedZombieAlias = state.getAlias();
                updateSelectionFrames();
                updateDetailResources();
                this.detailsPanel.showZombie(state);
                return;
            }
        }
    }

    private void updateDetailResources() {
        this.detailsPanel.setResources(
                this.dataSource.getMintCount(),
                this.dataSource.getGemCount(),
                this.dataSource.getCoinCount());
        boolean debug = this.dataSource.isDebugModeEnabled() && this.dataSource.supportsCurrencyCheats();
        this.detailsPanel.setDebugCurrencyControls(
                debug,
                this::debugAddGemsFromDetails,
                this::debugAddCoinsFromDetails
        );
    }

    private void debugAddCoins() {
        this.dataSource.cheatAddCoins(DEBUG_COIN_INCREMENT);
        refresh();
        showStatus("Debug: +" + formatNumber(DEBUG_COIN_INCREMENT) + " coins");
    }

    private void debugAddGems() {
        this.dataSource.cheatAddGems(DEBUG_GEM_INCREMENT);
        refresh();
        showStatus("Debug: +" + formatNumber(DEBUG_GEM_INCREMENT) + " gems");
    }

    private void debugAddCoinsFromDetails() {
        debugAddCoins();
        updateDetailResources();
    }

    private void debugAddGemsFromDetails() {
        debugAddGems();
        updateDetailResources();
    }

    private void syncOpenDetailAfterRefresh() {
        for (int i = 0; i < this.visibleStates.size(); i++) {
            ZombieCollectionState state = this.visibleStates.get(i);
            if (state != null && state.isEncountered() && isSelected(state.getAlias())) {
                this.detailIndex = i;
                updateDetailResources();
                this.detailsPanel.showZombie(state);
                return;
            }
        }
        hideDetails();
    }

    private void togglePopup(Table popup) {
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

    private void showStatus(String message) {
        String text = message == null || message.trim().isEmpty() ? "Done." : message.trim();
        this.statusLabel.setText(text);
        this.statusLabel.setVisible(true);
        this.statusLabel.clearActions();
        this.statusLabel.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(1f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(2.7f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.3f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.visible(false)
        ));
    }

    private Button createDebugPlusButton(Runnable action) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = this.skin.getDrawable(PLUS_UP);
        style.down = this.skin.getDrawable(PLUS_DOWN);
        Button button = new Button(style);
        Label plus = new Label("+", this.skin, "medium_outline");
        plus.setFontScale(0.72f);
        plus.setAlignment(Align.center);
        plus.setTouchable(Touchable.disabled);
        button.add(plus).grow();
        makeClickable(button, action);
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private Label createTabLabel(String text) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(0.82f);
        label.setAlignment(Align.center);
        return label;
    }

    private Label createFooterLabel(String text) {
        Label label = new Label(text, this.skin, "secondary");
        label.setFontScale(1.05f);
        label.setAlignment(Align.left);
        label.setColor(new Color(0.25f, 0.16f, 0.055f, 1f));
        return label;
    }

    private Label createResourceLabel(String text) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(0.82f);
        label.setAlignment(Align.left);
        return label;
    }

    private Label createStatusLabel() {
        Label label = new Label("", this.skin, "medium_outline");
        label.setFontScale(0.63f);
        label.setAlignment(Align.center);
        label.setBounds(575f, 205f, 770f, 42f);
        label.setVisible(false);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private Image resourceImage(String resourceId) {
        return PvzVisualTheme.resourceImage(this.assets, this.skin, resourceId, Scaling.stretch);
    }

    private Image colorImage(Color color) {
        Image image = new Image(this.skin.getDrawable(WHITE_PIXEL));
        image.setColor(color);
        return image;
    }

    private void makeClickable(Actor actor, Runnable action) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
    }

    private String formatNumber(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private ZombieAnimationCatalog createAnimationCatalog() {
        try {
            return new ZombieAnimationCatalog();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
