package model.GameMenuRelated;

import lombok.Getter;
import model.minigame.MiniGame;

import java.util.ArrayList;

@Getter
public class Page {
    private PageName pageName;
    private ArrayList<QuestObj> questObjects;
    private ArrayList<MiniGame> miniGames;

    public Page(PageName pageName) {
        this.pageName = pageName;
        this.questObjects = new ArrayList<QuestObj>();
        this.miniGames = new ArrayList<MiniGame>();
    }

}
