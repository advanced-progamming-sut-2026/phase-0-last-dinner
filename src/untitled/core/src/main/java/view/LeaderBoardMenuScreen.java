package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import controller.ApplicationController;
import controller.LeaderBoardController;
import model.User.LeaderboardEntry;
import model.User.LeaderboardSortField;
import model.User.User;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LeaderBoardMenuScreen implements Screen {
    public interface Navigator {
        void onBack();
    }

    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String PANEL_DRAWABLE = "image_ui_mainmenu_mm_settings_tab_10";
    private static final String ROW_DRAWABLE = "image_ui_dialog_asset_inner_bkgd_10";
    private static final String COIN_DRAWABLE = "image_ui_hud_ingame_coin";
    private static final String GEM_DRAWABLE = "image_ui_hud_ingame_gem";

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final float TABLE_WIDTH = 1080f;
    private static final float RANK_WIDTH = 55f;
    private static final float USERNAME_WIDTH = 145f;
    private static final float NICKNAME_WIDTH = 145f;
    private static final float PROGRESS_WIDTH = 175f;
    private static final float MINIGAMES_WIDTH = 120f;
    private static final float DAILY_WIDTH = 120f;
    private static final float NON_DAILY_WIDTH = 145f;
    private static final float MEOW_WIDTH = 135f;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();

    private Stage stage;
    private Table headerTable;
    private Table rowsTable;
    private LeaderBoardController leaderBoardController;
    private LeaderboardSortField sortField = LeaderboardSortField.MEOW_POINTS;
    private boolean ascending;

    private TextButton sortFieldButton;
    private TextButton sortOrderButton;
    private Table sortPopup;

    public LeaderBoardMenuScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null)
            throw new IllegalArgumentException("Controller and navigator are required");

        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new Stage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);

        Skin skin = PvzSkin.get();
        this.leaderBoardController = this.controller.getOrCreateLeaderboardController();
        this.stage.addActor(createBackground());

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20f);
        this.stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable(PANEL_DRAWABLE));
        panel.pad(24f);

        Table titleBar = new Table();
        titleBar.add(new Label("LEADERBOARD", skin, "big_outline")).expandX().left();
        titleBar.add(createWallet()).right();

        Label description = new Label("Choose a field and order to sort players.", skin, "secondary");
        Table sortControls = createSortControls();

        this.headerTable = new Table();
        this.rowsTable = new Table();
        this.rowsTable.top();

        refreshHeader();
        refreshRows();

        ScrollPane scrollPane = new ScrollPane(this.rowsTable);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);

        TextButton backButton = new TextButton("BACK", skin, "brown");
        backButton.addListener(createListener(this::returnToPreviousMenu));

        panel.add(titleBar).width(TABLE_WIDTH).padBottom(8f).row();
        panel.add(description).left().padBottom(10f).row();
        panel.add(sortControls).width(TABLE_WIDTH).left().padBottom(10f).row();
        panel.add(this.headerTable).width(TABLE_WIDTH).padBottom(8f).row();
        panel.add(scrollPane).width(TABLE_WIDTH).height(360f).row();
        panel.add(backButton).size(160f, 52f).padTop(12f);

        root.add(panel).width(1180f).height(670f);
    }

    private void refreshHeader() {
        this.headerTable.clearChildren();

        addStaticHeader("#", RANK_WIDTH);
        addStaticHeader("Username", USERNAME_WIDTH);
        addStaticHeader("Nickname", NICKNAME_WIDTH);
        addStaticHeader("Progress", PROGRESS_WIDTH);
        addStaticHeader("Minigames", MINIGAMES_WIDTH);
        addStaticHeader("Daily", DAILY_WIDTH);
        addStaticHeader("Non-daily", NON_DAILY_WIDTH);
        addStaticHeader("Meow Points", MEOW_WIDTH);
    }

    private void addStaticHeader(String title, float width) {
        Label label = new Label(title, PvzSkin.get(), "medium");
        label.setAlignment(Align.center);
        this.headerTable.add(label).width(width).height(44f).padRight(4f);
    }

    private Table createSortControls() {
        Skin skin = PvzSkin.get();

        this.sortFieldButton = new TextButton("SORT BY: " + getSortFieldName(this.sortField), skin, "green_small");
        this.sortOrderButton = new TextButton(getSortOrderName(), skin, "brown");

        this.sortFieldButton.addListener(createListener(this::toggleSortPopup));
        this.sortOrderButton.addListener(createListener(() -> {
            this.ascending = !this.ascending;
            this.sortOrderButton.setText(getSortOrderName());
            hideSortPopup();
            refreshRows();
        }));

        this.sortPopup = createSortPopup();
        this.stage.addActor(this.sortPopup);

        Table controls = new Table();
        controls.left();
        controls.add(new Label("SORT PLAYERS", skin, "medium")).padRight(12f);
        controls.add(this.sortFieldButton).size(270f, 46f).padRight(10f);
        controls.add(this.sortOrderButton).size(180f, 46f);
        return controls;
    }

    private Table createSortPopup() {
        Table popup = new Table();
        popup.setBackground(PvzSkin.get().getDrawable(ROW_DRAWABLE));
        popup.pad(10f);
        popup.top();

        Label title = new Label("SORT BY", PvzSkin.get(), "medium");
        title.setAlignment(Align.center);
        popup.add(title).growX().height(38f).padBottom(5f).row();

        addSortOption(popup, LeaderboardSortField.USERNAME);
        addSortOption(popup, LeaderboardSortField.PROGRESS);
        addSortOption(popup, LeaderboardSortField.MINIGAMES);
        addSortOption(popup, LeaderboardSortField.DAILY_QUESTS);
        addSortOption(popup, LeaderboardSortField.NON_DAILY_QUESTS);
        addSortOption(popup, LeaderboardSortField.MEOW_POINTS);

        popup.setBounds(120f, 220f, 340f, 325f);
        popup.setVisible(false);
        return popup;
    }

    private void addSortOption(Table popup, LeaderboardSortField field) {
        TextButton option = new TextButton(getSortFieldName(field), PvzSkin.get(), "green_small");
        option.addListener(createListener(() -> selectSortField(field)));
        popup.add(option).growX().height(40f).padBottom(4f).row();
    }

    private void selectSortField(LeaderboardSortField field) {
        this.sortField = field;
        this.sortFieldButton.setText("SORT BY: " + getSortFieldName(field));
        hideSortPopup();
        refreshRows();
    }

    private void toggleSortPopup() {
        boolean visible = !this.sortPopup.isVisible();
        this.sortPopup.setVisible(visible);

        if (visible)
            this.sortPopup.toFront();
    }

    private void hideSortPopup() {
        if (this.sortPopup != null)
            this.sortPopup.setVisible(false);
    }

    private String getSortOrderName() {
        return this.ascending ? "ASCENDING" : "DESCENDING";
    }

    private String getSortFieldName(LeaderboardSortField field) {
        switch (field) {
            case USERNAME:
                return "Username";
            case PROGRESS:
                return "Progress";
            case MINIGAMES:
                return "Minigames";
            case DAILY_QUESTS:
                return "Daily quests";
            case NON_DAILY_QUESTS:
                return "Non-daily quests";
            case MEOW_POINTS:
            default:
                return "Meow points";
        }
    }

    private void refreshRows() {
        this.rowsTable.clearChildren();

        List<LeaderboardEntry> entries = this.leaderBoardController.showLeaderboard(
            this.sortField,
            this.ascending
        );

        if (entries == null || entries.isEmpty()) {
            Label emptyLabel = new Label("No players were found.", PvzSkin.get(), "secondary");
            this.rowsTable.add(emptyLabel).width(TABLE_WIDTH).padTop(80f);
            return;
        }

        for (LeaderboardEntry entry : entries)
            addEntryRow(entry);
    }

    private void addEntryRow(LeaderboardEntry entry) {
        User currentUser = this.controller.getCurrentUser();
        boolean current = currentUser != null && currentUser.getUsername() != null
            && currentUser.getUsername().equalsIgnoreCase(entry.getUsername());

        String username = entry.getUsername() == null ? "-" : entry.getUsername();
        if (current)
            username += " (YOU)";

        Table row = new Table();
        row.setBackground(PvzSkin.get().getDrawable(ROW_DRAWABLE));
        row.pad(4f);

        addCell(row, String.valueOf(entry.getRank()), RANK_WIDTH, current);
        addCell(row, username, USERNAME_WIDTH, current);
        addCell(row, entry.getNickname(), NICKNAME_WIDTH, current);
        addCell(row, formatProgress(entry), PROGRESS_WIDTH, current);
        addCell(row, String.valueOf(entry.getCompletedMinigames()), MINIGAMES_WIDTH, current);
        addCell(row, String.valueOf(entry.getCompletedDailyQuests()), DAILY_WIDTH, current);
        addCell(row, String.valueOf(entry.getCompletedNonDailyQuests()), NON_DAILY_WIDTH, current);
        addCell(row, String.valueOf(entry.getMeowPoints()), MEOW_WIDTH, current);

        this.rowsTable.add(row).width(TABLE_WIDTH).padBottom(5f).row();
    }

    private void addCell(Table row, String value, float width, boolean current) {
        Label label = new Label(value == null || value.isEmpty() ? "-" : value, PvzSkin.get(), "secondary");
        label.setAlignment(Align.center);
        label.setEllipsis(true);

        if (current)
            label.setColor(Color.valueOf("ffe27a"));

        row.add(label).width(width).height(48f).padRight(4f);
    }

    private String formatProgress(LeaderboardEntry entry) {
        String chapter = entry.getLastChapter();

        if (chapter == null || "NOT_STARTED".equals(chapter) || entry.getLastLevel() <= 0)
            return "Not started";

        return formatEnumName(chapter) + " L" + entry.getLastLevel();
    }

    private String formatEnumName(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty())
                continue;

            if (result.length() > 0)
                result.append(' ');

            result.append(Character.toUpperCase(word.charAt(0)));
            result.append(word.substring(1));
        }

        return result.toString();
    }

    private Table createWallet() {
        User user = this.controller.getCurrentUser();
        int coins = user == null ? 0 : user.getGold();
        int diamonds = user == null ? 0 : user.getDiamond();

        Table wallet = new Table();
        wallet.add(createCurrency(COIN_DRAWABLE, coins)).padRight(14f);
        wallet.add(createCurrency(GEM_DRAWABLE, diamonds));
        return wallet;
    }

    private Table createCurrency(String drawableName, int amount) {
        Image icon = new Image(PvzSkin.get().getDrawable(drawableName));
        icon.setScaling(Scaling.fit);

        Label amountLabel = new Label(String.valueOf(amount), PvzSkin.get(), "medium");
        amountLabel.setAlignment(Align.left);

        Table currency = new Table();
        currency.add(icon).size(34f, 30f).padRight(6f);
        currency.add(amountLabel).minWidth(50f).left();
        return currency;
    }

    private void returnToPreviousMenu() {
        this.controller.execute("menu exit");
        this.navigator.onBack();
    }

    private ChangeListener createListener(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    private Image createBackground() {
        Texture texture = new Texture(Gdx.files.internal(BACKGROUND_PATH));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.loadedTextures.add(texture);

        Image background = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        background.setScaling(Scaling.fill);
        background.setFillParent(true);
        return background;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (this.sortPopup != null && this.sortPopup.isVisible())
                hideSortPopup();
            else
                returnToPreviousMenu();

            return;
        }

        ScreenUtils.clear(Color.valueOf("2f4b2f"));
        this.stage.act(Math.min(delta, 1f / 30f));
        this.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (this.stage != null)
            this.stage.dispose();

        for (Texture texture : this.loadedTextures)
            texture.dispose();

        this.loadedTextures.clear();
    }
}
