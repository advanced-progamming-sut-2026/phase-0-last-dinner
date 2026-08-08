package model.collection;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class CollectionStateResult {
    private final CollectionActionStatus status;
    private final String message;
    private final List<PlantCollectionState> plants;
    private final List<ZombieCollectionState> zombies;
    private final int gold;

    public CollectionStateResult(
            CollectionActionStatus status,
            String message,
            List<PlantCollectionState> plants,
            List<ZombieCollectionState> zombies,
            int gold
    ) {
        this.status = status == null
                ? CollectionActionStatus.INVALID
                : status;
        this.message = message == null ? "" : message;
        this.plants = plants == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<>(plants)
                );
        this.zombies = zombies == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<>(zombies)
                );
        this.gold = Math.max(0, gold);
    }

    public boolean isSuccessful() {
        return this.status == CollectionActionStatus.SUCCESS;
    }

    public static CollectionStateResult plants(
            List<PlantCollectionState> plants,
            int gold
    ) {
        return new CollectionStateResult(
                CollectionActionStatus.SUCCESS,
                "Plant collection loaded.",
                plants,
                Collections.emptyList(),
                gold
        );
    }

    public static CollectionStateResult zombies(
            List<ZombieCollectionState> zombies,
            int gold
    ) {
        return new CollectionStateResult(
                CollectionActionStatus.SUCCESS,
                "Zombie collection loaded.",
                Collections.emptyList(),
                zombies,
                gold
        );
    }

    public static CollectionStateResult failure(
            CollectionActionStatus status,
            String message,
            int gold
    ) {
        return new CollectionStateResult(
                status,
                message,
                Collections.emptyList(),
                Collections.emptyList(),
                gold
        );
    }
}
