package model.mechanism;
import model.GameMenuRelated.Quest;
import model.GameMenuRelated.QuestObj;
import model.Plant;
import model.User.User;
import model.chapters.Chapter;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.zombie.Zombie;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
public class QuestProgressTracker {
    private static final int QUICK_ACTION_TICKS = 300;
    private final User user;
    private final Board board;
    private final Chapter chapter;
    private final DifficultyConfig difficultyConfig;
    private final boolean daytime;
    private final long levelStartTick;
    private final int rowCount;
    private final int columnCount;
    private final int emptyColumn;
    private final int undefendedRow;
    private final int crossIndex;
    private final int[] firstColumnKillsByRow;
    private final Set<Integer> mowerRows = new HashSet<>();
    private final Set<Zombie> pendingMowerKills = Collections.newSetFromMap(new IdentityHashMap<>());
    private int lostPlants;
    private int explosivePlantsUsed;
    private int placedPlantCount;
    private int sunProducerPlantCount;
    private int quickActionKills;
    private boolean familyKillSeen;
    private boolean invalidFamilyKill;
    private boolean forbiddenFamilyUsed;
    private boolean nightPlantUsed;
    private boolean nonNightPlantUsed;
    private boolean emptyColumnUsed;
    private boolean undefendedRowUsed;
    private boolean undefendedCrossUsed;
    private boolean levelFinished;
    public QuestProgressTracker(
            User user,
            Board board,
            GameClock gameClock,
            Chapter chapter,
            DifficultyConfig difficultyConfig,
            boolean daytime
    ) {
        this.user = user;
        this.board = board;
        this.chapter = chapter;
        this.difficultyConfig = difficultyConfig;
        this.daytime = daytime;
        this.levelStartTick = gameClock == null ? 0 : gameClock.getCurrentTick();
        this.rowCount = this.findBoardSize(false, 5);
        this.columnCount = this.findBoardSize(true, 9);
        this.emptyColumn = this.boardIndex(Quest.EMPTY_COLUMN, this.columnCount);
        this.undefendedRow = this.boardIndex(Quest.UNDEFENDED_ROW, this.rowCount);
        this.crossIndex = this.boardIndex(
                Quest.UNDEFENDED_CROSS,
                Math.min(this.rowCount, this.columnCount)
        );
        this.firstColumnKillsByRow = new int[this.rowCount];
        if (this.user != null) {
            this.user.initializeMissingFields();
        }
        this.resetLevelQuestProgress();
        this.restoreCounterProgress();
    }
    public void onPlantPlaced(Plant plant, Position position) {
        if (plant == null || position == null || this.levelFinished) {
            return;
        }
        this.placedPlantCount++;
        if (this.isExplosive(plant)) {
            this.explosivePlantsUsed++;
            this.setProgress(
                    Quest.PROFESSIONAL_DEMOLITION,
                    this.percentage(this.explosivePlantsUsed, 3)
            );
        }
        if (this.isSunProducer(plant)) {
            this.sunProducerPlantCount++;
        }
        if (this.isNightPlant(plant)) {
            this.nightPlantUsed = true;
        } else {
            this.nonNightPlantUsed = true;
        }
        if (this.matchesFamily(plant, this.questValue(Quest.BLOOMING_WITH_LIMITS))) {
            this.forbiddenFamilyUsed = true;
        }
        if (position.getX() == this.emptyColumn) {
            this.emptyColumnUsed = true;
        }
        if (position.getY() == this.undefendedRow) {
            this.undefendedRowUsed = true;
        }
        if (position.getX() == this.crossIndex || position.getY() == this.crossIndex) {
            this.undefendedCrossUsed = true;
        }
    }
    public void onPlantDestroyed() {
        if (!this.levelFinished && this.lostPlants < Integer.MAX_VALUE) {
            this.lostPlants++;
        }
    }
    public void onZombieKilled(ZombieKillEvent event) {
        if (event == null || event.getZombie() == null || this.levelFinished) {
            return;
        }
        Zombie zombie = event.getZombie();
        boolean mowerKill = this.pendingMowerKills.remove(zombie);
        Plant sourcePlant = event.getSourcePlant();
        this.updateChapterHunter();
        this.updateQuickAction(event.getTick());
        this.updatePlantKillQuests(sourcePlant);
        this.updateFamilyMassacre(sourcePlant);
        if (!mowerKill) {
            this.updateAlmostWinner(zombie);
        }
    }
    public void onMowerKills(List<Zombie> killedZombies) {
        if (killedZombies == null || killedZombies.isEmpty() || this.levelFinished) {
            return;
        }
        int killedCount = 0;
        for (Zombie zombie : killedZombies) {
            if (zombie == null) {
                continue;
            }
            killedCount++;
            this.pendingMowerKills.add(zombie);
            Position position = zombie.getPosition();
            if (position != null && position.getY() >= 0 && position.getY() < this.rowCount) {
                this.mowerRows.add(position.getY());
            }
        }
        if (killedCount > 0 && this.user != null) {
            int total = this.user.addQuestCounter(Quest.MOWING_TIME, killedCount);
            this.setProgress(
                    Quest.MOWING_TIME,
                    this.percentage(total, this.questNumber(Quest.MOWING_TIME, 10))
            );
        }
        this.setProgress(
                Quest.ALMOST_WINNER,
                Math.min(99, this.percentage(this.validFirstColumnKills(), 10))
        );
    }
    public void onSunCollected(int amount) {
        if (amount <= 0 || this.user == null || this.levelFinished) {
            return;
        }
        int total = this.user.addQuestCounter(Quest.DAILY_SUN_COLLECTOR, amount);
        this.setProgress(
                Quest.DAILY_SUN_COLLECTOR,
                this.percentage(total, this.questNumber(Quest.DAILY_SUN_COLLECTOR, 3000))
        );
    }
    public void onLevelFinished(boolean won) {
        if (this.levelFinished) {
            return;
        }
        this.levelFinished = true;
        this.finishWinningStreak(won);
        this.setProgress(
                Quest.ECONOMICAL_GARDENER,
                won && this.lostPlants <= this.questNumber(Quest.ECONOMICAL_GARDENER, 0) ? 100 : 0
        );
        this.setProgress(
                Quest.DEFENSE_MASTER,
                won && this.hasExactlyZeroSun() ? 100 : 0
        );
        this.setProgress(Quest.SYMMETRY, won && this.isSymmetricGarden() ? 100 : 0);
        this.setProgress(
                Quest.FAMILY_MASSACRE,
                won && this.familyKillSeen && !this.invalidFamilyKill ? 100 : 0
        );
        this.setProgress(
                Quest.BLOOMING_WITH_LIMITS,
                won && !this.forbiddenFamilyUsed ? 100 : 0
        );
        this.setProgress(
                Quest.NIGHT_OR_MORNING,
                won && this.daytime && this.nightPlantUsed && !this.nonNightPlantUsed ? 100 : 0
        );
        this.setProgress(
                Quest.ALMOST_WINNER,
                this.validFirstColumnKills() >= 10 ? 100 : this.percentage(this.validFirstColumnKills(), 10)
        );
        this.setProgress(
                Quest.ASYMMETRIC_GARDEN,
                won && this.hasPlants() && !this.isSymmetricGarden() ? 100 : 0
        );
        this.setProgress(
                Quest.CLOUDY_DAY,
                won && this.placedPlantCount == 3 && this.sunProducerPlantCount == 3 ? 100 : 0
        );
        this.setProgress(Quest.EMPTY_COLUMN, won && !this.emptyColumnUsed ? 100 : 0);
        this.setProgress(Quest.UNDEFENDED_ROW, won && !this.undefendedRowUsed ? 100 : 0);
        this.setProgress(Quest.UNDEFENDED_CROSS, won && !this.undefendedCrossUsed ? 100 : 0);
    }
    private void updateChapterHunter() {
        if (this.user == null || this.chapter == null || this.chapter.getChapter() == null) {
            return;
        }
        if (!this.normalized(this.chapter.getChapter().name())
                .equals(this.normalized(this.questValue(Quest.CHAPTER_HUNTER)))) {
            return;
        }
        int total = this.user.addQuestCounter(Quest.CHAPTER_HUNTER, 1);
        this.setProgress(Quest.CHAPTER_HUNTER, this.percentage(total, 50));
    }
    private void updateQuickAction(long killTick) {
        if (killTick - this.levelStartTick < 0 || killTick - this.levelStartTick > QUICK_ACTION_TICKS) {
            return;
        }
        if (this.quickActionKills < Integer.MAX_VALUE) {
            this.quickActionKills++;
        }
        this.setProgress(Quest.QUICK_ACTION, this.percentage(this.quickActionKills, 10));
    }
    private void updatePlantKillQuests(Plant sourcePlant) {
        if (this.user == null || sourcePlant == null) {
            return;
        }
        if (this.samePlant(sourcePlant, this.questValue(Quest.PROFESSIONAL_PLANT_PLAYER))) {
            int total = this.user.addQuestCounter(Quest.PROFESSIONAL_PLANT_PLAYER, 1);
            this.setProgress(Quest.PROFESSIONAL_PLANT_PLAYER, this.percentage(total, 10));
        }
        if (this.samePlant(sourcePlant, "Cactus")) {
            int total = this.user.addQuestCounter(Quest.ONLY_CACTUS, 1);
            this.setProgress(Quest.ONLY_CACTUS, this.percentage(total, 10));
        }
    }
    private void updateFamilyMassacre(Plant sourcePlant) {
        if (sourcePlant == null) {
            return;
        }
        this.familyKillSeen = true;
        if (!this.matchesFamily(sourcePlant, this.questValue(Quest.FAMILY_MASSACRE))) {
            this.invalidFamilyKill = true;
        }
    }
    private void updateAlmostWinner(Zombie zombie) {
        Position position = zombie.getPosition();
        if (position == null || position.getX() != 0
                || position.getY() < 0 || position.getY() >= this.rowCount
                || !this.mowerRows.contains(position.getY())) {
            return;
        }
        int row = position.getY();
        if (this.firstColumnKillsByRow[row] < Integer.MAX_VALUE) {
            this.firstColumnKillsByRow[row]++;
        }
        this.setProgress(
                Quest.ALMOST_WINNER,
                Math.min(99, this.percentage(this.validFirstColumnKills(), 10))
        );
    }
    private void finishWinningStreak(boolean won) {
        if (this.user == null) {
            return;
        }
        boolean maximumDifficulty = this.difficultyConfig != null
                && this.difficultyConfig.getLevel() == 5;
        int streak;
        if (won && maximumDifficulty) {
            streak = this.user.addQuestCounter(Quest.WINNING_STREAK, 1);
        } else {
            this.user.setQuestCounter(Quest.WINNING_STREAK, 0);
            streak = 0;
        }
        this.setProgress(Quest.WINNING_STREAK, this.percentage(streak, 5));
    }
    private boolean hasExactlyZeroSun() {
        return this.board != null
                && this.board.getSunSystem() != null
                && this.board.getSunSystem().getSunAmount() == 0;
    }
    private boolean isSymmetricGarden() {
        if (this.board == null || !this.hasPlants()) {
            return false;
        }
        for (int row = 0; row < this.rowCount / 2; row++) {
            int mirrorRow = this.rowCount - 1 - row;
            for (int column = 0; column < this.columnCount; column++) {
                if (!this.plantSignature(column, row).equals(this.plantSignature(column, mirrorRow))) {
                    return false;
                }
            }
        }
        return true;
    }
    private boolean hasPlants() {
        return this.board != null && !this.board.getAllPlants().isEmpty();
    }
    private Map<String, Integer> plantSignature(int column, int row) {
        Map<String, Integer> signature = new HashMap<>();
        if (this.board == null) {
            return signature;
        }
        for (Plant plant : this.board.getPlantsAt(new Position(column, row))) {
            if (plant == null || plant.isDead()) {
                continue;
            }
            String name = this.normalized(plant.getName());
            signature.put(name, signature.getOrDefault(name, 0) + 1);
        }
        return signature;
    }
    private boolean isExplosive(Plant plant) {
        return this.hasCategory(plant, PlantCategory.EXPLOSIVE)
                || this.hasTag(plant, PlantTag.EXPLOSIVE);
    }
    private boolean isSunProducer(Plant plant) {
        return this.hasCategory(plant, PlantCategory.SUN_PRODUCER)
                || this.hasTag(plant, PlantTag.SUN);
    }
    private boolean isNightPlant(Plant plant) {
        return this.hasTag(plant, PlantTag.NIGHT) || this.hasTag(plant, PlantTag.SHROOM);
    }
    private boolean matchesFamily(Plant plant, String family) {
        if (plant == null) {
            return false;
        }
        String target = this.normalized(family);
        if (target.isEmpty()) {
            return false;
        }
        if (plant.getCategories() != null) {
            for (PlantCategory category : plant.getCategories()) {
                if (category != null && this.normalized(category.name()).equals(target)) {
                    return true;
                }
            }
        }
        if (plant.getTags() != null) {
            for (PlantTag tag : plant.getTags()) {
                if (tag != null && this.normalized(tag.name()).equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean hasCategory(Plant plant, PlantCategory category) {
        return plant != null && plant.getCategories() != null && plant.getCategories().contains(category);
    }
    private boolean hasTag(Plant plant, PlantTag tag) {
        return plant != null && plant.getTags() != null && plant.getTags().contains(tag);
    }
    private boolean samePlant(Plant plant, String plantName) {
        return plant != null && this.normalized(plant.getName()).equals(this.normalized(plantName));
    }
    private int validFirstColumnKills() {
        long total = 0;
        for (int row = 0; row < this.firstColumnKillsByRow.length; row++) {
            total += this.firstColumnKillsByRow[row];
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
    private void resetLevelQuestProgress() {
        Quest[] levelQuests = {
                Quest.ECONOMICAL_GARDENER,
                Quest.DEFENSE_MASTER,
                Quest.QUICK_ACTION,
                Quest.PROFESSIONAL_DEMOLITION,
                Quest.SYMMETRY,
                Quest.FAMILY_MASSACRE,
                Quest.BLOOMING_WITH_LIMITS,
                Quest.NIGHT_OR_MORNING,
                Quest.ALMOST_WINNER,
                Quest.ASYMMETRIC_GARDEN,
                Quest.CLOUDY_DAY,
                Quest.EMPTY_COLUMN,
                Quest.UNDEFENDED_ROW,
                Quest.UNDEFENDED_CROSS
        };
        for (Quest quest : levelQuests) {
            QuestObj questObject = this.questObject(quest);
            if (questObject != null && !questObject.isCompleted() && !questObject.isRewardClaimed()) {
                questObject.setCompletionPercentage(0);
            }
        }
    }
    private void restoreCounterProgress() {
        if (this.user == null) {
            return;
        }
        this.restoreCounter(Quest.DAILY_SUN_COLLECTOR, this.questNumber(Quest.DAILY_SUN_COLLECTOR, 3000));
        this.restoreCounter(Quest.CHAPTER_HUNTER, 50);
        this.restoreCounter(Quest.PROFESSIONAL_PLANT_PLAYER, 10);
        this.restoreCounter(Quest.ONLY_CACTUS, 10);
        this.restoreCounter(Quest.WINNING_STREAK, 5);
        this.restoreCounter(Quest.MOWING_TIME, this.questNumber(Quest.MOWING_TIME, 10));
    }
    private void restoreCounter(Quest quest, int target) {
        this.setProgress(quest, this.percentage(this.user.getQuestCounter(quest), target));
    }
    private void setProgress(Quest quest, int progress) {
        QuestObj questObject = this.questObject(quest);
        if (questObject == null || questObject.isRewardClaimed()
                || (questObject.isCompleted() && progress < 100)) {
            return;
        }
        boolean wasCompleted = questObject.isCompleted();
        questObject.setCompletionPercentage(progress);
        if (!wasCompleted && questObject.isCompleted() && this.user != null) {
            this.user.recordQuestCompletion(quest);
        }
    }
    private QuestObj questObject(Quest quest) {
        if (this.user == null || this.user.getTravelLog() == null || quest == null) {
            return null;
        }
        return this.user.getTravelLog().findQuest(quest);
    }
    private String questValue(Quest quest) {
        QuestObj questObject = this.questObject(quest);
        return questObject == null ? "" : questObject.getVariableValue();
    }
    private int questNumber(Quest quest, int fallback) {
        String value = this.questValue(quest);
        if (value == null) {
            return fallback;
        }
        String digits = value.replaceAll("[^0-9-]", "");
        if (digits.isEmpty() || "-".equals(digits)) {
            return fallback;
        }
        try {
            return Math.max(0, Integer.parseInt(digits));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
    private int boardIndex(Quest quest, int size) {
        if (size <= 0) {
            return 0;
        }
        int userIndex = this.questNumber(quest, 1);
        return Math.max(0, Math.min(size - 1, userIndex - 1));
    }
    private int percentage(int amount, int target) {
        if (target <= 0) {
            return 100;
        }
        long percent = (long) Math.max(0, amount) * 100 / target;
        return (int) Math.min(100, percent);
    }
    private int findBoardSize(boolean columns, int fallback) {
        if (this.board == null || this.board.getTiles() == null) {
            return fallback;
        }
        int maximum = -1;
        for (Tile tile : this.board.getTiles()) {
            if (tile == null || tile.getPosition() == null) {
                continue;
            }
            maximum = Math.max(
                    maximum,
                    columns ? tile.getPosition().getX() : tile.getPosition().getY()
            );
        }
        return maximum < 0 ? fallback : maximum + 1;
    }
    private String normalized(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
