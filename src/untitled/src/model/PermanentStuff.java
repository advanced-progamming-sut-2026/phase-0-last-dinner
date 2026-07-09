package model;

import lombok.Getter;

@Getter
public enum PermanentStuff {
    POT(
            1,
            2000,
            CurrencyType.COIN,
            "Unlocks one greenhouse slot; maximum 20 slots."
    ),
    NEXT_LEVEL_PLANT_FOOD(
            1,
            3,
            CurrencyType.DIAMOND,
            "Adds one plant food at the start of the next level; maximum 3."
    ),
    RANDOM_SEED_PACKET(
            5,
            1000,
            CurrencyType.COIN,
            "Gives 5 seed packets for a random unlocked plant."
    ),
    SELECTED_SEED_PACKET(
            10,
            5,
            CurrencyType.DIAMOND,
            "Gives 10 seed packets for a selected unlocked plant."
    ),
    CURRENCY_EXCHANGE(
            500,
            5,
            CurrencyType.DIAMOND,
            "Exchanges 5 diamonds for 500 coins."
    );

    private final int purchaseAmount;
    private final int price;
    private final CurrencyType currency;
    private final String description;

    PermanentStuff(
            int purchaseAmount,
            int price,
            CurrencyType currency,
            String description
    ) {
        this.purchaseAmount = purchaseAmount;
        this.price = price;
        this.currency = currency;
        this.description = description;
    }

}
