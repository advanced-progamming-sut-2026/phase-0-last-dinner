package controller;

import lombok.Getter;
import model.Greenhouse.Greenhouse;
import model.Greenhouse.GreenhouseActionResult;
import model.Greenhouse.GreenhouseActionStatus;
import model.Greenhouse.GreenhouseBoard;
import model.Greenhouse.GreenhouseStateResult;
import model.Greenhouse.Pot;
import model.User.User;
import model.mechanism.Position;
import model.plant.PlantUpgradeService;
import view.CommandHandler;
import view.greenhouse.GreenhouseView;
import view.greenhouse.GreenhouseViewObserver;
import view.shop.ShopView;

@Getter
public class GreenhouseController implements GreenhouseViewObserver {

    private static final int BASE_POT_UNLOCK_COST = 1000;
    private static final int POT_UNLOCK_COST_INCREMENT = 500;

    private final User user;
    private final PlantUpgradeService plantUpgradeService;

    public GreenhouseController(
        GreenhouseView view,
        User user
    ) {
        this(
            view,
            user,
            null
        );
    }

    public GreenhouseController(
        GreenhouseView view,
        User user,
        PlantUpgradeService plantUpgradeService
    ) {
        if (view == null) {
            throw new IllegalArgumentException(
                "Greenhouse view cannot be null."
            );
        }

        if (user == null) {
            throw new IllegalArgumentException(
                "User cannot be null."
            );
        }

        this.user = user;
        this.plantUpgradeService =
            plantUpgradeService;

        user.initializeMissingFields();

        view.setObserver(this);
    }

    @Override
    public GreenhouseStateResult onShowGreenhouseRequested() {

        return GreenhouseStateResult.from(
            getGreenhouse(),
            user.getGold(),
            user.getDiamond()
        );
    }

    @Override
    public GreenhouseActionResult onPlantPotRequested(Position position) {
        Pot pot = getPot(position);
        GreenhouseActionResult invalidPot = validatePlantPot(position, pot);
        if (invalidPot != null) {
            return invalidPot;
        }

        String plantedPlantName =
            getGreenhouse().plantRandom(
                position,
                user.getUnlockedPlants()
            );

        if (plantedPlantName == null) {
            return failure(
                GreenhouseActionStatus.PLANTING_FAILED,
                "Could not plant in this pot.",
                position
            );
        }

        return GreenhouseActionResult.planted(
            position,
            plantedPlantName,
            user.getGold(),
            user.getDiamond()
        );
    }

    @Override
    public GreenhouseActionResult onCollectRequested(Position position) {
        Pot pot = getPot(position);
        GreenhouseActionResult invalidPot = validateCollectPot(position, pot);
        if (invalidPot != null) {
            return invalidPot;
        }

        String plantName = pot.getPlantName();

        boolean boostAlreadyStored = getGreenhouse().hasStoredBoost(plantName);
        String harvestedPlantName = getGreenhouse().harvest(position);

        if (harvestedPlantName == null) {
            return failure(
                GreenhouseActionStatus.HARVEST_FAILED,
                "Could not collect this plant.",
                position
            );
        }

        int coinsEarned = 0;
        boolean boostStored = false;

        if (isMarigold(harvestedPlantName)) {
            coinsEarned = Greenhouse.MARIGOLD_REWARD_COINS;

            user.setGold((int) Math.min(
                Integer.MAX_VALUE,
                (long) user.getGold() + coinsEarned
            ));
        } else {
            boostStored =
                !boostAlreadyStored
                    && getGreenhouse()
                    .hasStoredBoost(
                        harvestedPlantName
                    );
        }

        return GreenhouseActionResult.harvested(
            position,
            harvestedPlantName,
            coinsEarned,
            boostStored,
            user.getGold(),
            user.getDiamond()
        );
    }

    @Override
    public GreenhouseActionResult onGrowRequested(Position position) {
        Pot pot = getPot(position);
        GreenhouseActionResult invalidPot = validateGrowPot(position, pot);
        if (invalidPot != null) {
            return invalidPot;
        }

        int diamondCost = getGreenhouse().getSpeedUpCost(position);

        if (diamondCost <= 0) {
            return failure(
                GreenhouseActionStatus.PLANT_ALREADY_READY,
                "This plant is already ready to collect.",
                position
            );
        }

        if (user.getDiamond() < diamondCost) {
            return failure(
                GreenhouseActionStatus.NOT_ENOUGH_DIAMONDS,
                "Not enough diamonds. Required: "
                    + diamondCost
                    + ", available: "
                    + user.getDiamond(),
                position
            );
        }

        String plantName = pot.getPlantName();
        boolean accelerated = getGreenhouse().speedUpGrowth(position);
        if (!accelerated) {
            return failure(
                GreenhouseActionStatus.INVALID_ACTION,
                "Could not accelerate this plant.",
                position
            );
        }

        user.setDiamond(user.getDiamond() - diamondCost);
        return GreenhouseActionResult.growthAccelerated(
            position,
            plantName,
            diamondCost,
            user.getGold(),
            user.getDiamond()
        );
    }

