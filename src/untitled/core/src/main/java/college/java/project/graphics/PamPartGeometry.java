package college.java.project.graphics;

import com.badlogic.gdx.math.Rectangle;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Compatibility bridge for partBoundsByFrame across old and new libPVZ releases. */
final class PamPartGeometry {
    private PamPartGeometry() {
    }

    static Rectangle[] partBoundsByFrame(
            PamPlayer player,
            String pamPath,
            String clipName,
            String partName
    ) {
        if (player == null || blank(pamPath) || blank(clipName) || blank(partName)) {
            return new Rectangle[0];
        }
        try {
            player.loadSync(pamPath);
            ClipRef clip = player.getClip(pamPath, clipName);
            if (clip == null) {
                return new Rectangle[0];
            }
            Rectangle[] modern = invokeModern(player, clip, partName);
            return modern == null ? readLegacy(clip, partName) : modern;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return new Rectangle[0];
        }
    }

    private static Rectangle[] invokeModern(PamPlayer player, ClipRef clip, String partName)
            throws ReflectiveOperationException {
        Method method;
        try {
            method = player.getClass().getMethod("partBoundsByFrame", ClipRef.class, String.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
        Object result = method.invoke(player, clip, partName);
        return result instanceof Rectangle[] ? (Rectangle[]) result : new Rectangle[0];
    }

    private static Rectangle[] readLegacy(ClipRef clip, String partName)
            throws ReflectiveOperationException {
        Object baked = field(clip, "ba");
        int[] range = (int[]) field(clip, "range");
        Object framesArray = field(baked, "frames");
        Object partsArray = field(baked, "parts");
        float canvasWidth = floatField(baked, "canvasWidth");
        float canvasHeight = floatField(baked, "canvasHeight");
        int frameCount = Math.max(0, range[1] - range[0] + 1);
        Rectangle[] result = new Rectangle[frameCount];
        for (int index = 0; index < frameCount; index++) {
            Object frame = Array.get(framesArray, range[0] + index);
            result[index] = boundsOfFrame(frame, partsArray, partName, canvasWidth, canvasHeight);
        }
        return result;
    }

    private static Rectangle boundsOfFrame(
            Object frame,
            Object partsArray,
            String partName,
            float canvasWidth,
            float canvasHeight
    ) throws ReflectiveOperationException {
        int count = intField(frame, "count");
        float[] corners = (float[]) field(frame, "corners");
        int[] partIds = (int[]) field(frame, "partIds");
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int index = 0; index < count; index++) {
            Object part = Array.get(partsArray, partIds[index]);
            if (!belongsTo(part, partName)) {
                continue;
            }
            int start = index * 8;
            for (int corner = 0; corner < 8; corner += 2) {
                float x = corners[start + corner];
                float y = corners[start + corner + 1];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }
        return new Rectangle(
                minX - canvasWidth / 2f,
                minY - canvasHeight / 2f,
                maxX - minX,
                maxY - minY
        );
    }

    private static boolean belongsTo(Object part, String wanted) throws ReflectiveOperationException {
        Object current = part;
        while (current != null) {
            Object name = publicField(current, "name");
            if (wanted.equals(name)) {
                return true;
            }
            current = publicField(current, "parent");
        }
        return false;
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Object publicField(Object owner, String name) throws ReflectiveOperationException {
        return owner.getClass().getField(name).get(owner);
    }

    private static int intField(Object owner, String name) throws ReflectiveOperationException {
        return ((Number) field(owner, name)).intValue();
    }

    private static float floatField(Object owner, String name) throws ReflectiveOperationException {
        return ((Number) field(owner, name)).floatValue();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
