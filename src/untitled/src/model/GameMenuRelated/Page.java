package model.GameMenuRelated;

import lombok.Getter;
import model.minigame.MiniGame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

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

    public void addQuest(QuestObj questObject) {
        if (questObject == null) {
            return;
        }

        this.questObjects.add(questObject);
        this.questObjects.sort(new Comparator<QuestObj>() {
            @Override
            public int compare(QuestObj first, QuestObj second) {
                return Integer.compare(
                        second.getQuest().getPriority().ordinal(),
                        first.getQuest().getPriority().ordinal()
                );
            }
        });
    }

    public QuestObj findQuest(String questName) {
        if (questName == null) {
            return null;
        }

        String normalized = normalize(questName);

        for (QuestObj questObject : this.questObjects) {
            if (questObject == null || questObject.getQuest() == null) {
                continue;
            }

            Quest quest = questObject.getQuest();
            if (normalize(quest.name()).equals(normalized)
                    || normalize(quest.getDisplayName()).equals(normalized)) {
                return questObject;
            }
        }

        return null;
    }

    private String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

}
