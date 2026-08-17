package college.java.project.graphics;

import model.collection.ZombieCollectionState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Preview/test data source. It does not mutate model state. */
public final class StaticZombieCollectionDataSource implements ZombieCollectionDataSource {
    private final List<ZombieCollectionState> zombies;
    private final int mintCount;
    private final int gemCount;
    private final int coinCount;

    public StaticZombieCollectionDataSource(List<ZombieCollectionState> zombies) {
        this(zombies, 0, 0, 0);
    }

    public StaticZombieCollectionDataSource(
            List<ZombieCollectionState> zombies,
            int mintCount,
            int gemCount,
            int coinCount
    ) {
        this.zombies = zombies == null ? Collections.emptyList() : new ArrayList<>(zombies);
        this.mintCount = Math.max(0, mintCount);
        this.gemCount = Math.max(0, gemCount);
        this.coinCount = Math.max(0, coinCount);
    }

    @Override
    public List<ZombieCollectionState> loadZombies() {
        return Collections.unmodifiableList(this.zombies);
    }

    @Override
    public int getMintCount() {
        return this.mintCount;
    }

    @Override
    public int getGemCount() {
        return this.gemCount;
    }

    @Override
    public int getCoinCount() {
        return this.coinCount;
    }
}
