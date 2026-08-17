package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import model.level.LevelType;
import model.mechanism.Position;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;

/** Mandatory visual rules for special Phase 2 gameplay modes. */
public final class GameplayLevelRulesLayer extends Group {
    private static final Color DEADLINE = new Color(0.96f, 0.10f, 0.07f, 0.78f);
    private static final Color PROTECTED = new Color(0.20f, 1f, 0.38f, 0.80f);
    private static final Color NECROMANCY = new Color(0.74f, 0.19f, 0.95f, 0.58f);
    private static final Color MAX_WATER = new Color(0.18f, 0.82f, 1f, 0.80f);
    private static final Color PANEL = new Color(0.08f, 0.06f, 0.04f, 0.82f);
    private static final String CHALLENGE_BG = "IMAGE_UI_HUD_INGAME_CHALLENGE_BACKGROUND";
    private static final String CHALLENGE_ZOMBIE = "IMAGE_UI_HUD_INGAME_CHALLENGE_ZOMBIE_HEAD";
    private static final String CHALLENGE_SUN = "IMAGE_UI_HUD_INGAME_CHALLENGE_SUN_PRODUCED_ICON";
    private static final String CHALLENGE_SUN_TIMER = "IMAGE_UI_HUD_INGAME_CHALLENGE_SUN_TIMER_ICON";
    private static final String CHALLENGE_PLANT = "IMAGE_UI_HUD_INGAME_CHALLENGE_PLANT_COUNT_ICON";
    private static final String CHALLENGE_PLANT_LOST = "IMAGE_UI_HUD_INGAME_CHALLENGE_PLANT_LOST_ICON";

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private final Skin skin = PvzSkin.get();
    private String signature = "";

