package college.java.project.graphics;

import model.zombie.ArmorType;
import model.zombie.ZombieArmor;
import pvz.libpvz.pam.PamPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Selects embedded PAM armor parts while keeping one shared zombie transform. */
final class ZombieArmorVisibility {
    private ZombieArmorVisibility() {
    }

    static Map<String, Boolean> forArmor(PamPlayer player, String pamPath, ZombieArmor armor) {
        return forArmors(player, pamPath, armor == null ? List.of() : List.of(armor));
    }

    static Map<String, Boolean> forArmors(PamPlayer player, String pamPath, List<ZombieArmor> armors) {
        if (player == null || pamPath == null) {
            return Map.of();
        }
        try {
            PamPlayer.AnimationPart root = player.getParts(pamPath);
            Map<String, Boolean> result = new HashMap<>();
            disableArmorVariants(root, result);
            if (armors != null) {
                for (ZombieArmor armor : armors) {
                    enableArmor(root, armor, result);
                }
            }
            return result;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    static Map<String, Boolean> forDroppedArmor(PamPlayer player, String pamPath, ZombieArmor armor) {
        if (player == null || pamPath == null || armor == null || armor.getDefinition() == null
                || armor.getDefinition().getType() == null) {
            return Map.of();
        }
        try {
            PamPlayer.AnimationPart root = player.getParts(pamPath);
            Map<String, Boolean> result = new HashMap<>();
            disableAllParts(root, result);
            List<PartPath> candidates = new ArrayList<>();
            collectMatches(root, selectors(armor.getDefinition().getType()), new ArrayList<>(), candidates);
            PartPath best = bestVariant(candidates, "damage_02");
            if (best == null) {
                best = bestGeneric(candidates);
            }
            if (best == null) {
                return Map.of();
            }
            for (String name : best.path) {
                result.put(name, Boolean.TRUE);
            }
            return result;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    static Map<String, Boolean> forPulledArmor(PamPlayer player, String pamPath, ZombieArmor armor) {
        if (player == null || pamPath == null || armor == null || armor.getDefinition() == null
                || armor.getDefinition().getType() == null) {
            return Map.of();
        }
        try {
            PamPlayer.AnimationPart root = player.getParts(pamPath);
            Map<String, Boolean> result = new HashMap<>();
            disableAllParts(root, result);
            List<PartPath> candidates = new ArrayList<>();
            collectMatches(root, selectors(armor.getDefinition().getType()), new ArrayList<>(), candidates);
            PartPath best = bestVariant(candidates, "norm");
            if (best == null) {
                best = bestGeneric(candidates);
            }
            if (best == null) {
                return Map.of();
            }
            for (String name : best.path) {
                result.put(name, Boolean.TRUE);
            }
            return result;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static void disableAllParts(PamPlayer.AnimationPart part, Map<String, Boolean> result) {
        if (part == null) {
            return;
        }
        if (part.name != null && !part.name.isBlank()) {
            result.put(part.name, Boolean.FALSE);
        }
        for (PamPlayer.AnimationPart child : part.children) {
            disableAllParts(child, result);
        }
    }

    private static void disableArmorVariants(PamPlayer.AnimationPart part, Map<String, Boolean> result) {
        if (part == null) {
            return;
        }
        String name = normalized(part.name);
        if (!part.children.isEmpty() && isArmorPart(name)) {
            result.put(part.name, Boolean.FALSE);
        }
        for (PamPlayer.AnimationPart child : part.children) {
            disableArmorVariants(child, result);
        }
    }

    private static void enableArmor(
            PamPlayer.AnimationPart root,
            ZombieArmor armor,
            Map<String, Boolean> result
    ) {
        if (armor == null || armor.isDestroyed() || armor.isDropped() || armor.getDefinition() == null
                || armor.getDefinition().getType() == null) {
            return;
        }
        List<String> selectors = selectors(armor.getDefinition().getType());
        if (selectors.isEmpty()) {
            return;
        }
        String wantedVariant = variant(armor);
        List<PartPath> candidates = new ArrayList<>();
        collectMatches(root, selectors, new ArrayList<>(), candidates);
        PartPath best = bestVariant(candidates, wantedVariant);
        if (best == null) {
            best = bestGeneric(candidates);
        }
        if (best == null) {
            return;
        }
        for (String name : best.path) {
            result.put(name, Boolean.TRUE);
        }
    }

    private static void collectMatches(
            PamPlayer.AnimationPart part,
            List<String> selectors,
            List<String> path,
            List<PartPath> matches
    ) {
        if (part == null) {
            return;
        }
        List<String> nextPath = path;
        if (part.name != null && !part.name.isBlank()) {
            nextPath = new ArrayList<>(path);
            nextPath.add(part.name);
            String name = normalized(part.name);
            if (!part.children.isEmpty() && matchesSelector(name, selectors)) {
                matches.add(new PartPath(name, nextPath));
            }
        }
        for (PamPlayer.AnimationPart child : part.children) {
            collectMatches(child, selectors, nextPath, matches);
        }
    }

    private static PartPath bestVariant(List<PartPath> candidates, String wanted) {
        for (PartPath candidate : candidates) {
            if (variantOf(candidate.normalizedName).equals(wanted)) {
                return candidate;
            }
        }
        if ("damage_02".equals(wanted)) {
            for (PartPath candidate : candidates) {
                if ("damage_01".equals(variantOf(candidate.normalizedName))) {
                    return candidate;
                }
            }
        }
        for (PartPath candidate : candidates) {
            if ("norm".equals(variantOf(candidate.normalizedName))) {
                return candidate;
            }
        }
        return null;
    }

    private static PartPath bestGeneric(List<PartPath> candidates) {
        for (PartPath candidate : candidates) {
            if (variantOf(candidate.normalizedName).isEmpty()) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean isArmorPart(String name) {
        for (ArmorType type : ArmorType.values()) {
            if (matchesSelector(name, selectors(type))) {
                return true;
            }
        }
        return false;
    }


    private static boolean matchesSelector(String name, List<String> selectors) {
        for (String selector : selectors) {
            if (name.contains(selector)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> selectors(ArmorType type) {
        return switch (type) {
            case CONE -> List.of("armor_cone", "cone");
            case BUCKET -> List.of("armor_bucket", "bucket");
            case BRICK -> List.of("armor_brick", "brick");
            case SHOULDER_ARMOR -> List.of("shoulder_armor", "shoulderarmor");
            case CROWN -> List.of("armor_crown", "crown");
            case NEWSPAPER -> List.of("newspaper", "paper");
            case ICE_BLOCK -> List.of("ice_block", "iceblock");
            case SARCOPHAGUS -> List.of("sarcophagus");
            case SURFBOARD -> List.of("surf_board", "surfboard");
        };
    }

    private static String variant(ZombieArmor armor) {
        int base = Math.max(1, armor.getDefinition().getBaseHealth());
        float health = Math.max(0, armor.getCurrentHealth()) / (float) base;
        if (health > 0.66f) {
            return "norm";
        }
        if (health > 0.33f) {
            return "damage_01";
        }
        return "damage_02";
    }

    private static String variantOf(String name) {
        if (name.contains("damage_02") || name.contains("damage02") || name.contains("damage2")
                || name.contains("dmg2")) {
            return "damage_02";
        }
        if (name.contains("damage_01") || name.contains("damage01") || name.contains("damage1")
                || name.contains("dmg1")) {
            return "damage_01";
        }
        if (name.contains("norm") || name.contains("normal")) {
            return "norm";
        }
        return "";
    }

    private static String normalized(String value) {
        if (value == null) {
            return "";
        }
        int separator = value.indexOf('|');
        String partName = separator < 0 ? value : value.substring(0, separator);
        return partName.toLowerCase(Locale.ROOT).trim();
    }

    private static final class PartPath {
        private final String normalizedName;
        private final List<String> path;

        private PartPath(String normalizedName, List<String> path) {
            this.normalizedName = normalizedName;
            this.path = path;
        }
    }
}
