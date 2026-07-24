import controller.ApplicationController;
import model.Plant;
import model.GameMenuRelated.Quest;
import model.User.UserRepository;
import model.User.User;
import model.chapters.ChapterBigWaveBeach;
import model.chapters.ChapterMedieval;
import model.level.DeadlineLevel;
import model.level.Level;
import model.level.LevelType;
import model.level.LoveYourPlantsLevel;
import model.level.NormalLevel;
import model.mechanism.Board;
import model.mechanism.GameEngine;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tickable;
import model.mechanism.Wave;
import model.minigame.beghouledminigame.BeghouledMiniGame;
import model.minigame.beghouledminigame.PlantUpgradeOption;
import model.minigame.wallnutbowlingminigame.BowlingWallnutType;
import model.minigame.wallnutbowlingminigame.RollingWallnut;
import model.minigame.wallnutbowlingminigame.WallnutBowlingIntegration;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.plant.PlantDefinition;
import model.zombie.Zombie;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UntestedGameSectionsRegressionTest {
    @Test
    public void endingGameStopsRemainingTickablesInCurrentTick() {
        GameEngine engine = new GameEngine();
        AtomicInteger laterTicks = new AtomicInteger();
        engine.register(engine::endGame);
        engine.register(laterTicks::incrementAndGet);

        engine.advanceTime();

        assertEquals(0, laterTicks.get());
    }

    @Test
    public void reconfiguringLevelStopsPreviousTickableLevel() {
        PlantZombieGame game = Main.loadApplication().createGame();
        CountingLevel first = new CountingLevel();
        CountingLevel second = new CountingLevel();

        game.configureLevel(first);
        game.configureLevel(second);
        game.advanceTime(1);

        assertEquals(0, first.getTickCount());
        assertEquals(1, second.getTickCount());
    }

    @Test
    public void waveEndFinalizesLevelBeforeEngineStops() {
        PlantZombieGame game = Main.loadApplication().createGame();
        SingleWaveLevel level = new SingleWaveLevel();
        game.configureLevel(level);
        Zombie zombie = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(zombie);
        level.getWaves().get(0).addZombie(zombie);

        game.advanceTime(1);
        assertFalse(level.isCompleted());
        game.getCombatSystem().applyDirectDamageToZombie(zombie, Integer.MAX_VALUE);
        game.advanceTime(1);

        assertTrue(level.isCompleted());
        assertFalse(game.getEngine().isGameRunning());
    }

    @Test
    public void mowerAlsoKillsZombiesCreatedByDeathHandlers() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie barrelRoller = game.spawnZombie("ZombieBarrelRoller", 0);

        assertNotNull(barrelRoller);
        assertTrue(game.getBoard().handleZombieAtHouse(barrelRoller));
        assertTrue(game.getBoard().getLawnMower(0).isUsed());
        assertFalse(game.getBoard().hasLivingZombies());
    }

    @Test
    public void beghouledUpgradeUpdatesFutureRefillTypes() throws Exception {
        BeghouledMiniGame game = Main.loadApplication().createGame().createBeghouledMiniGame();
        assertTrue(game.startStage(1));
        PlantUpgradeOption option = this.findUpgradePresentOnBoard(game);
        assertNotNull(option);

        Field sunAmount = BeghouledMiniGame.class.getDeclaredField("sunAmount");
        sunAmount.setAccessible(true);
        sunAmount.setInt(game, option.getSunCost());
        game.upgradePlants(option);

        assertEquals(0, game.getSunAmount());
        for (PlantDefinition definition : game.getAvailablePlantTypes()) {
            assertFalse(definition.getName().equalsIgnoreCase(
                    option.getSourcePlant().getName()
            ));
        }
    }

    @Test
    public void medievalGraveDoesNotFormUnderZombie() {
        Main application = Main.loadApplication();
        Board board = new Board();
        Position occupiedPosition = new Position(4, 2);
        for (model.mechanism.Tile tile : board.getTiles()) {
            tile.setTerrainType(TerrainType.WATER);
        }
        board.getTile(occupiedPosition).setTerrainType(TerrainType.CLASSIC);
        Zombie zombie = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                occupiedPosition
        );
        board.addZombie(zombie, occupiedPosition);

        new ChapterMedieval().spawnGrave(board);

        assertEquals(TerrainType.CLASSIC, board.getTile(occupiedPosition).getTerrainType());
    }

    @Test
    public void deadlineLossIsLatchedBetweenBatchedTicks() {
        PlantZombieGame game = Main.loadApplication().createGame();
        DeadlineLevel level = new DeadlineLevel(
                null,
                Collections.<Plant>emptyList(),
                0
        );
        game.configureLevel(level);
        Zombie zombie = game.spawnZombie("ZombieDefault", new Position(4, 0));
        assertNotNull(zombie);
        zombie.setCurrentSpeed(10);
        game.getBoard().addProjectile(new Projectile(
                "10000",
                new Position(1, 0),
                1,
                ProjectileType.NORMAL,
                null
        ));

        game.advanceTime(2);

        assertFalse(game.getEngine().isGameRunning());
    }

    @Test
    public void loveYourPlantsCountsSelfRemovingPlants() {
        PlantZombieGame game = Main.loadApplication().createGame();
        LoveYourPlantsLevel level = new LoveYourPlantsLevel(
                null,
                Collections.<Plant>emptyList(),
                0
        );
        game.configureLevel(level);
        game.getSunSystem().addSun(1000);

        for (int column = 0; column < 5; column++) {
            game.getPlantingSystem().removeAllCooldowns();
            assertTrue(game.plant("Cherry Bomb", new Position(column, 0)));
        }

        assertTrue(level.isLoseConditionMet());
    }

    @Test
    public void almostWinnerCountsOnlyRowsWithoutMower() {
        Main application = Main.loadApplication();
        User user = new User();
        user.initializeMissingFields();
        PlantZombieGame game = new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory(),
                user.getPlantUpgradeService(),
                null,
                user
        );
        game.configureLevel(new NormalLevel(
                null,
                Collections.<Plant>emptyList(),
                0
        ));

        this.killFirstColumnZombies(game, 0, 10);
        assertEquals(99, user.getTravelLog().findQuest(
                Quest.ALMOST_WINNER
        ).getCompletionPercentage());

        Zombie mowerTarget = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(mowerTarget);
        assertTrue(game.getBoard().handleZombieAtHouse(mowerTarget));
        assertEquals(0, user.getTravelLog().findQuest(
                Quest.ALMOST_WINNER
        ).getCompletionPercentage());

        this.killFirstColumnZombies(game, 1, 10);
        game.getQuestProgressTracker().onLevelFinished(true);
        assertEquals(100, user.getTravelLog().findQuest(
                Quest.ALMOST_WINNER
        ).getCompletionPercentage());
    }

    @Test
    public void beachWaterLevelChangesForEveryWave() {
        ChapterBigWaveBeach chapter = new ChapterBigWaveBeach();
        Board board = chapter.buildBoard();
        String previousState = this.terrainState(board);

        for (int index = 0; index < 100; index++) {
            chapter.changeWaterLevel(board);
            String currentState = this.terrainState(board);
            assertFalse(previousState.equals(currentState));
            previousState = currentState;
        }
    }

    @Test
    public void wallnutHitsZombieOnItsStartingTileBeforeMoving() {
        Position start = new Position(3, 1);
        StartingTileZombieIntegration integration =
                new StartingTileZombieIntegration(start);
        RollingWallnut wallnut = new RollingWallnut(
                BowlingWallnutType.BOWLING_WALLNUT,
                start,
                100,
                500,
                1,
                integration
        );

        wallnut.onTick();

        assertEquals(1, integration.getDamageCalls());
        assertEquals(start, wallnut.getPosition());

        wallnut.onTick();

        assertEquals(new Position(4, 2), wallnut.getPosition());
    }

    @Test
    public void genericMeowPlantPickStartsMeowPointLevel() throws Exception {
        ApplicationController controller = this.loggedInController("meowroute");

        assertEquals("MEOW_POINT_MENU", controller.execute("menu meow-point"));
        assertEquals("PLANT_PICK_MENU", controller.execute("menu enter plant-pick"));
        controller.execute("add plant -t Peashooter");
        controller.execute("start game");

        assertEquals(LevelType.MEOW_POINT,
                controller.getCurrentGame().getActiveLevel().getLevelType());
    }

    @Test
    public void genericMenuCanEnterGreenhouse() throws Exception {
        ApplicationController controller = this.loggedInController("greenhouseroute");

        assertEquals("GAME_MENU", controller.execute("menu enter game"));
        assertEquals("GREENHOUSE_MENU", controller.execute("menu enter greenhouse"));
    }

    @Test
    public void openMinigameDoesNotCaptureGlobalMenuCommands() throws Exception {
        ApplicationController controller = this.loggedInController("minigamemenu");
        this.openVasebreaker(controller);

        assertEquals("TRAVEL_LOG_MENU", controller.execute("menu show current"));
        assertEquals("GAME_MENU", controller.execute("menu exit"));
        controller.execute("menu travel-log");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previousOutput = System.out;
        try {
            System.setOut(new PrintStream(output));
            controller.execute("back to game menu");
        } finally {
            System.setOut(previousOutput);
        }

        assertEquals(model.Menu.MenuType.GAME_MENU, controller.getCurrentMenu());
        assertTrue(output.toString().contains("Returning to game menu."));
        assertFalse(output.toString().contains("Invalid"));
    }

    @Test
    public void zombieSpeedIsAppliedPerSecond() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie zombie = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(zombie);
        double startingX = zombie.getExactX();

        game.advanceTime(10);

        assertEquals(startingX - zombie.getCurrentSpeed(), zombie.getExactX(), 0.000001);
    }

    @Test
    public void peashooterDefeatsBasicZombieBeforeItReachesTheMower() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(50);
        assertTrue(game.plant("Peashooter", new Position(0, 0)));
        Zombie zombie = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(zombie);

        game.advanceTime(250);

        assertTrue(zombie.isDead());
        assertFalse(game.getBoard().getLawnMower(0).isUsed());
        assertFalse(game.getBoard().getPlantsAt(new Position(0, 0)).isEmpty());
    }

    @Test
    public void walkedBreachUsesMowerBeforeASecondBreachEndsGame() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie first = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(first);

        game.advanceTime(500);

        assertTrue(first.isDead());
        assertTrue(game.getBoard().getLawnMower(0).isUsed());
        assertFalse(game.getBoard().isBrainEaten());
        assertTrue(game.getEngine().isGameRunning());

        Zombie second = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(second);
        game.advanceTime(500);

        assertTrue(game.getBoard().isBrainEaten());
        assertFalse(game.getEngine().isGameRunning());
    }

    private PlantUpgradeOption findUpgradePresentOnBoard(BeghouledMiniGame game) {
        for (PlantUpgradeOption option : game.getUpgradeOptions()) {
            for (int y = 1; y <= 5; y++) {
                for (int x = 1; x <= 9; x++) {
                    PlantDefinition plant = game.getIntegration().getPlantAt(new Position(x, y));
                    if (plant != null && plant.getName().equalsIgnoreCase(
                            option.getSourcePlant().getName()
                    )) {
                        return option;
                    }
                }
            }
        }
        return null;
    }

    private void killFirstColumnZombies(PlantZombieGame game, int row, int count) {
        for (int index = 0; index < count; index++) {
            Zombie zombie = game.spawnZombie(
                    "ZombieDefault",
                    new Position(0, row)
            );
            assertNotNull(zombie);
            game.getCombatSystem().applyDirectDamageToZombie(
                    zombie,
                    Integer.MAX_VALUE
            );
        }
    }

    private String terrainState(Board board) {
        StringBuilder state = new StringBuilder();
        for (model.mechanism.Tile tile : board.getTiles()) {
            state.append(tile.getTerrainType()).append('|');
        }
        return state.toString();
    }

    private ApplicationController loggedInController(String username) throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-untested-route").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );
        controller.execute(
                "register -u " + username + " -p Strong#123 Strong#123 "
                        + "-n Player -e " + username + "@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u " + username + " -p Strong#123");
        return controller;
    }

    private void openVasebreaker(ApplicationController controller) {
        controller.execute("menu enter game");
        controller.execute("menu travel-log");
        controller.execute("travel log page MINIGAMES");
        controller.execute("minigame play Vasebreaker");
        controller.execute("vasebreaker start -s 1");
    }

    private static final class CountingLevel extends Level implements Tickable {
        private int tickCount;

        private CountingLevel() {
            super(LevelType.DEADLINE);
        }

        @Override
        public void start() {
            this.setStarted(true);
        }

        @Override
        public boolean isWinConditionMet() {
            return false;
        }

        @Override
        public boolean isLoseConditionMet() {
            return false;
        }

        @Override
        public void onTick() {
            this.tickCount++;
        }

        private int getTickCount() {
            return this.tickCount;
        }
    }

    private static final class SingleWaveLevel extends Level {
        private SingleWaveLevel() {
            super(LevelType.NORMAL);
            this.setWaves(Collections.singletonList(new Wave(1, 0, true)));
        }

        @Override
        public void start() {
            this.setStarted(true);
        }

        @Override
        public boolean isWinConditionMet() {
            return this.areAllWavesDefeated();
        }

        @Override
        public boolean isLoseConditionMet() {
            return false;
        }
    }

    private static final class StartingTileZombieIntegration
            implements WallnutBowlingIntegration {
        private final Position zombiePosition;
        private int damageCalls;

        private StartingTileZombieIntegration(Position zombiePosition) {
            this.zombiePosition = zombiePosition;
        }

        private int getDamageCalls() {
            return this.damageCalls;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void prepareStage(int stageNumber) {
        }

        @Override
        public void startZombieWaves(int stageNumber) {
        }

        @Override
        public int getNormalZombieHealth() {
            return 100;
        }

        @Override
        public int getCherryBombDamage() {
            return 500;
        }

        @Override
        public boolean hasZombieAt(Position position) {
            return this.zombiePosition.equals(position);
        }

        @Override
        public void damageFirstZombieAt(Position position, int damage) {
            this.damageCalls++;
        }

        @Override
        public void crushZombiesAt(Position position) {
        }

        @Override
        public void explodeAt(Position centre, int radius, int damage) {
        }

        @Override
        public void advanceOneTick() {
        }

        @Override
        public boolean areAllWavesFinished() {
            return false;
        }

        @Override
        public boolean hasAliveZombies() {
            return true;
        }

        @Override
        public boolean isBrainEaten() {
            return false;
        }
    }
}
