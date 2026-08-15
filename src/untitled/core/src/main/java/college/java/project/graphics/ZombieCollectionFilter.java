package college.java.project.graphics;

import model.collection.ZombieCollectionState;

public enum ZombieCollectionFilter {
    ALL("All zombies") {
        @Override
        public boolean matches(ZombieCollectionState state) {
            return state != null;
        }
    },
    ENCOUNTERED("Encountered") {
        @Override
        public boolean matches(ZombieCollectionState state) {
            return state != null && state.isEncountered();
        }
    },
    UNDISCOVERED("Undiscovered") {
        @Override
        public boolean matches(ZombieCollectionState state) {
            return state != null && !state.isEncountered();
        }
    };

    private final String displayName;

    ZombieCollectionFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public abstract boolean matches(ZombieCollectionState state);
}
