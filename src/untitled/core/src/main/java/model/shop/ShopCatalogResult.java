package model.shop;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ShopCatalogResult {
    private final List<ShopItemState> items;
    private final int coins;
    private final int diamonds;

    private ShopCatalogResult(
            List<ShopItemState> items,
            int coins,
            int diamonds
    ) {
        this.items = immutableItems(items);
        this.coins = Math.max(0, coins);
        this.diamonds = Math.max(0, diamonds);
    }

    public static ShopCatalogResult from(
            Shop shop,
            int coins,
            int diamonds
    ) {
        List<ShopItemState> states =
                new ArrayList<>();

        if (shop != null) {
            for (PermanentStuff item
                    : shop.getPermanentItems()) {

                ShopItemState state =
                        ShopItemState.from(item);

                if (state != null) {
                    states.add(state);
                }
            }
        }

        return new ShopCatalogResult(
                states,
                coins,
                diamonds
        );
    }

    private static List<ShopItemState>
    immutableItems(
            List<ShopItemState> items
    ) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(items)
        );
    }
}