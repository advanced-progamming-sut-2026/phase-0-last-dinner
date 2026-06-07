package model.GameMenuRelated;

import model.minigame.MiniGame;

import java.util.ArrayList;

public class Page {
    private PageName pageName;
    private ArrayList<QuestObj> questObjects;
    private ArrayList<MiniGame> miniGames;

    public Page(PageName pageName) {
        this.pageName = pageName;
        this.questObjects = new ArrayList<QuestObj>();
        this.miniGames = new ArrayList<MiniGame>();
    }

    public PageName getPageName() {
        return pageName;
    }

    public ArrayList<QuestObj> getQuestObjects() {
        return questObjects;
    }

    public ArrayList<MiniGame> getMiniGames() {
        return miniGames;
    }
}
