package model.shop;

import lombok.Getter;

@Getter
public enum DailyOffer {
    DISCOUNTED_SEED_PACKET(
            10,
            2000,
            20,
            1600,
            CurrencyType.COIN
    );

    private final int seedPacketAmount;
    private final int basePrice;
    private final int discountPercent;
    private final int finalPrice;
    private final CurrencyType currency;

    DailyOffer(
            int seedPacketAmount,
            int basePrice,
            int discountPercent,
            int finalPrice,
            CurrencyType currency
    ) {
        this.seedPacketAmount = seedPacketAmount;
        this.basePrice = basePrice;
        this.discountPercent = discountPercent;
        this.finalPrice = finalPrice;
        this.currency = currency;
    }

}
