package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import model.mechanism.Wave;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;

/** Compact in-game wave progress bar with visible wave boundaries. */
public final class GameplayWaveProgressBar extends Group {
    private static final String METER = "IMAGE_UI_HUD_INGAME_PROGRESS_METER";
    private static final String METER_FILL = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FILL";
    private static final String FLAG = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_DEFAULT";
    private static final String FLAG_POLE = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_POLE";
    private static final String ZOMBIE_HEAD = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBIEHEAD";
    private static final Color TRACK_COLOR = new Color(0.08f, 0.08f, 0.06f, 0.82f);
    private static final Color FILL_COLOR = new Color(0.57f, 0.82f, 0.20f, 0.96f);
    private static final Color MARKER_COLOR = new Color(0.95f, 0.86f, 0.54f, 0.96f);

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private final Skin skin;
    private final Image track;
    private final Image fill;
    private final Image zombieHead;
    private final List<Actor> markers = new ArrayList<>();
    private boolean ownsAssets;
    private int markerCount = -1;

    public GameplayWaveProgressBar(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayWaveProgressBar(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        this.skin = PvzSkin.get();
        this.track = resourceOrPixel(METER, TRACK_COLOR);
        this.fill = resourceOrPixel(METER_FILL, FILL_COLOR);
        this.zombieHead = resourceImage(ZOMBIE_HEAD);
        addActor(this.track);
        addActor(this.fill);
        if (this.zombieHead != null) {
            addActor(this.zombieHead);
        }
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        this.assets.update();
        updateProgress();
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        layoutBar();
    }

    public float getProgress() {
        int total = Math.max(0, this.dataSource.getWaveCount());
        if (total == 0) {
            return 0f;
        }
        int index = Math.max(0, Math.min(total - 1, this.dataSource.getWaveIndex()));
        Wave current = this.dataSource.getCurrentWave();
        float currentProgress = 0f;
        if (current != null && current.isStarted()) {
            double remaining = current.getRemainingHealthPercentage();
            currentProgress = (float) Math.max(0d, Math.min(1d, 1d - remaining));
        }
        return Math.max(0f, Math.min(1f, (index + currentProgress) / total));
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void updateProgress() {
        int total = Math.max(0, this.dataSource.getWaveCount());
        if (total != this.markerCount) {
            rebuildMarkers(total);
        }
        layoutProgress();
    }

    private void rebuildMarkers(int total) {
        for (Actor marker : this.markers) {
            marker.remove();
        }
        this.markers.clear();
        this.markerCount = total;
        if (total <= 1) {
            layoutBar();
            return;
        }
        for (int index = 1; index < total; index++) {
            Actor marker = createWaveMarker();
            this.markers.add(marker);
            addActor(marker);
        }
        layoutBar();
    }

    private Actor createWaveMarker() {
        Image pole = resourceImage(FLAG_POLE);
        Image flag = resourceImage(FLAG);
        if (pole == null && flag == null) {
            return pixel(MARKER_COLOR);
        }
        Group marker = new Group();
        if (pole != null) {
            pole.setBounds(7f, 0f, 22f, 34f);
            marker.addActor(pole);
        }
        if (flag != null) {
            flag.setBounds(8f, 17f, 24f, 20f);
            marker.addActor(flag);
        }
        marker.setTouchable(Touchable.disabled);
        return marker;
    }

    private void layoutBar() {
        float barHeight = getHeight();
        this.track.setBounds(0f, 0f, getWidth(), barHeight);
        layoutProgress();
        int total = Math.max(0, this.markerCount);
        for (int index = 0; index < this.markers.size(); index++) {
            float x = getWidth() * (index + 1f) / total;
            this.markers.get(index).setBounds(x - 18f, -3f, 38f, getHeight() + 8f);
        }
    }

    private void layoutProgress() {
        float progress = getProgress();
        float leftInset = getWidth() * 0.052f;
        float usableWidth = getWidth() * 0.885f;
        float fillHeight = getHeight() * 0.50f;
        float fillY = (getHeight() - fillHeight) * 0.50f;
        this.fill.setBounds(leftInset, fillY, Math.max(0f, usableWidth * progress), fillHeight);
        if (this.zombieHead != null) {
            float headSize = Math.min(52f, getHeight() * 1.05f);
            float x = leftInset + usableWidth * progress - headSize * 0.50f;
            this.zombieHead.setBounds(x, (getHeight() - headSize) * 0.50f, headSize, headSize);
        }
    }

    private Image resourceOrPixel(String resourceId, Color fallbackColor) {
        Image image = resourceImage(resourceId);
        return image == null ? pixel(fallbackColor) : image;
    }

    private Image resourceImage(String resourceId) {
        Image image = PvzVisualTheme.resourceImage(this.assets, this.skin, resourceId, Scaling.stretch);
        if (image != null) {
            image.setTouchable(Touchable.disabled);
        }
        return image;
    }

    private Image pixel(Color color) {
        Image image = new Image(this.skin.newDrawable("white_pixel", color));
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
