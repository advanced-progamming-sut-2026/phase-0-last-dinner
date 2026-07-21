import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.PlantZombieGame;
import model.mechanism.PlantFoodSystem;
import model.mechanism.Position;
import model.mechanism.Sun;
import model.mechanism.SunSystem;
import model.mechanism.SunType;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.mechanism.Wave;
import model.mechanism.WaveManager;
import model.mechanism.ZombieSpawner;
import model.zombie.ConditionResistance;
import model.zombie.Zombie;
import model.zombie.ZombieArmorDefinition;
import model.zombie.ZombieChapter;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.ZombieType;
import model.zombie.behavior.GraveSummonerBehavior;
import model.zombie.behavior.SunStealerBehavior;
import model.zombie.behavior.WizardBehavior;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ZombieSpawnerAndZombieSpecialsTest {
    @Test
    public void spawnerUsesReachabilityToFillTheWholeWaveBudget() {
        ZombieDefinition deadEnd = this.definition(
                "DeadEnd",
                "Dead End",
                ZombieChapter.ALL_CHAPTERS,
                6,
                10000
        );
        ZombieDefinition exact = this.definition(
                "Exact",
                "Exact Four",
                ZombieChapter.ALL_CHAPTERS,
                4,
                1
        );
        TestRepository repository = new TestRepository(Arrays.asList(deadEnd, exact));
        ZombieSpawner spawner = new ZombieSpawner(
                new ZombieFactory(repository),
                repository,
                new Board()
        );
        List<String> events = new ArrayList<>();
        spawner.setRandom(new ZeroRandom());
        spawner.setListener(events::add);

        Wave wave = new Wave(2, 8, false);
        List<Zombie> spawned = spawner.spawnWave(wave);

        assertEquals(2, spawned.size());
        assertEquals(8, this.totalWaveCost(spawned));
        assertSame(exact, spawned.get(0).getDefinition());
        assertSame(exact, spawned.get(1).getDefinition());
        assertEquals(
                "Zombie Exact Four spawned at wave 2 in lane 1 which costed 4.",
                events.get(0)
        );
    }

    @Test
    public void spawnerIncludesCommonZombiesAndFiltersOtherChapters() {
        ZombieDefinition common = this.definition(
                "Common",
                "Common",
                ZombieChapter.ALL_CHAPTERS,
                2,
                1
        );
        ZombieDefinition ancient = this.definition(
                "Ancient",
                "Ancient",
                ZombieChapter.ANCIENT_EGYPT,
                3,
                1
        );
        ZombieDefinition beach = this.definition(
                "Beach",
                "Beach",
                ZombieChapter.BIG_WAVE_BEACH,
                1,
                1
        );
        TestRepository repository = new TestRepository(Arrays.asList(common, ancient, beach));
        ZombieSpawner spawner = new ZombieSpawner(
                new ZombieFactory(repository),
                repository,
                new Board()
        );
        spawner.setRandom(new ZeroRandom());
        spawner.setActiveChapter(ZombieChapter.ANCIENT_EGYPT);

        assertNull(spawner.chooseZombieDefinition(1));
        List<Zombie> spawned = spawner.spawnWave(new Wave(1, 5, false));

        assertEquals(5, this.totalWaveCost(spawned));
        for (Zombie zombie : spawned) {
            ZombieChapter chapter = zombie.getDefinition().getChapter();
            assertTrue(chapter == ZombieChapter.ALL_CHAPTERS
                    || chapter == ZombieChapter.ANCIENT_EGYPT);
        }

        spawner.setActiveChapter(ZombieChapter.ALL_CHAPTERS);
        assertSame(beach, spawner.chooseZombieDefinition(1));
    }

    @Test
    public void spawnerRejectsAWaveWhenExactFillIsImpossible() {
        ZombieDefinition costFour = this.definition(
                "Four",
                "Four",
                ZombieChapter.BIG_WAVE_BEACH,
                4,
                1
        );
        ZombieDefinition costSix = this.definition(
                "Six",
                "Six",
                ZombieChapter.BIG_WAVE_BEACH,
                6,
                1
        );
        TestRepository repository = new TestRepository(Arrays.asList(costFour, costSix));
        ZombieSpawner spawner = new ZombieSpawner(
                new ZombieFactory(repository),
                repository,
                new Board()
        );

        assertTrue(spawner.spawnWave(new Wave(1, 7, false)).isEmpty());

        spawner.setActiveChapter(ZombieChapter.MEDIEVAL);
        assertTrue(spawner.spawnWave(new Wave(2, 8, false)).isEmpty());
    }

    @Test
    public void dataLikeWaveBudgetOf125DoesNotSpawnAPartialWave() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getZombieSpawner().setRandom(new ZeroRandom());

        List<Zombie> spawned = game.getZombieSpawner().spawnWave(new Wave(2, 125, false));

        assertTrue(spawned.isEmpty());
    }

    @Test
    public void bundledSpecialTypesAndCommonKnightChapterAreInferredCorrectly() {
        PlantZombieGame game = Main.loadApplication().createGame();
        String[] specialAliases = {
                "ZombieModernAllStar",
                "ZombieLostCityJane",
                "ZombieCrystalSkull",
                "ZombieProspector",
                "ZombiePiano",
                "ZombieArcade"
        };

        for (String alias : specialAliases) {
            ZombieDefinition definition = game.getZombieSpawner()
                    .getDefinitionRepository()
                    .findByAlias(alias);
            assertEquals(alias, ZombieType.SPECIAL, definition.getType());
        }

        ZombieDefinition knight = game.getZombieSpawner()
                .getDefinitionRepository()
                .findByAlias("ZombieDarkArmor3");
        assertEquals(ZombieChapter.ALL_CHAPTERS, knight.getChapter());
    }

    @Test
    public void wizardCanRandomlyTransformAnEligiblePlantAnywhereOnTheBoard() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Peashooter", new Position(0, 0)));
        Plant distantPlant = game.getBoard().getPlantsAt(new Position(0, 0)).get(0);
        Zombie wizard = game.spawnZombie("ZombieWizard", 4);
        WizardBehavior behavior = wizard.findBehavior(WizardBehavior.class);
        behavior.setRandom(new ZeroRandom());

        behavior.activate(wizard, game.getBoard());

        assertTrue(distantPlant.isTransformed());
        wizard.die();
        assertFalse(distantPlant.isTransformed());
    }

    @Test
    public void raHasNoLifetimeCapAndRefundsAllStolenGroundSun() {
        PlantZombieGame game = Main.loadApplication().createGame();
        SunSystem sunSystem = game.getSunSystem();
        int walletBefore = sunSystem.getSunAmount();

        for (int index = 0; index < 3; index++) {
            Sun sun = new Sun(SunType.SPECIAL, new Position(index, 0), 0);
            sun.reachGround();
            sunSystem.getSuns().add(sun);
        }

        Zombie ra = game.spawnZombie("ZombieRa", 0);
        SunStealerBehavior behavior = ra.findBehavior(SunStealerBehavior.class);
        behavior.activate(ra, game.getBoard());
        behavior.activate(ra, game.getBoard());
        behavior.activate(ra, game.getBoard());

        assertTrue(sunSystem.getSuns().isEmpty());
        ra.die();
        assertEquals(walletBefore + 3 * SunType.SPECIAL.getValue(), sunSystem.getSunAmount());
    }

    @Test
    public void tombRaiserOnlyCreatesGravesOnEmptyClassicGround() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie tombRaiser = game.spawnZombie("ZombieTombRaiser", 4);
        Board board = game.getBoard();

        for (Tile tile : board.getTiles()) {
            board.setTerrain(tile.getPosition(), TerrainType.WATER);
        }

        Position emptyClassic = new Position(0, 0);
        Position occupiedClassic = tombRaiser.getPosition();
        board.setTerrain(emptyClassic, TerrainType.CLASSIC);
        board.setTerrain(occupiedClassic, TerrainType.CLASSIC);

        GraveSummonerBehavior behavior = tombRaiser.findBehavior(GraveSummonerBehavior.class);
        behavior.setRandom(new ZeroRandom());
        behavior.activate(tombRaiser, board);

        assertEquals(TerrainType.GRAVE, board.getTile(emptyClassic).getTerrainType());
        assertEquals(TerrainType.CLASSIC, board.getTile(occupiedClassic).getTerrainType());

        int graveCount = 0;
        for (Tile tile : board.getTiles()) {
            if (tile.getTerrainType() == TerrainType.GRAVE) {
                graveCount++;
            }
        }
        assertEquals(1, graveCount);
    }

    @Test
    public void waveAndDeathEventsMatchTheProjectSpecification() {
        List<String> waveEvents = new ArrayList<>();
        WaveManager regularManager = new WaveManager(
                Collections.singletonList(new Wave(3, 0, false)),
                null
        );
        regularManager.setListener(waveEvents::add);
        regularManager.onTick();
        assertEquals("Wave 3 started.", waveEvents.get(0));

        WaveManager finalManager = new WaveManager(
                Collections.singletonList(new Wave(4, 0, true)),
                null
        );
        finalManager.setListener(waveEvents::add);
        finalManager.onTick();
        assertEquals("The final wave has come.", waveEvents.get(1));

        ZombieDefinition definition = this.definition(
                "TestType",
                "Test Type",
                ZombieChapter.ALL_CHAPTERS,
                1,
                1
        );
        TestRepository repository = new TestRepository(Collections.singletonList(definition));
        Board board = new Board();
        Zombie zombie = new ZombieFactory(repository).create(definition, new Position(3, 2));
        board.addZombie(zombie, zombie.getPosition());
        CombatSystem combat = new CombatSystem(board);
        List<String> combatEvents = new ArrayList<>();
        combat.setListener(combatEvents::add);

        combat.killZombie(zombie);

        assertEquals(
                "Zombie of type Test Type is dead at (4, 3)",
                combatEvents.get(0)
        );
    }

    @Test
    public void glowingZombiePlantFoodEventMatchesTheProjectSpecification() {
        ZombieDefinition definition = this.definition(
                "GlowingTest",
                "Glowing Test",
                ZombieChapter.ALL_CHAPTERS,
                1,
                1
        );
        TestRepository repository = new TestRepository(Collections.singletonList(definition));
        Board board = new Board();
        PlantFoodSystem plantFoodSystem = new PlantFoodSystem(board);
        CombatSystem combat = new CombatSystem(board);
        List<String> events = new ArrayList<>();
        combat.setListener(events::add);

        Zombie zombie = new ZombieFactory(repository).create(definition, new Position(2, 1));
        zombie.setGlowing(true);
        board.addZombie(zombie, zombie.getPosition());
        combat.killZombie(zombie);

        assertEquals(1, plantFoodSystem.getPlantFoodAmount());
        assertEquals(
                "The glowing zombie dropeed a plant food; you have 1 plant foods now.",
                events.get(0)
        );
        assertEquals("Zombie of type Glowing Test is dead at (3, 2)", events.get(1));
    }

    private int totalWaveCost(List<Zombie> zombies) {
        int total = 0;
        for (Zombie zombie : zombies) {
            total += zombie.getDefinition().getWavePointCost();
        }
        return total;
    }

    private ZombieDefinition definition(
            String alias,
            String displayName,
            ZombieChapter chapter,
            int cost,
            int weight
    ) {
        return new ZombieDefinition(
                alias,
                displayName,
                "",
                ZombieType.BASIC,
                chapter,
                190,
                100,
                0.1,
                cost,
                weight,
                false,
                Collections.<ZombieArmorDefinition>emptyList(),
                Collections.<ConditionResistance>emptyList()
        );
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be positive");
            }
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0;
        }
    }

    private static final class TestRepository implements ZombieDefinitionRepository {
        private final List<ZombieDefinition> definitions;

        private TestRepository(List<ZombieDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public ZombieDefinition findByAlias(String alias) {
            for (ZombieDefinition definition : this.definitions) {
                if (definition.getAlias().equalsIgnoreCase(alias)) {
                    return definition;
                }
            }
            return null;
        }

        @Override
        public List<ZombieDefinition> findByChapter(ZombieChapter chapter) {
            List<ZombieDefinition> matching = new ArrayList<>();
            for (ZombieDefinition definition : this.definitions) {
                if (chapter == ZombieChapter.ALL_CHAPTERS
                        || definition.getChapter() == ZombieChapter.ALL_CHAPTERS
                        || definition.getChapter() == chapter) {
                    matching.add(definition);
                }
            }
            return matching;
        }

        @Override
        public List<ZombieDefinition> findAll() {
            return this.definitions;
        }
    }
}
