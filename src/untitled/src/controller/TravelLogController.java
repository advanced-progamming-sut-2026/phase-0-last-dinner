package controller;

import model.GameMenuRelated.Page;
import model.GameMenuRelated.PageName;
import model.GameMenuRelated.Quest;
import model.GameMenuRelated.QuestObj;
import model.GameMenuRelated.TravelLog;
import model.Plant;
import model.User.User;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.minigame.beghouledminigame.BeghouledMiniGame;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import view.CommandHandler;
import view.beghouled.BeghouledView;
import view.travellog.TravelLogView;
import view.travellog.TravelLogViewObserver;
import view.izombie.IZombieView;
import view.vasebreaker.VaseBreakerView;
import view.wallnutbowling.WallnutBowlingView;
import view.zombotany.ZombotanyView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TravelLogController
        implements TravelLogViewObserver {

    private final TravelLog travelLog;
    private final User user;
    private final PlantDefinitionRepository plantDefinitions;
    private final Random random;

    private PageName currentPageName;

    public TravelLogController(
            TravelLogView view,
            TravelLog travelLog
    ) {
        this(view, travelLog, null, null, new Random());
    }

    public TravelLogController(
            TravelLogView view,
            TravelLog travelLog,
            User user,
            PlantDefinitionRepository plantDefinitions
    ) {
        this(view, travelLog, user, plantDefinitions, new Random());
    }

    TravelLogController(
            TravelLogView view,
            TravelLog travelLog,
            User user,
            PlantDefinitionRepository plantDefinitions,
            Random random
    ) {
        if (view == null) {
            throw new IllegalArgumentException(
                    "Travel Log view cannot be null."
            );
        }

        if (travelLog == null) {
            throw new IllegalArgumentException(
                    "Travel Log cannot be null."
            );
        }

        this.travelLog = travelLog;
        this.user = user;
        this.plantDefinitions = plantDefinitions;
        this.random = random == null ? new Random() : random;
        currentPageName = PageName.ADVENTURE;

        view.setObserver(this);
    }

    @Override
    public String onClaimQuestRequested(String questName) {
        Page page = this.travelLog.getPage(this.currentPageName);
        QuestObj questObject = page == null ? null : page.findQuest(questName);

        if (questObject == null) {
            return "Quest was not found on the current page.";
        }
        if (!questObject.isCompleted()) {
            return "Quest is not completed yet.";
        }
        if (questObject.isRewardClaimed()) {
            return "Quest reward was already claimed.";
        }
        if (this.user == null) {
            return "Quest rewards are not connected to a user.";
        }

        this.user.initializeMissingFields();
        if (!this.applyReward(questObject)) {
            return "Quest reward could not be applied.";
        }

        questObject.claimReward();
        this.user.setCompletedQuests(safeAdd(this.user.getCompletedQuests(), 1));
        this.user.addNews("Quest completed: " + questObject.getQuest().getDisplayName());
        return "Quest reward claimed: " + questObject.getReward();
    }

    @Override
    public Page onShowCurrentPageRequested() {
        return travelLog.getPage(
                currentPageName
        );
    }

    @Override
    public Page onChangePageRequested(
            PageName pageName
    ) {
        if (pageName == null) {
            return null;
        }

        Page page = travelLog.getPage(pageName);

        if (page != null) {
            currentPageName = pageName;
        }

        return page;
    }

    @Override
    public CommandHandler onOpenMiniGameRequested(
            MiniGameType miniGameType
    ) {
        if (miniGameType == null) {
            return null;
        }

        MiniGame miniGame =
                travelLog.findMiniGame(
                        miniGameType
                );

        if (miniGame == null) {
            return null;
        }

        return createMiniGameHandler(miniGame);
    }

    public PageName getCurrentPageName() {
        return currentPageName;
    }

    public TravelLog getTravelLog() {
        return travelLog;
    }

    private CommandHandler createMiniGameHandler(
            MiniGame miniGame
    ) {
        if (miniGame instanceof VasebreakerMiniGame) {
            return createVasebreakerHandler(
                    (VasebreakerMiniGame) miniGame
            );
        }

        if (miniGame
                instanceof WallnutBowlingMiniGame) {

            return createWallnutBowlingHandler(
                    (WallnutBowlingMiniGame) miniGame
            );
        }

        if (miniGame instanceof IZombieMiniGame) {
            return createIZombieHandler(
                    (IZombieMiniGame) miniGame
            );
        }

        if (miniGame instanceof BeghouledMiniGame) {
            return createBeghouledHandler(
                    (BeghouledMiniGame) miniGame
            );
        }

        if (miniGame instanceof ZombotanyMiniGame) {
            return createZombotanyHandler(
                    (ZombotanyMiniGame) miniGame
            );
        }


        return null;
    }

    private CommandHandler createVasebreakerHandler(
            VasebreakerMiniGame miniGame
    ) {
        VaseBreakerView miniGameView =
                new VaseBreakerView();

        new VasebreakerController(
                miniGameView,
                miniGame
        );

        return miniGameView;
    }

    private CommandHandler
    createWallnutBowlingHandler(
            WallnutBowlingMiniGame miniGame
    ) {
        WallnutBowlingView miniGameView =
                new WallnutBowlingView();

        new WallnutBowlingController(
                miniGameView,
                miniGame
        );

        return miniGameView;
    }

    private CommandHandler createIZombieHandler(
            IZombieMiniGame miniGame
    ) {
        IZombieView miniGameView =
                new IZombieView();

        new IZombieController(
                miniGameView,
                miniGame
        );

        return miniGameView;
    }

    private CommandHandler createBeghouledHandler(
            BeghouledMiniGame game
    ) {
        BeghouledView view = new BeghouledView();

        new BeghouledController(
                view,
                game
        );

        return view;
    }

    private CommandHandler createZombotanyHandler(
            ZombotanyMiniGame game
    ) {
        ZombotanyView view =
                new ZombotanyView();

        new ZombotanyController(
                view,
                game
        );

        return view;
    }

    private boolean applyReward(QuestObj questObject) {
        Quest quest = questObject.getQuest();

        switch (quest) {
            case DAILY_SUN_COLLECTOR:
                this.addCoins(Math.max(1, parseNumber(questObject.getVariableValue(), 3000) / 100));
                return true;
            case CHAPTER_HUNTER:
                return this.addRandomSeedPackets(10);
            case PROFESSIONAL_PLANT_PLAYER:
                return this.unlockRandomPlant();
            case ONLY_CACTUS:
                this.addDiamonds(20);
                return true;
            case ECONOMICAL_GARDENER:
                return this.addRandomSeedPackets(
                        Math.max(0, 20 - parseNumber(questObject.getVariableValue(), 0))
                );
            case DEFENSE_MASTER:
                this.addDiamonds(200);
                return true;
            case QUICK_ACTION:
                this.addCoins(500);
                return true;
            case PROFESSIONAL_DEMOLITION:
                this.addCoins(100);
                return true;
            case SYMMETRY:
                this.addCoins(500);
                return true;
            case FAMILY_MASSACRE:
                this.addCoins(1000);
                return true;
            case BLOOMING_WITH_LIMITS:
                this.addDiamonds(100);
                return true;
            case NIGHT_OR_MORNING:
                this.addDiamonds(20);
                return true;
            case WINNING_STREAK:
                this.addCoins(5000);
                return true;
            case ALMOST_WINNER:
                this.addCoins(300);
                return true;
            case ASYMMETRIC_GARDEN:
                this.addCoins(800);
                return true;
            case CLOUDY_DAY:
                this.addDiamonds(10);
                return true;
            case EMPTY_COLUMN:
                this.addDiamonds(10);
                return true;
            case UNDEFENDED_ROW:
                this.addDiamonds(20);
                return true;
            case UNDEFENDED_CROSS:
                this.addDiamonds(25);
                return true;
            case MOWING_TIME:
                this.addDiamonds(parseNumber(questObject.getVariableValue(), 10));
                return true;
            default:
                return false;
        }
    }

    private boolean addRandomSeedPackets(int amount) {
        if (amount <= 0) {
            return false;
        }

        List<Plant> unlockedPlants = this.user.getUnlockedPlants();
        List<Plant> availablePlants = new ArrayList<>();

        if (unlockedPlants != null) {
            for (Plant plant : unlockedPlants) {
                if (plant != null && plant.getName() != null) {
                    availablePlants.add(plant);
                }
            }
        }

        if (availablePlants.isEmpty()) {
            return false;
        }

        Plant selectedPlant = availablePlants.get(this.random.nextInt(availablePlants.size()));
        this.user.getPlantUpgradeService().addSeedPackets(selectedPlant.getName(), amount);
        return true;
    }

    private boolean unlockRandomPlant() {
        if (this.plantDefinitions == null || this.plantDefinitions.findAll() == null) {
            return false;
        }

        List<PlantDefinition> lockedPlants = new ArrayList<>();
        for (PlantDefinition definition : this.plantDefinitions.findAll()) {
            if (definition != null && !this.isUnlocked(definition.getName())) {
                lockedPlants.add(definition);
            }
        }

        if (lockedPlants.isEmpty()) {
            return false;
        }

        PlantDefinition selected = lockedPlants.get(this.random.nextInt(lockedPlants.size()));
        Plant plant = new PlantFactory(this.user.getPlantUpgradeService()).create(selected);
        this.user.getUnlockedPlants().add(plant);
        return true;
    }

    private boolean isUnlocked(String plantName) {
        if (plantName == null || this.user.getUnlockedPlants() == null) {
            return false;
        }

        for (Plant plant : this.user.getUnlockedPlants()) {
            if (plant != null && plant.getName() != null
                    && plant.getName().equalsIgnoreCase(plantName)) {
                return true;
            }
        }

        return false;
    }

    private void addCoins(int amount) {
        this.user.setGold(safeAdd(this.user.getGold(), amount));
    }

    private void addDiamonds(int amount) {
        this.user.setDiamond(safeAdd(this.user.getDiamond(), amount));
    }

    private int parseNumber(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int safeAdd(int currentValue, int amount) {
        long total = (long) currentValue + Math.max(0, amount);
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }
}
