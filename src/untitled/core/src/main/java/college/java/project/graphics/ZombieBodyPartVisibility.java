package college.java.project.graphics;

import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class ZombieBodyPartVisibility {
    private ZombieBodyPartVisibility() {
    }

    static Map<String, Boolean> forParticle(PamPlayer player, String pamPath, String particleName) {
        if (player == null || pamPath == null || particleName == null || particleName.isBlank()) {
            return Map.of();
        }
        try {
            PamPlayer.AnimationPart root = player.getParts(pamPath);
            PamPlayer.AnimationPart wanted = find(root, particleName);
            if (wanted == null) {
                return Map.of();
            }
            Map<String, Boolean> visibility = new HashMap<>();
            disableAll(root, visibility);
            enablePath(root, wanted, visibility);
            enableSubtree(wanted, visibility);
            return visibility;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static PamPlayer.AnimationPart find(PamPlayer.AnimationPart part, String wanted) {
        if (part == null) {
            return null;
        }
        if (normalize(part.name).equals(normalize(wanted))) {
            return part;
        }
        for (PamPlayer.AnimationPart child : part.children) {
            PamPlayer.AnimationPart found = find(child, wanted);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean enablePath(
            PamPlayer.AnimationPart current,
            PamPlayer.AnimationPart wanted,
            Map<String, Boolean> visibility
    ) {
        if (current == null) {
            return false;
        }
        if (current == wanted) {
            if (current.name != null && !current.name.isBlank()) {
                visibility.put(current.name, Boolean.TRUE);
            }
            return true;
        }
        for (PamPlayer.AnimationPart child : current.children) {
            if (enablePath(child, wanted, visibility)) {
                if (current.name != null && !current.name.isBlank()) {
                    visibility.put(current.name, Boolean.TRUE);
                }
                return true;
            }
        }
        return false;
    }

    private static void enableSubtree(PamPlayer.AnimationPart part, Map<String, Boolean> visibility) {
        if (part == null) {
            return;
        }
        if (part.name != null && !part.name.isBlank()) {
            visibility.put(part.name, Boolean.TRUE);
        }
        for (PamPlayer.AnimationPart child : part.children) {
            enableSubtree(child, visibility);
        }
    }

    private static void disableAll(PamPlayer.AnimationPart part, Map<String, Boolean> visibility) {
        if (part == null) {
            return;
        }
        if (part.name != null && !part.name.isBlank()) {
            visibility.put(part.name, Boolean.FALSE);
        }
        for (PamPlayer.AnimationPart child : part.children) {
            disableAll(child, visibility);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
