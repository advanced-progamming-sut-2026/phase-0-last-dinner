package view.shop;

import lombok.Getter;
import lombok.Setter;
import model.mechanism.Position;
import model.shop.DailyOfferResult;
import model.shop.ShopActionResult;
import model.shop.ShopCatalogResult;
import model.shop.ShopItemState;
import view.CommandHandler;

import java.util.regex.Matcher;

@Setter
@Getter
public class ShopView implements CommandHandler {

    private ShopViewObserver observer;

    @Override
    public void handleCommand(String input) {
        if (observer == null) {
            System.out.println(
                    "Shop controller is not connected."
            );
            return;
        }
        Matcher matcher;
        matcher = ShopCommands.SHOP_LIST.getMatcher(input);
        if (matcher != null) {
            showShopList();
            return;
        }
        matcher = ShopCommands.SHOP_DAILY.getMatcher(input);
        if (matcher != null) {
            showDailyOffer();
            return;
        }
        matcher = ShopCommands.SHOP_BUY.getMatcher(input);
        if (matcher != null) {
            handleBuy(matcher);
            return;
        }
        System.out.println("Invalid shop command.");
    }

    private void showShopList() {
        ShopCatalogResult catalog = observer.onShopListRequested();
        if (catalog == null) {
            System.out.println("Shop catalog is not available.");
            return;
        }
        System.out.println("Shop items");
        System.out.println("Coins: " + catalog.getCoins() + " | Diamonds: " + catalog.getDiamonds());
        System.out.println("----------------------------------------");
        if (catalog.getItems().isEmpty()) {
            System.out.println("No permanent items are available.");
            return;
        }
        for (ShopItemState item : catalog.getItems()) {
            printShopItem(item);
        }
    }

    private void printShopItem(ShopItemState item) {
        if (item == null)
            return;
        System.out.println("ID: " + item.getItemId());
        System.out.println("Name: " + item.getItemName());
        System.out.println("Purchase amount: " + item.getPurchaseAmount());
        System.out.println("Price: " + item.getPrice() + " " + item.getCurrency());
        System.out.println("Description: " + item.getDescription());
        System.out.println("----------------------------------------");
    }

    private void showDailyOffer() {
        DailyOfferResult result = observer.onDailyOfferRequested();
        if (result == null || !result.isAvailable()) {
            System.out.println("Daily offer is not available.");
            if (result != null)
                printWallet(result.getCoins(), result.getDiamonds());
            return;
        }
        System.out.println("Daily offer");
        System.out.println("ID: " + result.getOfferId());
        System.out.println("Date: " + result.getOfferDate());
        System.out.println("Plant: " + result.getPlantName());
        System.out.println("Seed packets: " + result.getSeedPacketAmount());
        System.out.println("Base price: " + result.getBasePrice() + " " + result.getCurrency());
        System.out.println("Discount: " + result.getDiscountPercent() + "%");
        System.out.println("Final price: " + result.getFinalPrice() + " " + result.getCurrency());
        if (result.isPurchased())
            System.out.println("Status: already purchased");
        else if (result.isPurchasable())
            System.out.println("Status: available");
        else
            System.out.println("Status: unavailable");
        printWallet(result.getCoins(), result.getDiamonds());
    }

    private void handleBuy(Matcher matcher) {
        String itemId = matcher.group("itemId");
        String countText = matcher.group("count");
        String plantType = matcher.group("plantType");
        int count;
        try {
            count = Integer.parseInt(countText);
        } catch (NumberFormatException exception) {
            System.out.println("Purchase count is invalid.");
            return;
        }
        ShopActionResult result =
                observer.onBuyRequested(
                        itemId,
                        count,
                        plantType
                );
        printActionResult(result);
    }

    private void printActionResult(
            ShopActionResult result
    ) {
        if (result == null) {
            System.out.println("Shop purchase failed.");
            return;
        }
        System.out.println(result.getMessage());
        if (!result.isSuccessful()) {
            printWallet(
                    result.getRemainingCoins(),
                    result.getRemainingDiamonds()
            );
            return;
        }
        if (!result
                .getUnlockedPotPositions()
                .isEmpty()) {
            System.out.println("Unlocked positions:");
            for (Position position
                    : result
                    .getUnlockedPotPositions()) {
                System.out.println("- " + position);
            }
        }
        if (result.getSeedPacketsReceived() > 0) {
            System.out.println("Seed packets received: " + result.getSeedPacketsReceived());
            System.out.println("Plant: " + result.getPlantName());
        }
        if (result.getPlantFoodReceived() > 0) {
            System.out.println("Plant food stored: " + result.getPlantFoodReceived());
        }
        if (result.getCoinsReceived() > 0) {
            System.out.println("Coins received: " + result.getCoinsReceived());
        }
        printWallet(
                result.getRemainingCoins(),
                result.getRemainingDiamonds()
        );
    }

    private void printWallet(int coins, int diamonds) {
        System.out.println("Coins: " + coins + " | Diamonds: " + diamonds);
    }
}
