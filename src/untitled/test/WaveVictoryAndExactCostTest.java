import model.User.User;
import model.level.NormalLevel;
import model.level.Level;
import model.level.LevelFactory;
import model.level.LevelType;
import model.mechanism.Board;
import model.mechanism.DifficultyConfig;
import model.mechanism.GameClock;
import model.mechanism.GameEngine;
import model.mechanism.Position;
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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WaveVictoryAndExactCostTest {
    @Test
    public void levelVictoryWaitsForLivingZombiesOutsideTheWaves() {
        NormalLevel level = new NormalLevel(null, Collections.emptyList(), 2400);
        for (Wave wave : level.getWaves()) {
            wave.start();
        }

        ZombieDefinition definition = this.definition("Extra", 100);
        Zombie zombie = new ZombieFactory().create(definition, new Position(8, 0));
        level.getBoard().addZombie(zombie, zombie.getPosition());

        assertFalse(level.isWinConditionMet());

        zombie.die();

        assertTrue(level.isWinConditionMet());
    }

    @Test
    public void waveManagerWaitsForLivingZombiesOutsideTheWaves() {
        ZombieDefinition definition = this.definition("Extra", 100);
        TestRepository repository = new TestRepository(Collections.singletonList(definition));
        Board board = new Board();
        ZombieSpawner spawner = new ZombieSpawner(new ZombieFactory(repository), repository, board);
        GameEngine engine = new GameEngine(board);
        WaveManager manager = new WaveManager(
                Collections.singletonList(new Wave(1, 0, true)),
                spawner,
                engine
        );
        Zombie zombie = new ZombieFactory(repository).create(definition, new Position(8, 0));
        board.addZombie(zombie, zombie.getPosition());

        manager.onTick();
        manager.onTick();

        assertTrue(engine.isGameRunning());

        zombie.die();
        manager.onTick();

        assertFalse(engine.isGameRunning());
    }

    @Test
    public void mowerKilledWaveAllowsNextWaveToStart() {
        ZombieDefinition definition = this.definition("Mower target", 100);
        TestRepository repository = new TestRepository(Collections.singletonList(definition));
        Board board = new Board();
        Zombie zombie = new ZombieFactory(repository).create(definition, new Position(8, 0));
        board.addZombie(zombie, zombie.getPosition());

        Wave firstWave = new Wave(1, 100, false);
        firstWave.addZombie(zombie);
        Wave finalWave = new Wave(2, 100, true);
        WaveManager manager = new WaveManager(Arrays.asList(firstWave, finalWave), null);

        manager.onTick();
        assertTrue(board.handleZombieAtHouse(zombie));
        assertEquals(0.0, firstWave.getRemainingHealthPercentage(), 0.0);

        manager.onTick();
        assertEquals(finalWave, manager.getCurrentWave());
    }

    @Test
    public void spawnerUsesOnlyAnExactWaveCostCombination() {
        ZombieDefinition costFour = this.definition("Four", 4);
        ZombieDefinition costSix = this.definition("Six", 6);
        TestRepository repository = new TestRepository(Arrays.asList(costFour, costSix));
        ZombieSpawner spawner = new ZombieSpawner(
                new ZombieFactory(repository),
                repository,
                new Board()
        );
        spawner.setRandom(new ZeroRandom());

        List<Zombie> exact = spawner.spawnWave(new Wave(1, 8, false));
        List<Zombie> impossible = spawner.spawnWave(new Wave(2, 7, false));

        assertEquals(8, this.totalAdjustedCost(exact, 3));
        assertTrue(impossible.isEmpty());
    }

    @Test
    public void normalLevelBudgetsAreExactlySpawnableAtEveryDifficulty() {
        int[] expectedBudgets = {2400, 3000, 3750, 7500};

        for (int difficultyLevel = 1; difficultyLevel <= 5; difficultyLevel++) {
            Main application = Main.loadApplication();
            Board board = new Board();
            ZombieSpawner spawner = new ZombieSpawner(
                    application.getZombieFactory(),
                    application.getZombieDefinitions(),
                    board
            );
            User user = new User();
            user.setDifficultyLevel(difficultyLevel);
            spawner.setDifficultyConfig(new DifficultyConfig(user));
            spawner.setRandom(new ZeroRandom());
            NormalLevel level = new NormalLevel(null, Collections.emptyList(), 2400);

            for (int index = 0; index < expectedBudgets.length; index++) {
                Wave wave = level.getWaves().get(index);
                List<Zombie> spawned = spawner.spawnWave(wave);

                assertFalse(spawned.isEmpty());
                assertEquals(expectedBudgets[index], this.totalAdjustedCost(spawned, difficultyLevel));
            }
        }
    }

    @Test
    public void specialAndScoringLevelsUseTheSameFourExactBudgets() {
        int[] expectedBudgets = {2400, 3000, 3750, 7500};
        LevelType[] levelTypes = {
                LevelType.CONVEYOR_BELT,
                LevelType.DEADLINE,
                LevelType.NIGHT_OPS,
                LevelType.LOVE_YOUR_PLANTS,
                LevelType.MEOW_POINT
        };
        LevelFactory factory = new LevelFactory();

        for (LevelType levelType : levelTypes) {
            Level level = factory.create(
                    levelType,
                    null,
                    Collections.emptyList(),
                    2400,
                    new GameClock()
            );

            assertEquals(4, level.getWaves().size());
            for (int index = 0; index < expectedBudgets.length; index++) {
                assertEquals(
                        expectedBudgets[index],
                        level.getWaves().get(index).getDifficulty(),
                        0.0
                );
            }
        }
    }

    private int totalAdjustedCost(List<Zombie> zombies, int difficultyLevel) {
        int total = 0;

        for (Zombie zombie : zombies) {
            total += Math.max(1, (int) Math.round(
                    zombie.getDefinition().getWavePointCost() * 3.0 / difficultyLevel
            ));
        }

        return total;
    }

    private ZombieDefinition definition(String alias, int cost) {
        return new ZombieDefinition(
                alias,
                alias,
                "",
                ZombieType.BASIC,
                ZombieChapter.ALL_CHAPTERS,
                190,
                100,
                0.1,
                cost,
                1,
                false,
                Collections.<ZombieArmorDefinition>emptyList(),
                Collections.<ConditionResistance>emptyList()
        );
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
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
            return new ArrayList<>(this.definitions);
        }

        @Override
        public List<ZombieDefinition> findAll() {
            return this.definitions;
        }
    }
}
