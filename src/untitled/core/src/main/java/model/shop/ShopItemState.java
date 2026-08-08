package model.shop;

import lombok.Getter;

@Getter
public class ShopItemState {

    private final String itemId;
    private final String itemName;
    private final int purchaseAmount;
    private final int price;
    private final CurrencyType currency;
    private final String description;

    private ShopItemState(
            String itemId,
            String itemName,
            int purchaseAmount,
            int price,
            CurrencyType currency,
            String description
    ) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.purchaseAmount = purchaseAmount;
        this.price = price;
        this.currency = currency;
        this.description = description;
    }

    public static ShopItemState from(
            PermanentStuff item
    ) {
        if (item == null) {
            return null;
        }

        String displayName =
                item.name()
                        .toLowerCase()
                        .replace('_', ' ');

        return new ShopItemState(
                item.getItemId(),
                displayName,
                item.getPurchaseAmount(),
                item.getPrice(),
                item.getCurrency(),
                item.getDescription()
        );
    }
}