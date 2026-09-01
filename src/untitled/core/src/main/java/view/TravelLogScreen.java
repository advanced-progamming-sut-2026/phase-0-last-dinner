package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import controller.ApplicationController;
import controller.TravelLogController;
import model.GameMenuRelated.Page;
import model.GameMenuRelated.PageName;
import model.GameMenuRelated.QuestObj;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.minigame.StageProgressMiniGame;

public class TravelLogScreen implements Screen {

    public interface Navigator {
        void onBack();
    }

    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String CARD_BACKGROUND = "image_ui_quests_panel_edge_to_edge_ten";
    private static final String PANEL_BACKGROUND = "image_ui_mainmenu_mm_settings_tab_10";
    private static final String PROGRESS_BAR_STYLE = "xp_green";
    private static final String ACTIVE_TAB_STYLE = "green";
    private static final String INACTIVE_TAB_STYLE = "brown";
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final float LIST_WIDTH = 620f;
    private static final float LIST_HEIGHT = 360f;
    private static final float CARD_WIDTH = 580f;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final MiniGameLauncher miniGameLauncher;
    private final List<Texture> loadedTextures = new ArrayList<>();
    private final Map<PageName, TextButton> tabButtons = new LinkedHashMap<>();

    private Stage stage;
    private Label statusLabel;
    private Table listContainer;
    private PageName activePage = PageName.ADVENTURE;

    public TravelLogScreen(ApplicationController controller, Navigator navigator) {
        this(controller, navigator, null);
    }
    public TravelLogScreen(ApplicationController controller, Navigator navigator, MiniGameLauncher miniGameLauncher) {
        if (controller == null || navigator == null) {
            throw new IllegalArgumentException("Controller and navigator are required");
        }
        this.controller = controller;
        this.navigator = navigator;
        this.miniGameLauncher = miniGameLauncher;
    }

