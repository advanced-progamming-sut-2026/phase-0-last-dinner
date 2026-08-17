package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import model.Plant;
import model.chapters.ChapterType;
import model.zombie.Zombie;
import pvz.skin.PvzSkin;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** View-only chapter event explanations inferred from live Phase 1 state changes. */
public final class GameplayChapterEventLayer extends Group {
    private static final Color ICE_WIND = new Color(0.58f, 0.92f, 1f, 0.30f);
    private static final Color SANDSTORM = new Color(0.91f, 0.70f, 0.36f, 0.34f);

    private final GameplaySeedBankDataSource seedDataSource;
    private final GameplayWorldDataSource worldDataSource;
    private final Map<Plant, Integer> freezeLevels = new IdentityHashMap<>();
    private final Map<Zombie, Boolean> seenZombies = new IdentityHashMap<>();
    private boolean initialized;

    public GameplayChapterEventLayer(
            GameplaySeedBankDataSource seedDataSource,
            GameplayWorldDataSource worldDataSource
    ) {
        this.seedDataSource = seedDataSource;
        this.worldDataSource = worldDataSource;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        detectIceWind();
        detectEgyptSandstorm();
        cleanupIdentityMaps();
        this.initialized = true;
    }

    private void detectIceWind() {
        if (this.worldDataSource.getChapterType() != ChapterType.ICE_CAVES) {
            rememberFreezeLevels();
            return;
        }
        boolean[] affectedRows = new boolean[GameplayBoardInteractionLayer.ROW_COUNT];
        List<Plant> plants = this.seedDataSource.getPlantsOnBoard();
        for (Plant plant : plants) {
            if (plant == null || plant.getPosition() == null) {
                continue;
            }
            int level = this.worldDataSource.getPlantCoverState(plant).getFreezeLevel();
            int previous = this.freezeLevels.getOrDefault(plant, level);
            if (this.initialized && level > previous) {
                int row = plant.getPosition().getY();
                if (row >= 0 && row < affectedRows.length) {
                    affectedRows[row] = true;
                }
            }
            this.freezeLevels.put(plant, level);
        }
        for (int row = 0; row < affectedRows.length; row++) {
            if (affectedRows[row]) {
                showRowEvent(row, ICE_WIND, "ICE WIND!");
            }
        }
    }

    private void rememberFreezeLevels() {
        for (Plant plant : this.seedDataSource.getPlantsOnBoard()) {
            if (plant != null) {
                this.freezeLevels.put(
                        plant,
                        this.worldDataSource.getPlantCoverState(plant).getFreezeLevel()
                );
            }
        }
    }

    private void detectEgyptSandstorm() {
        List<Zombie> zombies = this.worldDataSource.getZombiesOnBoard();
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null) {
                continue;
            }
            boolean isNew = !this.seenZombies.containsKey(zombie);
            this.seenZombies.put(zombie, Boolean.TRUE);
            if (!this.initialized || !isNew
                    || this.worldDataSource.getChapterType() != ChapterType.ANCIENT_EGYPT) {
                continue;
            }
            if (zombie.getExactX() < 7.5d) {
                showRowEvent(zombie.getPosition().getY(), SANDSTORM, "SANDSTORM!");
            }
        }
    }

    private void showRowEvent(int row, Color color, String text) {
        float rowHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float y = (GameplayBoardInteractionLayer.ROW_COUNT - 1 - row) * rowHeight;
        Group event = new Group();
        event.setTouchable(Touchable.disabled);
        event.setBounds(0f, y, getWidth(), rowHeight);
        Image wash = new Image(PvzSkin.get().newDrawable("white_pixel", color));
        wash.setBounds(0f, 0f, event.getWidth(), event.getHeight());
        event.addActor(wash);
        for (int index = 0; index < 7; index++) {
            Image streak = new Image(PvzSkin.get().newDrawable(
                    "white_pixel",
                    new Color(1f, 1f, 1f, text.startsWith("ICE") ? 0.48f : 0.22f)
            ));
            float streakWidth = event.getWidth() * (0.075f + index * 0.007f);
            float streakX = event.getWidth() * (0.08f + index * 0.125f);
            float streakY = event.getHeight() * (0.18f + (index % 3) * 0.22f);
            streak.setBounds(streakX, streakY, streakWidth, Math.max(4f, event.getHeight() * 0.035f));
            streak.setRotation(text.startsWith("ICE") ? -7f : 8f);
            event.addActor(streak);
        }
        Image labelBack = new Image(PvzSkin.get().newDrawable(
                "white_pixel",
                new Color(0.05f, 0.04f, 0.03f, 0.62f)
        ));
        labelBack.setBounds(event.getWidth() * 0.38f, event.getHeight() * 0.28f,
                event.getWidth() * 0.24f, event.getHeight() * 0.44f);
        event.addActor(labelBack);
        Label label = new Label(text, PvzSkin.get(), "default");
        label.setAlignment(Align.center);
        label.setFontScale(0.88f);
        label.setBounds(event.getWidth() * 0.38f, event.getHeight() * 0.28f,
                event.getWidth() * 0.24f, event.getHeight() * 0.44f);
        event.addActor(label);
        event.getColor().a = 0f;
        event.addAction(Actions.sequence(
                Actions.fadeIn(0.12f),
                Actions.delay(1.0f),
                Actions.fadeOut(0.45f),
                Actions.removeActor()
        ));
        addActor(event);
    }

    private void cleanupIdentityMaps() {
        this.freezeLevels.keySet().removeIf(plant -> !containsIdentity(
                this.seedDataSource.getPlantsOnBoard(), plant));
        this.seenZombies.keySet().removeIf(zombie -> !containsIdentity(
                this.worldDataSource.getZombiesOnBoard(), zombie));
    }

    private <T> boolean containsIdentity(List<T> list, T wanted) {
        if (list == null) {
            return false;
        }
        for (T item : list) {
            if (item == wanted) {
                return true;
            }
        }
        return false;
    }
}
