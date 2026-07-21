package model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import model.Plant;
import model.plant.PlantUpgradeService;
import model.shop.Shop;
import model.chapters.Chapter;
import model.Greenhouse.Greenhouse;
import model.GameMenuRelated.Quest;
import model.GameMenuRelated.QuestCategory;
import model.GameMenuRelated.QuestObj;
import model.GameMenuRelated.TravelLog;
import model.chapters.ChapterAncientEgypt;
import model.chapters.ChapterBigWaveBeach;
import model.chapters.ChapterIceCaves;
import model.chapters.ChapterMedieval;
import model.chapters.ChapterType;
import model.level.LevelType;
import model.minigame.MiniGameType;
import model.minigame.MiniGame;
import model.minigame.StageProgressMiniGame;
import model.zombie.Zombie;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String username;
    private String hashedPassword;
    private String nickname;
    private String email;
    private int questionNum;
    private String securityAnswer;
    private UserGender gender;
    private boolean stayLoggedIn=false;
    private Greenhouse greenhouse;
    // travel log graph runtime dare va dakhele json zakhire nemishe
    private transient TravelLog travelLog;
    private int diamond;
    private int gold;
    private transient Chapter chapter;
    private ChapterType savedChapterType;
    private int level;
    private int difficultyLevel=3;
    private int completedMinigames;
    private Set<MiniGameType> completedMiniGameTypes;
    private Map<MiniGameType, Integer> highestUnlockedMiniGameStages;
    private int completedQuests;
    private int completedDailyQuests;
    private int completedNonDailyQuests;
    private int gamesPlayed;
    private int maxObtainedMeowPoints;
    private ArrayList<String> unreadNews;
    private ArrayList<String> allNews;
    private ArrayList<Plant> unlockedPlants;
    private ArrayList<String> encounteredZombieAliases;
    private transient ArrayList<Zombie> zombies;
    //اینا برا شاپ و گرینهوس ان
    private Shop shop;
    private int nextLevelPlantFood;
    private PlantUpgradeService plantUpgradeService;
    private Map<String, Integer> questProgressByName;
    private Map<String, Integer> questCountersByName;
    private Set<String> claimedQuestNames;
    private Set<String> countedCompletedQuestNames;
    private LocalDate lastDailyQuestResetDate;
    // level haye adventure ke yek bar tamam shodan
    private Set<String> completedAdventureLevels;
    private ChapterType lastCompletedChapterType;
    private int lastCompletedLevel;
    private int completedAdventureLevelCount;

    public User(
            String username,
            String hashedPassword,
            String nickname,
            String email,
            int questionNum,
            String securityAnswer,
            UserGender gender
    ) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.nickname = nickname;
        this.email = email;
        this.questionNum = questionNum;
        this.securityAnswer = securityAnswer;
        this.gender = gender;
        this.greenhouse = new Greenhouse();
        this.travelLog = new TravelLog();
        this.level = 1;
        this.unreadNews = new ArrayList<>();
        this.allNews = new ArrayList<>();
        this.unlockedPlants = new ArrayList<>();
        this.zombies = new ArrayList<>();
        this.shop = new Shop();
        this.nextLevelPlantFood = 0;
        this.plantUpgradeService = new PlantUpgradeService();
        this.encounteredZombieAliases = new ArrayList<>();
        this.questProgressByName = new HashMap<>();
        this.questCountersByName = new HashMap<>();
        this.claimedQuestNames = new HashSet<>();
        this.countedCompletedQuestNames = new HashSet<>();
        this.lastDailyQuestResetDate = LocalDate.now();
        this.completedAdventureLevels = new HashSet<>();
        this.completedMiniGameTypes = new HashSet<>();
        this.highestUnlockedMiniGameStages = new HashMap<>();
    }

    public void increaseNextLevelPlantFood(){
        if(this.nextLevelPlantFood >= 3)
            this.nextLevelPlantFood = 2;
        this.nextLevelPlantFood++;
    }

    public void decreaseNextLevelPlantFood(){
        if(this.nextLevelPlantFood <= 0)
            this.nextLevelPlantFood = 1;
        this.nextLevelPlantFood--;
    }

    public void initializeMissingFields() {
        boolean restoreTravelLogState = this.initializeGeneralFields();
        this.initializeQuestFields();

        if (restoreTravelLogState) {
            this.restoreQuestState();
            this.restoreMiniGameState();
        }
        this.resetDailyQuestState();
        this.completedQuests = Math.max(
                this.completedQuests,
                this.completedDailyQuests + this.completedNonDailyQuests
        );
    }

    private boolean initializeGeneralFields() {
        boolean restoreTravelLogState = false;
        if(this.difficultyLevel < 1 || this.difficultyLevel > 5)
            this.difficultyLevel = 3;
        if(this.greenhouse == null)
            this.greenhouse = new Greenhouse();
        if(this.travelLog == null) {
            this.travelLog = new TravelLog();
            restoreTravelLogState = true;
        }
        if (this.chapter == null && this.savedChapterType != null)
            this.chapter = this.createChapter(this.savedChapterType);
        if(this.shop == null)
            this.shop = new Shop();
        if(zombies == null)
            this.zombies = new ArrayList<>();
        if(unlockedPlants == null)
            this.unlockedPlants = new ArrayList<>();
        if(allNews == null)
            this.allNews = new ArrayList<>();
        if(unreadNews == null)
            this.unreadNews = new ArrayList<>();
        if(nextLevelPlantFood <= 0)
            this.nextLevelPlantFood = 0;
        if(nextLevelPlantFood >= 3)
            this.nextLevelPlantFood = 3;
        if (this.plantUpgradeService == null)
            this.plantUpgradeService = new PlantUpgradeService();
        if (this.encounteredZombieAliases == null)
            this.encounteredZombieAliases = new ArrayList<>();
        return restoreTravelLogState;
    }

    private void initializeQuestFields() {
        if (this.completedDailyQuests < 0)
            this.completedDailyQuests = 0;
        if (this.completedNonDailyQuests < 0)
            this.completedNonDailyQuests = 0;
        if (this.questProgressByName == null)
            this.questProgressByName = new HashMap<>();
        if (this.questCountersByName == null)
            this.questCountersByName = new HashMap<>();
        if (this.claimedQuestNames == null)
            this.claimedQuestNames = new HashSet<>();
        if (this.countedCompletedQuestNames == null)
            this.countedCompletedQuestNames = new HashSet<>();
        this.countedCompletedQuestNames.addAll(this.claimedQuestNames);
        if (this.completedAdventureLevels == null)
            this.completedAdventureLevels = new HashSet<>();
        if (this.completedMiniGameTypes == null)
            this.completedMiniGameTypes = new HashSet<>();
        if (this.highestUnlockedMiniGameStages == null)
            this.highestUnlockedMiniGameStages = new HashMap<>();
        if (this.completedAdventureLevelCount < this.completedAdventureLevels.size())
            this.completedAdventureLevelCount = this.completedAdventureLevels.size();
        if (this.completedAdventureLevelCount > 0)
            this.level = this.completedAdventureLevelCount + 1;
    }

    private void resetDailyQuestState() {
        LocalDate today = LocalDate.now();
        if (this.lastDailyQuestResetDate == null) {
            this.lastDailyQuestResetDate = today;
        } else if (!this.lastDailyQuestResetDate.equals(today)) {
            this.travelLog.resetDailyQuests();
            this.removeSavedDailyQuestState();
            this.lastDailyQuestResetDate = today;
        }
    }

    public boolean recordMiniGameCompletion(MiniGameType miniGameType) {
        if (miniGameType == null) {
            return false;
        }

        this.initializeMissingFields();
        if (!this.completedMiniGameTypes.add(miniGameType)) {
            return false;
        }

        this.highestUnlockedMiniGameStages.put(miniGameType, 3);

        if (this.completedMinigames < Integer.MAX_VALUE) {
            this.completedMinigames++;
        }
        return true;
    }

    public boolean recordMiniGameStageProgress(MiniGameType miniGameType, int stageNumber) {
        if (miniGameType == null || stageNumber <= 1) {
            return false;
        }

        this.initializeMissingFields();
        int unlockedStage = Math.min(3, stageNumber);
        int previousStage = this.highestUnlockedMiniGameStages.getOrDefault(miniGameType, 1);
        if (unlockedStage <= previousStage) {
            return false;
        }

        this.highestUnlockedMiniGameStages.put(miniGameType, unlockedStage);
        this.addNews("New minigame stage unlocked: "
                + miniGameType.name() + " stage " + unlockedStage);
        return true;
    }

    public int getQuestCounter(Quest quest) {
        if (quest == null) {
            return 0;
        }

        this.initializeMissingFields();
        return Math.max(0, this.questCountersByName.getOrDefault(quest.name(), 0));
    }

    public void setQuestCounter(Quest quest, int value) {
        if (quest == null) {
            return;
        }

        this.initializeMissingFields();
        this.questCountersByName.put(quest.name(), Math.max(0, value));
    }

    public int addQuestCounter(Quest quest, int amount) {
        if (quest == null || amount <= 0) {
            return this.getQuestCounter(quest);
        }

        int current = this.getQuestCounter(quest);
        int updated = current > Integer.MAX_VALUE - amount
                ? Integer.MAX_VALUE
                : current + amount;
        this.questCountersByName.put(quest.name(), updated);
        return updated;
    }

    public boolean recordQuestCompletion(Quest quest) {
        if (quest == null) {
            return false;
        }

        this.initializeMissingFields();
        if (!this.countedCompletedQuestNames.add(quest.name())) {
            return false;
        }

        this.completedQuests = safeIncrement(this.completedQuests);
        if (quest.getCategory() == QuestCategory.DAILY) {
            this.completedDailyQuests = safeIncrement(this.completedDailyQuests);
        } else {
            this.completedNonDailyQuests = safeIncrement(this.completedNonDailyQuests);
        }
        return true;
    }

    public void prepareForSave() {
        this.initializeMissingFields();

        if (this.chapter != null) {
            this.savedChapterType = this.chapter.getChapter();
        }

        this.questProgressByName.clear();
        this.claimedQuestNames.clear();

        for (Quest quest : Quest.values()) {
            QuestObj questObject = this.travelLog.findQuest(quest);
            if (questObject == null) {
                continue;
            }

            this.questProgressByName.put(quest.name(), questObject.getCompletionPercentage());
            if (questObject.isRewardClaimed()) {
                this.claimedQuestNames.add(quest.name());
            }
        }

        for (MiniGameType miniGameType : MiniGameType.values()) {
            MiniGame miniGame = this.travelLog.findMiniGame(miniGameType);
            if (miniGame instanceof StageProgressMiniGame) {
                int unlockedStage = ((StageProgressMiniGame) miniGame).getHighestUnlockedStage();
                this.highestUnlockedMiniGameStages.put(
                        miniGameType,
                        Math.max(1, Math.min(3, unlockedStage))
                );
            }
        }
    }

    public boolean recordEncounteredZombie(String alias) {
        if (alias == null || alias.trim().isEmpty())
            return false;

        this.initializeMissingFields();

        for (String encounteredAlias : this.encounteredZombieAliases) {
            if (encounteredAlias != null && encounteredAlias.equalsIgnoreCase(alias.trim()))
                return false;
        }

        this.encounteredZombieAliases.add(alias.trim());
        this.addNews("New zombie unlocked: " + alias.trim());
        return true;
    }

    public void addNews(String news) {
        if (news == null || news.trim().isEmpty())
            return;

        this.initializeMissingFields();
        this.allNews.add(news.trim());
        this.unreadNews.add(news.trim());
    }

    public boolean hasUnreadNews() {
        return this.unreadNews != null && !this.unreadNews.isEmpty();
    }

    public boolean hasEncounteredZombie(String alias) {
        if (alias == null || this.encounteredZombieAliases == null)
            return false;

        for (String encounteredAlias : this.encounteredZombieAliases) {
            if (encounteredAlias != null && encounteredAlias.equalsIgnoreCase(alias.trim()))
                return true;
        }

        return false;
    }

    public boolean isChapterUnlocked(ChapterType chapterType) {
        if (chapterType == null) {
            return false;
        }

        this.initializeMissingFields();
        if (chapterType == ChapterType.ANCIENT_EGYPT) {
            return true;
        }

        if (this.savedChapterType != null
                && chapterType.ordinal() <= this.savedChapterType.ordinal()) {
            return true;
        }

        ChapterType previousChapter = ChapterType.values()[chapterType.ordinal() - 1];
        return this.completedLevelCount(previousChapter) >= 2;
    }

    public boolean isAdventureLevelUnlocked(ChapterType chapterType, LevelType levelType) {
        if (!this.isChapterUnlocked(chapterType) || levelType == null) {
            return false;
        }

        if (levelType == LevelType.NORMAL) {
            return true;
        }

        if (this.completedAdventureLevels.isEmpty()
                && this.savedChapterType != null
                && chapterType.ordinal() <= this.savedChapterType.ordinal()) {
            return true;
        }

        return this.completedAdventureLevels.contains(this.adventureLevelKey(
                chapterType,
                LevelType.NORMAL
        ));
    }

    public boolean recordAdventureLevelCompletion(ChapterType chapterType, LevelType levelType) {
        if (chapterType == null || levelType == null || levelType == LevelType.MEOW_POINT
                || levelType == LevelType.BOSS) {
            return false;
        }

        this.initializeMissingFields();
        if (!this.completedAdventureLevels.add(this.adventureLevelKey(chapterType, levelType))) {
            return false;
        }

        this.completedAdventureLevelCount = this.completedAdventureLevels.size();
        this.level = this.completedAdventureLevelCount + 1;
        this.lastCompletedChapterType = chapterType;
        this.lastCompletedLevel = levelType == LevelType.NORMAL ? 1 : 2;
        this.chapter = this.createChapter(chapterType);
        this.savedChapterType = chapterType;

        if (levelType == LevelType.NORMAL) {
            this.addNews("New special level unlocked in " + chapterType.name());
        } else if (chapterType.ordinal() + 1 < ChapterType.values().length) {
            this.addNews("New chapter unlocked: "
                    + ChapterType.values()[chapterType.ordinal() + 1].name());
        }

        return true;
    }

    private int completedLevelCount(ChapterType chapterType) {
        int count = 0;
        String prefix = chapterType.name() + ":";

        for (String key : this.completedAdventureLevels) {
            if (key != null && key.startsWith(prefix)) {
                count++;
            }
        }

        return count;
    }

    private String adventureLevelKey(ChapterType chapterType, LevelType levelType) {
        return chapterType.name() + ":" + levelType.name();
    }

    private void restoreQuestState() {
        for (Quest quest : Quest.values()) {
            QuestObj questObject = this.travelLog.findQuest(quest);
            if (questObject == null) {
                continue;
            }

            int progress = this.questProgressByName.getOrDefault(quest.name(), 0);
            questObject.restoreState(progress, this.claimedQuestNames.contains(quest.name()));
        }
    }

    private void restoreMiniGameState() {
        for (MiniGameType miniGameType : MiniGameType.values()) {
            MiniGame miniGame = this.travelLog.findMiniGame(miniGameType);
            if (!(miniGame instanceof StageProgressMiniGame)) {
                continue;
            }

            boolean completed = this.completedMiniGameTypes.contains(miniGameType);
            int unlockedStage = completed
                    ? 3
                    : this.highestUnlockedMiniGameStages.getOrDefault(miniGameType, 1);
            this.highestUnlockedMiniGameStages.put(miniGameType, unlockedStage);
            ((StageProgressMiniGame) miniGame).restoreHighestUnlockedStage(unlockedStage);
            if (completed) {
                miniGame.setCompleted(true);
                miniGame.setAllStagesCompleted(true);
            }
        }
    }

    private void removeSavedDailyQuestState() {
        for (Quest quest : Quest.values()) {
            if (quest.getCategory() != QuestCategory.DAILY) {
                continue;
            }

            this.questProgressByName.remove(quest.name());
            this.questCountersByName.remove(quest.name());
            this.claimedQuestNames.remove(quest.name());
            this.countedCompletedQuestNames.remove(quest.name());
        }
    }

    private static int safeIncrement(int value) {
        return value == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, value) + 1;
    }

    private Chapter createChapter(ChapterType chapterType) {
        if (chapterType == null) {
            return null;
        }

        switch (chapterType) {
            case ANCIENT_EGYPT:
                return new ChapterAncientEgypt();
            case ICE_CAVES:
                return new ChapterIceCaves();
            case BIG_WAVE_BEACH:
                return new ChapterBigWaveBeach();
            case MEDIEVAL:
                return new ChapterMedieval();
            default:
                return null;
        }
    }
}
