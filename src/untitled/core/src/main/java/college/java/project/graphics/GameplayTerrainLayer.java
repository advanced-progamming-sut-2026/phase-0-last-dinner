package college.java.project.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.chapters.ChapterType;
import model.mechanism.GraveLootType;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Draws chapter-aware board terrain, graves and current Big Wave Beach shoreline. */
public final class GameplayTerrainLayer extends Group {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;
    private static final Color WATER = new Color(0.08f, 0.59f, 0.84f, 0.25f);
    private static final Color LOW_BEACH = new Color(0.18f, 0.70f, 0.89f, 0.10f);
    private static final Color FROZEN = new Color(0.63f, 0.91f, 1f, 0.42f);
    private static final Color SLIPPERY = new Color(0.80f, 0.95f, 1f, 0.28f);
    private static final Color NECROMANCY = new Color(0.49f, 0.15f, 0.68f, 0.30f);
    private static final Color CRATER = new Color(0.10f, 0.08f, 0.05f, 0.35f);
    private static final Color DAMAGE_FLASH = new Color(1f, 0.68f, 0.58f, 1f);
    private static final float DAMAGE_FLASH_SECONDS = 0.12f;
    private static final String BEACH_WATER_SQUARE =
            "IMAGE_BACKGROUNDS_WATER_SQUARE_WATER_SQUARE_190X210";
    private static final String BEACH_WATER_PAM = "WATER_SQUARE";
    private static final String BEACH_WATER_UNDERLAYER_PAM = "WATER_UNDERLAYER";
    private static final String BEACH_TIDE_LINE_PAM = "WATER_TIDE_LINE";
    private static final String BEACH_TIDE_LINE =
            "IMAGE_BACKGROUNDS_WATER_TIDE_LINE_WATER_TIDE_LINE_161X397";
    private static final String BEACH_WATER_SIGN = "IMAGE_BACKGROUNDS_BEACH_WATERSIGN";
    private static final String[] EGYPT_GRAVE_STAGES = {
            "IMAGE_GRAVESTONES_EGYPT_HIEROGLYPH_EGYPT_HIEROGLYPH_118X148",
            "IMAGE_GRAVESTONES_EGYPT_HIEROGLYPH_EGYPT_HIEROGLYPH_113X145",
            "IMAGE_GRAVESTONES_EGYPT_HIEROGLYPH_EGYPT_HIEROGLYPH_110X145",
            "IMAGE_GRAVESTONES_EGYPT_HIEROGLYPH_EGYPT_HIEROGLYPH_109X119"
    };
    private static final String[] DARK_GRAVE_STAGES = {
            "IMAGE_GRAVESTONES_DARK_NOOP_DARK_NOOP_132X160",
            "IMAGE_GRAVESTONES_DARK_NOOP_DARK_NOOP_132X156",
            "IMAGE_GRAVESTONES_DARK_NOOP_DARK_NOOP_125X149",
            "IMAGE_GRAVESTONES_DARK_NOOP_DARK_NOOP_93X89"
    };
    private static final String[] DARK_SUN_GRAVE_STAGES = {
            "IMAGE_GRAVESTONES_DARK_SUN_DARK_SUN_132X160",
            "IMAGE_GRAVESTONES_DARK_SUN_DARK_SUN_132X157",
            "IMAGE_GRAVESTONES_DARK_SUN_DARK_SUN_132X144",
            "IMAGE_GRAVESTONES_DARK_SUN_DARK_SUN_93X91"
    };
    private static final String[] DARK_PLANT_FOOD_GRAVE_STAGES = {
            "IMAGE_GRAVESTONES_DARK_PLANTFOOD_DARK_PLANTFOOD_132X160",
            "IMAGE_GRAVESTONES_DARK_PLANTFOOD_DARK_PLANTFOOD_132X157",
            "IMAGE_GRAVESTONES_DARK_PLANTFOOD_DARK_PLANTFOOD_129X144",
            "IMAGE_GRAVESTONES_DARK_PLANTFOOD_DARK_PLANTFOOD_93X95"
    };

    private final GameplayWorldDataSource dataSource;
    private final GameAssetManager assets;
    private final PamAnimationCatalog pamCatalog = new PamAnimationCatalog();
    private boolean ownsAssets;
    private String terrainSignature = "";
    private final Map<Integer, Integer> terrainHealth = new HashMap<>();

