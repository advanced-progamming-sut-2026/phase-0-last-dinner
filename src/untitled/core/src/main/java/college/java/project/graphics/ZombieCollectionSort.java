package college.java.project.graphics;

import model.collection.ZombieCollectionState;

import java.util.Comparator;

public enum ZombieCollectionSort {
    DEFAULT("Default", null),
    NAME("Name", Comparator.comparing(
            ZombieCollectionState::getDisplayName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
    )),
    CHAPTER("Chapter", Comparator.comparing(
            state -> String.valueOf(state.getChapter()),
            String.CASE_INSENSITIVE_ORDER
    )),
    TOUGHNESS("Toughness", Comparator.comparingInt(ZombieCollectionState::getHitpoints).reversed()),
    SPEED("Speed", Comparator.comparingDouble(ZombieCollectionState::getSpeed).reversed());

    private final String displayName;
    private final Comparator<ZombieCollectionState> comparator;

    ZombieCollectionSort(String displayName, Comparator<ZombieCollectionState> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Comparator<ZombieCollectionState> getComparator() {
        return this.comparator;
    }
}
