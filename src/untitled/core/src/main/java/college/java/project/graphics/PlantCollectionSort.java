package college.java.project.graphics;

import model.collection.PlantCollectionState;
import java.util.Comparator;
import java.util.Locale;

public enum PlantCollectionSort {
    DEFAULT("Default"), NAME("Name"), LEVEL("Level"), SUN_COST("Sun Cost"), SEED_PROGRESS("Seed Progress");
    private final String displayName;
    PlantCollectionSort(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return this.displayName; }
    public Comparator<PlantCollectionState> comparator() {
        switch (this) {
            case NAME: return Comparator.comparing(PlantCollectionSort::nameKey);
            case LEVEL: return Comparator.comparingInt(PlantCollectionState::getCurrentLevel).reversed()
                    .thenComparing(PlantCollectionSort::nameKey);
            case SUN_COST: return Comparator.comparingInt(PlantCollectionState::getSunCost)
                    .thenComparing(PlantCollectionSort::nameKey);
            case SEED_PROGRESS: return Comparator.comparingDouble(PlantCollectionSort::seedRatio).reversed()
                    .thenComparing(PlantCollectionSort::nameKey);
            default: return (left, right) -> 0;
        }
    }
    private static String nameKey(PlantCollectionState state) {
        return state == null || state.getName() == null ? "" : state.getName().toLowerCase(Locale.ROOT);
    }
    private static double seedRatio(PlantCollectionState state) {
        return state == null || state.getRequiredSeedPackets() <= 0 ? 0d
                : (double) state.getSeedPackets() / state.getRequiredSeedPackets();
    }
}
