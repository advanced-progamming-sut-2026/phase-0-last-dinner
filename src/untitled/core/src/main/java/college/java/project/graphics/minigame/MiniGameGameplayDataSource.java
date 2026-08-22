package college.java.project.graphics.minigame;

import college.java.project.graphics.GameplaySeedBankDataSource;
import college.java.project.graphics.GameplayWorldDataSource;
import model.Plant;
import model.User.User;
import model.chapters.ChapterType;
import model.collection.PlantCollectionState;
import model.mechanism.Board;
import model.mechanism.LawnMower;
import model.mechanism.PlantFoodSystem;
import model.mechanism.PlantStatus;
import model.mechanism.Position;
import model.mechanism.Sun;
import model.mechanism.SunSystem;
import model.mechanism.Tile;
import model.minigame.MiniGame;
import model.plant.Projectile;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.concurrent.ThreadLocalRandom;

public class MiniGameGameplayDataSource implements GameplaySeedBankDataSource, GameplayWorldDataSource {

    private final MiniGame miniGame;
    private final User user;
    private final LongSupplier currentTickSupplier;
    private final ChapterType chapterType;

    public MiniGameGameplayDataSource(
        MiniGame miniGame,
        User user,
        LongSupplier currentTickSupplier
    ) {
        this(
            miniGame,
            user,
            currentTickSupplier,
            null
        );
    }

    protected MiniGameGameplayDataSource(
        MiniGame miniGame,
        User user,
        LongSupplier currentTickSupplier,
        ChapterType chapterType
    ) {
        if (miniGame == null)
            throw new IllegalArgumentException(
                "Minigame is required"
            );

        this.miniGame = miniGame;
        this.user = user;

        this.currentTickSupplier =
            currentTickSupplier == null
                ? () -> 0L
                : currentTickSupplier;

        this.chapterType = chapterType == null
            ? randomChapter()
            : chapterType;
    }

    private static ChapterType randomChapter() {
        ChapterType[] chapters = ChapterType.values();
        int index = ThreadLocalRandom.current().nextInt(chapters.length);
        return chapters[index];
    }

    protected final MiniGame getMiniGame() {
        return this.miniGame;
    }

    protected final Board getBoard() {
        return this.miniGame.getBoard();
    }

    @Override
    public List<PlantCollectionState> getSelectedPlants() {
        return Collections.emptyList();
    }

    @Override
    public List<PlantStatus> getPlantStatuses() {
        return Collections.emptyList();
    }

    @Override
    public int getSunAmount() {
        SunSystem sunSystem = getSunSystem();
        return sunSystem == null ? 0 : Math.max(0, sunSystem.getSunAmount());
    }

    @Override
    public int getPlantFoodCount() {
        PlantFoodSystem plantFoodSystem = getPlantFoodSystem();
        return plantFoodSystem == null ? 0 : Math.max(0, plantFoodSystem.getPlantFoodAmount());
    }

    @Override
    public int getPlantFoodAmount() {
        return getPlantFoodCount();
    }

    @Override
    public boolean isBoosted(String plantName) {
        return false;
    }

    @Override
    public boolean plant(String plantName, int column, int row) {
        return false;
    }

    @Override
    public boolean hasPlantAt(int column, int row) {
        Board board = getBoard();

        return board != null && !board.getPlantsAt(new Position(column, row)).isEmpty();
    }

    @Override
    public Plant getTopPlantAt(int column, int row) {
        Board board = getBoard();

        if (board == null)
            return null;

        List<Plant> plants = board.getPlantsAt(new Position(column, row));

        return plants.isEmpty() ? null : plants.getLast();
    }

    @Override
    public List<Plant> getPlantsOnBoard() {
        Board board = getBoard();

        return board == null ? Collections.emptyList() : new ArrayList<>(board.getAllPlants());
    }

    @Override
    public List<Zombie> getZombiesOnBoard() {
        Board board = getBoard();

        return board == null ? Collections.emptyList() : new ArrayList<>(board.getAllZombies());
    }

    @Override
    public List<Projectile> getProjectiles() {
        Board board = getBoard();

        return board == null ? Collections.emptyList() : new ArrayList<>(board.getProjectiles());
    }

    @Override
    public List<Sun> getGroundSuns() {
        SunSystem sunSystem = getSunSystem();

        return sunSystem == null ? Collections.emptyList() : new ArrayList<>(sunSystem.getSuns());
    }

    @Override
    public List<LawnMower> getLawnMowers() {
        Board board = getBoard();

        return board == null ? Collections.emptyList() : new ArrayList<>(board.getLawnMowers());
    }

    @Override
    public List<Tile> getTiles() {
        Board board = getBoard();

        return board == null ? Collections.emptyList() : new ArrayList<>(board.getTiles());
    }

    @Override
    public long getCurrentTick() {
        return Math.max(0L, this.currentTickSupplier.getAsLong());
    }

    @Override
    public boolean collectSun(Sun sun) {
        SunSystem sunSystem = getSunSystem();

        return sunSystem != null && sunSystem.collectSun(sun) > 0;
    }

    @Override
    public int getCoinCount() {
        return this.user == null ? 0 : Math.max(0, this.user.getGold());
    }

    @Override
    public int getGemCount() {
        return this.user == null ? 0 : Math.max(0, this.user.getDiamond());
    }

    @Override
    public boolean isLevelWon() {
        return this.miniGame.isWinConditionMet();
    }

    @Override
    public boolean isLevelLost() {
        return this.miniGame.isLoseConditionMet();
    }

    private SunSystem getSunSystem() {
        Board board = getBoard();
        return board == null ? null : board.getSunSystem();
    }

    private PlantFoodSystem getPlantFoodSystem() {
        Board board = getBoard();
        return board == null ? null : board.getPlantFoodSystem();
    }

    @Override
    public ChapterType getChapterType() {
        return this.chapterType;
    }
}
