package controller;

import model.GameMenuRelated.Page;
import model.GameMenuRelated.PageName;
import model.GameMenuRelated.TravelLog;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.minigame.beghouledminigame.BeghouledMiniGame;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import view.CommandHandler;
import view.beghouled.BeghouledView;
import view.travellog.TravelLogView;
import view.travellog.TravelLogViewObserver;
import view.izombie.IZombieView;
import view.vasebreaker.VaseBreakerView;
import view.wallnutbowling.WallnutBowlingView;
import view.zombotany.ZombotanyView;

public class TravelLogController
        implements TravelLogViewObserver {

    private final TravelLog travelLog;

    private PageName currentPageName;

    public TravelLogController(
            TravelLogView view,
            TravelLog travelLog
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
        currentPageName = PageName.ADVENTURE;

        view.setObserver(this);
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
}