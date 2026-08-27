package view;

import college.java.project.graphics.GameAssetManager;
import college.java.project.graphics.PamAnimationActor;
import college.java.project.graphics.PamAnimationCatalog;
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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.ApplicationController;
import model.Greenhouse.GreenhouseActionResult;
import model.Greenhouse.GreenhouseActionStatus;
import model.Greenhouse.GreenhouseBoard;
import model.Greenhouse.GreenhousePotState;
import model.Greenhouse.GreenhouseStateResult;
import model.mechanism.Position;
import pvz.skin.PvzSkin;
import view.greenhouse.GreenhouseViewObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GreenhouseScreen implements Screen {

    public interface Navigator {
        void onBack();

        void openShop();
    }

    private static final String BACKGROUND_PATH = "Assets/Exports/greenhouse.png";
    private static final String GROWING_POT_PATH = "Assets/Exports/Growing_Plant_Slot_184x161.png";
    private static final String READY_POT_PATH = "Assets/Exports/Growing_Plant_Slot_184x161_2.png";
    private static final String SHOP_ICON_PATH = "Assets/Exports/hud_event_shop.png";
    private static final String COIN_ICON_PATH = "Assets/Exports/buttons_coin_buy_normal.png";
    private static final String DIAMOND_ICON_PATH = "Assets/Exports/buttons_premium_normal.png";

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private static final float POT_WIDTH = 184f * 0.6f;
    private static final float POT_HEIGHT = 161f * 0.6f;

    private static final float GRID_LEFT = 195f + 90f;
    private static final float GRID_TOP = 560f - 140f;
    private static final float GRID_COLUMN_SPACING = 300f - 100f;
    private static final float GRID_ROW_SPACING = 150f + 10f;

    private static final float SHOP_ICON_SIZE = 96f;
    private static final float HUD_MARGIN = 50f;
    private static final float WALLET_PILL_HEIGHT = 40f;
    private static final float COIN_BOX_START = 0.326f;
    private static final float COIN_BOX_END = 0.785f;
    private static final float DIAMOND_BOX_START = 0.348f;
    private static final float DIAMOND_BOX_END = 0.740f;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();
    private final GameAssetManager assets = new GameAssetManager();
    private final PamAnimationCatalog animationCatalog = new PamAnimationCatalog();

    private Stage stage;
    private Skin skin;
    private Label statusLabel;
    private Label coinsLabel;
    private Label diamondsLabel;
    private Table potLayer;
    private GreenhouseViewObserver observer;

    public GreenhouseScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null) {
            throw new IllegalArgumentException("Controller and navigator are required");
        }
        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        this.skin = PvzSkin.get();

        this.controller.execute("menu enter greenhouse");
        this.observer = this.controller.getOrCreateGreenhouseView().getObserver();

        this.stage.addActor(this.createImageFill(BACKGROUND_PATH));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        this.stage.addActor(root);

        this.statusLabel = new Label("", this.skin, "secondary");

        root.top();
        root.add(this.statusLabel).top().padTop(6f).row();
        this.potLayer = new Table();
        this.potLayer.setFillParent(true);
        this.stage.addActor(this.potLayer);
        buildHud();

        TextButton backButton = new TextButton("Back", this.skin, "brown");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.execute("menu exit");
                navigator.onBack();
            }
        });
        backButton.setPosition(20f, 20f);
        this.stage.addActor(backButton);

        refresh();
    }

    private void buildHud() {
        ImageButton shopButton = createIconButton(SHOP_ICON_PATH, SHOP_ICON_SIZE);
        shopButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                navigator.openShop();
            }
        });
        shopButton.setPosition(HUD_MARGIN, VIRTUAL_HEIGHT - HUD_MARGIN - SHOP_ICON_SIZE);
        this.stage.addActor(shopButton);
        WalletPill diamondPill = this.createWalletPill(DIAMOND_ICON_PATH, DIAMOND_BOX_START, DIAMOND_BOX_END);
        WalletPill coinPill = this.createWalletPill(COIN_ICON_PATH, COIN_BOX_START, COIN_BOX_END);
        this.diamondsLabel = diamondPill.amountLabel;
        this.coinsLabel = coinPill.amountLabel;

        diamondPill.root.setPosition(
            VIRTUAL_WIDTH - HUD_MARGIN - diamondPill.pillWidth,
            VIRTUAL_HEIGHT - HUD_MARGIN - WALLET_PILL_HEIGHT
        );
        this.stage.addActor(diamondPill.root);

        coinPill.root.setPosition(
            VIRTUAL_WIDTH - HUD_MARGIN - diamondPill.pillWidth - coinPill.pillWidth - 12f,
            VIRTUAL_HEIGHT - HUD_MARGIN - WALLET_PILL_HEIGHT
        );
        this.stage.addActor(coinPill.root);
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

    private WalletPill createWalletPill(String assetPath, float boxStart, float boxEnd) {
        Texture texture = this.loadTexture(assetPath);
        float aspect = (float) texture.getWidth() / (float) texture.getHeight();
        float pillWidth = WALLET_PILL_HEIGHT * aspect;

        Image pillImage = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        Table pillBox = new Table();
        pillBox.add(pillImage).size(pillWidth, WALLET_PILL_HEIGHT);

        Label amountLabel = new Label("0", this.skin, "default");
        amountLabel.setColor(Color.WHITE);
        amountLabel.setAlignment(Align.center);
        Container<Label> amountContainer = new Container<>(amountLabel);
        amountContainer.padLeft(pillWidth * boxStart);
        amountContainer.padRight(pillWidth * (1f - boxEnd));
        amountContainer.align(Align.center);

        Stack pill = new Stack();
        pill.add(pillBox);
        pill.add(amountContainer);
        pill.setSize(pillWidth, WALLET_PILL_HEIGHT);
        return new WalletPill(pill, amountLabel, pillWidth);
    }
    private void refresh() {
        GreenhouseStateResult state = this.observer.onShowGreenhouseRequested();
        if (state == null) {
            this.statusLabel.setText("Greenhouse is not available.");
            return;
        }

        this.coinsLabel.setText(String.valueOf(state.getCoins()));
        this.diamondsLabel.setText(String.valueOf(state.getDiamonds()));

        this.potLayer.clearChildren();
        for (int y = 1; y <= GreenhouseBoard.ROW_COUNT; y++) {
            for (int x = 1; x <= GreenhouseBoard.COLUMN_COUNT; x++) {
                GreenhousePotState pot = state.getPot(x, y);
                Actor potActor = buildPotActor(pot, x, y);
                float posX = GRID_LEFT + (x - 1) * GRID_COLUMN_SPACING;
                float posY = GRID_TOP - (y - 1) * GRID_ROW_SPACING;
                potActor.setBounds(posX, posY, POT_WIDTH, POT_HEIGHT);
                this.potLayer.addActor(potActor);
            }
        }
    }

    private Actor buildPotActor(GreenhousePotState pot, int x, int y) {
        if (pot == null || !pot.isUnlocked()) {
            return lockedPotActor(x, y);
        }
        if (pot.isEmpty()) {
            return emptyPotActor(x, y);
        }
        if (pot.isReady()) {
            return growingPotActor(pot, x, y, READY_POT_PATH, true);
        }
        return growingPotActor(pot, x, y, GROWING_POT_PATH, false);
    }

    private Actor lockedPotActor(int x, int y) {
        Stack stack = new Stack();
        Table locked = new Table();
        locked.setBackground(this.skin.newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.45f)));
        stack.add(locked);

        int cost = this.observer.getPotUnlockCost();
        TextButton buyButton = new TextButton("BUY (" + cost + ")", this.skin, "purple");
        buyButton.getLabel().setFontScale(0.7f);
        buyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                runAction(() -> observer.onBuyPotRequested(new Position(x, y)));
            }
        });
        Table buttonLayer = new Table();
        buttonLayer.add(buyButton).size(100f, 36f);
        stack.add(buttonLayer);
        return stack;
    }

    private Actor emptyPotActor(int x, int y) {
        Stack stack = new Stack();
        Table dirt = new Table();
        dirt.setBackground(this.skin.newDrawable("white_pixel", new Color(0.36f, 0.22f, 0.11f, 1f)));
        stack.add(dirt);
        TextButton plantButton = new TextButton("PLANT", this.skin, "green_small");
        plantButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                runAction(() -> observer.onPlantPotRequested(new Position(x, y)));
            }
        });
        Table buttonLayer = new Table();
        buttonLayer.add(plantButton).size(90f, 36f);
        stack.add(buttonLayer);
        return stack;
    }

    private Actor growingPotActor(GreenhousePotState pot, int x, int y, String potArtPath, boolean ready) {
        Stack stack = new Stack();

        Image potImage = new Image(new TextureRegionDrawable(new TextureRegion(this.loadTexture(potArtPath))));
        potImage.setScaling(Scaling.fit);
        stack.add(potImage);

        Actor plantAnimation = buildIdleAnimationActor(pot.getPlantName());
        if (plantAnimation != null) {
            Table animationLayer = new Table();
            animationLayer.bottom();
            animationLayer.setTouchable(Touchable.disabled);
            animationLayer.add(plantAnimation).size(POT_WIDTH * 2.65f, POT_HEIGHT * 2.65f).padBottom(POT_HEIGHT * -0.5f);
            stack.add(animationLayer);
        }

        if (ready) {
            stack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float clickX, float clickY) {
                    runAction(() -> observer.onCollectRequested(new Position(x, y)));
                }
            });
        } else {
            Table overlay = new Table();
            overlay.bottom();
            Label hoursLabel = new Label(pot.getRemainingGrowthHours() + "h", this.skin, "secondary");
            TextButton speedUpButton = new TextButton("SPEED UP", this.skin, "purple");
            speedUpButton.getLabel().setFontScale(0.7f);
            speedUpButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    runAction(() -> observer.onGrowRequested(new Position(x, y)));
                }
            });
            overlay.add(hoursLabel).padBottom(2f).row();
            overlay.add(speedUpButton).size(90f, 28f);
            stack.add(overlay);
        }

        return stack;
    }

    private Actor buildIdleAnimationActor(String plantName) {
        PamAnimationCatalog.AnimationInfo info = this.animationCatalog.find(plantName);
        if (info == null) {
            return null;
        }
        String idleClip = info.getIdleClip();
        if (idleClip == null) {
            return null;
        }
        return new PamAnimationActor(
            this.assets.getPamPlayer(),
            info.getPath(),
            idleClip,
            info.getCanvasWidth(),
            info.getCanvasHeight()
        );
    }
    private void runAction(Supplier<GreenhouseActionResult> action) {
        GreenhouseActionResult result = action.get();
        this.controller.save();
        if (result != null) {
            this.statusLabel.setText(result.getMessage());
            if (result.isSuccessful() && result.getStatus() == GreenhouseActionStatus.HARVESTED) {
                showHarvestAnnouncement(result);
            }
        }
        refresh();
    }

    private void showHarvestAnnouncement(GreenhouseActionResult result) {
        Table backdrop = new Table();
        backdrop.setFillParent(true);
        backdrop.setTouchable(Touchable.enabled);
        backdrop.setBackground(this.skin.getDrawable("modal_background"));
        backdrop.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                backdrop.remove();
            }
        });

        Table content = new Table();
        content.setTouchable(Touchable.enabled);
        content.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(30f);
        content.add(new Label("REWARD COLLECTED!", this.skin, "big_outline")).padBottom(16f).row();
        content.add(new Label(result.getPlantName() + " was harvested.", this.skin, "medium")).padBottom(10f).row();
        if (result.getCoinsEarned() > 0) {
            content.add(new Label("+" + result.getCoinsEarned() + " coins", this.skin, "secondary")).padBottom(10f).row();
        } else if (result.isBoostStored()) {
            content.add(new Label("Plant food boost stored!", this.skin, "secondary")).padBottom(10f).row();
        }

        TextButton okButton = new TextButton("OK", this.skin, "green");
        okButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                backdrop.remove();
            }
        });
        content.add(okButton).size(140f, 48f).padTop(10f);

        Table frame = new Table();
        frame.setBackground(this.skin.getDrawable("image_ui_dialog_asset_dialogborder_10"));
        frame.pad(16f);
        frame.add(content).grow();

        Table wrapper = new Table();
        wrapper.setFillParent(true);
        wrapper.add(frame).width(480f);
        backdrop.addActor(wrapper);

        this.stage.addActor(backdrop);
    }

    private ImageButton createIconButton(String assetPath, float targetHeight) {
        Texture texture = this.loadTexture(assetPath);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        float aspect = (float) texture.getWidth() / (float) texture.getHeight();
        button.getImageCell().size(targetHeight * aspect, targetHeight);
        return button;
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
        this.assets.update();
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
        this.assets.dispose();
        for (Texture texture : this.loadedTextures) {
            texture.dispose();
        }
        this.loadedTextures.clear();
    }
}
