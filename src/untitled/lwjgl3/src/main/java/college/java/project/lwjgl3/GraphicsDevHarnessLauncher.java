package college.java.project.lwjgl3;

import college.java.project.graphics.GraphicsDevHarnessGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class GraphicsDevHarnessLauncher {
    private GraphicsDevHarnessLauncher() {
    }

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        String mode = args == null || args.length == 0 ? "collection" : args[0];
        int width = parseDimension(args, 1, 1280);
        int height = parseDimension(args, 2, 720);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("PvZ2 Graphics Dev Harness - " + mode);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setWindowedMode(width, height);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        new Lwjgl3Application(new GraphicsDevHarnessGame(mode), configuration);
    }

    private static int parseDimension(String[] args, int index, int fallback) {
        if (args == null || index >= args.length) {
            return fallback;
        }
        try {
            return Math.max(640, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
