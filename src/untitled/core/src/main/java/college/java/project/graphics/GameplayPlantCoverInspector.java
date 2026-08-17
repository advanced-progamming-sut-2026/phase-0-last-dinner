package college.java.project.graphics;

import model.Plant;
import model.mechanism.PlantCoverSystem;

import java.lang.reflect.Field;
import java.util.Map;

/** Reads visual-only plant cover state without changing the Phase 1 model contract. */
public final class GameplayPlantCoverInspector {
    private static final int MAX_FREEZE_LEVEL = 3;

    private GameplayPlantCoverInspector() {
    }

    public static State inspect(Plant plant, PlantCoverSystem coverSystem) {
        if (plant == null) {
            return State.NONE;
        }

        int freezeLevel = clampFreezeLevel(plant.getFreezeLevel());
        boolean octopus = false;
        int coverHealth = coverSystem == null ? 0 : coverSystem.getCoverHealth(plant);

        if (coverSystem != null) {
            freezeLevel = Math.max(freezeLevel, reflectedSnowballHits(coverSystem, plant));
            String coverType = reflectedCoverType(coverSystem, plant);
            if ("FROZEN".equals(coverType)) {
                freezeLevel = MAX_FREEZE_LEVEL;
            } else if ("OCTOPUS".equals(coverType)) {
                octopus = true;
            }
        }

        return new State(clampFreezeLevel(freezeLevel), octopus, Math.max(0, coverHealth));
    }

    private static int reflectedSnowballHits(PlantCoverSystem coverSystem, Plant plant) {
        Object map = fieldValue(coverSystem, "snowballHits");
        if (!(map instanceof Map<?, ?>)) {
            return 0;
        }
        Object value = ((Map<?, ?>) map).get(plant);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static String reflectedCoverType(PlantCoverSystem coverSystem, Plant plant) {
        Object map = fieldValue(coverSystem, "covers");
        if (!(map instanceof Map<?, ?>)) {
            return "";
        }
        Object cover = ((Map<?, ?>) map).get(plant);
        if (cover == null) {
            return "";
        }
        Object type = fieldValue(cover, "type");
        return type == null ? "" : type.toString();
    }

    private static Object fieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static int clampFreezeLevel(int level) {
        return Math.max(0, Math.min(MAX_FREEZE_LEVEL, level));
    }

    public static final class State {
        public static final State NONE = new State(0, false, 0);

        private final int freezeLevel;
        private final boolean octopusCovered;
        private final int coverHealth;

        private State(int freezeLevel, boolean octopusCovered, int coverHealth) {
            this.freezeLevel = freezeLevel;
            this.octopusCovered = octopusCovered;
            this.coverHealth = coverHealth;
        }

        public int getFreezeLevel() {
            return this.freezeLevel;
        }

        public boolean isOctopusCovered() {
            return this.octopusCovered;
        }

        public int getCoverHealth() {
            return this.coverHealth;
        }
    }
}
