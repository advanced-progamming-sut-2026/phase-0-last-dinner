package model.shop;

import lombok.Getter;

@Getter
public enum PermanentStuff {

    POT(
            "pot",
            1,
            2000,
            CurrencyType.COIN,
            "Unlocks one greenhouse slot; maximum 20 slots."
    ),

    NEXT_LEVEL_PLANT_FOOD(
            "plant-food",
            1,
            3,
            CurrencyType.DIAMOND,
            "Adds one plant food at the start of the next level; maximum 3."
    ),

    RANDOM_SEED_PACKET(
            "random-seed-packet",
            5,
            1000,
            CurrencyType.COIN,
            "Gives 5 seed packets for a random unlocked plant."
    ),

    SELECTED_SEED_PACKET(
            "selected-seed-packet",
            10,
            5,
            CurrencyType.DIAMOND,
            "Gives 10 seed packets for a selected unlocked plant."
    ),

    CURRENCY_EXCHANGE(
            "currency-exchange",
            500,
            5,
            CurrencyType.DIAMOND,
            "Exchanges 5 diamonds for 500 coins."
    );

    private final String itemId;
    private final int purchaseAmount;
    private final int price;
    private final CurrencyType currency;
    private final String description;

    PermanentStuff(
            String itemId,
            int purchaseAmount,
            int price,
            CurrencyType currency,
            String description
    ) {
        this.itemId = itemId;
        this.purchaseAmount = purchaseAmount;
        this.price = price;
        this.currency = currency;
        this.description = description;
    }
}