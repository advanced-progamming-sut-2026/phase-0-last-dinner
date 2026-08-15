package view;


public final class GameSettings {

    private static final int MIN_SPEED = 1;
    private static final int MAX_SPEED = 3;
    private static final int DEFAULT_SPEED = 1;

    private static int gameSpeed = DEFAULT_SPEED;
    private static boolean showGrid = false;
    private static boolean debugMode = false;

    private GameSettings() {
    }

    public static int getGameSpeed() {
        return gameSpeed;
    }

    public static void setGameSpeed(int speed) {
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            throw new IllegalArgumentException("Game speed must be between " + MIN_SPEED + " and " + MAX_SPEED);
        }
        gameSpeed = speed;
    }

    public static boolean isShowGrid() {
        return showGrid;
    }

    public static void setShowGrid(boolean value) {
        showGrid = value;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean value) {
        debugMode = value;
    }
}
