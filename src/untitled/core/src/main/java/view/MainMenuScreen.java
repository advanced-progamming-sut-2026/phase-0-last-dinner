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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import controller.ApplicationController;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
public class MainMenuScreen implements Screen {

    public interface Navigator {
        void openGameMenu();

        boolean openMultiplayer();

        boolean hasPendingMultiplayerInvitation();

        void openSettingsMenu();

        void openNewsMenu();

        void openProfileMenu();

        void openMeowPoint();

        void onLoggedOut();
    }

    private static final String PROFILE_ICON_PATH = "Assets/Exports/profile.png";
    private static final String SETTINGS_ICON_PATH = "Assets/Exports/settings.png";
    private static final String NEWS_ICON_PATH = "Assets/Exports/news.png";
    private static final String MEOW_POINT_ICON_PATH = "Assets/Exports/QuestIcons_LOTD.png";
    private static final String BACKGROUND_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_MAINMENU_BACKGROUND_768_00/mainmenu_background.png";
    private static final String LOGO_PATH =
        "Assets/Exports/ATLASIMAGE_ATLAS_UI_MAINMENULOGO_768_00/pvz2_logo_horizontal.png";
    private static final float ICON_HEIGHT = 72f;
    private static final float LOGO_WIDTH = 420f;
    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private final ApplicationController controller;
    private final Navigator navigator;
    private final List<Texture> loadedTextures = new ArrayList<>();
    private Stage stage;
    private Label statusLabel;

    private boolean openingMultiplayer;

    public MainMenuScreen(ApplicationController controller, Navigator navigator) {
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

        Image background = this.createImageFill(BACKGROUND_PATH);
        this.stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        this.stage.addActor(root);

        ImageButton profileButton = this.createIconButton(PROFILE_ICON_PATH, ICON_HEIGHT);
        ImageButton settingsButton = this.createIconButton(SETTINGS_ICON_PATH, ICON_HEIGHT);
        ImageButton meowPointButton = this.createIconButton(MEOW_POINT_ICON_PATH, ICON_HEIGHT);
        this.attachCommand(profileButton, "menu enter profile", this.navigator::openProfileMenu);
        this.attachCommand(settingsButton, "menu enter settings", this.navigator::openSettingsMenu);
        meowPointButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.openMeowPoint();
            }
        });

        Table topLeftGroup = new Table();
        topLeftGroup.add(profileButton).padRight(12);
        topLeftGroup.add(settingsButton).padRight(12);
        topLeftGroup.add(meowPointButton);

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

        int unreadNewsCount = this.controller.getOrCreateNewsController().getUnreadNewsCount();

        Actor newsIcon = this.createNewsIcon(unreadNewsCount);

        Image logo = this.createImageAspect(LOGO_PATH, LOGO_WIDTH);

        this.statusLabel = new Label(unreadNewsCount > 0 ? "You have unread news!" : "", skin, "secondary");

        TextButton playButton = this.menuButton("Play", skin, "green", "menu enter game",
            this.navigator::openGameMenu);

        TextButton multiplayerButton = new TextButton("Multiplayer", skin, "purple");

        multiplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean opened = navigator.openMultiplayer();

                if (!opened)
                    statusLabel.setText("Multiplayer is not connected to the server yet.");
            }
        });

        Table centerGroup = new Table();
        centerGroup.add(playButton).width(280).height(70).padTop(32).padBottom(12).row();
        centerGroup.add(multiplayerButton).width(280).height(70).padBottom(12).row();
        centerGroup.add(this.statusLabel).padTop(4);

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

    private Actor createNewsIcon(int unreadNewsCount) {
        ImageButton newsButton = this.createIconButton(NEWS_ICON_PATH, ICON_HEIGHT);
        newsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openNewsDialog();
            }
        });
        if (unreadNewsCount <= 0) {
            return newsButton;
        }

        float badgeSize = 22f;
        Image badgeCircle = new Image(new TextureRegionDrawable(new TextureRegion(this.createBadgeTexture((int) badgeSize))));
        Label countLabel = new Label(unreadNewsCount > 9 ? "9+" : String.valueOf(unreadNewsCount), PvzSkin.get(), "default");
        countLabel.setColor(Color.WHITE);
        countLabel.setFontScale(0.75f);
        countLabel.setAlignment(Align.center);

        Stack badge = new Stack(badgeCircle, countLabel);
        Container<Stack> badgeContainer = new Container<>(badge);
        badgeContainer.size(badgeSize, badgeSize);
        badgeContainer.align(Align.topRight);

        Stack newsWithBadge = new Stack();
        newsWithBadge.add(newsButton);
        newsWithBadge.add(badgeContainer);
        return newsWithBadge;
    }

    private void openNewsDialog() {
        new NewsDialog(this.controller, PvzSkin.get()).open(this.stage);
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
        if (!this.openingMultiplayer && this.navigator.hasPendingMultiplayerInvitation())
            this.openingMultiplayer = this.navigator.openMultiplayer();

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
