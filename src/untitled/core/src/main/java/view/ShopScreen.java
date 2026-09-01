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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.ApplicationController;
import college.java.project.graphics.GameplaySoundPlayer;
import model.Plant;
import model.User.User;
import model.shop.CurrencyType;
import model.shop.DailyOfferResult;
import model.shop.PermanentStuff;
import model.shop.ShopActionResult;
import model.shop.ShopCatalogResult;
import model.shop.ShopItemState;
import pvz.skin.PvzSkin;
import view.shop.ShopViewObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopScreen implements Screen {

    public interface Navigator {
        void onBack();
    }

    private static final String COIN_ICON_PATH = "Assets/Exports/buttons_coin_buy_normal.png";
    private static final String DIAMOND_ICON_PATH = "Assets/Exports/buttons_premium_normal.png";

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private static final float HUD_MARGIN = 50f;
    private static final float WALLET_PILL_HEIGHT = 40f;
    private static final float COIN_BOX_START = 0.326f;
    private static final float COIN_BOX_END = 0.785f;
    private static final float DIAMOND_BOX_START = 0.348f;
    private static final float DIAMOND_BOX_END = 0.740f;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();

    private Stage stage;
    private Skin skin;
    private Label statusLabel;
    private Label coinsLabel;
    private Label diamondsLabel;
    private Table itemRows;
    private ShopViewObserver observer;

    public ShopScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null) {
            throw new IllegalArgumentException("Controller and navigator are required");
        }
        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new college.java.project.graphics.SfxStage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        this.skin = PvzSkin.get();

        this.observer = this.controller.getOrCreateShopController();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        this.stage.addActor(root);

        this.statusLabel = new Label("", this.skin, "secondary");
        root.top();
        root.add(this.statusLabel).top().padTop(6f).row();

        this.itemRows = new Table();
        this.itemRows.top();

        ScrollPane scrollPane = new ScrollPane(this.itemRows, this.skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        Table listFrame = new Table();
        listFrame.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        listFrame.pad(16f);
        listFrame.add(scrollPane).grow();

        root.row();
        root.add(listFrame).grow().padTop(10f).row();

        buildHud();

        TextButton backButton = new TextButton("Back", this.skin, "brown");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                navigator.onBack();
            }
        });
        backButton.setPosition(20f, 20f);
        this.stage.addActor(backButton);

        refresh();
    }

    private void buildHud() {
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
        if (this.observer == null) {
            this.statusLabel.setText("Shop is not available.");
            return;
        }
        ShopCatalogResult catalog = this.observer.onShopListRequested();
        DailyOfferResult daily = this.observer.onDailyOfferRequested();
        if (catalog == null) {
            this.statusLabel.setText("Shop is not available.");
            return;
        }

        this.coinsLabel.setText(String.valueOf(catalog.getCoins()));
        this.diamondsLabel.setText(String.valueOf(catalog.getDiamonds()));

        this.itemRows.clearChildren();
        if (daily != null && daily.isAvailable()) {
            this.itemRows.add(buildDailyOfferRow(daily)).growX().padBottom(12f).row();
        }
        for (ShopItemState item : catalog.getItems()) {
            this.itemRows.add(buildPermanentItemRow(item)).growX().padBottom(12f).row();
        }
    }

    private Table buildRowFrame() {
        Table row = new Table();
        row.setBackground(this.skin.newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.35f)));
        row.pad(10f);
        return row;
    }

    private Table buildDailyOfferRow(DailyOfferResult daily) {
        Table row = buildRowFrame();

        Table info = new Table();
        info.left();
        Label title = new Label("Daily Offer - " + daily.getSeedPacketAmount() + " " + daily.getPlantName() + " seed packets", this.skin, "secondary");
        info.add(title).left().row();
        String priceText = daily.getBasePrice() + " -> " + daily.getFinalPrice() + " " + currencyLabel(daily.getCurrency())
            + " (" + daily.getDiscountPercent() + "% off)";
        Label price = new Label(priceText, this.skin, "default");
        info.add(price).left().padTop(4f).row();
        row.add(info).growX().left();

        if (daily.isPurchased()) {
            row.add(new Label("Purchased", this.skin, "secondary")).size(120f, 36f).padLeft(12f);
        } else if (!daily.isPurchasable()) {
            row.add(new Label("Unavailable", this.skin, "secondary")).size(120f, 36f).padLeft(12f);
        } else {
            TextButton buyButton = new TextButton("BUY", this.skin, "green");
            buyButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showConfirmDialog(
                        "Buy " + daily.getSeedPacketAmount() + " " + daily.getPlantName() + " seed packets for "
                            + daily.getFinalPrice() + " " + currencyLabel(daily.getCurrency()) + "?",
                        () -> purchase(daily.getOfferId(), null)
                    );
                }
            });
            row.add(buyButton).size(120f, 36f).padLeft(12f);
        }

        return row;
    }

    private Table buildPermanentItemRow(ShopItemState item) {
        Table row = buildRowFrame();

        Table info = new Table();
        info.left();
        Label title = new Label(capitalizeWords(item.getItemName()), this.skin, "secondary");
        info.add(title).left().row();
        Label description = new Label(item.getDescription(), this.skin, "default");
        description.setWrap(true);
        info.add(description).left().width(500f).padTop(2f).row();
        Label price = new Label(item.getPrice() + " " + currencyLabel(item.getCurrency()), this.skin, "default");
        info.add(price).left().padTop(4f).row();
        row.add(info).growX().left();

        TextButton buyButton = new TextButton("BUY", this.skin, "green");
        boolean isSelectedSeedPacket = PermanentStuff.SELECTED_SEED_PACKET.getItemId().equalsIgnoreCase(item.getItemId());
        buyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isSelectedSeedPacket) {
                    showPlantPicker(item);
                } else {
                    showConfirmDialog(
                        "Buy " + capitalizeWords(item.getItemName()) + " for " + item.getPrice() + " "
                            + currencyLabel(item.getCurrency()) + "?",
                        () -> purchase(item.getItemId(), null)
                    );
                }
            }
        });
        row.add(buyButton).size(120f, 36f).padLeft(12f);

        return row;
    }

    private String currencyLabel(CurrencyType currency) {
        if (currency == null) {
            return "";
        }
        String name = currency.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String capitalizeWords(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String[] parts = value.split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private void showPlantPicker(ShopItemState item) {
        User user = this.controller.getCurrentUser();
        List<Plant> unlockedPlants = user == null ? List.of() : user.getUnlockedPlants();

        Table content = new Table();
        content.setTouchable(Touchable.enabled);
        content.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(24f);
        content.add(new Label("Choose a plant", this.skin, "medium")).padBottom(12f).row();

        if (unlockedPlants == null || unlockedPlants.isEmpty()) {
            content.add(new Label("No unlocked plants available.", this.skin, "secondary")).padBottom(12f).row();
        } else {
            Table plantList = new Table();
            for (Plant plant : unlockedPlants) {
                if (plant == null || plant.getName() == null || plant.getName().trim().isEmpty()) {
                    continue;
                }
                String plantName = plant.getName();
                TextButton plantButton = new TextButton(plantName, this.skin, "purple");
                plantButton.getLabel().setFontScale(0.8f);
                plantButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        closeModal();
                        showConfirmDialog(
                            "Buy " + item.getPurchaseAmount() + " " + plantName + " seed packets for "
                                + item.getPrice() + " " + currencyLabel(item.getCurrency()) + "?",
                            () -> purchase(item.getItemId(), plantName)
                        );
                    }
                });
                plantList.add(plantButton).size(200f, 40f).pad(4f).row();
            }
            ScrollPane plantScroll = new ScrollPane(plantList, this.skin);
            plantScroll.setFadeScrollBars(false);
            content.add(plantScroll).height(240f).padBottom(12f).row();
        }

        TextButton cancelButton = new TextButton("Cancel", this.skin, "brown");
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeModal();
            }
        });
        content.add(cancelButton).size(140f, 44f);

        showModal(content, 480f);
    }

    private void showConfirmDialog(String message, Runnable onConfirm) {
        Table content = new Table();
        content.setTouchable(Touchable.enabled);
        content.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(30f);
        Label messageLabel = new Label(message, this.skin, "medium");
        messageLabel.setWrap(true);
        messageLabel.setAlignment(Align.center);
        content.add(messageLabel).width(360f).padBottom(20f).row();

        Table buttons = new Table();
        TextButton confirmButton = new TextButton("Confirm", this.skin, "green");
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeModal();
                onConfirm.run();
            }
        });
        TextButton cancelButton = new TextButton("Cancel", this.skin, "brown");
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeModal();
            }
        });
        buttons.add(confirmButton).size(140f, 44f).padRight(12f);
        buttons.add(cancelButton).size(140f, 44f);
        content.add(buttons);

        showModal(content, 460f);
    }

    private void purchase(String itemId, String plantType) {
        if (this.observer == null) {
            return;
        }
        ShopActionResult result = this.observer.onBuyRequested(itemId, 1, plantType);
        if (result != null && result.isSuccessful()) {
            GameplaySoundPlayer.shared().play(GameplaySoundPlayer.Effect.COIN);
        }
        this.controller.save();
        showResultPopup(result);
        refresh();
    }

    private void showResultPopup(ShopActionResult result) {
        Table content = new Table();
        content.setTouchable(Touchable.enabled);
        content.setBackground(this.skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        content.pad(30f);

        if (result == null) {
            content.add(new Label("Shop purchase failed.", this.skin, "big_outline")).padBottom(16f).row();
        } else if (result.isSuccessful()) {
            content.add(new Label("PURCHASE SUCCESSFUL!", this.skin, "big_outline")).padBottom(16f).row();
            content.add(new Label(result.getMessage(), this.skin, "medium")).padBottom(10f).row();
            if (result.getSeedPacketsReceived() > 0) {
                content.add(new Label(
                    result.getSeedPacketsReceived() + " seed packet(s) for " + result.getPlantName(),
                    this.skin, "secondary"
                )).padBottom(6f).row();
            }
            if (result.getPotsUnlocked() > 0) {
                content.add(new Label(result.getPotsUnlocked() + " greenhouse slot(s) unlocked", this.skin, "secondary"))
                    .padBottom(6f).row();
            }
            if (result.getPlantFoodReceived() > 0) {
                content.add(new Label(result.getPlantFoodReceived() + " plant food stored", this.skin, "secondary"))
                    .padBottom(6f).row();
            }
            if (result.getCoinsReceived() > 0) {
                content.add(new Label("+" + result.getCoinsReceived() + " coins", this.skin, "secondary"))
                    .padBottom(6f).row();
            }
        } else {
            content.add(new Label("PURCHASE FAILED", this.skin, "big_outline")).padBottom(16f).row();
            content.add(new Label(result.getMessage(), this.skin, "medium")).padBottom(10f).row();
        }

        TextButton okButton = new TextButton("OK", this.skin, "green");
        okButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeModal();
            }
        });
        content.add(okButton).size(140f, 48f).padTop(10f);

        showModal(content, 480f);
    }

    private Table activeModal;

    private void showModal(Table content, float width) {
        closeModal();

        Table backdrop = new Table();
        backdrop.setFillParent(true);
        backdrop.setTouchable(Touchable.enabled);
        backdrop.setBackground(this.skin.getDrawable("modal_background"));

        Table frame = new Table();
        frame.setBackground(this.skin.getDrawable("image_ui_dialog_asset_dialogborder_10"));
        frame.pad(16f);
        frame.add(content).grow();

        Table wrapper = new Table();
        wrapper.setFillParent(true);
        wrapper.add(frame).width(width);
        backdrop.addActor(wrapper);

        this.activeModal = backdrop;
        this.stage.addActor(backdrop);
    }

    private void closeModal() {
        if (this.activeModal != null) {
            this.activeModal.remove();
            this.activeModal = null;
        }
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
