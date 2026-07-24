import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.PlantZombieGame;
import model.mechanism.TerrainType;
import model.mechanism.Wave;
import model.mechanism.WaveManager;
import model.plant.PlantFactory;
import model.zombie.ConditionResistance;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieChapter;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MergedGameplaySystemsTest {
    private static final double EPSILON = 0.000001;

    @Test
    public void slipperyTerrainRejectsPlantsAndChangesZombieLane() {
        Main application = Main.loadApplication();
        Board board = new Board();
        Position slipperyPosition = new Position(7, 1);
        board.setTerrain(slipperyPosition, TerrainType.SLIPPERY_UP);

        Plant peashooter = new PlantFactory().create(
                application.getPlantDefinitions().findByName("Peashooter")
        );
        assertFalse(board.getTile(slipperyPosition).canPlacePlant(peashooter));

        ZombieDefinition definition = new ZombieDefinition(
                "TestZombie",
                "Test Zombie",
                "",
                ZombieType.BASIC,
                ZombieChapter.ALL_CHAPTERS,
                190,
                100,
                10.0,
                100,
                1,
                false,
                new ArrayList<>(),
                new ArrayList<ConditionResistance>()
        );
        Zombie zombie = new Zombie(
                definition,
                new Position(8, 1),
                definition.getHitpoints(),
                definition.getSpeed(),
                new ArrayList<ZombieArmor>(),
                new ArrayList<>(),
                null
        );
        board.addZombie(zombie, zombie.getPosition());

        zombie.move();

        assertEquals(new Position(7, 0), zombie.getPosition());
        assertTrue(board.getZombiesAt(new Position(7, 0)).contains(zombie));
    }

    @Test
    public void waveProgressIncludesZombieArmorHealth() {
        Main application = Main.loadApplication();
        ZombieDefinition armoredDefinition = null;

        for (ZombieDefinition definition : application.getZombieDefinitions().findAll()) {
            if (definition.getArmorDefinitions() != null && !definition.getArmorDefinitions().isEmpty()) {
                armoredDefinition = definition;
                break;
            }
        }

        assertTrue(armoredDefinition != null);
        Zombie zombie = application.getZombieFactory().create(armoredDefinition, new Position(8, 0));
        Wave wave = new Wave(1, 100, false);
        wave.addZombie(zombie);
        wave.start();

        int totalHealth = armoredDefinition.getHitpoints();
        for (ZombieArmor armor : zombie.getArmors()) {
            totalHealth += armor.getDefinition().getBaseHealth();
        }

        zombie.takeDamage(100);
        assertEquals((double) (totalHealth - 100) / totalHealth,
                wave.getRemainingHealthPercentage(), EPSILON);
    }

    @Test
    public void waveManagerCanBeConfiguredAfterConstruction() {
        WaveManager manager = new WaveManager();
        manager.onTick();
        assertFalse(manager.isStarted());

        Wave wave = new Wave(1, 100, true);
        manager.configureWaves(Arrays.asList(wave));
        manager.onTick();

        assertTrue(manager.isStarted());
        assertTrue(wave.isStarted());
        assertEquals(wave, manager.getCurrentWave());
    }

    @Test
    public void applicationGameWiresWaveSpawnerAndStatusService() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Wave wave = new Wave(1, 150, true);
        game.configureWaves(Arrays.asList(wave));

        game.advanceTime(1);

        assertTrue(wave.isStarted());
        assertEquals(1, game.getGameStatusService().getCurrentWaveNumber());
        assertFalse(game.getBoard().getAllZombies().isEmpty());
    }
}
