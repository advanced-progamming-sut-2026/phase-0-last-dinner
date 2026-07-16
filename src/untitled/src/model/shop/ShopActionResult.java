package model.shop;

import lombok.Getter;
import model.mechanism.Position;

import java.util.Collections;
import java.util.List;

@Getter
public class ShopActionResult {

    private final ShopActionStatus status;
    private final String message;

    private final String itemId;
    private final int purchaseCount;

    private final String plantName;
    private final int seedPacketsReceived;

    private final int potsUnlocked;
    private final List<Position> unlockedPotPositions;

    private final int plantFoodReceived;

    private final int coinsSpent;
    private final int diamondsSpent;
    private final int coinsReceived;

    private final int remainingCoins;
    private final int remainingDiamonds;

    private ShopActionResult(
            ShopActionStatus status,
            String message,
            String itemId,
            int purchaseCount,
            String plantName,
            int seedPacketsReceived,
            int potsUnlocked,
            List<Position> unlockedPotPositions,
            int plantFoodReceived,
            int coinsSpent,
            int diamondsSpent,
            int coinsReceived,
            int remainingCoins,
            int remainingDiamonds
    ) {
        this.status = status;
        this.message = message;
        this.itemId = itemId;
        this.purchaseCount =
                Math.max(0, purchaseCount);
        this.plantName = plantName;
        this.seedPacketsReceived =
                Math.max(0, seedPacketsReceived);
        this.potsUnlocked =
                Math.max(0, potsUnlocked);
        this.unlockedPotPositions =
                immutablePositions(
                        unlockedPotPositions
                );
        this.plantFoodReceived =
                Math.max(0, plantFoodReceived);
        this.coinsSpent =
                Math.max(0, coinsSpent);
        this.diamondsSpent =
                Math.max(0, diamondsSpent);
        this.coinsReceived =
                Math.max(0, coinsReceived);
        this.remainingCoins =
                Math.max(0, remainingCoins);
        this.remainingDiamonds =
                Math.max(0, remainingDiamonds);
    }

    public static ShopActionResult
    potPurchased(
            String itemId,
            int purchaseCount,
            List<Position> positions,
            int coinsSpent,
            int remainingCoins,
            int remainingDiamonds
    ) {
        int unlockedCount =
                positions == null
                        ? 0
                        : positions.size();

        return new ShopActionResult(
                ShopActionStatus.PURCHASE_SUCCESS,
                unlockedCount
                        + " greenhouse pot(s) unlocked.",
                itemId,
                purchaseCount,
                null,
                0,
                unlockedCount,
                positions,
                0,
                coinsSpent,
                0,
                0,
                remainingCoins,
                remainingDiamonds
        );
    }

    public static ShopActionResult
    plantFoodPurchased(
            String itemId,
            int purchaseCount,
            int plantFoodReceived,
            int diamondsSpent,
            int remainingCoins,
            int remainingDiamonds
    ) {
        return new ShopActionResult(
                ShopActionStatus.PURCHASE_SUCCESS,
                plantFoodReceived
                        + " plant food stored "
                        + "for the next level.",
                itemId,
                purchaseCount,
                null,
                0,
                0,
                null,
                plantFoodReceived,
                0,
                diamondsSpent,
                0,
                remainingCoins,
                remainingDiamonds
        );
    }

    public static ShopActionResult
    seedPacketsPurchased(
            String itemId,
            int purchaseCount,
            String plantName,
            int seedPacketsReceived,
            int coinsSpent,
            int diamondsSpent,
            int remainingCoins,
            int remainingDiamonds
    ) {
        return new ShopActionResult(
                ShopActionStatus.PURCHASE_SUCCESS,
                seedPacketsReceived
                        + " seed packet(s) received for "
                        + plantName
                        + ".",
                itemId,
                purchaseCount,
                plantName,
                seedPacketsReceived,
                0,
                null,
                0,
                coinsSpent,
                diamondsSpent,
                0,
                remainingCoins,
                remainingDiamonds
        );
    }

    public static ShopActionResult
    currencyExchanged(
            String itemId,
            int purchaseCount,
            int diamondsSpent,
            int coinsReceived,
            int remainingCoins,
            int remainingDiamonds
    ) {
        return new ShopActionResult(
                ShopActionStatus.PURCHASE_SUCCESS,
                diamondsSpent
                        + " diamond(s) exchanged for "
                        + coinsReceived
                        + " coin(s).",
                itemId,
                purchaseCount,
                null,
                0,
                0,
                null,
                0,
                0,
                diamondsSpent,
                coinsReceived,
                remainingCoins,
                remainingDiamonds
        );
    }

    public static ShopActionResult failure(
            ShopActionStatus status,
            String message,
            String itemId,
            int purchaseCount,
            int remainingCoins,
            int remainingDiamonds
    ) {
        return new ShopActionResult(
                status,
                message,
                itemId,
                purchaseCount,
                null,
                0,
                0,
                null,
                0,
                0,
                0,
                0,
                remainingCoins,
                remainingDiamonds
        );
    }

    public boolean isSuccessful() {
        return status
                == ShopActionStatus
                .PURCHASE_SUCCESS;
    }

    private static List<Position>
    immutablePositions(
            List<Position> positions
    ) {
        if (positions == null
                || positions.isEmpty()) {

            return Collections.emptyList();
        }

        return List.copyOf(positions);
    }
}