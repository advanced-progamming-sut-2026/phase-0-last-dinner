package view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.ApplicationController;
import controller.NewsController;
import java.util.List;

public class NewsDialog extends Table {
    private static final String PANEL_BACKGROUND = "image_ui_dialog_asset_inner_bkgd_10";
    private enum Tab {
        UNREAD,
        ALL
    }

    private final ApplicationController controller;
    private final Skin skin;
    private final Table listTable = new Table();
    private final TextButton unreadTab;
    private final TextButton allTab;
    private Tab activeTab = Tab.UNREAD;
    private Image blocker;
    private Texture blockerTexture;

    public NewsDialog(ApplicationController controller, Skin skin) {
        super(skin);
        if (controller == null || skin == null) {
            throw new IllegalArgumentException("Controller and skin are required");
        }
        this.controller = controller;
        this.skin = skin;

        this.setBackground(skin.getDrawable(PANEL_BACKGROUND));
        this.pad(20);

        Label title = new Label("News", skin, "big");

        this.unreadTab = new TextButton("Unread", skin, "green");
        this.allTab = new TextButton("All", skin, "brown");
        this.unreadTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showTab(Tab.UNREAD);
            }
        });
        this.allTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showTab(Tab.ALL);
            }
        });

        Table tabsRow = new Table();
        tabsRow.add(this.unreadTab).width(120).height(40).padRight(8);
        tabsRow.add(this.allTab).width(120).height(40);

        ScrollPane scrollPane = new ScrollPane(this.listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        TextButton closeButton = new TextButton("Close", skin, "brown");
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                close();
            }
        });

        this.add(title).padBottom(12).row();
        this.add(tabsRow).padBottom(12).row();
        this.add(scrollPane).width(420).height(320).row();
        this.add(closeButton).width(140).height(44).padTop(16);
    }
    public void open(Stage stage) {
        this.blockerTexture = this.createSolidTexture(Color.BLACK);
        this.blocker = new Image(new TextureRegionDrawable(new TextureRegion(this.blockerTexture)));
        this.blocker.setColor(0f, 0f, 0f, 0.55f);
        this.blocker.setSize(stage.getWidth(), stage.getHeight());
        this.blocker.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                close();
            }
        });
        stage.addActor(this.blocker);

        this.showTab(Tab.UNREAD);
        stage.addActor(this);
        this.pack();
        this.setPosition(
            Math.round((stage.getWidth() - this.getWidth()) / 2f),
            Math.round((stage.getHeight() - this.getHeight()) / 2f));
    }

    private void close() {
        this.remove();
        if (this.blocker != null) {
            this.blocker.remove();
        }
        if (this.blockerTexture != null) {
            this.blockerTexture.dispose();
            this.blockerTexture = null;
        }
    }

    private Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void showTab(Tab tab) {
        this.activeTab = tab;
        this.refreshTabStyles();
        NewsController newsController = this.controller.getOrCreateNewsController();
        List<String> news = tab == Tab.UNREAD
            ? newsController.showUnreadNews()
            : newsController.showAllNews();
        this.populate(news);
    }

    private void refreshTabStyles() {
        this.unreadTab.setStyle(this.skin.get(
            this.activeTab == Tab.UNREAD ? "green" : "brown", TextButton.TextButtonStyle.class));
        this.allTab.setStyle(this.skin.get(
            this.activeTab == Tab.ALL ? "green" : "brown", TextButton.TextButtonStyle.class));
    }

    private void populate(List<String> news) {
        this.listTable.clear();
        if (news == null || news.isEmpty()) {
            Label empty = new Label("No news was found.", this.skin, "secondary");
            empty.setColor(Color.BLACK);
            empty.setFontScale(1.15f);
            this.listTable.add(empty).pad(16);
        } else {
            for (String item : news) {
                Label label = new Label(item, this.skin, "default");
                label.setWrap(true);
                label.setColor(Color.BLACK);
                label.setFontScale(1.15f);
                this.listTable.add(label).width(400).padBottom(10).left().row();
            }
        }
        this.pack();
    }
}
