package controller;

import model.Greenhouse.Greenhouse;
import model.Greenhouse.GreenhouseBoard;
import model.Greenhouse.Pot;
import model.Plant;
import model.User.User;
import model.mechanism.Position;
import model.plant.PlantUpgradeService;
import model.shop.DailyOffer;
import model.shop.DailyOfferResult;
import model.shop.DailyOfferState;
import model.shop.PermanentStuff;
import model.shop.Shop;
import model.shop.ShopActionResult;
import model.shop.ShopActionStatus;
import model.shop.ShopCatalogResult;
import view.shop.ShopView;
import view.shop.ShopViewObserver;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShopController
        implements ShopViewObserver {

    private static final int MAXIMUM_PLANT_FOOD = 3;

    private final User user;
    private final Shop shop;
    private final PlantUpgradeService plantUpgradeService;
    private final Random random;
    private final Clock clock;

    public ShopController(
            ShopView view,
            User user,
            PlantUpgradeService plantUpgradeService
    ) {
        this(
                view,
                user,
                plantUpgradeService,
                new Random(),
                Clock.systemDefaultZone()
        );
    }

    public ShopController(
            ShopView view,
            User user,
            PlantUpgradeService plantUpgradeService,
            Random random,
            Clock clock
    ) {
        if (view == null) {
            throw new IllegalArgumentException(
                    "Shop view cannot be null."
            );
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null."
            );
        }

        if (plantUpgradeService == null) {
            throw new IllegalArgumentException(
                    "Plant upgrade service cannot be null."
            );
        }

        if (random == null || clock == null) {
            throw new IllegalArgumentException(
                    "Random and clock cannot be null."
            );
        }

        user.initializeMissingFields();

        this.user = user;
        this.shop = user.getShop();
        this.plantUpgradeService =
                plantUpgradeService;
        this.random = random;
        this.clock = clock;

        view.setObserver(this);
    }

    @Override
    public ShopCatalogResult
    onShopListRequested() {
        return ShopCatalogResult.from(
                shop,
                user.getGold(),
                user.getDiamond()
        );
    }

    @Override
    public DailyOfferResult
    onDailyOfferRequested() {
        LocalDate currentDate =
                LocalDate.now(clock);

        DailyOfferState state =
                shop.getOrRefreshDailyOffer(
                        currentDate,
                        user.getUnlockedPlants(),
                        random
                );

        return DailyOfferResult.from(
                state,
                currentDate,
                user.getGold(),
                user.getDiamond()
        );
    }

    @Override
    public ShopActionResult onBuyRequested(
            String itemId,
            int count,
            String plantType
    ) {
        if (itemId == null
                || itemId.trim().isEmpty()) {

            return failure(
                    ShopActionStatus.INVALID_ITEM,
                    "Item id is required.",
                    itemId,
                    count
            );
        }

        if (count <= 0) {
            return failure(
                    ShopActionStatus.INVALID_COUNT,
                    "Purchase count must be positive.",
                    itemId,
                    count
            );
        }

        LocalDate currentDate =
                LocalDate.now(clock);

        DailyOfferState dailyOfferState =
                shop.getOrRefreshDailyOffer(
                        currentDate,
                        user.getUnlockedPlants(),
                        random
                );

        if (isDailyOfferId(itemId)) {
            return buyDailyOffer(
                    itemId,
                    count,
                    dailyOfferState,
                    currentDate
            );
        }

        PermanentStuff item =
                shop.findPermanentItemById(
                        itemId
                );

        if (item == null) {
            return failure(
                    ShopActionStatus.INVALID_ITEM,
                    "Shop item was not found.",
                    itemId,
                    count
            );
        }

        switch (item) {
            case POT:
                return buyPots(item, count);
            case NEXT_LEVEL_PLANT_FOOD:
                return buyPlantFood(
                    item,
                    count
                );
            case RANDOM_SEED_PACKET:
                return buyRandomSeedPackets(
                    item,
                    count
                );
            case SELECTED_SEED_PACKET:
                return buySelectedSeedPackets(
                    item,
                    count,
                    plantType
                );
            case CURRENCY_EXCHANGE:
                return exchangeCurrency(
                    item,
                    count
                );
            default:
                return failure(
                    ShopActionStatus.PURCHASE_FAILED,
                    "This item cannot be purchased.",
                    itemId,
                    count
                );
        }
    }

    private ShopActionResult buyPots(
            PermanentStuff item,
            int count
    ) {
        Greenhouse greenhouse =
                user.getGreenhouse();

        if (greenhouse == null) {
            return failure(
                    ShopActionStatus.SHOP_NOT_AVAILABLE,
                    "Greenhouse is not available.",
                    item.getItemId(),
                    count
            );
        }

        int unlockedPotCount =
                greenhouse.getBoard()
                        .getUnlockedPotCount();

        int lockedPotCount =
                GreenhouseBoard
                        .MAXIMUM_POT_COUNT
                        - unlockedPotCount;

        if (count > lockedPotCount) {
            return failure(
                    ShopActionStatus
                            .MAXIMUM_CAPACITY_REACHED,
                    "Not enough locked greenhouse pots.",
                    item.getItemId(),
                    count
            );
        }

        Integer totalPrice =
                safeMultiply(
                        item.getPrice(),
                        count
                );

        if (totalPrice == null) {
            return valueTooLarge(
                    item.getItemId(),
                    count
            );
        }

        if (user.getGold() < totalPrice) {
            return failure(
                    ShopActionStatus.NOT_ENOUGH_COINS,
                    "Not enough coins. Required: "
                            + totalPrice,
                    item.getItemId(),
                    count
            );
        }

        List<Position> unlockedPositions =
                new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Pot unlockedPot =
                    greenhouse.unlockNextPot();

            if (unlockedPot == null) {
                return failure(
                        ShopActionStatus.PURCHASE_FAILED,
                        "Could not unlock greenhouse pot.",
                        item.getItemId(),
                        count
                );
            }

            unlockedPositions.add(
                    unlockedPot.getPosition()
            );
        }

        user.setGold(
                user.getGold() - totalPrice
        );

        return ShopActionResult.potPurchased(
                item.getItemId(),
                count,
                unlockedPositions,
                totalPrice,
                user.getGold(),
                user.getDiamond()
        );
    }

    private ShopActionResult buyPlantFood(
            PermanentStuff item,
            int count
    ) {
        Integer plantFoodAmount =
                safeMultiply(
                        item.getPurchaseAmount(),
                        count
                );

        Integer totalPrice =
                safeMultiply(
                        item.getPrice(),
                        count
                );

        if (plantFoodAmount == null
                || totalPrice == null) {

            return valueTooLarge(
                    item.getItemId(),
                    count
            );
        }

        long newAmount =
                (long) user.getNextLevelPlantFood()
                        + plantFoodAmount;

        if (newAmount > MAXIMUM_PLANT_FOOD) {
            return failure(
                    ShopActionStatus
                            .MAXIMUM_CAPACITY_REACHED,
                    "Maximum stored plant food is 3.",
                    item.getItemId(),
                    count
            );
        }

        if (user.getDiamond() < totalPrice) {
            return failure(
                    ShopActionStatus
                            .NOT_ENOUGH_DIAMONDS,
                    "Not enough diamonds. Required: "
                            + totalPrice,
                    item.getItemId(),
                    count
            );
        }

        user.setDiamond(
                user.getDiamond() - totalPrice
        );

        user.setNextLevelPlantFood(
                (int) newAmount
        );

        return ShopActionResult
                .plantFoodPurchased(
                        item.getItemId(),
                        count,
                        plantFoodAmount,
                        totalPrice,
                        user.getGold(),
                        user.getDiamond()
                );
    }

    private ShopActionResult
    buyRandomSeedPackets(
            PermanentStuff item,
            int count
    ) {
        List<Plant> unlockedPlants =
                getEligibleUnlockedPlants();

        if (unlockedPlants.isEmpty()) {
            return failure(
                    ShopActionStatus
                            .NO_UNLOCKED_PLANTS,
                    "No unlocked plant is available.",
                    item.getItemId(),
                    count
            );
        }

        Integer seedPacketAmount =
                safeMultiply(
                        item.getPurchaseAmount(),
                        count
                );

        Integer totalPrice =
                safeMultiply(
                        item.getPrice(),
                        count
                );

        if (seedPacketAmount == null
                || totalPrice == null) {

            return valueTooLarge(
                    item.getItemId(),
                    count
            );
        }

        if (user.getGold() < totalPrice) {
            return failure(
                    ShopActionStatus.NOT_ENOUGH_COINS,
                    "Not enough coins. Required: "
                            + totalPrice,
                    item.getItemId(),
                    count
            );
        }

        Plant selectedPlant =
                unlockedPlants.get(
                        random.nextInt(
                                unlockedPlants.size()
                        )
                );

        plantUpgradeService.addSeedPackets(
                selectedPlant.getName(),
                seedPacketAmount
        );

        user.setGold(
                user.getGold() - totalPrice
        );

        return ShopActionResult
                .seedPacketsPurchased(
                        item.getItemId(),
                        count,
                        selectedPlant.getName(),
                        seedPacketAmount,
                        totalPrice,
                        0,
                        user.getGold(),
                        user.getDiamond()
                );
    }

    private ShopActionResult
    buySelectedSeedPackets(
            PermanentStuff item,
            int count,
            String plantType
    ) {
        String cleanPlantType =
                removeQuotes(plantType);

        if (cleanPlantType == null
                || cleanPlantType.isEmpty()) {

            return failure(
                    ShopActionStatus
                            .PLANT_TYPE_REQUIRED,
                    "Plant type is required for "
                            + "selected seed packets.",
                    item.getItemId(),
                    count
            );
        }

        Plant selectedPlant =
                findUnlockedPlant(
                        cleanPlantType
                );

        if (selectedPlant == null) {
            return failure(
                    ShopActionStatus
                            .PLANT_NOT_UNLOCKED,
                    "Selected plant is not unlocked.",
                    item.getItemId(),
                    count
            );
        }

        Integer seedPacketAmount =
                safeMultiply(
                        item.getPurchaseAmount(),
                        count
                );

        Integer totalPrice =
                safeMultiply(
                        item.getPrice(),
                        count
                );

        if (seedPacketAmount == null
                || totalPrice == null) {

            return valueTooLarge(
                    item.getItemId(),
                    count
            );
        }

        if (user.getDiamond() < totalPrice) {
            return failure(
                    ShopActionStatus
                            .NOT_ENOUGH_DIAMONDS,
                    "Not enough diamonds. Required: "
                            + totalPrice,
                    item.getItemId(),
                    count
            );
        }

        plantUpgradeService.addSeedPackets(
                selectedPlant.getName(),
                seedPacketAmount
        );

        user.setDiamond(
                user.getDiamond() - totalPrice
        );

        return ShopActionResult
                .seedPacketsPurchased(
                        item.getItemId(),
                        count,
                        selectedPlant.getName(),
                        seedPacketAmount,
                        0,
                        totalPrice,
                        user.getGold(),
                        user.getDiamond()
                );
    }

    private ShopActionResult exchangeCurrency(
            PermanentStuff item,
            int count
    ) {
        Integer coinsReceived =
                safeMultiply(
                        item.getPurchaseAmount(),
                        count
                );

        Integer diamondsSpent =
                safeMultiply(
                        item.getPrice(),
                        count
                );

        if (coinsReceived == null
                || diamondsSpent == null) {

            return valueTooLarge(
                    item.getItemId(),
                    count
            );
        }

        if (user.getDiamond()
                < diamondsSpent) {

            return failure(
                    ShopActionStatus
                            .NOT_ENOUGH_DIAMONDS,
                    "Not enough diamonds. Required: "
                            + diamondsSpent,
                    item.getItemId(),
                    count
            );
        }

        long newCoinAmount =
                (long) user.getGold()
                        + coinsReceived;

        if (newCoinAmount
                > Integer.MAX_VALUE) {

            return valueTooLarge(
                    item.getItemId(),
                    count
            );
        }

        user.setDiamond(
                user.getDiamond()
                        - diamondsSpent
        );

        user.setGold(
                (int) newCoinAmount
        );

        return ShopActionResult
                .currencyExchanged(
                        item.getItemId(),
                        count,
                        diamondsSpent,
                        coinsReceived,
                        user.getGold(),
                        user.getDiamond()
                );
    }

    private ShopActionResult buyDailyOffer(
            String itemId,
            int count,
            DailyOfferState state,
            LocalDate currentDate
    ) {
        if (count != 1) {
            return failure(
                    ShopActionStatus
                            .DAILY_OFFER_INVALID_COUNT,
                    "Daily offer can only be bought once.",
                    itemId,
                    count
            );
        }

        if (state == null
                || !state.getOfferId()
                .equalsIgnoreCase(itemId)) {

            return failure(
                    ShopActionStatus
                            .DAILY_OFFER_UNAVAILABLE,
                    "Daily offer is not available.",
                    itemId,
                    count
            );
        }

        if (state.isPurchased()) {
            return failure(
                    ShopActionStatus
                            .DAILY_OFFER_ALREADY_PURCHASED,
                    "Daily offer has already been purchased.",
                    itemId,
                    count
            );
        }

        if (!state.canPurchase(currentDate)) {
            return failure(
                    ShopActionStatus
                            .DAILY_OFFER_UNAVAILABLE,
                    "Daily offer is expired.",
                    itemId,
                    count
            );
        }

        Plant selectedPlant =
                findUnlockedPlant(
                        state.getPlantName()
                );

        if (selectedPlant == null) {
            return failure(
                    ShopActionStatus
                            .PLANT_NOT_UNLOCKED,
                    "Daily offer plant is not unlocked.",
                    itemId,
                    count
            );
        }

        DailyOffer offer =
                state.getOffer();

        int totalPrice =
                offer.getFinalPrice();

        if (user.getGold() < totalPrice) {
            return failure(
                    ShopActionStatus.NOT_ENOUGH_COINS,
                    "Not enough coins. Required: "
                            + totalPrice,
                    itemId,
                    count
            );
        }

        plantUpgradeService.addSeedPackets(
                selectedPlant.getName(),
                offer.getSeedPacketAmount()
        );

        user.setGold(
                user.getGold() - totalPrice
        );

        state.markAsPurchased();

        return ShopActionResult
                .seedPacketsPurchased(
                        itemId,
                        count,
                        selectedPlant.getName(),
                        offer.getSeedPacketAmount(),
                        totalPrice,
                        0,
                        user.getGold(),
                        user.getDiamond()
                );
    }

    private List<Plant>
    getEligibleUnlockedPlants() {
        List<Plant> result =
                new ArrayList<>();

        if (user.getUnlockedPlants() == null) {
            return result;
        }

        for (Plant plant
                : user.getUnlockedPlants()) {

            if (plant == null
                    || plant.getName() == null
                    || plant.getName()
                    .trim()
                    .isEmpty()) {

                continue;
            }

            result.add(plant);
        }

        return result;
    }

    private Plant findUnlockedPlant(
            String plantName
    ) {
        if (plantName == null) {
            return null;
        }

        String cleanName =
                plantName.trim();

        for (Plant plant
                : getEligibleUnlockedPlants()) {

            if (plant.getName()
                    .equalsIgnoreCase(
                            cleanName
                    )) {

                return plant;
            }
        }

        return null;
    }

    private boolean isDailyOfferId(
            String itemId
    ) {
        return itemId != null
                && itemId.trim()
                .toLowerCase()
                .startsWith("daily-");
    }

    private String removeQuotes(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String result = value.trim();

        if (result.length() >= 2) {
            char first = result.charAt(0);
            char last =
                    result.charAt(
                            result.length() - 1
                    );

            if ((first == '"' && last == '"')
                    || (first == '\''
                    && last == '\'')) {

                result = result.substring(
                        1,
                        result.length() - 1
                ).trim();
            }
        }

        return result;
    }

    private Integer safeMultiply(
            int value,
            int count
    ) {
        long result =
                (long) value * count;

        if (result < 0
                || result > Integer.MAX_VALUE) {

            return null;
        }

        return (int) result;
    }

    private ShopActionResult valueTooLarge(
            String itemId,
            int count
    ) {
        return failure(
                ShopActionStatus.VALUE_TOO_LARGE,
                "Purchase value is too large.",
                itemId,
                count
        );
    }

    private ShopActionResult failure(
            ShopActionStatus status,
            String message,
            String itemId,
            int count
    ) {
        return ShopActionResult.failure(
                status,
                message,
                itemId,
                count,
                user.getGold(),
                user.getDiamond()
        );
    }
}
