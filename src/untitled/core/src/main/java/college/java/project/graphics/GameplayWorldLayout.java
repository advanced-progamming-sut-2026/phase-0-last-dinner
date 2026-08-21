package college.java.project.graphics;

/** Shared 1920x1080 geometry registered to the original PvZ2 768p board art. */
public final class GameplayWorldLayout {
    public static final float STAGE_WIDTH = 1920f;
    public static final float STAGE_HEIGHT = 1080f;

    public static final float BACKGROUND_SOURCE_WIDTH = 1024f;
    public static final float BACKGROUND_SOURCE_HEIGHT = 768f;
    public static final float BACKGROUND_SCALE = STAGE_HEIGHT / BACKGROUND_SOURCE_HEIGHT;
    public static final float BACKGROUND_WIDTH = BACKGROUND_SOURCE_WIDTH * BACKGROUND_SCALE;
    public static final float BACKGROUND_HEIGHT = STAGE_HEIGHT;
    public static final float BACKGROUND_X = (STAGE_WIDTH - BACKGROUND_WIDTH) / 2f;
    public static final float BACKGROUND_Y = 0f;

    /*
     * Pixel-registered 9x5 board rectangle measured from the original center
     * chapter textures. These same grid lines are shared by Egypt, Ice Caves,
     * Big Wave Beach and Dark Ages 768p backgrounds.
     */
    private static final float BOARD_LEFT_SOURCE_X = 250f;
    private static final float BOARD_RIGHT_SOURCE_X = 992f;
    private static final float BOARD_TOP_SOURCE_Y = 197f;
    private static final float BOARD_BOTTOM_SOURCE_Y = 693f;

    public static final float LAWN_X = BACKGROUND_X
            + BOARD_LEFT_SOURCE_X * BACKGROUND_SCALE;
    public static final float LAWN_Y = (BACKGROUND_SOURCE_HEIGHT - BOARD_BOTTOM_SOURCE_Y)
            * BACKGROUND_SCALE;
    public static final float LAWN_WIDTH = (BOARD_RIGHT_SOURCE_X - BOARD_LEFT_SOURCE_X)
            * BACKGROUND_SCALE;
    public static final float LAWN_HEIGHT = (BOARD_BOTTOM_SOURCE_Y - BOARD_TOP_SOURCE_Y)
            * BACKGROUND_SCALE;

    /*
     * Original PvZ2 characters do not stand on the lower tile border. Their
     * visual ground/contact point sits inside the tile, roughly around the
     * lower fifth of the cell. Keeping these anchors here makes PAM, static
     * fallback and cursor-preview placement agree across the whole lawn.
     */
    public static final float PLANT_GROUND_ANCHOR_FACTOR = 0.16f;
    public static final float ZOMBIE_GROUND_ANCHOR_FACTOR = 0.18f;

    private GameplayWorldLayout() {
    }

    public static float cellWidth() {
        return LAWN_WIDTH / GameplayBoardInteractionLayer.COLUMN_COUNT;
    }

    public static float cellHeight() {
        return LAWN_HEIGHT / GameplayBoardInteractionLayer.ROW_COUNT;
    }

    public static float cellCenterX(int column) {
        return LAWN_X + (column + 0.5f) * cellWidth();
    }

    public static float cellCenterY(int row) {
        return LAWN_Y
                + (GameplayBoardInteractionLayer.ROW_COUNT - row - 0.5f) * cellHeight();
    }
}
