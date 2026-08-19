package college.java.project.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.PlantPickController;
import model.collection.CollectionActionResult;
import model.chapters.ChapterType;
import model.collection.PlantCollectionState;
import model.plant.PlantCategory;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Phase 2 graphical plant selection screen. The same PlantCard actor used by
 * Collection is reused in both the chooser grid and the selected seed bank.
 */
@SuppressWarnings("PMD.ExcessiveClassLength")
public final class PlantPickScreen implements Screen {
    public static final float WORLD_WIDTH = PvzVisualTheme.WORLD_WIDTH;
    public static final float WORLD_HEIGHT = PvzVisualTheme.WORLD_HEIGHT;

    private static final int GRID_COLUMNS = 9;
    private static final int DEBUG_COIN_INCREMENT = 1000;
    private static final int DEBUG_GEM_INCREMENT = 10;
    private static final float GRID_CARD_SCALE = 0.70f;
    private static final float BANK_CARD_SCALE = 0.76f;
    private static final float RIGHT_PANEL_WIDTH = 255f;
    private static final float RIGHT_PANEL_HEIGHT = 860f;
    private static final String WHITE_PIXEL = "white_pixel";
    private static final String CHOOSER_SORT_FAMILY =
            "IMAGE_UI_CHOOSER_BUTTON_SORT_BY_FAMILY";
    private static final String CHOOSER_SORT_FAMILY_PRESS =
            "IMAGE_UI_CHOOSER_BUTTON_SORT_BY_FAMILY_PRESS";
    private static final String CHOOSER_SLOT =
            "IMAGE_UI_CHOOSER_SEEDPACKET9SLICE_BKGD";
    private static final String CHOOSER_GRADIENT_TOP = "IMAGE_UI_CHOOSER_GRADIENT_TOP";
    private static final String CHOOSER_GRADIENT_BOTTOM = "IMAGE_UI_CHOOSER_GRADIENT_BOTTOM";
    private static final String BACK_UP = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_DOWN = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";

    private static final Color BACKGROUND = PvzVisualTheme.CHOOSER_NIGHT;
    private static final Color NIGHT_PANEL = new Color(0.035f, 0.075f, 0.12f, 0.96f);
    private static final Color WOOD_DARK = PvzVisualTheme.CHOOSER_INNER;
    private static final Color WOOD = PvzVisualTheme.CHOOSER_WOOD;
    private static final Color WOOD_LIGHT = new Color(0.70f, 0.37f, 0.10f, 1f);
    private static final Color PAPER = new Color(0.94f, 0.88f, 0.66f, 1f);
    private static final Color GREEN_HEADER = new Color(0.26f, 0.66f, 0.06f, 1f);
    private static final Color GREEN_DARK = new Color(0.10f, 0.33f, 0.06f, 1f);
    private static final Color SHADE = new Color(0f, 0f, 0f, 0.30f);

    private final Stage stage;
    private final Skin skin;
    private final GameAssetManager assets;
    private final PlantPickDataSource dataSource;
    private final PamAnimationCatalog animationCatalog;
    private final PlantCardVisualCatalog visualCatalog;
    private final ChapterType chapterType;
    private final Table grid;
    private final Table selectedBank;
    private final Table detailCardHost;
    private final Label nameLabel;
    private final Label descriptionLabel;
    private final Label detailStatusLabel;
    private final Label selectedCountLabel;
    private final Label statusLabel;
    private final TextButton upgradeButton;
    private final TextButton boostButton;
    private final TextButton startButton;
    private final AlmanacResourceStrip resourceStrip;
    private final List<PlantCollectionState> availableStates = new ArrayList<>();
    private final List<PlantCard> gridCards = new ArrayList<>();
    private final Set<String> previousSelectedNames = new LinkedHashSet<>();
    private PlantCollectionState activeState;
    private boolean sortByFamily;
    private PlantCollectionFilter activeFilter = PlantCollectionFilter.ALL;
    private TextButton filterButton;
    private Runnable onStart;
    private Runnable onClose;
    private boolean startTransitionPending;

    public PlantPickScreen(PlantPickDataSource dataSource) {
        this(dataSource, ChapterType.ANCIENT_EGYPT);
    }

