package model.collection;

import lombok.Getter;

@Getter
public class CollectionActionResult {
    private final CollectionActionStatus status;
    private final String message;
    private final String plantName;
    private final int previousLevel;
    private final int currentLevel;
    private final int remainingSeedPackets;
    private final int remainingGold;
    private final int spentCoins;

    public CollectionActionResult(
            CollectionActionStatus status,
            String message,
            String plantName,
            int previousLevel,
            int currentLevel,
            int remainingSeedPackets,
            int remainingGold,
            int spentCoins
    ) {
        this.status = status == null
                ? CollectionActionStatus.INVALID
                : status;
        this.message = message == null ? "" : message;
        this.plantName = plantName;
        this.previousLevel = Math.max(0, previousLevel);
        this.currentLevel = Math.max(0, currentLevel);
        this.remainingSeedPackets = Math.max(0, remainingSeedPackets);
        this.remainingGold = Math.max(0, remainingGold);
        this.spentCoins = Math.max(0, spentCoins);
    }

    public boolean isSuccessful() {
        return this.status == CollectionActionStatus.SUCCESS;
    }

    public static CollectionActionResult plantPurchased(
            String plantName,
            int remainingGold
    ) {
        return new CollectionActionResult(
                CollectionActionStatus.SUCCESS,
                plantName + " purchased successfully.",
                plantName,
                1,
                1,
                0,
                remainingGold,
                2000
        );
    }

    public static CollectionActionResult plantUpgraded(
            String plantName,
            int previousLevel,
            int currentLevel,
            int remainingSeedPackets,
            int remainingGold,
            int spentCoins
    ) {
        return new CollectionActionResult(
                CollectionActionStatus.SUCCESS,
                plantName + " upgraded to level " + currentLevel + ".",
                plantName,
                previousLevel,
                currentLevel,
                remainingSeedPackets,
                remainingGold,
                spentCoins
        );
    }

    public static CollectionActionResult failure(
            CollectionActionStatus status,
            String message,
            String plantName,
            int currentLevel,
            int remainingSeedPackets,
            int remainingGold
    ) {
        return new CollectionActionResult(
                status,
                message,
                plantName,
                currentLevel,
                currentLevel,
                remainingSeedPackets,
                remainingGold,
                0
        );
    }
}