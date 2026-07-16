package model.Greenhouse;

import lombok.Getter;
import model.mechanism.Position;

@Getter
public class GreenhouseActionResult {

    private final GreenhouseActionStatus status;
    private final String message;
    private final Position position;
    private final String plantName;

    private final int coinsEarned;
    private final int diamondsSpent;

    private final int remainingCoins;
    private final int remainingDiamonds;

    private final boolean boostStored;

    private GreenhouseActionResult(
            GreenhouseActionStatus status,
            String message,
            Position position,
            String plantName,
            int coinsEarned,
            int diamondsSpent,
            int remainingCoins,
            int remainingDiamonds,
            boolean boostStored
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Greenhouse action status cannot be null."
            );
        }

        this.status = status;
        this.message = message == null ? "" : message;
        this.position = position;
        this.plantName = plantName;
        this.coinsEarned = Math.max(0, coinsEarned);
        this.diamondsSpent = Math.max(0, diamondsSpent);
        this.remainingCoins = Math.max(0, remainingCoins);
        this.remainingDiamonds =
                Math.max(0, remainingDiamonds);
        this.boostStored = boostStored;
    }

    public static GreenhouseActionResult planted(
            Position position,
            String plantName,
            int remainingCoins,
            int remainingDiamonds
    ) {
        return new GreenhouseActionResult(
                GreenhouseActionStatus.PLANTED,
                plantName + " was planted at "
                        + position + ".",
                position,
                plantName,
                0,
                0,
                remainingCoins,
                remainingDiamonds,
                false
        );
    }

    public static GreenhouseActionResult harvested(
            Position position,
            String plantName,
            int coinsEarned,
            boolean boostStored,
            int remainingCoins,
            int remainingDiamonds
    ) {
        String message;

        if (coinsEarned > 0) {
            message = plantName
                    + " was harvested. "
                    + coinsEarned
                    + " coins were received.";
        } else if (boostStored) {
            message = plantName
                    + " was harvested. Its boost was stored.";
        } else {
            message = plantName
                    + " was harvested. Its boost was already stored.";
        }

        return new GreenhouseActionResult(
                GreenhouseActionStatus.HARVESTED,
                message,
                position,
                plantName,
                coinsEarned,
                0,
                remainingCoins,
                remainingDiamonds,
                boostStored
        );
    }

    public static GreenhouseActionResult growthAccelerated(
            Position position,
            String plantName,
            int diamondsSpent,
            int remainingCoins,
            int remainingDiamonds
    ) {
        return new GreenhouseActionResult(
                GreenhouseActionStatus.GROWTH_ACCELERATED,
                plantName
                        + " is now ready to harvest. "
                        + diamondsSpent
                        + " diamonds were spent.",
                position,
                plantName,
                0,
                diamondsSpent,
                remainingCoins,
                remainingDiamonds,
                false
        );
    }

    public static GreenhouseActionResult failure(
            GreenhouseActionStatus status,
            String message,
            Position position,
            int remainingCoins,
            int remainingDiamonds
    ) {
        if (status == GreenhouseActionStatus.PLANTED
                || status == GreenhouseActionStatus.HARVESTED
                || status
                == GreenhouseActionStatus.GROWTH_ACCELERATED) {

            throw new IllegalArgumentException(
                    "A successful status cannot be used as a failure."
            );
        }

        return new GreenhouseActionResult(
                status,
                message,
                position,
                null,
                0,
                0,
                remainingCoins,
                remainingDiamonds,
                false
        );
    }

    public boolean isSuccessful() {
        return this.status == GreenhouseActionStatus.PLANTED
                || this.status
                == GreenhouseActionStatus.HARVESTED
                || this.status
                == GreenhouseActionStatus.GROWTH_ACCELERATED;
    }
}