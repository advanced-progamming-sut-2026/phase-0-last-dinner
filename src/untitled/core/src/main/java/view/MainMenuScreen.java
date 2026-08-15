package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.ApplicationController;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
public class MainMenuScreen implements Screen {

    public interface Navigator {
        void openGameMenu();

        void openSettingsMenu();

        void openNewsMenu();

        void openProfileMenu();

        void onLoggedOut();
    }

    private static final String PROFILE_ICON_PATH = "Assets/Exports/profile.png";
    private static final String SETTINGS_ICON_PATH = "Assets/Exports/settings.png";
    private static final String NEWS_ICON_PATH = "Assets/Exports/news.png";
    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String LOGO_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_UI_MAINMENULOGO_768_00/pvz2_logo_horizontal.png";
    private static final float ICON_HEIGHT = 72f;
    private static final float LOGO_WIDTH = 420f;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();
    private Stage stage;
    private Label statusLabel;

    public MainMenuScreen(ApplicationController controller, Navigator navigator) {
        if (controller == null || navigator == null) {
            throw new IllegalArgumentException("Controller and navigator are required");
        }
        this.controller = controller;
        this.navigator = navigator;
    }

    @Override
    public void show() {
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(this.stage);
        Skin skin = PvzSkin.get();

        Image background = this.createImageFill(BACKGROUND_PATH);
        this.stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        this.stage.addActor(root);

        // Top-left corner: profile + settings icons side by side.
        ImageButton profileButton = this.createIconButton(PROFILE_ICON_PATH, ICON_HEIGHT);
        ImageButton settingsButton = this.createIconButton(SETTINGS_ICON_PATH, ICON_HEIGHT);
        this.attachCommand(profileButton, "menu enter profile", this.navigator::openProfileMenu);
        this.attachCommand(settingsButton, "menu enter settings", this.navigator::openSettingsMenu);

        Table topLeftGroup = new Table();
        topLeftGroup.add(profileButton).padRight(12);
        topLeftGroup.add(settingsButton);

        TextButton logoutButton = new TextButton("Logout", skin, "brown");
        logoutButton.getLabel().setFontScale(0.8f);
        logoutButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String result = controller.execute("menu logout");
                statusLabel.setText(result);
                if ("Logout successful".equals(result)) {
                    navigator.onLoggedOut();
                }
            }
        });

        // Checked early so we know whether to attach the unread-news badge below.
        String menuStatus = this.controller.execute("menu show current");
        boolean hasUnreadNews = menuStatus != null && menuStatus.contains("[new news]");
        Actor newsIcon = this.createNewsIcon(hasUnreadNews);
        Image logo = this.createImageAspect(LOGO_PATH, LOGO_WIDTH);

        this.statusLabel = new Label(hasUnreadNews ? "You have unread news!" : "", skin, "secondary");
        TextButton playButton = this.menuButton("Play", skin, "green", "menu enter game", this.navigator::openGameMenu);

        Table centerGroup = new Table();
        centerGroup.add(playButton).width(260).height(70).padTop(48).padBottom(16).row();
        centerGroup.add(this.statusLabel).padTop(8);

        root.top().left();
        root.add(topLeftGroup).top().left();
        root.add().expandX();
        root.add(logoutButton).top().right().size(110, 44);
        root.row();
        root.add(logo).colspan(3).center().padTop(4);
        root.row();
        root.add(centerGroup).colspan(3).expand().center();
        root.row();
        root.add().colspan(2).expandX();
        root.add(newsIcon).bottom().right();
    }

    private Image createImageFill(String assetPath) {
        Texture texture = this.loadTexture(assetPath);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setScaling(Scaling.fill);
        image.setFillParent(true);
        return image;
    }

    private Image createImageAspect(String assetPath, float targetWidth) {
        Texture texture = this.loadTexture(assetPath);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        float aspect = (float) texture.getWidth() / (float) texture.getHeight();
        image.setSize(targetWidth, targetWidth / aspect);
        return image;
    }

    private ImageButton createIconButton(String assetPath, float targetHeight) {
        Texture texture = this.loadTexture(assetPath);
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton button = new ImageButton(drawable);
        float aspect = (float) texture.getWidth() / (float) texture.getHeight();
        button.getImageCell().size(targetHeight * aspect, targetHeight);
        return button;
    }

    private Texture loadTexture(String assetPath) {
        Texture texture = new Texture(Gdx.files.internal(assetPath));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.loadedTextures.add(texture);
        return texture;
    }

    private void attachCommand(Actor actor, String command, Runnable onSuccess) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String result = controller.execute(command);
                if (result != null && !result.isEmpty()) {
                    statusLabel.setText(result);
                }
                onSuccess.run();
            }
        });
    }

    private TextButton menuButton(String text, Skin skin, String style, String command, Runnable onSuccess) {
        TextButton button = new TextButton(text, skin, style);
        this.attachCommand(button, command, onSuccess);
        return button;
    }

    private Actor createNewsIcon(boolean hasUnreadNews) {
        ImageButton newsButton = this.createIconButton(NEWS_ICON_PATH, ICON_HEIGHT);
        this.attachCommand(newsButton, "menu enter news", this.navigator::openNewsMenu);
        if (!hasUnreadNews) {
            return newsButton;
        }

        float badgeSize = 22f;
        Image badgeCircle = new Image(new TextureRegionDrawable(new TextureRegion(this.createBadgeTexture((int) badgeSize))));
        Label exclamationMark = new Label("!", PvzSkin.get(), "default");
        exclamationMark.setColor(Color.WHITE);
        exclamationMark.setFontScale(0.85f);
        exclamationMark.setAlignment(Align.center);

        Stack badge = new Stack(badgeCircle, exclamationMark);
        Container<Stack> badgeContainer = new Container<>(badge);
        badgeContainer.size(badgeSize, badgeSize);
        badgeContainer.align(Align.topRight);

        Stack newsWithBadge = new Stack();
        newsWithBadge.add(newsButton);
        newsWithBadge.add(badgeContainer);
        return newsWithBadge;
    }

    private Texture createBadgeTexture(int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fillCircle(diameter / 2, diameter / 2, (int)(diameter / 2f));
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
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