    @Override
    public void show() {
        this.stage = new college.java.project.graphics.SfxStage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        Skin skin = PvzSkin.get();

        this.stage.addActor(this.createImageFill(BACKGROUND_PATH));

        this.controller.execute("menu enter travel-log");

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        this.stage.addActor(root);

        Label title = new Label("Quests", skin, "big");

        this.listContainer = new Table();
        ScrollPane scrollPane = new ScrollPane(this.listContainer, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        this.statusLabel = new Label("", skin, "secondary");

        TextButton backButton = new TextButton("Back", skin, "brown");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.execute("menu exit");
                navigator.onBack();
            }
        });

        Table panel = new Table();
        panel.setBackground(skin.getDrawable(PANEL_BACKGROUND));
        panel.pad(28);
        panel.add(title).padBottom(12).row();
        panel.add(this.buildTabsRow(skin)).padBottom(12).row();
        panel.add(scrollPane).width(LIST_WIDTH).height(LIST_HEIGHT).row();
        panel.add(this.statusLabel).padTop(8).row();
        panel.add(backButton).size(130, 44).padTop(12);

        root.add(panel);

        this.showPage(PageName.ADVENTURE);
    }

    private Table buildTabsRow(Skin skin) {
        this.tabButtons.clear();
        Table row = new Table();
        for (PageName pageName : PageName.values()) {
            TextButton button = new TextButton(
                this.displayPageName(pageName), skin,
                pageName == this.activePage ? ACTIVE_TAB_STYLE : INACTIVE_TAB_STYLE);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showPage(pageName);
                }
            });
            this.tabButtons.put(pageName, button);
            row.add(button).width(96).height(36).padRight(4);
        }
        return row;
    }

    private void showPage(PageName pageName) {
        this.activePage = pageName;
        this.refreshTabStyles();

        TravelLogController travelLogController = this.controller.getOrCreateTravelLogController();
        if (travelLogController == null) {
            this.statusLabel.setText("Login is required.");
            return;
        }

        Page page = travelLogController.onChangePageRequested(pageName);
        if (pageName == PageName.MINIGAMES) {
            this.populateMiniGames(page);
        } else {
            this.populateQuests(page);
        }
    }

    private void refreshTabStyles() {
        Skin skin = PvzSkin.get();
        for (Map.Entry<PageName, TextButton> entry : this.tabButtons.entrySet()) {
            String styleName = entry.getKey() == this.activePage ? ACTIVE_TAB_STYLE : INACTIVE_TAB_STYLE;
            entry.getValue().setStyle(skin.get(styleName, TextButton.TextButtonStyle.class));
        }
    }

    private void populateQuests(Page page) {
        Skin skin = PvzSkin.get();
        this.listContainer.clear();

        List<QuestObj> quests = page == null ? null : page.getQuestObjects();
        if (quests == null || quests.isEmpty()) {
            Label empty = new Label("This page currently has no quests.", skin, "secondary");
            empty.setColor(Color.BLACK);
            this.listContainer.add(empty).pad(16);
            return;
        }

        for (QuestObj questObject : quests) {
            if (questObject == null || questObject.getQuest() == null) {
                continue;
            }
            this.listContainer.add(this.buildQuestCard(questObject, skin)).width(CARD_WIDTH).padBottom(10).row();
        }
    }

    private Table buildQuestCard(QuestObj questObject, Skin skin) {
        Table card = new Table();
        card.setBackground(skin.getDrawable(CARD_BACKGROUND));
        card.pad(10);

        Label nameLabel = new Label(questObject.getQuest().getDisplayName(), skin, "medium");
        nameLabel.setColor(Color.BLACK);

        Label descriptionLabel = new Label(questObject.getCompletionCondition(), skin, "secondary");
        descriptionLabel.setColor(Color.BLACK);
        descriptionLabel.setWrap(true);

        Label rewardLabel = new Label("Reward: " + questObject.getReward(), skin, "secondary");
        rewardLabel.setColor(Color.BLACK);

        ProgressBar progressBar = new ProgressBar(0f, 100f, 1f, false, skin, PROGRESS_BAR_STYLE);
        progressBar.setAnimateDuration(0f);
        progressBar.setValue(questObject.getCompletionPercentage());

        Label percentLabel = new Label(questObject.getCompletionPercentage() + "%", skin, "default");
        percentLabel.setColor(Color.BLACK);

        card.add(nameLabel).left().colspan(3).row();
        card.add(descriptionLabel).width(CARD_WIDTH - 40).left().colspan(3).padTop(4).row();
        card.add(rewardLabel).left().padTop(8);
        card.add(progressBar).width(180).padTop(8).padLeft(10);
        card.add(percentLabel).padTop(8).padLeft(6).row();

        if (questObject.isCompleted()) {
            if (questObject.isRewardClaimed()) {
                Label claimedLabel = new Label("Reward claimed", skin, "secondary");
                claimedLabel.setColor(Color.BLACK);
                card.add(claimedLabel).left().colspan(3).padTop(8);
            } else {
                TextButton claimButton = new TextButton("Claim", skin, "green");
                claimButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        claimQuest(questObject);
                    }
                });
                card.add(claimButton).width(110).height(36).left().colspan(3).padTop(8);
            }
        }

        return card;
    }

    private void claimQuest(QuestObj questObject) {
        TravelLogController travelLogController = this.controller.getOrCreateTravelLogController();
        if (travelLogController == null) {
            return;
        }
        String result = travelLogController.onClaimQuestRequested(questObject.getQuest().name());
        this.statusLabel.setText(result);
        this.showPage(this.activePage);
    }

    private void populateMiniGames(Page page) {
        Skin skin = PvzSkin.get();
        this.listContainer.clear();

        List<MiniGame> miniGames = page == null ? null : page.getMiniGames();
        if (miniGames == null || miniGames.isEmpty()) {
            Label empty = new Label("No minigames are available.", skin, "secondary");
            empty.setColor(Color.BLACK);
            this.listContainer.add(empty).pad(16);
            return;
        }

        for (MiniGame miniGame : miniGames) {
            if (miniGame == null) {
                continue;
            }
            this.listContainer.add(this.buildMiniGameCard(miniGame, skin)).width(CARD_WIDTH).padBottom(10).row();
        }
    }

    private Table buildMiniGameCard(MiniGame miniGame, Skin skin) {
        Table card = new Table();
        card.setBackground(skin.getDrawable(CARD_BACKGROUND));
        card.pad(10);

        Label nameLabel = new Label(this.displayMiniGameName(miniGame.getType()), skin, "medium");
        nameLabel.setColor(Color.BLACK);

        int highestUnlockedStage = this.highestUnlockedStage(miniGame);

        String state;
        if (miniGame.isAllStagesCompleted())
            state = "Completed";
        else if (miniGame.isStarted())
            state = "In progress";
        else
            state = "Unlocked stages: " + highestUnlockedStage + "/3";

        Label stateLabel = new Label(state, skin, "secondary");
        stateLabel.setColor(Color.BLACK);

        Table stageButtons = new Table();

        for (int stageNumber = 1; stageNumber <= 3; stageNumber++) {
            final int selectedStage = stageNumber;
            boolean unlocked = stageNumber <= highestUnlockedStage;

            String buttonText = unlocked ? "Stage " + stageNumber : "Stage " + stageNumber + " Locked";

            TextButton stageButton = new TextButton(buttonText, skin, unlocked ? "green" : "brown");

            stageButton.setDisabled(!unlocked);

            stageButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!stageButton.isDisabled())
                        playMiniGame(miniGame, selectedStage);
                }
            });

            stageButtons.add(stageButton).width(170).height(38).padRight(stageNumber < 3 ? 6 : 0);
        }

        card.add(nameLabel).left();
        card.add(stateLabel).right().expandX().row();

        card.add(stageButtons).left().colspan(2).padTop(10);

        return card;
    }

    private void playMiniGame(MiniGame miniGame, int stageNumber) {
        if (miniGame == null || stageNumber < 1 || stageNumber > 3)
            return;

        int highestUnlockedStage = this.highestUnlockedStage(miniGame);

        if (stageNumber > highestUnlockedStage) {
            this.statusLabel.setText("Stage " + stageNumber + " is locked.");
            return;
        }

        MiniGameType type = miniGame.getType();

        boolean opened = this.miniGameLauncher != null && this.miniGameLauncher.open(type, stageNumber);

        this.statusLabel.setText(opened ? this.displayMiniGameName(type) + " stage " + stageNumber + " opened."
                : this.displayMiniGameName(type) + " stage " + stageNumber + " isn't wired up yet.");
    }

    private int highestUnlockedStage(MiniGame miniGame) {
        if (miniGame instanceof StageProgressMiniGame) {
            StageProgressMiniGame stageGame = (StageProgressMiniGame) miniGame;
            return Math.max(1, Math.min(3, stageGame.getHighestUnlockedStage()));
        }

        return 1;
    }

    private String displayPageName(PageName pageName) {
        String raw = pageName.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private String displayMiniGameName(MiniGameType type) {
        if (type == null) {
            return "Unknown";
        }
        switch (type) {
            case VASEBREAKER:
                return "Vasebreaker";
            case WALLNUT_BOWLING:
                return "Wall-nut Bowling";
            case I_ZOMBIE:
                return "I, Zombie";
            case BEGHOULED:
                return "Beghouled";
            case ZOMBOTANY:
                return "Zombotany";
            default:
                return "Unknown";
        }
    }

    private Image createImageFill(String assetPath) {
        Texture texture = this.loadTexture(assetPath);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setScaling(Scaling.fill);
        image.setFillParent(true);
        return image;
    }

    private Texture loadTexture(String assetPath) {
        Texture texture = new Texture(Gdx.files.internal(assetPath));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.loadedTextures.add(texture);
        return texture;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.valueOf("2f4b2f"));
        this.stage.act(delta);
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
        this.stage.dispose();
        for (Texture texture : this.loadedTextures) {
            texture.dispose();
        }
        this.loadedTextures.clear();
    }
}
