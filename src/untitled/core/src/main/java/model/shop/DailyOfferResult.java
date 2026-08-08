package model.shop;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyOfferResult {

    private final boolean available;

    private final String offerId;
    private final LocalDate offerDate;
    private final String plantName;

    private final int seedPacketAmount;
    private final int basePrice;
    private final int discountPercent;
    private final int finalPrice;

    private final CurrencyType currency;

    private final boolean purchased;
    private final boolean purchasable;

    private final int coins;
    private final int diamonds;

    private DailyOfferResult(
            boolean available,
            String offerId,
            LocalDate offerDate,
            String plantName,
            int seedPacketAmount,
            int basePrice,
            int discountPercent,
            int finalPrice,
            CurrencyType currency,
            boolean purchased,
            boolean purchasable,
            int coins,
            int diamonds
    ) {
        this.available = available;
        this.offerId = offerId;
        this.offerDate = offerDate;
        this.plantName = plantName;
        this.seedPacketAmount =
                Math.max(0, seedPacketAmount);
        this.basePrice =
                Math.max(0, basePrice);
        this.discountPercent =
                Math.max(0, discountPercent);
        this.finalPrice =
                Math.max(0, finalPrice);
        this.currency = currency;
        this.purchased = purchased;
        this.purchasable = purchasable;
        this.coins = Math.max(0, coins);
        this.diamonds = Math.max(0, diamonds);
    }

    public static DailyOfferResult from(
            DailyOfferState state,
            LocalDate currentDate,
            int coins,
            int diamonds
    ) {
        if (state == null
                || state.getOffer() == null) {

            return unavailable(
                    coins,
                    diamonds
            );
        }

        DailyOffer offer = state.getOffer();

        return new DailyOfferResult(
                true,
                state.getOfferId(),
                state.getOfferDate(),
                state.getPlantName(),
                offer.getSeedPacketAmount(),
                offer.getBasePrice(),
                offer.getDiscountPercent(),
                offer.getFinalPrice(),
                offer.getCurrency(),
                state.isPurchased(),
                state.canPurchase(currentDate),
                coins,
                diamonds
        );
    }

    public static DailyOfferResult unavailable(
            int coins,
            int diamonds
    ) {
        return new DailyOfferResult(
                false,
                null,
                null,
                null,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                coins,
                diamonds
        );
    }
}