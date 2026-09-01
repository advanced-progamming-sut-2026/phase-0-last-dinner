package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import controller.ApplicationController;
import model.User.User;
import model.chapters.ChapterType;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameMenuScreen implements Screen {

    public interface Navigator {
        void openChapterMenu(ChapterType chapter);

        void openGreenhouse();

        void openTravelLog();

        void openLeaderboard();

        void openCollectionMenu();

        void onBack();
    }

    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final float TILE_SIZE = 200f;
    private static final float HUD_ICON_SIZE = 64f;
    private static final String LOCK_ICON_PATH = "Assets/Exports/perk_icon_locked.png";
    private static final float LOCK_ICON_SIZE = 96f;

    private static final String TRAVEL_LOG_ICON_PATH = "Assets/Exports/buttons_hud_task_list_normal.png";
    private static final String GREENHOUSE_ICON_PATH = "Assets/Exports/buttons_hud_zg_normal.png";
    private static final String LEADERBOARD_ICON_PATH = "Assets/Exports/icon.png";
    private static final String COLLECTION_ICON_PATH = "Assets/Exports/QuestIcons_Plant.png";

    private static final String COIN_PILL_PATH = "Assets/Exports/buttons_coin_buy_normal.png";
    private static final String DIAMOND_PILL_PATH = "Assets/Exports/buttons_premium_normal.png";
    private static final float WALLET_PILL_HEIGHT = 40f;
    private static final float COIN_BOX_START = 0.326f;
    private static final float COIN_BOX_END = 0.785f;
    private static final float DIAMOND_BOX_START = 0.348f;
    private static final float DIAMOND_BOX_END = 0.740f;

    private static final Map<ChapterType, String> CHAPTER_ART = new LinkedHashMap<>();
    private static final Map<ChapterType, String> CHAPTER_LABEL = new LinkedHashMap<>();

    static {
        CHAPTER_ART.put(ChapterType.ANCIENT_EGYPT, "Assets/Exports/zomboss_node_egypt_914x994.png");
        CHAPTER_ART.put(ChapterType.ICE_CAVES, "Assets/Exports/iceCaves.png");
        CHAPTER_ART.put(ChapterType.BIG_WAVE_BEACH, "Assets/Exports/beach.png");
        CHAPTER_ART.put(ChapterType.MEDIEVAL, "Assets/Exports/medieval.png");

        CHAPTER_LABEL.put(ChapterType.ANCIENT_EGYPT, "Ancient Egypt");
        CHAPTER_LABEL.put(ChapterType.ICE_CAVES, "Ice Caves");
        CHAPTER_LABEL.put(ChapterType.BIG_WAVE_BEACH, "Big Wave Beach");
        CHAPTER_LABEL.put(ChapterType.MEDIEVAL, "Medieval");
    }

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();

    private Stage stage;
    private Label statusLabel;

    public GameMenuScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null) {
            throw new IllegalArgumentException("Controller and navigator are required");
        }
        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new college.java.project.graphics.SfxStage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        Skin skin = PvzSkin.get();

        this.stage.addActor(this.createImageFill(BACKGROUND_PATH));

        TextButton backButton = new TextButton("Back", skin, "brown");
        backButton.getLabel().setFontScale(0.8f);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.execute("menu exit");
                navigator.onBack();
            }
        });

        User user = this.controller.getCurrentUser();
        int gold = user == null ? 0 : user.getGold();
        int diamonds = user == null ? 0 : user.getDiamond();
        Actor travelLogIcon = this.createIconAction(TRAVEL_LOG_ICON_PATH, "menu enter travel-log", this.navigator::openTravelLog);
        Actor greenhouseIcon = this.createIconAction(GREENHOUSE_ICON_PATH, "menu greenhouse", this.navigator::openGreenhouse);
        Actor leaderboardIcon = this.createIconAction(
            LEADERBOARD_ICON_PATH,
            "menu enter leaderboard",
            this.navigator::openLeaderboard
        );        Actor collectionIcon = this.createIconLink(COLLECTION_ICON_PATH, this.navigator::openCollectionMenu);

        Table topLeftGroup = new Table();
        topLeftGroup.add(travelLogIcon).size(HUD_ICON_SIZE).padRight(10);
        topLeftGroup.add(greenhouseIcon).size(HUD_ICON_SIZE).padRight(10);
        topLeftGroup.add(leaderboardIcon).size(HUD_ICON_SIZE).padRight(10);
        topLeftGroup.add(collectionIcon).size(HUD_ICON_SIZE);

        WalletPill coinPill = this.createWalletPill(COIN_PILL_PATH, gold, COIN_BOX_START, COIN_BOX_END, skin);
        WalletPill diamondPill = this.createWalletPill(DIAMOND_PILL_PATH, diamonds, DIAMOND_BOX_START, DIAMOND_BOX_END, skin);
        this.attachCheatButton(coinPill, COIN_BOX_END, "coin", 1000);
        this.attachCheatButton(diamondPill, DIAMOND_BOX_END, "diamond", 5);

        Table walletGroup = new Table();
        walletGroup.add(coinPill.root).padBottom(8).row();
        walletGroup.add(diamondPill.root);

        Table chapterGrid = new Table();
        chapterGrid.defaults().pad(12);
        int column = 0;
        for (Map.Entry<ChapterType, String> entry : CHAPTER_ART.entrySet()) {
            ChapterType chapter = entry.getKey();
            boolean unlocked = user != null && user.isChapterUnlocked(chapter);
            chapterGrid.add(this.createChapterTile(chapter, entry.getValue(), skin, unlocked))
                .size(TILE_SIZE, TILE_SIZE + 32);
            column++;
            if (column == 2) {
                chapterGrid.row();
                column = 0;
            }
        }

        this.statusLabel = new Label("", skin, "secondary");

        Table backBox = new Table();
        backBox.add(backButton).size(110, 44);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        this.stage.addActor(root);

        root.top();
        root.add(topLeftGroup).top().left();
        root.add().expandX();
        root.add(walletGroup).top().right();
        root.row();
        root.add(chapterGrid).colspan(3).expand().center();
        root.row();
        root.add().left();
        root.add().expandX();
        root.add(backBox).bottom().right();
        root.row();
        root.add(this.statusLabel).colspan(3).center();
    }

    private static final class WalletPill {
        private final Stack root;
        private final Label amountLabel;
        private final float pillWidth;

        private WalletPill(Stack root, Label amountLabel, float pillWidth) {
            this.root = root;
            this.amountLabel = amountLabel;
            this.pillWidth = pillWidth;
        }
    }

    private WalletPill createWalletPill(String assetPath, int amount, float boxStart, float boxEnd, Skin skin) {
        Texture texture = this.loadTexture(assetPath);
        float aspect = (float) texture.getWidth() / (float) texture.getHeight();
        float pillWidth = WALLET_PILL_HEIGHT * aspect;

        Image pillImage = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        Table pillBox = new Table();
        pillBox.add(pillImage).size(pillWidth, WALLET_PILL_HEIGHT);

        Label amountLabel = new Label(String.valueOf(amount), skin, "default");
        amountLabel.setColor(Color.WHITE);
        amountLabel.setAlignment(Align.center);
        Container<Label> amountContainer = new Container<>(amountLabel);
        amountContainer.padLeft(pillWidth * boxStart);
        amountContainer.padRight(pillWidth * (1f - boxEnd));
        amountContainer.align(Align.center);

        Stack pill = new Stack();
        pill.add(pillBox);
        pill.add(amountContainer);
        return new WalletPill(pill, amountLabel, pillWidth);
    }

    private void attachCheatButton(WalletPill pill, float boxEnd, String currency, int amount) {
        Table plusRegion = new Table();
        plusRegion.setTouchable(Touchable.enabled);
        plusRegion.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!GameSettings.isDebugMode()) {
                    return;
                }
                String result = controller.execute("menu cheat add " + amount + " " + currency);
                if (result != null && !result.isEmpty()) {
                    statusLabel.setText(result);
                }
                User refreshedUser = controller.getCurrentUser();
                if (refreshedUser != null) {
                    int updated = "coin".equalsIgnoreCase(currency)
                        ? refreshedUser.getGold()
                        : refreshedUser.getDiamond();
                    pill.amountLabel.setText(String.valueOf(updated));
                }
            }
        });

        Container<Table> plusContainer = new Container<>(plusRegion);
        plusContainer.padLeft(pill.pillWidth * boxEnd);
        plusContainer.fill();
        pill.root.add(plusContainer);
    }

    private Stack createChapterTile(ChapterType chapter, String assetPath, Skin skin, boolean unlocked) {
        Image art = this.createImageFit(assetPath, TILE_SIZE, TILE_SIZE);
        if (!unlocked) {
            art.setColor(0.35f, 0.35f, 0.35f, 1f);
        }

        Label nameLabel = new Label(CHAPTER_LABEL.get(chapter), skin, "secondary");
        nameLabel.setAlignment(Align.center);

        Table namePlate = new Table();
        namePlate.setBackground(skin.getDrawable("image_ui_if_bundle_reward_multiplier_bg_10"));
        namePlate.add(nameLabel).padLeft(10).padRight(10).padTop(4).padBottom(6);

        Container<Table> nameContainer = new Container<>(namePlate);
        nameContainer.width(TILE_SIZE);
        nameContainer.align(Align.bottom);

        Stack tile = new Stack();
        tile.add(art);
        if (!unlocked) {
            Image lockIcon = this.createImageFit(LOCK_ICON_PATH, LOCK_ICON_SIZE, LOCK_ICON_SIZE);
            Table lockBox = new Table();
            lockBox.add(lockIcon).size(LOCK_ICON_SIZE, LOCK_ICON_SIZE);
            Container<Table> lockContainer = new Container<>(lockBox);
            lockContainer.align(Align.center);
            tile.add(lockContainer);
            tile.setTouchable(Touchable.disabled);
        } else {
            tile.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    String result = controller.execute("menu enter chapter -c " + chapter.name());
                    if (result != null && !result.isEmpty()) {
                        statusLabel.setText(result);
                    }
                    navigator.openChapterMenu(chapter);
                }
            });
        }

        Table withLabel = new Table();
        withLabel.add(tile).size(TILE_SIZE, TILE_SIZE).row();
        withLabel.add(nameContainer).padTop(6);

        Stack wrapper = new Stack();
        wrapper.add(withLabel);
        return wrapper;
    }

    private Actor createIconAction(String assetPath, String command, Runnable onSuccess) {
        Image icon = this.createImageFit(assetPath, HUD_ICON_SIZE, HUD_ICON_SIZE);
        Stack button = new Stack();
        button.add(icon);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String result = controller.execute(command);
                if (result != null && !result.isEmpty()) {
                    statusLabel.setText(result);
                }
                onSuccess.run();
            }
        });
        return button;
    }

    private Actor createIconLink(String assetPath, Runnable onClick) {
        Image icon = this.createImageFit(assetPath, HUD_ICON_SIZE, HUD_ICON_SIZE);
        Stack button = new Stack();
        button.add(icon);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
        return button;
    }

    private Image createImageFill(String assetPath) {
        Texture texture = this.loadTexture(assetPath);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setScaling(Scaling.fill);
        image.setFillParent(true);
        return image;
    }

    private Image createImageFit(String assetPath, float width, float height) {
        Texture texture = this.loadTexture(assetPath);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setScaling(Scaling.fit);
        image.setAlign(Align.center);
        image.setSize(width, height);
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
