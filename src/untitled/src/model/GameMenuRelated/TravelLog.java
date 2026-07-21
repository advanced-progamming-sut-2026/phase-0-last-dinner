package model.GameMenuRelated;

import lombok.Getter;
import model.minigame.MiniGame;
import model.minigame.MiniGameFactory;
import model.minigame.MiniGameType;

@Getter
public class TravelLog {

    private Page[] pages;

    public TravelLog() {
        this(new MiniGameFactory());
    }

    public TravelLog(MiniGameFactory miniGameFactory) {
        if (miniGameFactory == null) {
            miniGameFactory = new MiniGameFactory();
        }

        pages = createPages();

        initialiseQuests();
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

    public QuestObj findQuest(Quest quest) {
        if (quest == null) {
            return null;
        }

        for (Page page : this.pages) {
            if (page == null) {
                continue;
            }

            QuestObj questObject = page.findQuest(quest.name());
            if (questObject != null) {
                return questObject;
            }
        }

        return null;
    }

    public QuestObj findQuest(String questName) {
        if (questName == null) {
            return null;
        }

        for (Page page : this.pages) {
            if (page == null) {
                continue;
            }

            QuestObj questObject = page.findQuest(questName);
            if (questObject != null) {
                return questObject;
            }
        }

        return null;
    }

    public boolean setProgress(Quest quest, int completionPercentage) {
        QuestObj questObject = this.findQuest(quest);
        if (questObject == null) {
            return false;
        }

        questObject.setCompletionPercentage(completionPercentage);
        return true;
    }

    public boolean addProgress(Quest quest, int percentage) {
        QuestObj questObject = this.findQuest(quest);
        if (questObject == null || percentage <= 0) {
            return false;
        }

        questObject.addProgress(percentage);
        return true;
    }

    public void resetDailyQuests() {
        for (Quest quest : Quest.values()) {
            if (quest.getCategory() != QuestCategory.DAILY) {
                continue;
            }

            QuestObj questObject = this.findQuest(quest);
            if (questObject != null) {
                questObject.reset();
            }
        }
    }

    private void initialiseQuests() {
        for (Quest quest : Quest.values()) {
            Page page = this.getPage(pageFor(quest.getCategory()));
            if (page != null && page.findQuest(quest.name()) == null) {
                page.addQuest(new QuestObj(quest));
            }
        }
    }

    private PageName pageFor(QuestCategory category) {
        if (category == QuestCategory.MAIN) {
            return PageName.ADVENTURE;
        }
        if (category == QuestCategory.EPIC) {
            return PageName.CHALLENGES;
        }
        return PageName.COMMUNITY;
    }

    private static Page[] createPages() {
        return new Page[]{
                new Page(PageName.ADVENTURE),
                new Page(PageName.SPECIAL),
                new Page(PageName.MINIGAMES),
                new Page(PageName.COMMUNITY),
                new Page(PageName.CHALLENGES),
                new Page(PageName.MYSTERY)
        };
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
