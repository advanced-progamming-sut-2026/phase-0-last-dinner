package view.shop;

import model.shop.DailyOfferResult;
import model.shop.ShopActionResult;
import model.shop.ShopCatalogResult;

public interface ShopViewObserver {

    ShopCatalogResult onShopListRequested();

    DailyOfferResult onDailyOfferRequested();

    ShopActionResult onBuyRequested(String itemId, int count, String plantType);
}