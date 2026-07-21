import model.GameMenuRelated.Quest;
import model.Plant;
import model.User.User;
import model.User.UserGender;
import model.level.NormalLevel;
import model.mechanism.Board;
import model.mechanism.DifficultyConfig;
import model.mechanism.GameClock;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.QuestProgressTracker;
import model.mechanism.SunSystem;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import model.zombie.Zombie;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuestProgressIntegrationTest {
    @Test
    public void collectedSunKeepsRawProgressAndCountsCompletionOnce() {
        Main application = Main.loadApplication();
        User user = this.createUser("sun");
        PlantZombieGame game = this.createConfiguredGame(application, user);
        Plant sunflower = this.createPlant(application, "Sunflower");
        Position position = new Position(2, 2);
        sunflower.setPosition(position);

        assertNotNull(game.getSunSystem().addPlantSun(sunflower, 2999));
        assertEquals(2999, game.collectSun(position));
        assertEquals(2999, user.getQuestCounter(Quest.DAILY_SUN_COLLECTOR));
        assertEquals(
                99,
                user.getTravelLog().findQuest(Quest.DAILY_SUN_COLLECTOR).getCompletionPercentage()
        );
        assertEquals(0, user.getCompletedDailyQuests());

        assertNotNull(game.getSunSystem().addPlantSun(sunflower, 1));
        assertEquals(1, game.collectSun(position));
        assertEquals(3000, user.getQuestCounter(Quest.DAILY_SUN_COLLECTOR));
        assertTrue(user.getTravelLog().findQuest(Quest.DAILY_SUN_COLLECTOR).isCompleted());
        assertEquals(1, user.getCompletedDailyQuests());
        assertEquals(1, user.getCompletedQuests());

        assertNotNull(game.getSunSystem().addPlantSun(sunflower, 50));
        assertEquals(50, game.collectSun(position));
        assertEquals(3050, user.getQuestCounter(Quest.DAILY_SUN_COLLECTOR));
        assertEquals(1, user.getCompletedDailyQuests());
        assertEquals(1, user.getCompletedQuests());
    }

    @Test
    public void directCombatKeepsThePlantThatCausedEachKill() {
        Main application = Main.loadApplication();
        User user = this.createUser("source");
        PlantZombieGame game = this.createConfiguredGame(application, user);
        Plant peashooter = this.createPlant(application, "Peashooter");
        Plant cactus = this.createPlant(application, "Cactus");

        for (int index = 0; index < 10; index++) {
            Zombie zombie = game.spawnZombie("ZombieDefault", index % 5);
            assertNotNull(zombie);
            game.getCombatSystem().applyDirectDamageToZombie(
                    zombie,
                    Integer.MAX_VALUE,
                    peashooter
            );
        }

        assertEquals(10, user.getQuestCounter(Quest.PROFESSIONAL_PLANT_PLAYER));
        assertTrue(user.getTravelLog().findQuest(Quest.PROFESSIONAL_PLANT_PLAYER).isCompleted());
        assertEquals(0, user.getQuestCounter(Quest.ONLY_CACTUS));

        for (int index = 0; index < 10; index++) {
            Zombie zombie = game.spawnZombie("ZombieDefault", index % 5);
            assertNotNull(zombie);
            game.getCombatSystem().applyDirectDamageToZombie(
                    zombie,
                    Integer.MAX_VALUE,
                    cactus
            );
        }

        assertEquals(10, user.getQuestCounter(Quest.PROFESSIONAL_PLANT_PLAYER));
        assertEquals(10, user.getQuestCounter(Quest.ONLY_CACTUS));
        assertTrue(user.getTravelLog().findQuest(Quest.ONLY_CACTUS).isCompleted());
    }

    @Test
    public void mowerKillsAdvanceOnlyTheMowerCounter() {
        Main application = Main.loadApplication();
        User user = this.createUser("mower");
        PlantZombieGame game = this.createConfiguredGame(application, user);
        List<Zombie> zombies = new ArrayList<>();

        for (int index = 0; index < 10; index++) {
            Zombie zombie = game.spawnZombie("ZombieDefault", 0);
            assertNotNull(zombie);
            zombies.add(zombie);
        }

        assertTrue(game.getBoard().handleZombieAtHouse(zombies.get(0)));
        assertEquals(10, user.getQuestCounter(Quest.MOWING_TIME));
        assertTrue(user.getTravelLog().findQuest(Quest.MOWING_TIME).isCompleted());
        assertEquals(
                0,
                user.getTravelLog().findQuest(Quest.ALMOST_WINNER).getCompletionPercentage()
        );

        for (Zombie zombie : zombies) {
            assertTrue(zombie.isDead());
        }
    }

    @Test
    public void levelOutcomeChecksRequireAWinAndOnlyCountOnce() {
        Main application = Main.loadApplication();
        User user = this.createUser("outcome");
        Board losingBoard = new Board();
        QuestProgressTracker losingTracker = this.createTracker(user, losingBoard, true);

        losingTracker.onLevelFinished(false);

        assertFalse(user.getTravelLog().findQuest(Quest.ECONOMICAL_GARDENER).isCompleted());
        assertFalse(user.getTravelLog().findQuest(Quest.DEFENSE_MASTER).isCompleted());
        assertFalse(user.getTravelLog().findQuest(Quest.EMPTY_COLUMN).isCompleted());
        assertEquals(0, user.getCompletedQuests());

        Board winningBoard = new Board();
        SunSystem sunSystem = new SunSystem(winningBoard, new GameClock());
        sunSystem.addSun(-sunSystem.getSunAmount());
        QuestProgressTracker winningTracker = this.createTracker(user, winningBoard, true);
        PlantDefinition sunflowerDefinition = application.getPlantDefinitions().findByName("Sunflower");
        PlantFactory plantFactory = new PlantFactory();

        this.placeForTracker(
                winningBoard,
                winningTracker,
                plantFactory.create(sunflowerDefinition),
                new Position(1, 1)
        );
        this.placeForTracker(
                winningBoard,
                winningTracker,
                plantFactory.create(sunflowerDefinition),
                new Position(2, 2)
        );
        this.placeForTracker(
                winningBoard,
                winningTracker,
                plantFactory.create(sunflowerDefinition),
                new Position(3, 3)
        );

        winningTracker.onLevelFinished(true);

        assertTrue(user.getTravelLog().findQuest(Quest.ECONOMICAL_GARDENER).isCompleted());
        assertTrue(user.getTravelLog().findQuest(Quest.DEFENSE_MASTER).isCompleted());
        assertTrue(user.getTravelLog().findQuest(Quest.CLOUDY_DAY).isCompleted());
        assertTrue(user.getTravelLog().findQuest(Quest.EMPTY_COLUMN).isCompleted());
        assertTrue(user.getTravelLog().findQuest(Quest.UNDEFENDED_ROW).isCompleted());
        assertTrue(user.getTravelLog().findQuest(Quest.UNDEFENDED_CROSS).isCompleted());
        assertTrue(user.getTravelLog().findQuest(Quest.ASYMMETRIC_GARDEN).isCompleted());
        assertFalse(user.getTravelLog().findQuest(Quest.SYMMETRY).isCompleted());
        assertFalse(user.getTravelLog().findQuest(Quest.BLOOMING_WITH_LIMITS).isCompleted());

        int completedBeforeDuplicateFinish = user.getCompletedQuests();
        winningTracker.onLevelFinished(true);
        assertEquals(completedBeforeDuplicateFinish, user.getCompletedQuests());
    }

    @Test
    public void incompleteLevelCountersResetWhenTheNextLevelStarts() {
        Main application = Main.loadApplication();
        User user = this.createUser("level-reset");
        QuestProgressTracker firstLevel = this.createTracker(user, new Board(), true);
        Plant cherryBomb = this.createPlant(application, "Cherry Bomb");

        firstLevel.onPlantPlaced(cherryBomb, new Position(1, 1));
        firstLevel.onPlantPlaced(cherryBomb, new Position(2, 1));
        assertEquals(
                66,
                user.getTravelLog().findQuest(Quest.PROFESSIONAL_DEMOLITION)
                        .getCompletionPercentage()
        );

        this.createTracker(user, new Board(), true);
        assertEquals(
                0,
                user.getTravelLog().findQuest(Quest.PROFESSIONAL_DEMOLITION)
                        .getCompletionPercentage()
        );
    }

    @Test
    public void dailyResetClearsRawProgressAndAllowsTheNextCompletion() {
        Main application = Main.loadApplication();
        User user = this.createUser("daily-reset");
        PlantZombieGame game = this.createConfiguredGame(application, user);
        QuestProgressTracker tracker = game.getQuestProgressTracker();

        tracker.onSunCollected(3000);
        assertEquals(1, user.getCompletedDailyQuests());
        assertEquals(3000, user.getQuestCounter(Quest.DAILY_SUN_COLLECTOR));

        user.setLastDailyQuestResetDate(LocalDate.now().minusDays(1));
        user.initializeMissingFields();

        assertEquals(0, user.getQuestCounter(Quest.DAILY_SUN_COLLECTOR));
        assertFalse(user.getTravelLog().findQuest(Quest.DAILY_SUN_COLLECTOR).isCompleted());
        assertEquals(1, user.getCompletedDailyQuests());

        tracker.onSunCollected(3000);
        assertTrue(user.getTravelLog().findQuest(Quest.DAILY_SUN_COLLECTOR).isCompleted());
        assertEquals(2, user.getCompletedDailyQuests());
    }

    private PlantZombieGame createConfiguredGame(Main application, User user) {
        PlantZombieGame game = new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory(),
                user.getPlantUpgradeService(),
                null,
                user
        );
        game.configureLevel(new NormalLevel(null, Collections.<Plant>emptyList(), 100));
        return game;
    }

    private QuestProgressTracker createTracker(User user, Board board, boolean daytime) {
        GameClock clock = new GameClock();
        if (board.getSunSystem() == null) {
            new SunSystem(board, clock);
        }
        return new QuestProgressTracker(
                user,
                board,
                clock,
                null,
                new DifficultyConfig(user),
                daytime
        );
    }

    private Plant createPlant(Main application, String name) {
        PlantDefinition definition = application.getPlantDefinitions().findByName(name);
        assertNotNull(definition);
        return new PlantFactory().create(definition);
    }

    private void placeForTracker(
            Board board,
            QuestProgressTracker tracker,
            Plant plant,
            Position position
    ) {
        plant.setPosition(position);
        plant.setBoard(board);
        board.getTile(position).addPlant(plant);
        tracker.onPlantPlaced(plant, position);
    }

    private User createUser(String suffix) {
        return new User(
                "quest-" + suffix,
                "hash",
                "Quest " + suffix,
                suffix + "@example.com",
                1,
                "blue",
                UserGender.MALE
        );
    }
}
