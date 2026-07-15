package model.GameMenuRelated;

import lombok.Getter;
import model.minigame.MiniGame;
import model.minigame.MiniGameFactory;
import model.minigame.MiniGameType;

@Getter
public class TravelLog {

    private final Page[] pages;

    public TravelLog() {
        this(new MiniGameFactory());
    }

    public TravelLog(MiniGameFactory miniGameFactory) {
        if (miniGameFactory == null) {
            miniGameFactory = new MiniGameFactory();
        }

        pages = new Page[]{
                new Page(PageName.ADVENTURE),
                new Page(PageName.SPECIAL),
                new Page(PageName.MINIGAMES),
                new Page(PageName.COMMUNITY),
                new Page(PageName.CHALLENGES),
                new Page(PageName.MYSTERY)
        };

        initialiseMiniGames(miniGameFactory);
    }

    public Page getPage(PageName pageName) {
        if (pageName == null) {
            return null;
        }

        for (Page page : pages) {
            if (page != null
                    && page.getPageName() == pageName) {

                return page;
            }
        }

        return null;
    }

    public MiniGame findMiniGame(
            MiniGameType miniGameType
    ) {
        if (miniGameType == null) {
            return null;
        }

        Page miniGamesPage =
                getPage(PageName.MINIGAMES);

        if (miniGamesPage == null
                || miniGamesPage.getMiniGames() == null) {

            return null;
        }

        for (MiniGame miniGame
                : miniGamesPage.getMiniGames()) {

            if (miniGame != null
                    && miniGame.getType()
                    == miniGameType) {

                return miniGame;
            }
        }

        return null;
    }

    private void initialiseMiniGames(
            MiniGameFactory miniGameFactory
    ) {
        Page miniGamesPage =
                getPage(PageName.MINIGAMES);

        if (miniGamesPage == null) {
            return;
        }

        for (MiniGameType type
                : MiniGameType.values()) {

            MiniGame miniGame =
                    miniGameFactory.create(type);

            if (miniGame != null) {
                miniGamesPage
                        .getMiniGames()
                        .add(miniGame);
            }
        }
    }
}