    public PlantPickScreen(PlantPickDataSource dataSource, ChapterType chapterType) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Plant pick data source is required");
        }
        this.dataSource = dataSource;
        this.chapterType = chapterType == null ? ChapterType.ANCIENT_EGYPT : chapterType;
        this.stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        this.skin = PvzSkin.get();
        this.assets = new GameAssetManager();
        this.animationCatalog = new PamAnimationCatalog();
        this.visualCatalog = new PlantCardVisualCatalog();
        this.grid = new Table();
        this.grid.top().left();
        this.selectedBank = new Table();
        this.selectedBank.top();
        this.detailCardHost = new Table();
        this.nameLabel = titleLabel("");
        this.descriptionLabel = bodyLabel("");
        this.detailStatusLabel = bodyLabel("");
        this.selectedCountLabel = titleLabel("");
        this.selectedCountLabel.setFontScale(0.56f);
        this.statusLabel = statusLabel();
        this.upgradeButton = actionButton("UPGRADE", "purple", this::upgradeActivePlant);
        this.boostButton = actionButton("BOOST", "green", this::boostActivePlant);
        this.startButton = actionButton("LET'S ROCK!", "purple", this::startGame);
        this.resourceStrip = new AlmanacResourceStrip(this.skin);
        build();
        refreshAll();
    }

    public Stage getStage() {
        return this.stage;
    }

    public int getAvailablePlantCount() {
        return this.availableStates.size();
    }

    public int getSelectedPlantCount() {
        return this.dataSource.getSelectedCount();
    }

    public String getActivePlantName() {
        return this.activeState == null ? null : this.activeState.getName();
    }

    public void setOnStart(Runnable action) {
        this.onStart = action;
    }

    public void setOnClose(Runnable action) {
        this.onClose = action;
    }

    public void setDebugModeEnabled(boolean enabled) {
        this.dataSource.setDebugModeEnabled(enabled);
        updateResources();
    }

    public void refreshAll() {
        loadAvailableStates();
        chooseActiveState();
        rebuildGrid();
        refreshSelectionVisuals();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.stage);
        CollectionUiAnimator.enterScreen(this.stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BACKGROUND);
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

    private void build() {
        this.stage.addActor(background());
        this.stage.addActor(selectedPanel());
        this.stage.addActor(chooserPanel());
        this.stage.addActor(rightPanel());
        this.stage.addActor(screenTitle());
        this.resourceStrip.setBounds(1160f, 1002f, 670f, 60f);
        this.stage.addActor(this.resourceStrip);
        this.stage.addActor(closeButton());
        this.stage.addActor(this.statusLabel);
    }

    private Actor background() {
        Stack stack = new Stack();
        stack.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        Image chapter = chapterBackgroundImage();
        if (chapter != null) {
            stack.add(chapter);
        } else {
            stack.add(colorImage(BACKGROUND));
        }
        stack.add(colorImage(new Color(0.01f, 0.018f, 0.018f, 0.28f)));
        Image bottom = resourceImage(CHOOSER_GRADIENT_BOTTOM);
        if (bottom != null) {
            bottom.setColor(1f, 1f, 1f, 0.40f);
            stack.add(bottom);
        }
        Image top = resourceImage(CHOOSER_GRADIENT_TOP);
        if (top != null) {
            top.setColor(1f, 1f, 1f, 0.66f);
            stack.add(top);
        }
        Table upperShade = colored(new Color(0f, 0f, 0f, 0.48f));
        upperShade.setBounds(0f, 974f, WORLD_WIDTH, 106f);
        stack.add(upperShade);
        return stack;
    }

    private Image chapterBackgroundImage() {
        try {
            TextureRegion source = this.assets.getTextureBank().region(
                    PvzChapterVisuals.backgroundResourceId(this.chapterType)
            );
            if (source == null) {
                return null;
            }
            Image image = new Image(new TextureRegionDrawable(source));
            image.setScaling(Scaling.fill);
            image.setTouchable(Touchable.disabled);
            return image;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Actor screenTitle() {
        Label title = titleLabel("CHOOSE YOUR PLANTS");
        title.setFontScale(1.16f);
        title.setAlignment(Align.center);
        title.setColor(PvzVisualTheme.TEXT_CREAM);
        title.setBounds(310f, 990f, 790f, 72f);
        title.setTouchable(Touchable.disabled);
        return title;
    }

    private Actor selectedPanel() {
        Table outer = framedPanel();
        outer.setBounds(24f, 94f, 258f, 860f);
        Table content = new Table();
        content.top();
        this.selectedCountLabel.setAlignment(Align.center);
        this.selectedCountLabel.setColor(PvzVisualTheme.TEXT_CREAM);
        content.add(this.selectedCountLabel).height(44f).growX();
        content.row();
        content.add(this.selectedBank).top().grow();
        outer.add(content).grow().pad(10f);
        return outer;
    }

    private Actor chooserPanel() {
        Table outer = framedPanel();
        outer.setBounds(298f, 94f, 1326f, 860f);
        Table content = new Table();
        content.top();
        content.add(detailPanel()).height(228f).growX().pad(10f, 12f, 5f, 12f);
        content.row();
        content.add(gridPanel()).grow().pad(5f, 12f, 10f, 12f);
        outer.add(content).grow();
        return outer;
    }

    private Actor rightPanel() {
        Stack stack = new Stack();
        stack.setBounds(1640f, 94f, RIGHT_PANEL_WIDTH, RIGHT_PANEL_HEIGHT);
        Image preview = chapterPreviewImage();
        if (preview != null) {
            stack.add(preview);
            stack.add(colorImage(new Color(0f, 0f, 0f, 0.34f)));
        } else {
            stack.add(colorImage(new Color(0.10f, 0.055f, 0.018f, 0.92f)));
        }

        Table panel = new Table();
        panel.top().pad(16f, 10f, 12f, 10f);
        Label chapterLabel = titleLabel(PvzChapterVisuals.displayName(this.chapterType));
        chapterLabel.setFontScale(0.56f);
        chapterLabel.setAlignment(Align.center);
        chapterLabel.setColor(new Color(0.98f, 0.90f, 0.65f, 1f));
        Label hint = darkBodyLabel("Pick your team, then rock!");
        hint.setAlignment(Align.center);
        Table badge = colored(new Color(0.03f, 0.025f, 0.012f, 0.76f));
        badge.pad(12f);
        badge.add(chapterLabel).growX();
        badge.row();
        badge.add(hint).growX().padTop(6f);
        panel.add(badge).growX().top();
        panel.row();
        panel.add().growY();
        panel.row();
        panel.add(this.startButton).size(240f, 92f).padBottom(8f);
        stack.add(panel);
        return stack;
    }

    private Image chapterPreviewImage() {
        try {
            TextureRegion source = this.assets.getTextureBank().region(
                    PvzChapterVisuals.backgroundResourceId(this.chapterType)
            );
            if (source == null) {
                return null;
            }
            float panelAspect = RIGHT_PANEL_WIDTH / RIGHT_PANEL_HEIGHT;
            int cropWidth = Math.min(
                    source.getRegionWidth(),
                    Math.max(1, Math.round(source.getRegionHeight() * panelAspect))
            );
            int cropX = Math.max(0, (source.getRegionWidth() - cropWidth) / 2);
            TextureRegion crop = new TextureRegion(
                    source,
                    cropX,
                    0,
                    cropWidth,
                    source.getRegionHeight()
            );
            Image image = new Image(new TextureRegionDrawable(crop));
            image.setScaling(Scaling.stretch);
            image.setTouchable(Touchable.disabled);
            return image;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Actor detailPanel() {
        Table panel = colored(PAPER);
        panel.pad(9f);
        Table header = colored(GREEN_HEADER);
        header.add(this.nameLabel).left().padLeft(14f).growX();
        panel.add(header).height(54f).growX().colspan(2);
        panel.row();
        panel.add(this.detailCardHost).size(220f, 138f).pad(8f);
        Table text = new Table();
        text.top().left();
        this.descriptionLabel.setWrap(true);
        this.detailStatusLabel.setWrap(true);
        text.add(this.descriptionLabel).growX().left().top().pad(4f, 6f, 4f, 6f);
        text.row();
        text.add(this.detailStatusLabel).growX().left().pad(4f, 6f, 8f, 6f);
        text.row();
        Table buttons = new Table();
        buttons.left();
        buttons.add(this.upgradeButton).size(200f, 56f).padRight(12f);
        buttons.add(this.boostButton).size(200f, 56f);
        text.add(buttons).left().padLeft(4f);
        panel.add(text).grow();
        return panel;
    }

    private Actor gridPanel() {
        Table panel = colored(WOOD_DARK);
        Table toolbar = new Table();
        toolbar.left();
        Label title = titleLabel("AVAILABLE PLANTS");
        title.setFontScale(0.74f);
        toolbar.add(title).left().growX();
        toolbar.add(sortFamilyButton()).size(52f).padRight(10f);
        Label sort = bodyLabel("Sort family");
        toolbar.add(sort).right().padRight(12f);
        this.filterButton = actionButton("FILTER: ALL", "brown", this::cycleFilter);
        this.filterButton.getLabel().setColor(PvzVisualTheme.TEXT_CREAM);
        toolbar.add(this.filterButton).size(250f, 50f).padRight(12f);
        panel.add(toolbar).height(58f).growX();
        panel.row();
        panel.add(scrollPane()).grow();
        return panel;
    }

    private ScrollPane scrollPane() {
        ScrollPane pane = new ScrollPane(this.grid);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        pane.setSmoothScrolling(true);
        return pane;
    }

    private Actor sortFamilyButton() {
        Drawable up = resourceDrawable(CHOOSER_SORT_FAMILY);
        Drawable down = resourceDrawable(CHOOSER_SORT_FAMILY_PRESS);
        if (up == null) {
            up = this.skin.getDrawable("image_ui_generic_greenbutton");
        }
        if (down == null) {
            down = up;
        }
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = up;
        style.imageDown = down;
        ImageButton button = new ImageButton(style);
        button.addListener(click(() -> {
            this.sortByFamily = !this.sortByFamily;
            rebuildGrid();
            showStatus(this.sortByFamily ? "Sorted by plant family." : "Default plant order.", true);
        }));
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }


    private void cycleFilter() {
        PlantCollectionFilter[] filters = {
                PlantCollectionFilter.ALL,
                PlantCollectionFilter.UPGRADEABLE,
                PlantCollectionFilter.SUN,
                PlantCollectionFilter.PEA,
                PlantCollectionFilter.LOBBER,
                PlantCollectionFilter.EXPLOSIVE,
                PlantCollectionFilter.MELEE,
                PlantCollectionFilter.DEFENSE,
                PlantCollectionFilter.SHARP,
                PlantCollectionFilter.TRAP,
                PlantCollectionFilter.FIRE,
                PlantCollectionFilter.COLD,
                PlantCollectionFilter.POISON,
                PlantCollectionFilter.MAGIC
        };
        int nextIndex = 0;
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == this.activeFilter) {
                nextIndex = (i + 1) % filters.length;
                break;
            }
        }
        this.activeFilter = filters[nextIndex];
        if (this.filterButton != null) {
            this.filterButton.setText("FILTER: " + filterLabel(this.activeFilter));
        }
        rebuildGrid();
        showStatus("Filter: " + this.activeFilter.getDisplayName(), true);
    }

    private String filterLabel(PlantCollectionFilter filter) {
        if (filter == null || filter == PlantCollectionFilter.ALL) {
            return "ALL";
        }
        return filter.name().replace('_', ' ');
    }

    private Actor closeButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = resourceDrawable(BACK_UP);
        style.imageDown = resourceDrawable(BACK_DOWN);
        ImageButton button = new ImageButton(style);
        button.setBounds(32f, 985f, 82f, 78f);
        button.addListener(click(() -> {
            this.dataSource.save();
            CollectionUiAnimator.leaveScreen(this.stage, () -> run(this.onClose));
        }));
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private void loadAvailableStates() {
        this.availableStates.clear();
        List<PlantCollectionState> states = this.dataSource.getPlants();
        if (states == null) {
            return;
        }
        for (PlantCollectionState state : states) {
            if (state != null && this.dataSource.isAvailable(state.getName())) {
                this.availableStates.add(state);
            }
        }
    }

    private void chooseActiveState() {
        String activeName = this.activeState == null ? null : this.activeState.getName();
        if (activeName != null) {
            for (PlantCollectionState state : this.availableStates) {
                if (sameName(activeName, state.getName())) {
                    this.activeState = state;
                    return;
                }
            }
        }
        this.activeState = this.availableStates.isEmpty() ? null : this.availableStates.get(0);
    }

    private void rebuildGrid() {
        this.grid.clearChildren();
        this.gridCards.clear();
        List<PlantCollectionState> states = new ArrayList<>();
        for (PlantCollectionState state : this.availableStates) {
            if (this.activeFilter.matches(state, this.dataSource.getCoins())) {
                states.add(state);
            }
        }
        if (this.sortByFamily) {
            states.sort(familyComparator());
        }
        int column = 0;
        for (PlantCollectionState state : states) {
            PlantCard card = createPlantCard(state, false);
            this.gridCards.add(card);
            this.grid.add(new ScaledPlantCard(card, GRID_CARD_SCALE))
                    .size(
                            PlantCard.CARD_WIDTH * GRID_CARD_SCALE,
                            PlantCard.CARD_HEIGHT * GRID_CARD_SCALE
                    )
                    .pad(4f, 5f, 4f, 5f);
            column++;
            if (column >= GRID_COLUMNS) {
                this.grid.row();
                column = 0;
            }
        }
        this.grid.invalidateHierarchy();
        reconcileActiveState(states);
        rebuildActiveDetail();
    }

    private void reconcileActiveState(List<PlantCollectionState> visibleStates) {
        String activeName = this.activeState == null ? null : this.activeState.getName();
        if (activeName != null) {
            for (PlantCollectionState state : visibleStates) {
                if (sameName(activeName, state.getName())) {
                    this.activeState = state;
                    return;
                }
            }
        }
        this.activeState = visibleStates.isEmpty() ? null : visibleStates.get(0);
    }

    private boolean isActiveStateVisible() {
        if (this.activeState == null) {
            return false;
        }
        for (PlantCollectionState state : this.availableStates) {
            if (this.activeFilter.matches(state, this.dataSource.getCoins())
                    && sameName(this.activeState.getName(), state.getName())) {
                return true;
            }
        }
        return false;
    }

    private PlantCard createPlantCard(PlantCollectionState state, boolean selectedBankCard) {
        PlantCard card = new PlantCard(
                this.skin,
                this.assets.getTextureBank(),
                this.assets.getPamPlayer(),
                this.animationCatalog.find(state.getName()),
                state,
                this.visualCatalog.find(state.getName())
        );
        card.setSelected(this.dataSource.isSelected(state.getName()));
        card.setBoosted(this.dataSource.isBoosted(state.getName()));
        card.setSunCostVisible(true);
        card.setActionListener(new PlantCard.PlantCardActionListener() {
            @Override
            public void onPlantCardClicked(PlantCard clicked) {
                CollectionUiAnimator.playClickPulse(clicked);
                setActiveState(state);
                togglePlant(state.getName());
            }

            @Override
            public void onUpgradeRequested(PlantCard ignored) {
                setActiveState(state);
                upgradeActivePlant();
            }
        });
        if (selectedBankCard) {
            card.setSeedProgressVisible(false);
            card.getAvailabilityLabel().setText("Selected");
        }
        return card;
    }

    private void refreshSelectionVisuals() {
        for (PlantCard card : this.gridCards) {
            card.setSelected(this.dataSource.isSelected(card.getPlantName()));
            card.setBoosted(this.dataSource.isBoosted(card.getPlantName()));
        }
        rebuildSelectedBank();
        rebuildActiveDetail();
        updateResources();
        int selected = this.dataSource.getSelectedCount();
        this.selectedCountLabel.setText("SELECTED  " + selected + "/" + this.dataSource.getSlotCount());
        boolean startDisabled = selected <= 0 || this.startTransitionPending;
        this.startButton.setDisabled(startDisabled);
        this.startButton.setTouchable(startDisabled ? Touchable.disabled : Touchable.enabled);
        this.startButton.setColor(startDisabled ? PvzVisualTheme.DISABLED_TINT : Color.WHITE);
        this.startButton.getLabel().setColor(startDisabled
                ? new Color(0.72f, 0.72f, 0.70f, 1f)
                : PvzVisualTheme.TEXT_CREAM);
    }

    private void rebuildSelectedBank() {
        this.selectedBank.clearChildren();
        List<PlantCollectionState> selected = selectedStates();
        Set<String> selectedNames = selectedNameSet(selected);
        int slots = this.dataSource.getSlotCount();
        for (int i = 0; i < slots; i++) {
            if (i < selected.size()) {
                PlantCollectionState state = selected.get(i);
                PlantCard card = createPlantCard(state, true);
                ScaledPlantCard scaled = new ScaledPlantCard(card, BANK_CARD_SCALE);
                if (!this.previousSelectedNames.contains(normalize(state.getName()))) {
                    scaled.getColor().a = 0f;
                    scaled.setScale(0.92f);
                    scaled.setX(-18f);
                    scaled.setOrigin(scaled.getPrefWidth() / 2f, scaled.getPrefHeight() / 2f);
                    scaled.addAction(Actions.parallel(
                            Actions.fadeIn(0.13f),
                            Actions.scaleTo(1f, 1f, 0.15f),
                            Actions.moveBy(18f, 0f, 0.15f)
                    ));
                }
                this.selectedBank.add(scaled)
                        .size(scaled.getPrefWidth(), scaled.getPrefHeight())
                        .padBottom(4f);
            } else {
                this.selectedBank.add(emptySlot())
                        .size(
                                PlantCard.CARD_WIDTH * BANK_CARD_SCALE,
                                PlantCard.PREVIEW_HEIGHT * BANK_CARD_SCALE
                        )
                        .padBottom(4f);
            }
            this.selectedBank.row();
        }
        this.previousSelectedNames.clear();
        this.previousSelectedNames.addAll(selectedNames);
        this.selectedBank.invalidateHierarchy();
    }

    private Set<String> selectedNameSet(List<PlantCollectionState> states) {
        Set<String> names = new LinkedHashSet<>();
        for (PlantCollectionState state : states) {
            if (state != null && state.getName() != null) {
                names.add(normalize(state.getName()));
            }
        }
        return names;
    }

    private Actor emptySlot() {
        Stack slot = new Stack();
        Drawable drawable = resourceDrawable(CHOOSER_SLOT);
        if (drawable == null) {
            drawable = this.skin.newDrawable(WHITE_PIXEL, new Color(0.16f, 0.20f, 0.17f, 1f));
        }
        Image background = new Image(drawable);
        background.setScaling(Scaling.stretch);
        background.setColor(1f, 1f, 1f, 0.66f);
        slot.add(background);
        return slot;
    }

    private List<PlantCollectionState> selectedStates() {
        List<PlantCollectionState> selected = new ArrayList<>();
        for (PlantCollectionState state : this.availableStates) {
            if (this.dataSource.isSelected(state.getName())) {
                selected.add(state);
            }
        }
        return selected;
    }

    private void setActiveState(PlantCollectionState state) {
        this.activeState = state;
        rebuildActiveDetail();
    }

    private void rebuildActiveDetail() {
        this.detailCardHost.clearChildren();
        PlantCollectionState state = this.activeState;
        if (state == null) {
            this.nameLabel.setText("NO PLANT");
            this.descriptionLabel.setText("");
            this.detailStatusLabel.setText("");
            this.upgradeButton.setText("UPGRADE");
            this.upgradeButton.setDisabled(true);
            this.upgradeButton.setTouchable(Touchable.disabled);
            this.upgradeButton.setColor(PvzVisualTheme.DISABLED_TINT);
            this.boostButton.setText("BOOST");
            this.boostButton.setDisabled(true);
            this.boostButton.setTouchable(Touchable.disabled);
            this.boostButton.setColor(PvzVisualTheme.DISABLED_TINT);
            return;
        }
        PlantCard card = createPlantCard(state, false);
        card.setSelected(this.dataSource.isSelected(state.getName()));
        card.setBoosted(this.dataSource.isBoosted(state.getName()));
        card.setSunCostVisible(true);
        this.detailCardHost.add(new ScaledPlantCard(card, 0.92f));
        this.nameLabel.setText(state.getName());
        this.descriptionLabel.setText(summary(state));
        this.detailStatusLabel.setText(detailStatus(state));
        int requiredCoins = Math.max(0, state.getRequiredCoins());
        this.upgradeButton.setText(requiredCoins > 0
                ? "UPGRADE  " + formatNumber(requiredCoins)
                : "UPGRADE");
        boolean canUpgrade = state.isUnlocked()
                && state.getCurrentLevel() < state.getMaximumLevel()
                && state.getRequiredSeedPackets() > 0
                && state.getSeedPackets() >= state.getRequiredSeedPackets()
                && this.dataSource.getCoins() >= requiredCoins;
        this.upgradeButton.setDisabled(!canUpgrade);
        this.upgradeButton.setTouchable(canUpgrade ? Touchable.enabled : Touchable.disabled);
        this.upgradeButton.setColor(canUpgrade ? Color.WHITE : PvzVisualTheme.DISABLED_TINT);
        boolean selected = this.dataSource.isSelected(state.getName());
        boolean boosted = this.dataSource.isBoosted(state.getName());
        boolean canBoost = selected
                && !boosted
                && this.dataSource.getGems() >= PlantPickController.BOOST_COST;
        this.boostButton.setText(boosted
                ? "BOOSTED"
                : selected ? "BOOST  " + PlantPickController.BOOST_COST : "SELECT FIRST");
        this.boostButton.setDisabled(!canBoost);
        this.boostButton.setTouchable(canBoost ? Touchable.enabled : Touchable.disabled);
        this.boostButton.setColor(canBoost ? Color.WHITE : PvzVisualTheme.DISABLED_TINT);
    }

    private String detailStatus(PlantCollectionState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("Sun ").append(state.getSunCost());
        builder.append("   LVL ").append(state.getCurrentLevel());
        if (this.dataSource.isSelected(state.getName())) {
            builder.append("   SELECTED");
        }
        if (this.dataSource.isGreenhouseBoosted(state.getName())) {
            builder.append("   GREENHOUSE BOOST");
        } else if (this.dataSource.isBoosted(state.getName())) {
            builder.append("   BOOSTED");
        }
        return builder.toString();
    }

    private void togglePlant(String plantName) {
        String message = this.dataSource.togglePlant(plantName);
        showStatus(message, actionSucceeded(message));
        refreshSelectionVisuals();
    }

    private void boostActivePlant() {
        if (!isActiveStateVisible()) {
            return;
        }
        String message = this.dataSource.boostPlant(this.activeState.getName());
        showStatus(message, actionSucceeded(message));
        refreshSelectionVisuals();
    }

    private void upgradeActivePlant() {
        if (!isActiveStateVisible()) {
            return;
        }
        CollectionActionResult result = this.dataSource.upgradePlant(this.activeState.getName());
        boolean success = result != null && result.isSuccessful();
        String message = result == null ? "Upgrade failed." : safe(result.getMessage());
        showStatus(message, success);
        if (success) {
            refreshAll();
        }
    }

    private void startGame() {
        if (this.startTransitionPending) {
            return;
        }
        String message = this.dataSource.startGame();
        boolean success = this.dataSource.isStarted();
        showStatus(message, success);
        if (!success) {
            return;
        }
        this.startTransitionPending = true;
        this.startButton.setDisabled(true);
        this.startButton.setTouchable(Touchable.disabled);
        this.dataSource.save();
        CollectionUiAnimator.leaveScreen(this.stage, () -> run(this.onStart));
    }

    private void updateResources() {
        this.resourceStrip.setCounts(0, this.dataSource.getGems(), this.dataSource.getCoins());
        boolean debug = this.dataSource.isDebugModeEnabled()
                && this.dataSource.supportsCurrencyCheats();
        this.resourceStrip.setDebugControls(
                debug,
                this::debugAddGems,
                this::debugAddCoins
        );
    }

    private void debugAddCoins() {
        this.dataSource.cheatAddCoins(DEBUG_COIN_INCREMENT);
        refreshAll();
        showStatus("Debug: +" + formatNumber(DEBUG_COIN_INCREMENT) + " coins", true);
    }

    private void debugAddGems() {
        this.dataSource.cheatAddGems(DEBUG_GEM_INCREMENT);
        refreshAll();
        showStatus("Debug: +" + formatNumber(DEBUG_GEM_INCREMENT) + " gems", true);
    }

    private Comparator<PlantCollectionState> familyComparator() {
        return Comparator
                .comparing(this::familyName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PlantCollectionState::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private String familyName(PlantCollectionState state) {
        PlantPacketCatalog.FamilyVisual family = PlantPacketCatalog.findFamily(state);
        if (family == null || family.getGlyphResourceId() == null) {
            return "zz";
        }
        return family.getGlyphResourceId();
    }

    private String summary(PlantCollectionState state) {
        String description = safe(state.getBaseAbilityDescription()).trim();
        if (!description.isEmpty() && !containsArabic(description)) {
            return description;
        }
        PlantCategory category = state.getCategories() == null || state.getCategories().isEmpty()
                ? null
                : state.getCategories().iterator().next();
        if (category == null) {
            return "A battle plant ready to join your seed bank.";
        }
        switch (category) {
            case SUN_PRODUCER:
                return "Produces sun to support your defenses.";
            case SHOOTER:
                return "Shoots enemies from a distance.";
            case LOBBER:
                return "Lobs projectiles over obstacles and front-line threats.";
            case EXPLOSIVE:
                return "Deals heavy burst damage to groups of zombies.";
            case MELEE_ATTACKER:
                return "Attacks zombies at close range.";
            case DEFENDER:
                return "Protects nearby plants and slows the zombie advance.";
            case MODIFIER:
                return "Changes the battlefield with a special support effect.";
            case STRIKE_THROUGH:
                return "Hits multiple targets along its attack path.";
            case HOMING:
                return "Tracks targets instead of relying on a straight lane.";
            case MINT:
                return "Temporarily empowers plants from its family.";
            default:
                return "A battle plant ready to join your seed bank.";
        }
    }

    private boolean containsArabic(String text) {
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (value >= '\u0600' && value <= '\u06FF') {
                return true;
            }
        }
        return false;
    }

    private Table framedPanel() {
        Table panel = colored(WOOD);
        panel.pad(8f);
        return panel;
    }

    private Table colored(Color color) {
        Table table = new Table();
        table.setBackground(this.skin.newDrawable(WHITE_PIXEL, color));
        return table;
    }

    private Image colorImage(Color color) {
        Image image = new Image(this.skin.getDrawable(WHITE_PIXEL));
        image.setColor(color);
        return image;
    }

    private Image resourceImage(String resourceId) {
        return PvzVisualTheme.resourceImage(this.assets, this.skin, resourceId, Scaling.stretch);
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureRegion region = this.assets.getTextureBank().region(resourceId);
            if (region != null) {
                return new TextureRegionDrawable(region);
            }
        } catch (RuntimeException ignored) {
            // The fallback below keeps the menu usable if the optional atlas is absent.
        }
        return null;
    }

    private TextButton actionButton(String text, String style, Runnable action) {
        TextButton button = new TextButton(text, this.skin, style);
        button.getLabel().setFontScale(0.75f);
        button.addListener(click(() -> {
            CollectionUiAnimator.playClickPulse(button);
            run(action);
        }));
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private Label titleLabel(String text) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(0.80f);
        return label;
    }

    private Label bodyLabel(String text) {
        Label label = new Label(text, this.skin, "secondary");
        label.setFontScale(0.78f);
        label.setColor(new Color(0.18f, 0.11f, 0.035f, 1f));
        return label;
    }


    private Label darkBodyLabel(String text) {
        Label label = new Label(text, this.skin, "medium_outline");
        label.setFontScale(0.58f);
        label.setColor(PvzVisualTheme.TEXT_CREAM);
        label.setWrap(true);
        return label;
    }

    private Label statusLabel() {
        Label label = new Label("", this.skin, "medium_outline");
        label.setFontScale(0.66f);
        label.setAlignment(Align.center);
        label.setBounds(460f, 24f, 1000f, 48f);
        label.setVisible(false);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private void showStatus(String message, boolean success) {
        this.statusLabel.setText(safe(message).isBlank() ? "Done." : message.trim());
        this.statusLabel.setColor(success ? Color.WHITE : new Color(1f, 0.42f, 0.32f, 1f));
        this.statusLabel.setVisible(true);
        this.statusLabel.clearActions();
        this.statusLabel.addAction(Actions.sequence(
                Actions.alpha(1f),
                Actions.delay(2.5f),
                Actions.fadeOut(0.25f),
                Actions.visible(false)
        ));
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                run(action);
            }
        };
    }

    private boolean actionSucceeded(String message) {
        String normalized = safe(message).toLowerCase(Locale.ROOT);
        return normalized.contains("was added")
                || normalized.contains("was removed")
                || normalized.contains("was boosted")
                || normalized.contains("already available from greenhouse")
                || normalized.contains("game started");
    }

    private boolean sameName(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private String formatNumber(int value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
