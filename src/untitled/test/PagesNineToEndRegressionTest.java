import controller.ApplicationController;
import model.GameMenuRelated.Quest;
import model.GameMenuRelated.QuestObj;
import model.Greenhouse.Greenhouse;
import model.Menu.MenuType;
import model.Plant;
import model.User.User;
import model.User.UserGender;
import model.User.UserRepository;
import model.User.LeaderboardEntry;
import model.chapters.ChapterAncientEgypt;
import model.chapters.ChapterType;
import model.level.MeowPointLevel;
import model.level.ConveyorBeltLevel;
import model.level.LevelType;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Board;
import model.mechanism.Wave;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import model.plant.PlantTag;
import model.minigame.MiniGameType;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.minigame.behavior.ZombotanyWallnutBehavior;
import model.zombie.Zombie;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PagesNineToEndRegressionTest {
    @Test
    public void userProgressAndUnlockedPlantsSurviveReload() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-user-state").resolve("users.json");
        UserRepository repository = new UserRepository(usersFile);
        User user = this.createUser();
        user.setChapter(new ChapterAncientEgypt());
        user.recordAdventureLevelCompletion(ChapterType.ANCIENT_EGYPT, LevelType.NORMAL);

        QuestObj quest = user.getTravelLog().findQuest(Quest.CHAPTER_HUNTER);
        quest.setCompletionPercentage(100);
        assertTrue(quest.claimReward());

        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        user.getUnlockedPlants().add(
                new PlantFactory(user.getPlantUpgradeService()).create(peashooter)
        );
        assertTrue(repository.add(user));

        User loaded = new UserRepository(usersFile).findByUsername(user.getUsername());
        assertNotNull(loaded);
        assertEquals(ChapterType.ANCIENT_EGYPT, loaded.getChapter().getChapter());
        assertEquals(100, loaded.getTravelLog().findQuest(Quest.CHAPTER_HUNTER).getCompletionPercentage());
        assertTrue(loaded.getTravelLog().findQuest(Quest.CHAPTER_HUNTER).isRewardClaimed());
        assertEquals("Peashooter", loaded.getUnlockedPlants().get(0).getName());
        assertEquals(1, loaded.getCompletedAdventureLevelCount());
        assertTrue(loaded.isAdventureLevelUnlocked(
                ChapterType.ANCIENT_EGYPT,
                LevelType.CONVEYOR_BELT
        ));
    }

    @Test
    public void selectedChapterAndLevelConfigureTheRunningGame() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-level-route").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u shayan -p Strong#123 Strong#123 "
                        + "-n Shayan -e shayan@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u shayan -p Strong#123");

        User user = controller.getCurrentUser();
        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        user.getUnlockedPlants().add(
                new PlantFactory(user.getPlantUpgradeService()).create(peashooter)
        );

        assertEquals("GAME_MENU", controller.execute("menu enter game"));
        controller.execute("menu enter chapter -c Ancient Egypt");
        assertTrue(controller.execute("show levels").contains("NORMAL"));
        assertEquals("PLANT_PICK_MENU", controller.execute("select level -t NORMAL"));
        controller.execute("add plant -t Peashooter");
        controller.execute("start game");

        PlantZombieGame game = controller.getCurrentGame();
        assertEquals(MenuType.MID_GAME_MENU, controller.getCurrentMenu());
        assertEquals(ChapterType.ANCIENT_EGYPT, game.getActiveChapter().getChapter());
        assertEquals(LevelType.NORMAL, game.getActiveLevel().getLevelType());
        assertEquals(4, game.getWaveManager().getWaves().size());
        assertEquals(
                TerrainType.GRAVE,
                game.getBoard().getTile(new Position(5, 1)).getTerrainType()
        );
    }

    @Test
    public void meowPointLevelReceivesRealCombatKills() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        MeowPointLevel level = new MeowPointLevel(
                null,
                Collections.<Plant>emptyList(),
                100,
                game.getEngine().getClock()
        );
        game.configureLevel(level);

        Zombie zombie = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(zombie);
        level.onTick();
        game.getCombatSystem().applyDirectDamageToZombie(zombie, Integer.MAX_VALUE);

        assertEquals(15, level.getPoint());
        level.calculatePoint();
        assertEquals(115, level.getPoint());
    }

    @Test
    public void greenhouseDoesNotChoosePlantsWithoutPlantFood() {
        Main application = Main.loadApplication();
        PlantDefinition goldBloom = application.getPlantDefinitions().findByName("Gold Bloom");
        Plant plant = new PlantFactory().create(goldBloom);
        assertFalse(plant.canReceivePlantFood());

        Greenhouse greenhouse = new Greenhouse();
        String planted = greenhouse.plantRandom(
                new Position(1, 1),
                Collections.singletonList(plant),
                0,
                new Random(1) {
                    @Override
                    public boolean nextBoolean() {
                        return false;
                    }
                }
        );

        assertEquals(Greenhouse.MARIGOLD_NAME, planted);
    }

    @Test
    public void adventureProgressUnlocksLevelsOnceAndKeepsLastCompletedLevel() {
        User user = this.createUser();

        assertTrue(user.isChapterUnlocked(ChapterType.ANCIENT_EGYPT));
        assertFalse(user.isChapterUnlocked(ChapterType.ICE_CAVES));
        assertFalse(user.isAdventureLevelUnlocked(
                ChapterType.ANCIENT_EGYPT,
                LevelType.CONVEYOR_BELT
        ));

        assertTrue(user.recordAdventureLevelCompletion(
                ChapterType.ANCIENT_EGYPT,
                LevelType.NORMAL
        ));
        assertTrue(user.getUnreadNews().get(0).contains("special level"));
        assertTrue(user.isAdventureLevelUnlocked(
                ChapterType.ANCIENT_EGYPT,
                LevelType.CONVEYOR_BELT
        ));
        assertFalse(user.recordAdventureLevelCompletion(
                ChapterType.ANCIENT_EGYPT,
                LevelType.NORMAL
        ));

        assertTrue(user.recordAdventureLevelCompletion(
                ChapterType.ANCIENT_EGYPT,
                LevelType.CONVEYOR_BELT
        ));
        assertTrue(user.getUnreadNews().get(1).contains("ICE_CAVES"));
        assertTrue(user.isChapterUnlocked(ChapterType.ICE_CAVES));
        assertEquals(2, user.getCompletedAdventureLevelCount());

        LeaderboardEntry entry = new LeaderboardEntry(1, user);
        assertEquals("ANCIENT_EGYPT", entry.getLastChapter());
        assertEquals(2, entry.getLastLevel());
    }

    @Test
    public void conveyorPlantsAreGeneratedAndPlacedWithoutSunCost() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        PlantDefinition definition = application.getPlantDefinitions().findByName("Peashooter");
        Plant template = new PlantFactory().create(definition);
        ConveyorBeltLevel level = new ConveyorBeltLevel(
                null,
                Collections.singletonList(template),
                100,
                game.getEngine().getClock()
        );
        game.configureLevel(level);

        assertEquals(1, level.getConveyorPlants().size());
        int sunBeforePlanting = game.getSunSystem().getSunAmount();
        assertTrue(game.plant("Peashooter", new Position(0, 0)));
        assertEquals(sunBeforePlanting, game.getSunSystem().getSunAmount());
        assertTrue(level.getConveyorPlants().isEmpty());
        assertEquals(1, game.getBoard().getPlantsAt(new Position(0, 0)).size());
    }

    @Test
    public void conveyorSelectionSkipsPlantPickMenu() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-conveyor-route").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u conveyor -p Strong#123 Strong#123 "
                        + "-n Conveyor -e conveyor@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u conveyor -p Strong#123");

        User user = controller.getCurrentUser();
        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        user.getUnlockedPlants().add(new PlantFactory().create(peashooter));
        user.recordAdventureLevelCompletion(ChapterType.ANCIENT_EGYPT, LevelType.NORMAL);

        controller.execute("menu enter game");
        controller.execute("menu enter chapter -c Ancient Egypt");
        assertEquals("MID_GAME_MENU", controller.execute("select level -t CONVEYOR_BELT"));
        assertEquals(LevelType.CONVEYOR_BELT, controller.getCurrentGame().getActiveLevel().getLevelType());
    }

    @Test
    public void finishedGamesReturnToTheRequiredMenu() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-game-finish-route").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u finishroute -p Strong#123 Strong#123 "
                        + "-n FinishRoute -e finishroute@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u finishroute -p Strong#123");
        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        controller.getCurrentUser().getUnlockedPlants().add(new PlantFactory().create(peashooter));

        controller.execute("menu enter game");
        controller.execute("menu enter chapter -c Ancient Egypt");
        controller.execute("select level -t NORMAL");
        controller.execute("add plant -t Peashooter");
        controller.execute("start game");
        controller.getCurrentGame().getActiveLevel().setCompleted(true);
        controller.getCurrentGame().getEngine().endGame();
        controller.execute("show map");
        assertEquals(MenuType.MAIN_MENU, controller.getCurrentMenu());

        controller.execute("menu enter game");
        controller.execute("menu enter chapter -c Ancient Egypt");
        controller.execute("select level -t NORMAL");
        controller.execute("add plant -t Peashooter");
        controller.execute("start game");
        controller.getCurrentGame().getEngine().endGame();
        controller.execute("show map");
        assertEquals(MenuType.CHAPTER_MENU, controller.getCurrentMenu());
    }

    @Test
    public void firePlantThawsFrozenPlantOverTime() {
        Main application = Main.loadApplication();
        PlantDefinition normalDefinition = application.getPlantDefinitions().findByName("Peashooter");
        PlantDefinition fireDefinition = null;

        for (PlantDefinition definition : application.getPlantDefinitions().findAll()) {
            if (definition != null && definition.getTags() != null
                    && definition.getTags().contains(PlantTag.FIRE)) {
                fireDefinition = definition;
                break;
            }
        }

        assertNotNull(fireDefinition);
        Plant frozenPlant = new PlantFactory().create(normalDefinition);
        Plant firePlant = new PlantFactory().create(fireDefinition);
        model.mechanism.Board board = new model.mechanism.Board();
        Position frozenPosition = new Position(1, 1);
        Position firePosition = new Position(2, 1);
        frozenPlant.setPosition(frozenPosition);
        frozenPlant.setBoard(board);
        firePlant.setPosition(firePosition);
        firePlant.setBoard(board);
        board.getTile(frozenPosition).addPlant(frozenPlant);
        board.getTile(firePosition).addPlant(firePlant);
        frozenPlant.addFreezeLevel();
        frozenPlant.addFreezeLevel();
        frozenPlant.addFreezeLevel();

        for (int i = 0; i < 100; i++) {
            frozenPlant.onTick();
        }

        assertFalse(frozenPlant.isFrozen());
    }

    @Test
    public void applicationRoutesCommandsIntoOpenMinigame() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-minigame-route").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u minigameroute -p Strong#123 Strong#123 "
                        + "-n MiniGameRoute -e minigameroute@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u minigameroute -p Strong#123");
        controller.execute("menu enter game");
        controller.execute("menu travel-log");
        controller.execute("travel log page MINIGAMES");
        controller.execute("minigame play Vasebreaker");
        controller.execute("vasebreaker start -s 1");

        assertTrue(controller.getCurrentUser().getTravelLog()
                .findMiniGame(MiniGameType.VASEBREAKER).isStarted());
    }

    @Test
    public void gameMenuCannotBypassChapterSelectionAndCheatDoesNotOverflow() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-menu-guard").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u -guard -p Strong#123 Strong#123 "
                        + "-n Guard -e guard@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u -guard -p Strong#123");
        controller.getCurrentUser().setGold(Integer.MAX_VALUE - 1);
        controller.execute("menu enter game");

        assertTrue(controller.execute("menu enter plant pick").contains("Cannot enter"));
        controller.execute("menu cheat add 10 coin");
        assertEquals(Integer.MAX_VALUE, controller.getCurrentUser().getGold());
    }

    @Test
    public void zombotanyUsesPlantSelectionBeforeStarting() {
        Main application = Main.loadApplication();
        ZombotanyMiniGame game = application.createGame().createZombotanyMiniGame();

        assertTrue(game.preparePlantSelection(1));
        assertFalse(game.isStarted());
        assertTrue(game.addSelectedPlant("Peashooter"));
        assertFalse(game.addSelectedPlant("Peashooter"));
        assertTrue(game.startSelectedStage());
        assertTrue(game.isStarted());
        assertTrue(game.isPlantSelected("peashooter"));
        assertFalse(game.isPlantSelected("Cherry Bomb"));
    }

    @Test
    public void zombotanyWaveBudgetActuallySpawnsZombies() {
        Main application = Main.loadApplication();
        ZombotanyMiniGame game = application.createGame().createZombotanyMiniGame();

        assertTrue(game.startStage(1));
        game.onTick();

        assertTrue(game.getAliveZombieCount() > 0);
    }

    @Test
    public void zombotanyWallnutScalesCurrentAndMaximumHealthTogether() {
        Main application = Main.loadApplication();
        Zombie zombie = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                new Position(8, 0)
        );
        int originalHealth = zombie.getHealth();
        Wave wave = new Wave(1, 100, false);
        wave.addZombie(zombie);

        new ZombotanyWallnutBehavior().activate(zombie, new Board());

        assertEquals(originalHealth * 4, zombie.getHealth());
        assertEquals(originalHealth * 4, zombie.getMaximumHealth());
        assertEquals(1.0, wave.getRemainingHealthPercentage(), 0.0);
    }

    private User createUser() {
        return new User(
                "shayan",
                "hash",
                "Shayan",
                "shayan@example.com",
                1,
                "blue",
                UserGender.MALE
        );
    }
}
