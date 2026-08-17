package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import model.chapters.ChapterType;
import pvz.skin.PvzSkin;

/** Required red, center-screen gameplay warnings for starts, waves and chapter events. */
public final class GameplayAlertLayer extends Group {
    private final GameplayWorldDataSource dataSource;
    private final Label alertLabel;
    private int lastWaveIndex = -1;
    private boolean openingShown;

    public GameplayAlertLayer(GameplayWorldDataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        this.dataSource = dataSource;
        this.alertLabel = new Label("", PvzSkin.get(), "medium_outline");
        this.alertLabel.setAlignment(Align.center);
        this.alertLabel.setFontScale(0.90f);
        this.alertLabel.setColor(new Color(1f, 0.12f, 0.08f, 1f));
        this.alertLabel.setTouchable(Touchable.disabled);
        addActor(this.alertLabel);
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!this.openingShown) {
            this.openingShown = true;
            showAlert("PLANT YOUR DEFENSES!");
        }
        int wave = Math.max(0, this.dataSource.getWaveIndex());
        if (this.lastWaveIndex < 0) {
            this.lastWaveIndex = wave;
            return;
        }
        if (wave != this.lastWaveIndex) {
            this.lastWaveIndex = wave;
            showWaveAlert();
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        this.alertLabel.setBounds(260f, getHeight() * 0.53f, getWidth() - 520f, 120f);
    }

    public void showAlert(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        this.alertLabel.setText(text.trim());
        this.alertLabel.clearActions();
        this.alertLabel.getColor().a = 1f;
        this.alertLabel.setVisible(true);
        this.alertLabel.addAction(Actions.sequence(
                Actions.delay(1.35f),
                Actions.fadeOut(0.28f),
                Actions.visible(false)
        ));
    }

    private void showWaveAlert() {
        ChapterType chapter = this.dataSource.getChapterType();
        if (chapter == ChapterType.MEDIEVAL) {
            showAlert("NECROMANCY! ZOMBIES ARE RISING!");
            return;
        }
        if (chapter == ChapterType.BIG_WAVE_BEACH) {
            showAlert("LOW TIDE! WATCH THE BEACH!");
            return;
        }
        showAlert("A HUGE WAVE OF ZOMBIES IS APPROACHING!");
    }
}
