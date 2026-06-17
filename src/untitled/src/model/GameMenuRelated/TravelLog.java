package model.GameMenuRelated;

import model.minigame.BeghouledMiniGame;
import model.minigame.IZombieMiniGame;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.WallnutBowlingMiniGame;
import model.minigame.ZombotanyMiniGame;

public class TravelLog {
    private Page[] pages;

    public TravelLog() {
        pages = new Page[] {
                new Page(PageName.ADVENTURE),
                new Page(PageName.SPECIAL),
                new Page(PageName.MINIGAMES),
                new Page(PageName.COMMUNITY),
                new Page(PageName.CHALLENGES),
                new Page(PageName.MYSTERY)
        };

        Page miniGamesPage = pages[2];
        miniGamesPage.getMiniGames().add(new VasebreakerMiniGame());
        miniGamesPage.getMiniGames().add(new WallnutBowlingMiniGame());
        miniGamesPage.getMiniGames().add(new IZombieMiniGame());
        miniGamesPage.getMiniGames().add(new BeghouledMiniGame());
        miniGamesPage.getMiniGames().add(new ZombotanyMiniGame());
    }

    public Page[] getPages() {
        return pages;
    }

    public Page getPage(PageName pageName) {
        return null;
    }
}