    @Override
    public GreenhouseActionResult onBuyPotRequested(Position position) {
        Pot pot = getPot(position);
        if (pot == null) {
            return failure(
                GreenhouseActionStatus.INVALID_POSITION,
                "Invalid greenhouse position. X must be from 1 to "
                    + GreenhouseBoard.COLUMN_COUNT
                    + " and Y must be from 1 to "
                    + GreenhouseBoard.ROW_COUNT + ".",
                position
            );
        }
        if (pot.isUnlocked()) {
            return failure(
                GreenhouseActionStatus.POT_ALREADY_UNLOCKED,
                "This pot is already unlocked.",
                position
            );
        }

        int cost = getPotUnlockCost();
        if (user.getGold() < cost) {
            return failure(
                GreenhouseActionStatus.NOT_ENOUGH_COINS,
                "Not enough coins. Required: "
                    + cost
                    + ", available: "
                    + user.getGold(),
                position
            );
        }

        user.setGold(user.getGold() - cost);
        pot.unlock();

        return GreenhouseActionResult.unlocked(
            position,
            cost,
            user.getGold(),
            user.getDiamond()
        );
    }

    @Override
    public int getPotUnlockCost() {
        int unlockedCount = getGreenhouse().getBoard().getUnlockedPotCount();
        int extraUnlocked = Math.max(0, unlockedCount - GreenhouseBoard.COLUMN_COUNT);
        return BASE_POT_UNLOCK_COST + extraUnlocked * POT_UNLOCK_COST_INCREMENT;
    }

    private GreenhouseActionResult validatePlantPot(Position position, Pot pot) {
        if (pot == null) {
            return failure(
                GreenhouseActionStatus.INVALID_POSITION,
                "Invalid greenhouse position. X must be from 1 to "
                    + GreenhouseBoard.COLUMN_COUNT
                    + " and Y must be from 1 to "
                    + GreenhouseBoard.ROW_COUNT + ".",
                position
            );
        }
        if (!pot.isUnlocked()) {
            return failure(GreenhouseActionStatus.POT_LOCKED, "This pot is locked.", position);
        }
        if (!pot.isEmpty()) {
            return failure(
                GreenhouseActionStatus.POT_OCCUPIED,
                "This pot is already occupied.",
                position
            );
        }
        return null;
    }

    private GreenhouseActionResult validateCollectPot(Position position, Pot pot) {
        GreenhouseActionResult invalidPot = validateOccupiedPot(position, pot);
        if (invalidPot != null) {
            return invalidPot;
        }
        if (!pot.isReady()) {
            return failure(
                GreenhouseActionStatus.PLANT_NOT_READY,
                "This plant is not ready. "
                    + pot.getRemainingGrowthHours(System.currentTimeMillis())
                    + " hour(s) remaining.",
                position
            );
        }
        return null;
    }

    private GreenhouseActionResult validateGrowPot(Position position, Pot pot) {
        GreenhouseActionResult invalidPot = validateOccupiedPot(position, pot);
        if (invalidPot != null) {
            return invalidPot;
        }
        if (pot.isReady()) {
            return failure(
                GreenhouseActionStatus.PLANT_ALREADY_READY,
                "This plant is already ready to collect.",
                position
            );
        }
        return null;
    }

    private GreenhouseActionResult validateOccupiedPot(Position position, Pot pot) {
        if (pot == null) {
            return failure(
                GreenhouseActionStatus.INVALID_POSITION,
                "Invalid greenhouse position.",
                position
            );
        }
        if (!pot.isUnlocked()) {
            return failure(GreenhouseActionStatus.POT_LOCKED, "This pot is locked.", position);
        }
        if (pot.isEmpty()) {
            return failure(GreenhouseActionStatus.POT_EMPTY, "This pot is empty.", position);
        }
        return null;
    }

    @Override
    public CommandHandler onEnterShopRequested() {
        user.initializeMissingFields();

        ShopView shopView = new ShopView();

        new ShopController(
            shopView,
            user,
            user.getPlantUpgradeService()
        );

        return shopView;
    }

    public Greenhouse getGreenhouse() {
        return user.getGreenhouse();
    }

    private Pot getPot(Position position) {
        if (position == null) {
            return null;
        }

        Greenhouse greenhouse = getGreenhouse();

        if (!greenhouse
            .getBoard()
            .isValidPosition(
                position.getX(),
                position.getY()
            )) {

            return null;
        }

        return greenhouse
            .getBoard()
            .getPot(position);
    }

    private boolean isMarigold(
        String plantName
    ) {
        return plantName != null
            && Greenhouse.MARIGOLD_NAME
            .equalsIgnoreCase(plantName);
    }

    private GreenhouseActionResult failure(
        GreenhouseActionStatus status,
        String message,
        Position position
    ) {
        return GreenhouseActionResult.failure(
            status,
            message,
            position,
            user.getGold(),
            user.getDiamond()
        );
    }
}
