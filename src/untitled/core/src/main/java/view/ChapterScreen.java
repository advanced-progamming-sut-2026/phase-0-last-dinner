package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import model.level.LevelType;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

/**
 * Level-select screen for a chapter (Normal + the one special level currently defined for
 * it). The visuals (nameplate boxes, damage/variable icons, lock overlay) are ours, but
 * what actually happens when an unlocked level is clicked is delegated to
 * {@code AdventureLevelSelectionScreen.openLevel(chapter, level)} - the teammate's already
 * working implementation that enters the chapter, selects the level, and routes to either
 * the plant-pick screen or straight into gameplay for CONVEYOR_BELT-type specials. We don't
 * re-derive any of that here to avoid diverging from a path that's already tested.
 */
public class ChapterScreen implements Screen {

    public interface Navigator {
        void onBack();
    }

    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String NAMEPLATE_DRAWABLE = "image_ui_if_bundle_reward_multiplier_bg_10";
    private static final String NORMAL_ICON_PATH = "Assets/Exports/damage_icon.png";
    private static final String SPECIAL_ICON_PATH = "Assets/Exports/variable_icon.png";
    private static final String LOCK_ICON_PATH = "Assets/Exports/perk_icon_locked.png";

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final float NAMEPLATE_WIDTH = 340f;
    private static final float NAMEPLATE_HEIGHT = 84f;
    private static final float LEVEL_ICON_SIZE = 64f;
    private static final float LOCK_ICON_SIZE = 56f;

    private final ApplicationController controller;
    private final ChapterType chapter;
    private final Navigator navigator;
    private final BiPredicate<ChapterType, LevelType> levelSelector;
    private final List<Texture> loadedTextures = new ArrayList<>();

    private Stage stage;
    private Label statusLabel;

    public ChapterScreen(
        ApplicationController controller,
        ChapterType chapter,
        BiPredicate<ChapterType, LevelType> levelSelector,
        Navigator navigator
    ) {
        if (controller == null || chapter == null || levelSelector == null || navigator == null) {
            throw new IllegalArgumentException("Controller, chapter, levelSelector and navigator are required");
        }
        this.controller = controller;
        this.chapter = chapter;
        this.levelSelector = levelSelector;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new Stage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(this.stage);
        Skin skin = PvzSkin.get();

        this.stage.addActor(this.createImageFill(BACKGROUND_PATH));

        this.statusLabel = new Label("", skin, "secondary");

        TextButton backButton = new TextButton("Back", skin, "brown");
        backButton.getLabel().setFontScale(0.8f);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.execute("menu exit");
                navigator.onBack();
            }
        });
        Table backBox = new Table();
        backBox.add(backButton).size(110, 44);

        Label title = new Label(prettyName(this.chapter.name()), skin, "big_outline");

        List<LevelType> levels = this.controller.getChapterController().getAvailableLevels();
        User user = this.controller.getCurrentUser();

        Table levelsColumn = new Table();
        for (LevelType level : levels) {
            if (level == null) {
                continue;
            }
            boolean unlocked = user != null && user.isAdventureLevelUnlocked(this.chapter, level);
            levelsColumn.add(this.createLevelRow(level, unlocked, skin)).padBottom(24).row();
        }

        // Same proven 3-column layout: expand filler is always the middle column, on
        // every row, so left/right columns never fight over expand space.
        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        this.stage.addActor(root);

        root.top();
        root.add(title).colspan(3).padTop(8).row();
        root.add().left();
        root.add().expandX();
        root.add().right();
        root.row();
        root.add(levelsColumn).colspan(3).expand().center();
        root.row();
        root.add(backBox).bottom().left();
        root.add().expandX();
        root.add().right();
        root.row();
        root.add(this.statusLabel).colspan(3).center();
    }

    private Stack createLevelRow(LevelType level, boolean unlocked, Skin skin) {
        boolean isNormal = level == LevelType.NORMAL;
        String iconPath = isNormal ? NORMAL_ICON_PATH : SPECIAL_ICON_PATH;
        String displayName = isNormal ? "Normal" : prettyName(level.name());

        Label nameLabel = new Label(displayName, skin, "big_outline");
        nameLabel.setAlignment(Align.center);
        Table namePlate = new Table();
        namePlate.setBackground(skin.getDrawable(NAMEPLATE_DRAWABLE));
        namePlate.add(nameLabel).padLeft(16).padRight(16).padTop(6).padBottom(10);
        if (!unlocked) {
            namePlate.getColor().set(0.6f, 0.6f, 0.6f, 1f);
        }
        Table namePlateBox = new Table();
        namePlateBox.add(namePlate).size(NAMEPLATE_WIDTH, NAMEPLATE_HEIGHT);

        Image levelIcon = this.createImageFit(iconPath, LEVEL_ICON_SIZE, LEVEL_ICON_SIZE);
        Table iconBox = new Table();
        iconBox.add(levelIcon).size(LEVEL_ICON_SIZE, LEVEL_ICON_SIZE);

        Table row = new Table();
        row.add(namePlateBox).padRight(16);
        row.add(iconBox);

        Stack rowStack = new Stack();
        rowStack.add(row);

        if (!unlocked) {
            Image lockIcon = this.createImageFit(LOCK_ICON_PATH, LOCK_ICON_SIZE, LOCK_ICON_SIZE);
            Table lockBox = new Table();
            lockBox.add(lockIcon).size(LOCK_ICON_SIZE, LOCK_ICON_SIZE);
            Container<Table> lockContainer = new Container<>(lockBox);
            lockContainer.align(Align.center);
            // Only cover the nameplate's own width, not the icon next to it, so the lock
            // visually sits on the box as asked, not floating over the whole row.
            lockContainer.padRight(LEVEL_ICON_SIZE + 16f);
            rowStack.add(lockContainer);
            rowStack.setTouchable(Touchable.disabled);
        } else {
            rowStack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ChapterScreen.this.selectLevel(level);
                }
            });
        }

        return rowStack;
    }

    private void selectLevel(LevelType level) {
        boolean success = this.levelSelector.test(this.chapter, level);
        if (!success) {
            // openLevel() shouldn't normally fail here since we only make unlocked rows
            // clickable, but if something changed underneath us, at least say so - its own
            // internal status label isn't visible to the player since that screen is never
            // actually shown.
            this.statusLabel.setText("Could not start this level. Please try again.");
        }
        // On success, openLevel() has already called setScreen(...) itself (either to the
        // plant-pick screen or straight into gameplay), which disposes this screen too.
    }

    private static String prettyName(String enumName) {
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
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
