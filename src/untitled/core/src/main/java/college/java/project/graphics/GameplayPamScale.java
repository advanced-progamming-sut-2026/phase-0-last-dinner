package college.java.project.graphics;

/** Shared native-PAM scale for gameplay entities on the 1920x1080 stage. */
public final class GameplayPamScale {
    public static final float PAM_AUTHORING_HEIGHT = 1200f;
    public static final float BACKGROUND_SOURCE_HEIGHT = 768f;
    public static final float WORLD_SCALE = GameplayWorldLayout.STAGE_HEIGHT / PAM_AUTHORING_HEIGHT;
    public static final float BACKGROUND_SCALE = GameplayWorldLayout.STAGE_HEIGHT / BACKGROUND_SOURCE_HEIGHT;
    public static final float BACKGROUND_TO_PAM_RATIO = BACKGROUND_SCALE / WORLD_SCALE;

    private GameplayPamScale() {
    }

    public static float actorWidth(float canvasWidth) {
        return canvasWidth * WORLD_SCALE;
    }

    public static float actorHeight(float canvasHeight) {
        return canvasHeight * WORLD_SCALE;
    }
}