    public GameplayLevelRulesLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
    }

    GameplayLevelRulesLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
        if (dataSource == null) {
            throw new IllegalArgumentException("Gameplay world data source is required");
        }
        if (assets == null) {
            throw new IllegalArgumentException("Game asset manager is required");
        }
        this.dataSource = dataSource;
        this.assets = assets;
        setTouchable(Touchable.childrenOnly);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        String next = signature();
        if (!next.equals(this.signature)) {
            this.signature = next;
            rebuild();
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        this.signature = "";
    }

    private void rebuild() {
        clearChildren();
        addDeadline();
        addProtectedSeeds();
        addNecromancyCells();
        addMaximumWaterLine();
        addStatusPanel();
        addPreparationControl();
    }

    private void addDeadline() {
        int deadline = this.dataSource.getDeadlineColumn();
        if (deadline < 0 || deadline >= GameplayBoardInteractionLayer.COLUMN_COUNT) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float x = (deadline + 0.5f) * cellWidth;
        Image line = colorImage(DEADLINE);
        line.setBounds(x - 6f, 0f, 12f, getHeight());
        line.setTouchable(Touchable.disabled);
        addActor(line);
        Label label = label("DEADLINE", 0.62f);
        label.setColor(Color.WHITE);
        label.setBounds(x - 80f, getHeight() - 38f, 160f, 30f);
        addActor(label);
    }

    private void addProtectedSeeds() {
        for (Position position : safePositions(this.dataSource.getProtectedSeedCells())) {
            addCellBorder(position, PROTECTED, "PROTECT");
        }
    }

    private void addNecromancyCells() {
        for (Position position : safePositions(this.dataSource.getNecromancyCells())) {
            addCellBorder(position, NECROMANCY, "NECROMANCY");
        }
    }

    private void addMaximumWaterLine() {
        int column = this.dataSource.getMaximumWaterColumn();
        if (column < 0 || column >= GameplayBoardInteractionLayer.COLUMN_COUNT) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float x = column * cellWidth;
        Image line = colorImage(MAX_WATER);
        line.setBounds(x - 3f, 0f, 6f, getHeight());
        line.setTouchable(Touchable.disabled);
        addActor(line);
        Label label = label("MAX TIDE", 0.48f);
        label.setBounds(x - 62f, 5f, 124f, 25f);
        addActor(label);
    }

    private void addStatusPanel() {
        List<String> lines = new ArrayList<>(this.dataSource.getTimedMissionStatusLines());
        if (this.dataSource.getLevelType() == LevelType.LOVE_YOUR_PLANTS
                && this.dataSource.getRemainingPlantCount() >= 0) {
            lines.add("PLANTS REMAINING: " + this.dataSource.getRemainingPlantCount());
        }
        if (lines.isEmpty()) {
            return;
        }
        Table panel = new Table();
        Drawable challengeBackground = resourceDrawable(CHALLENGE_BG);
        panel.setBackground(challengeBackground == null
                ? this.skin.newDrawable("white_pixel", PANEL)
                : challengeBackground);
        panel.pad(8f, 10f, 8f, 10f);
        for (String line : lines) {
            Image icon = challengeIcon(line);
            if (icon != null) {
                panel.add(icon).size(32f).left().padRight(7f);
            } else {
                panel.add().width(39f);
            }
            Label label = label(line, 0.56f);
            label.setAlignment(Align.left);
            label.setColor(PvzVisualTheme.TEXT_CREAM);
            panel.add(label).growX().left().height(32f);
            panel.row();
        }
        float panelHeight = Math.max(50f, lines.size() * 34f + 16f);
        panel.setBounds(8f, getHeight() - panelHeight - 8f, 350f, panelHeight);
        addActor(panel);
    }

    private void addPreparationControl() {
        if (!this.dataSource.isPreWavePlanting()) {
            return;
        }
        Table panel = new Table();
        Drawable challengeBackground = resourceDrawable(CHALLENGE_BG);
        panel.setBackground(challengeBackground == null
                ? this.skin.newDrawable("white_pixel", PANEL)
                : challengeBackground);
        panel.pad(8f);
        Label label = label("BUILD YOUR DEFENSE", 0.72f);
        TextButton start = new TextButton("START WAVE", this.skin, "brown");
        start.getLabel().setFontScale(0.70f);
        start.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dataSource.startPreparedWave();
                signature = "";
            }
        });
        CollectionUiAnimator.installHoverScale(start);
        panel.add(label).padRight(14f);
        panel.add(start).size(150f, 46f);
        panel.setBounds(getWidth() / 2f - 250f, getHeight() - 68f, 500f, 60f);
        addActor(panel);
    }

    private void addCellBorder(Position position, Color color, String text) {
        if (position == null || position.getX() < 0 || position.getX() >= 9
                || position.getY() < 0 || position.getY() >= 5) {
            return;
        }
        float cellWidth = getWidth() / GameplayBoardInteractionLayer.COLUMN_COUNT;
        float cellHeight = getHeight() / GameplayBoardInteractionLayer.ROW_COUNT;
        float x = position.getX() * cellWidth;
        float y = (GameplayBoardInteractionLayer.ROW_COUNT - 1 - position.getY()) * cellHeight;
        float thickness = 5f;
        addRect(x, y, cellWidth, thickness, color);
        addRect(x, y + cellHeight - thickness, cellWidth, thickness, color);
        addRect(x, y, thickness, cellHeight, color);
        addRect(x + cellWidth - thickness, y, thickness, cellHeight, color);
        Label label = label(text, 0.36f);
        label.setBounds(x + 4f, y + 4f, cellWidth - 8f, 22f);
        addActor(label);
    }

    private void addRect(float x, float y, float width, float height, Color color) {
        Image image = colorImage(color);
        image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled);
        addActor(image);
    }

    private Image colorImage(Color color) {
        return new Image(this.skin.newDrawable("white_pixel", color));
    }

    private Label label(String text, float scale) {
        Label label = new Label(text == null ? "" : text, this.skin, "default");
        label.setAlignment(Align.center);
        label.setFontScale(scale);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private Image challengeIcon(String line) {
        String normalized = line == null ? "" : line.toUpperCase();
        String resource = normalized.contains("ZOMB")
                ? CHALLENGE_ZOMBIE
                : normalized.contains("SUN") && (normalized.contains("TIME") || normalized.contains("SECOND"))
                ? CHALLENGE_SUN_TIMER
                : normalized.contains("SUN")
                ? CHALLENGE_SUN
                : normalized.contains("LOST") || normalized.contains("LOSE")
                ? CHALLENGE_PLANT_LOST
                : normalized.contains("PLANT")
                ? CHALLENGE_PLANT
                : null;
        if (resource == null) {
            return null;
        }
        Drawable drawable = resourceDrawable(resource);
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
            // Keep mandatory level rules readable when a cosmetic atlas is unavailable.
        }
        return null;
    }

    private List<Position> safePositions(List<Position> positions) {
        return positions == null ? List.of() : positions;
    }

    private String signature() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.dataSource.getLevelType()).append('|')
                .append(this.dataSource.getDeadlineColumn()).append('|')
                .append(this.dataSource.getRemainingPlantCount()).append('|')
                .append(this.dataSource.isPreWavePlanting()).append('|')
                .append(this.dataSource.getMaximumWaterColumn()).append('|');
        appendPositions(builder, this.dataSource.getProtectedSeedCells());
        appendPositions(builder, this.dataSource.getNecromancyCells());
        for (String line : this.dataSource.getTimedMissionStatusLines()) {
            builder.append(line).append(';');
        }
        return builder.toString();
    }

    private void appendPositions(StringBuilder builder, List<Position> positions) {
        if (positions == null) {
            return;
        }
        for (Position position : positions) {
            if (position != null) {
                builder.append(position.getX()).append(',').append(position.getY()).append(';');
            }
        }
    }
}
