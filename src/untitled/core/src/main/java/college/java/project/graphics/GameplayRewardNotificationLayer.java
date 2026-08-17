package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.mechanism.Loot;
import model.mechanism.LootType;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Shows required temporary notifications for auto-collected plant food and loot. */
public final class GameplayRewardNotificationLayer extends Group {
    private static final float NOTICE_WIDTH = 330f;
    private static final float NOTICE_HEIGHT = 62f;
    private static final String COIN_ICON =
            "IMAGE_EFFECTS_COIN_GOLD_COIN_GOLD_90X90";
    private static final String GEM_ICON =
            "IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_131X136";
    private static final String PLANT_FOOD_ICON =
            "IMAGE_EFFECTS_PLANTFOOD_PICKUP_PLANTFOOD_PICKUP_79X79";

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final Set<Loot> observedLoot = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean initialized;
    private int previousPlantFood;

    public GameplayRewardNotificationLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayRewardNotificationLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        if (!this.initialized) {
            initializeBaseline();
            return;
        }
        detectPlantFood();
        detectLoot();
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void initializeBaseline() {
        this.previousPlantFood = Math.max(0, this.dataSource.getPlantFoodAmount());
        List<Loot> history = this.dataSource.getLootHistory();
        if (history != null) {
            this.observedLoot.addAll(history);
        }
        this.initialized = true;
    }

    private void detectPlantFood() {
        int current = Math.max(0, this.dataSource.getPlantFoodAmount());
        if (current > this.previousPlantFood) {
            int gained = current - this.previousPlantFood;
            showNotice("PLANT FOOD  +" + gained, PLANT_FOOD_ICON, NoticeKind.PLANT_FOOD);
        }
        this.previousPlantFood = current;
    }

    private void detectLoot() {
        List<Loot> history = this.dataSource.getLootHistory();
        if (history == null) {
            return;
        }
        for (Loot loot : history) {
            if (loot == null || !loot.isCollected() || !this.observedLoot.add(loot)) {
                continue;
            }
            showLootNotice(loot);
        }
    }

    private void showLootNotice(Loot loot) {
        LootType type = loot.getType();
        int amount = Math.max(0, loot.getAmount());
        if (type == LootType.COIN) {
            showNotice("COINS  +" + amount, COIN_ICON, NoticeKind.COIN);
        } else if (type == LootType.DIAMOND) {
            showNotice("GEM  +" + amount, GEM_ICON, NoticeKind.GEM);
        } else if (type == LootType.POT) {
            showNotice("GREENHOUSE POT  +" + amount, null, NoticeKind.POT);
        }
    }

    private void showNotice(String text, String iconResourceId, NoticeKind kind) {
        Table notice = createNotice(text, iconResourceId, kind);
        float x = Math.max(20f, getWidth() - NOTICE_WIDTH - 34f);
        float y = Math.max(30f, getHeight() - 300f - activeNoticeOffset());
        notice.setBounds(x, y, NOTICE_WIDTH, NOTICE_HEIGHT);
        notice.getColor().a = 0f;
        addActor(notice);
        notice.addAction(Actions.sequence(
                Actions.parallel(Actions.fadeIn(0.12f), Actions.moveBy(0f, 18f, 0.20f)),
                Actions.delay(1.25f),
                Actions.parallel(Actions.fadeOut(0.45f), Actions.moveBy(0f, 42f, 0.45f)),
                Actions.removeActor()
        ));
    }

    private float activeNoticeOffset() {
        int visible = 0;
        for (Actor child : getChildren()) {
            if (child != null) {
                visible++;
            }
        }
        return Math.min(visible, 4) * (NOTICE_HEIGHT + 8f);
    }

    private Table createNotice(String text, String iconResourceId, NoticeKind kind) {
        Table table = new Table();
        table.setTouchable(Touchable.disabled);
        table.setBackground(PvzSkin.get().newDrawable("white_pixel", kind.background));
        Image icon = optionalIcon(iconResourceId);
        if (icon != null) {
            table.add(icon).size(48f).padLeft(10f).padRight(8f);
        }
        Label label = new Label(text, PvzSkin.get(), "medium_outline");
        label.setAlignment(Align.left);
        label.setFontScale(0.78f);
        table.add(label).growX().left().padRight(12f);
        return table;
    }

    private Image optionalIcon(String resourceId) {
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
        if (resourceId == null) {
            return null;
        }
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // Text-only fallback still satisfies the required notification.
        }
        return null;
    }

    private enum NoticeKind {
        COIN(new Color(0.35f, 0.24f, 0.05f, 0.92f)),
        GEM(new Color(0.05f, 0.27f, 0.48f, 0.92f)),
        POT(new Color(0.22f, 0.36f, 0.13f, 0.92f)),
        PLANT_FOOD(new Color(0.23f, 0.46f, 0.08f, 0.94f));

        private final Color background;

        NoticeKind(Color background) {
            this.background = background;
        }
    }
}
