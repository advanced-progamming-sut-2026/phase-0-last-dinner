package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** PvZ2-style shovel and Plant Food controls arranged around the gameplay lawn. */
public final class GameplayInteractionHud extends Group {
    private static final String SHOVEL_UP = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
    private static final String SHOVEL_DOWN = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON_DOWN";
    private static final String FOOD_UP = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
    private static final String FOOD_DOWN = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON_DOWN";
    private static final String FOOD_BANK = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BANK";
    private static final String FOOD_SLOT = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BANK_FILLED_SLOT";

    private final GameplaySeedBankDataSource dataSource;
    private final GameplaySeedBank seedBank;
    private final GameplayBoardInteractionLayer interactionLayer;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final ImageButton shovelButton;
    private final ImageButton foodButton;
    private final Table debugControls;
    private final List<Image> foodSlots = new ArrayList<>();

    public GameplayInteractionHud(
            GameplaySeedBankDataSource dataSource,
            GameplaySeedBank seedBank,
            GameplayBoardInteractionLayer interactionLayer
    ) {
        this(dataSource, seedBank, interactionLayer, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayInteractionHud(
            GameplaySeedBankDataSource dataSource,
            GameplaySeedBank seedBank,
            GameplayBoardInteractionLayer interactionLayer,
            GameAssetManager assets
    ) {
        if (dataSource == null || seedBank == null || interactionLayer == null || assets == null) {
            throw new IllegalArgumentException("Gameplay interaction HUD dependencies are required");
        }
        this.dataSource = dataSource;
        this.seedBank = seedBank;
        this.interactionLayer = interactionLayer;
        this.assets = assets;
        this.shovelButton = createButton(SHOVEL_UP, SHOVEL_DOWN);
        this.foodButton = createButton(FOOD_UP, FOOD_DOWN);
        this.debugControls = createDebugControls();
        setTouchable(Touchable.childrenOnly);
        build();
        installListeners();
        refresh();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        refresh();
    }

    public void refresh() {
        GameplayInteractionMode mode = this.interactionLayer.getMode();
        this.shovelButton.setChecked(mode == GameplayInteractionMode.SHOVEL);
        this.foodButton.setChecked(mode == GameplayInteractionMode.PLANT_FOOD);
        int count = Math.max(0, this.dataSource.getPlantFoodCount());
        boolean disabled = count <= 0;
        this.foodButton.setDisabled(disabled);
        this.foodButton.setColor(disabled ? new Color(0.55f, 0.55f, 0.55f, 0.85f) : Color.WHITE);
        for (int i = 0; i < this.foodSlots.size(); i++) {
            this.foodSlots.get(i).setVisible(i < count);
        }
        this.debugControls.setVisible(this.dataSource.isDebugModeEnabled());
    }

    public void updateAssets() {
        this.assets.update();
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void build() {
        // Plant Food sits at the lower-left in the original gameplay HUD.
        Image bank = resourceImage(FOOD_BANK);
        if (bank != null) {
            bank.setBounds(265f, 30f, 320f, 137f);
            addActor(bank);
        }

        this.foodButton.setBounds(292f, 55f, 82f, 87f);
        addActor(this.foodButton);

        Drawable slotDrawable = resourceDrawable(FOOD_SLOT);
        if (slotDrawable != null) {
            // The original 768px bank is 206x88. Its five sockets start at x=74
            // and repeat every 24px; the filled-slot artwork is exactly 25x25.
            // Project gameplay stores at most three Plant Foods, so we place the
            // three filled sprites over the first three sockets using the same
            // source-space geometry instead of hand-tuned screen offsets.
            float bankScaleX = 320f / 206f;
            float bankScaleY = 137f / 88f;
            float slotWidth = 25f * bankScaleX;
            float slotHeight = 25f * bankScaleY;
            float firstSlotX = 265f + 74f * bankScaleX;
            float slotY = 30f + 32f * bankScaleY;
            float slotStep = 24f * bankScaleX;
            for (int i = 0; i < 3; i++) {
                Image slot = new Image(slotDrawable);
                slot.setScaling(Scaling.fit);
                slot.setTouchable(Touchable.disabled);
                slot.setBounds(firstSlotX + i * slotStep, slotY, slotWidth, slotHeight);
                this.foodSlots.add(slot);
                addActor(slot);
            }
        }

        // The shovel is a spatial lawn control, so it belongs at the lower-right.
        this.shovelButton.setBounds(1745f, 41f, 124f, 124f);
        addActor(this.shovelButton);

        this.debugControls.setBounds(1668f, 47f, 86f, 96f);
        addActor(this.debugControls);
    }

    private void installListeners() {
        this.shovelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CollectionUiAnimator.playClickPulse(shovelButton);
                boolean wasActive = interactionLayer.getMode() == GameplayInteractionMode.SHOVEL;
                if (!wasActive) {
                    seedBank.clearSelection();
                }
                boolean active = interactionLayer.activateShovel();
                seedBank.showInteractionStatus(active
                        ? "Shovel: choose a planted tile."
                        : "Shovel cancelled.");
                refresh();
            }
        });
        this.foodButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CollectionUiAnimator.playClickPulse(foodButton);
                if (foodButton.isDisabled()) {
                    seedBank.showInteractionStatus("No Plant Food available.");
                    return;
                }
                boolean wasActive = interactionLayer.getMode() == GameplayInteractionMode.PLANT_FOOD;
                if (!wasActive) {
                    seedBank.clearSelection();
                }
                boolean active = interactionLayer.activatePlantFood();
                seedBank.showInteractionStatus(active
                        ? "Plant Food: choose an eligible plant."
                        : "Plant Food cancelled.");
                refresh();
            }
        });
        CollectionUiAnimator.installHoverScale(this.shovelButton);
        CollectionUiAnimator.installHoverScale(this.foodButton);
    }

    private Table createDebugControls() {
        Table controls = new Table();
        controls.setVisible(false);
        controls.add(debugButton("+SUN", () -> {
            if (this.dataSource.isDebugModeEnabled()) {
                this.dataSource.cheatAddSun(250);
                this.seedBank.refresh();
            }
        })).size(82f, 38f).row();
        controls.add(debugButton("+PF", () -> {
            if (this.dataSource.isDebugModeEnabled()) {
                this.dataSource.cheatAddPlantFood();
                this.seedBank.refresh();
            }
        })).size(82f, 38f).padTop(4f);
        return controls;
    }

    private TextButton debugButton(String text, Runnable action) {
        TextButton button = new TextButton(text, PvzSkin.get(), "brown");
        button.getLabel().setFontScale(0.45f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
                refresh();
            }
        });
        CollectionUiAnimator.installHoverScale(button);
        return button;
    }

    private ImageButton createButton(String upId, String downId) {
        Drawable up = resourceDrawable(upId);
        Drawable down = resourceDrawable(downId);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = up;
        style.imageDown = down == null ? up : down;
        style.imageChecked = down == null ? up : down;
        style.imageDisabled = up;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        return button;
    }

    private Image resourceImage(String resourceId) {
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return null;
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // Fallback keeps the HUD alive if an optional atlas is missing.
        }
        String normalized = resourceId.toLowerCase(Locale.ROOT);
        if (PvzSkin.get().has(normalized, Drawable.class)) {
            return PvzSkin.get().getDrawable(normalized);
        }
        return null;
    }
}
