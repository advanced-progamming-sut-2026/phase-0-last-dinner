package college.java.project.graphics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.chapters.ChapterType;
import pvz.libpvz.textures.TextureBank;

/** Draws original PvZ2 chapter backgrounds with their widescreen side extensions. */
public final class GameplayBackgroundLayer extends Group {
    private static final int SOURCE_HEIGHT = 768;
    private static final int CENTER_SOURCE_WIDTH = 1024;

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private boolean ownsAssets;
    private final Image leftBackground;
    private final Image centerBackground;
    private final Image rightBackground;
    private ChapterType renderedChapter;
    private int centerSourceWidth = CENTER_SOURCE_WIDTH;

    public GameplayBackgroundLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayBackgroundLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        this.leftBackground = backgroundImage();
        this.centerBackground = backgroundImage();
        this.rightBackground = backgroundImage();
        addActor(this.leftBackground);
        addActor(this.centerBackground);
        addActor(this.rightBackground);
        setTouchable(Touchable.disabled);
        refreshChapter();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        refreshChapter();
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        layoutBackground();
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private Image backgroundImage() {
        Image image = new Image();
        image.setScaling(Scaling.stretch);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private void refreshChapter() {
        ChapterType chapter = this.dataSource.getChapterType();
        if (chapter == null) {
            chapter = ChapterType.ANCIENT_EGYPT;
        }
        if (chapter == this.renderedChapter) {
            return;
        }
        try {
            TextureBank bank = this.assets.getTextureBank();
            String backgroundId = PvzChapterVisuals.backgroundResourceId(chapter);
            TextureRegion center = bank == null ? null : bank.region(backgroundId);
            TextureRegion left = bank == null ? null : bank.region(backgroundId + "_LEFT");
            TextureRegion right = bank == null ? null : bank.region(backgroundId + "_RIGHT");
            if (center == null) {
                return;
            }
            setCenterDrawable(center);
            setSideDrawables(left, right);
            this.renderedChapter = chapter;
            layoutBackground();
        } catch (RuntimeException ignored) {
            // Keep the last valid chapter background if an optional atlas is unavailable.
        }
    }

    private void setCenterDrawable(TextureRegion center) {
        int sourceWidth = Math.min(CENTER_SOURCE_WIDTH, center.getRegionWidth());
        int sourceHeight = Math.min(SOURCE_HEIGHT, center.getRegionHeight());
        TextureRegion cropped = new TextureRegion(center, 0, 0, sourceWidth, sourceHeight);
        this.centerSourceWidth = sourceWidth;
        this.centerBackground.setDrawable(new TextureRegionDrawable(cropped));
    }

    private void setSideDrawables(TextureRegion left, TextureRegion right) {
        if (left == null || right == null) {
            this.leftBackground.setDrawable(null);
            this.rightBackground.setDrawable(null);
            return;
        }
        int sidePixels = sideSourcePixels();
        int leftWidth = Math.min(sidePixels, left.getRegionWidth());
        int rightWidth = Math.min(sidePixels, right.getRegionWidth());
        int leftX = Math.max(0, left.getRegionWidth() - leftWidth);
        int sourceHeight = Math.min(SOURCE_HEIGHT, Math.min(
                left.getRegionHeight(),
                right.getRegionHeight()
        ));
        this.leftBackground.setDrawable(new TextureRegionDrawable(
                new TextureRegion(left, leftX, 0, leftWidth, sourceHeight)
        ));
        this.rightBackground.setDrawable(new TextureRegionDrawable(
                new TextureRegion(right, 0, 0, rightWidth, sourceHeight)
        ));
    }

    private int sideSourcePixels() {
        float scale = GameplayWorldLayout.BACKGROUND_HEIGHT / SOURCE_HEIGHT;
        float centerWidth = this.centerSourceWidth * scale;
        float sideStageWidth = Math.max(0f, (GameplayWorldLayout.STAGE_WIDTH - centerWidth) / 2f);
        return Math.max(1, Math.round(sideStageWidth / scale));
    }

    private void layoutBackground() {
        if (getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }
        float scale = getHeight() / SOURCE_HEIGHT;
        float centerWidth = this.centerSourceWidth * scale;
        float sideWidth = Math.max(0f, (getWidth() - centerWidth) / 2f);
        this.leftBackground.setBounds(0f, 0f, sideWidth, getHeight());
        this.centerBackground.setBounds(sideWidth, 0f, centerWidth, getHeight());
        this.rightBackground.setBounds(sideWidth + centerWidth, 0f, sideWidth, getHeight());
    }


}