    public GameplayTerrainLayer(GameplayWorldDataSource dataSource) {
        this(dataSource, new GameAssetManager());
        this.ownsAssets = true;
    }

    GameplayTerrainLayer(GameplayWorldDataSource dataSource, GameAssetManager assets) {
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
        List<Tile> tiles = this.dataSource.getTiles();
        String next = signature(tiles);
        if (!next.equals(this.terrainSignature)) {
            Set<Integer> damaged = damagedTiles(tiles);
            this.terrainSignature = next;
            rebuild(damaged);
            rememberTerrainHealth(tiles);
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        this.terrainSignature = "";
    }

    public void dispose() {
        if (this.ownsAssets) {
            this.assets.dispose();
        }
    }

    private void rebuild(Set<Integer> damaged) {
        clearChildren();
        List<Tile> tiles = sortedTiles();
        for (Tile tile : tiles) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }
            Actor actor = createTerrainActor(tile);
            if (actor == null) {
                continue;
            }
            position(actor, tile.getPosition(), tile.getTerrainType());
            actor.setTouchable(Touchable.disabled);
            if (damaged.contains(cellKey(tile.getPosition()))) {
                actor.setColor(DAMAGE_FLASH);
                actor.addAction(Actions.color(Color.WHITE, DAMAGE_FLASH_SECONDS));
            }
            addActor(actor);
        }
        addBeachWaterline(tiles);
    }

    private Set<Integer> damagedTiles(List<Tile> tiles) {
        Set<Integer> damaged = new HashSet<>();
        for (Tile tile : tiles) {
            if (!tracksDamage(tile)) {
                continue;
            }
            int key = cellKey(tile.getPosition());
            Integer previous = this.terrainHealth.get(key);
            if (previous != null && tile.getTerrainHealth() < previous) {
                damaged.add(key);
            }
        }
        return damaged;
    }

    private void rememberTerrainHealth(List<Tile> tiles) {
        this.terrainHealth.clear();
        for (Tile tile : tiles) {
            if (tracksDamage(tile)) {
                this.terrainHealth.put(cellKey(tile.getPosition()), tile.getTerrainHealth());
            }
        }
    }

    private boolean tracksDamage(Tile tile) {
        if (tile == null || tile.getPosition() == null) {
            return false;
        }
        TerrainType type = tile.getTerrainType();
        return type == TerrainType.GRAVE || type == TerrainType.FROZEN;
    }

    private int cellKey(Position position) {
        return position.getY() * COLUMN_COUNT + position.getX();
    }

    private List<Tile> sortedTiles() {
        List<Tile> result = new ArrayList<>(this.dataSource.getTiles());
        result.sort(Comparator.comparingInt((Tile tile) -> safeY(tile))
                .thenComparingInt(this::safeX));
        return result;
    }

    private int safeX(Tile tile) {
        return tile == null || tile.getPosition() == null ? 0 : tile.getPosition().getX();
    }

    private int safeY(Tile tile) {
        return tile == null || tile.getPosition() == null ? 0 : tile.getPosition().getY();
    }

    private Actor createTerrainActor(Tile tile) {
        TerrainType type = tile.getTerrainType();
        if (type == null || type == TerrainType.CLASSIC) {
            return null;
        }
        if (type == TerrainType.GRAVE) {
            return graveImage(tile);
        }
        if (type == TerrainType.WATER) {
            return waterImage();
        }
        if (type == TerrainType.LOW_BEACH) {
            return lowBeachImage();
        }
        if (type == TerrainType.FROZEN) {
            return overlay(FROZEN);
        }
        if (type == TerrainType.SLIPPERY_UP) {
            return slipperyActor(true);
        }
        if (type == TerrainType.SLIPPERY_DOWN) {
            return slipperyActor(false);
        }
        if (type == TerrainType.NECROMANCY) {
            return overlay(NECROMANCY);
        }
        if (type == TerrainType.CRATER) {
            return overlay(CRATER);
        }
        return null;
    }

    private Actor waterImage() {
        Actor animated = createPamTerrainActor(BEACH_WATER_PAM, 0.42f);
        if (animated != null) {
            return animated;
        }
        Drawable drawable = resourceDrawable(BEACH_WATER_SQUARE);
        if (drawable == null) {
            return overlay(WATER);
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.stretch);
        image.setColor(1f, 1f, 1f, 0.36f);
        return image;
    }

    private Actor lowBeachImage() {
        Group group = new Group();
        Image base = overlay(LOW_BEACH);
        group.addActor(base);
        Actor underlayer = createPamTerrainActor(BEACH_WATER_UNDERLAYER_PAM, 0.26f);
        if (underlayer != null) {
            underlayer.setBounds(0f, 0f, 1f, 1f);
            group.addActor(underlayer);
        }
        return group;
    }

    private Actor createPamTerrainActor(String animationName, float alpha) {
        GameplayPamEffectSupport.Effect effect = GameplayPamEffectSupport.create(
                this.assets,
                this.pamCatalog,
                animationName,
                true,
                "Water", "idle", "loop", "animation"
        );
        if (effect == null) {
            return null;
        }
        effect.actor.setColor(1f, 1f, 1f, alpha);
        return effect.actor;
    }

    private Image graveImage(Tile tile) {
        String resourceId = graveResource(tile);
        Drawable drawable = resourceDrawable(resourceId);
        if (drawable == null) {
            return overlay(new Color(0.28f, 0.24f, 0.20f, 0.55f));
        }
        Image image = new Image(drawable);
        image.setScaling(Scaling.fit);
        return image;
    }

    private String graveResource(Tile tile) {
        int stage = graveDamageStage(tile);
        if (this.dataSource.getChapterType() != ChapterType.MEDIEVAL) {
            return EGYPT_GRAVE_STAGES[stage];
        }
        GraveLootType loot = tile == null ? GraveLootType.NONE : tile.getGraveLoot();
        if (loot == GraveLootType.SUN) {
            return DARK_SUN_GRAVE_STAGES[stage];
        }
        if (loot == GraveLootType.PLANT_FOOD) {
            return DARK_PLANT_FOOD_GRAVE_STAGES[stage];
        }
        return DARK_GRAVE_STAGES[stage];
    }

    private int graveDamageStage(Tile tile) {
        int health = tile == null ? 700 : Math.max(0, tile.getTerrainHealth());
        if (health > 525) {
            return 0;
        }
        if (health > 350) {
            return 1;
        }
        if (health > 175) {
            return 2;
        }
        return 3;
    }

    private Actor slipperyActor(boolean up) {
        Group group = new Group();
        Image base = overlay(SLIPPERY);
        group.addActor(base);
        for (int index = 0; index < 3; index++) {
            addChevron(group, up, index);
        }
        return group;
    }

    private void addChevron(Group group, boolean up, int index) {
        Image left = overlay(new Color(0.83f, 0.98f, 1f, 0.78f));
        Image right = overlay(new Color(0.83f, 0.98f, 1f, 0.78f));
        float centerY = 0.27f + index * 0.20f;
        left.setOrigin(0f, 0f);
        right.setOrigin(0f, 0f);
        left.setRotation(up ? 35f : -35f);
        right.setRotation(up ? -35f : 35f);
        left.setBounds(0.28f, centerY, 0.27f, 0.055f);
        right.setBounds(0.49f, centerY, 0.27f, 0.055f);
        group.addActor(left);
        group.addActor(right);
    }

    private Image overlay(Color color) {
        Image image = new Image(PvzSkin.get().newDrawable("white_pixel", color));
        image.setScaling(Scaling.stretch);
        return image;
    }

    private Drawable beachTideDrawable() {
        try {
            TextureBank bank = this.assets.getTextureBank();
            TextureRegion region = bank == null ? null : bank.region(BEACH_TIDE_LINE);
            if (region == null) {
                return null;
            }
            int croppedWidth = Math.max(1, Math.round(region.getRegionWidth() * 0.30f));
            return new TextureRegionDrawable(new TextureRegion(
                    region,
                    0,
                    0,
                    croppedWidth,
                    region.getRegionHeight()
            ));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Drawable resourceDrawable(String resourceId) {
        try {
            TextureBank bank = this.assets.getTextureBank();
            if (bank != null && bank.region(resourceId) != null) {
                return new TextureRegionDrawable(bank.region(resourceId));
            }
        } catch (RuntimeException ignored) {
            // fallback color terrain state ro moshakhas negah midare
        }
        return null;
    }

    private void position(Actor actor, Position position, TerrainType type) {
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;
        float x = position.getX() * cellWidth;
        float y = (ROW_COUNT - 1 - position.getY()) * cellHeight;
        if (type == TerrainType.GRAVE) {
            actor.setBounds(x + cellWidth * 0.12f, y, cellWidth * 0.76f, cellHeight * 1.15f);
            return;
        }
        actor.setBounds(x + 1f, y + 1f, cellWidth - 2f, cellHeight - 2f);
        layoutSlipperyChildren(actor);
    }

    private void layoutSlipperyChildren(Actor actor) {
        if (!(actor instanceof Group)) {
            return;
        }
        Group group = (Group) actor;
        if (group.getChildren().size == 0) {
            return;
        }
        group.getChildren().get(0).setBounds(0f, 0f, group.getWidth(), group.getHeight());
        for (int index = 1; index < group.getChildren().size; index++) {
            Actor child = group.getChildren().get(index);
            child.setBounds(
                    child.getX() * group.getWidth(),
                    child.getY() * group.getHeight(),
                    child.getWidth() * group.getWidth(),
                    child.getHeight() * group.getHeight()
            );
        }
    }

    private void addBeachWaterline(List<Tile> tiles) {
        if (this.dataSource.getChapterType() != ChapterType.BIG_WAVE_BEACH) {
            return;
        }
        int waterColumn = Integer.MAX_VALUE;
        for (Tile tile : tiles) {
            if (tile != null && tile.getPosition() != null
                    && tile.getTerrainType() == TerrainType.WATER) {
                waterColumn = Math.min(waterColumn, tile.getPosition().getX());
            }
        }
        if (waterColumn == Integer.MAX_VALUE) {
            return;
        }
        float cellWidth = getWidth() / COLUMN_COUNT;
        float cellHeight = getHeight() / ROW_COUNT;
        float boundary = waterColumn * cellWidth;
        Actor animatedTide = createPamTerrainActor(BEACH_TIDE_LINE_PAM, 0.92f);
        if (animatedTide != null) {
            float lineHeight = getHeight();
            float lineWidth = lineHeight * 161f / 397f;
            animatedTide.setBounds(boundary - lineWidth * 0.50f, 0f, lineWidth, lineHeight);
            animatedTide.setTouchable(Touchable.disabled);
            addActor(animatedTide);
        } else {
            Drawable tideDrawable = beachTideDrawable();
            if (tideDrawable != null) {
                float lineHeight = getHeight();
                float lineWidth = lineHeight * 161f / 397f * 0.30f;
                Image tide = new Image(tideDrawable);
                tide.setScaling(Scaling.stretch);
                tide.setBounds(boundary - lineWidth * 0.34f, 0f, lineWidth, lineHeight);
                tide.setColor(1f, 1f, 1f, 0.82f);
                tide.setTouchable(Touchable.disabled);
                addActor(tide);
            } else {
                Image foam = overlay(new Color(1f, 1f, 1f, 0.42f));
                foam.setBounds(boundary - 3f, 0f, 6f, getHeight());
                addActor(foam);
            }
        }
        Drawable signDrawable = resourceDrawable(BEACH_WATER_SIGN);
        if (signDrawable != null) {
            float signHeight = cellHeight * 1.02f;
            float signWidth = signHeight * 146f / 263f;
            Image sign = new Image(signDrawable);
            sign.setScaling(Scaling.fit);
            sign.setBounds(
                    boundary - signWidth * 0.52f,
                    getHeight() - signHeight * 0.88f,
                    signWidth,
                    signHeight
            );
            sign.setTouchable(Touchable.disabled);
            addActor(sign);
        }
    }

    private String signature(List<Tile> tiles) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.dataSource.getChapterType()).append('|');
        for (Tile tile : tiles) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }
            builder.append(tile.getPosition().getX())
                    .append(':')
                    .append(tile.getPosition().getY())
                    .append(':')
                    .append(tile.getTerrainType())
                    .append(':')
                    .append(tile.getTerrainHealth())
                    .append(':')
                    .append(tile.getGraveLoot())
                    .append(';');
        }
        return builder.toString();
    }
}
