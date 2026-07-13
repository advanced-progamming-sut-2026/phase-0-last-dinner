import model.Plant;
import model.mechanism.Board;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.Sun;
import model.mechanism.SunSystem;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlantZombieIntegrationTest {
    @Test
    public void bundledDefinitionsLoadFromClasspath() {
        Main application = Main.loadApplication();

        assertEquals(69, application.getPlantDefinitions().findAll().size());
        assertEquals(28, application.getZombieDefinitions().findAll().size());
    }

    @Test
    public void everyBundledPlantAndZombieCanBeCreated() {
        Main application = Main.loadApplication();
        PlantFactory plantFactory = new PlantFactory();
        Set<String> zombieAliases = new HashSet<>();

        for (PlantDefinition definition : application.getPlantDefinitions().findAll()) {
            Plant plant = plantFactory.create(definition);
            assertNotNull(definition.getName(), plant);
        }

        for (ZombieDefinition definition : application.getZombieDefinitions().findAll()) {
            Zombie zombie = application.getZombieFactory().create(definition, new Position(8, 0));
            assertNotNull(definition.getAlias(), zombie);
            assertNotNull(definition.getAlias(), zombie.getBehavior());
            assertTrue("Duplicate zombie alias: " + definition.getAlias(), zombieAliases.add(definition.getAlias()));

            if (!definition.isCanSpawnPlantFood()) {
                assertFalse(definition.getAlias(), zombie.isGlowing());
            }
        }
    }

    @Test
    public void normalPlantAndImitaterCanUseIndependentTiles() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(500);

        assertTrue(game.plant("Peashooter", new Position(0, 0)));
        assertTrue(game.plantImitater("Peashooter", new Position(0, 1)));

        Plant original = game.getBoard().getPlantsAt(new Position(0, 0)).get(0);
        Plant copy = game.getBoard().getPlantsAt(new Position(0, 1)).get(0);
        assertEquals("Imitater: Peashooter", copy.getName());
        assertTrue(original.getBehavior() != copy.getBehavior());
    }

    @Test
    public void plantProducedSunStaysPendingUntilCollected() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(100);
        assertTrue(game.plant("Sunflower", new Position(1, 1)));

        Plant sunflower = game.getBoard().getPlantsAt(new Position(1, 1)).get(0);
        game.advanceTime(240);

        SunSystem sunSystem = game.getSunSystem();
        assertTrue(sunSystem.hasUncollectedSunFrom(sunflower));
        Sun produced = null;
        for (Sun sun : sunSystem.getSuns()) {
            if (sun.getProducer() == sunflower) {
                produced = sun;
                break;
            }
        }
        assertNotNull(produced);
        int walletBeforeCollection = sunSystem.getSunAmount();
        assertEquals(produced.getValue(), sunSystem.collectSun(produced));
        assertEquals(walletBeforeCollection + produced.getValue(), sunSystem.getSunAmount());
        assertFalse(sunSystem.hasUncollectedSunFrom(sunflower));
    }

    @Test
    public void lawnMowerIsConsumedAndSecondBreachEndsGame() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        Board board = game.getBoard();

        Zombie first = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(first);
        assertTrue(board.handleZombieAtHouse(first));
        assertTrue(first.isDead());
        assertTrue(board.getLawnMower(0).isUsed());
        assertFalse(board.isBrainEaten());

        Zombie second = game.spawnZombie("ZombieDefault", 0);
        assertNotNull(second);
        assertFalse(board.handleZombieAtHouse(second));
        assertTrue(board.isBrainEaten());
    }

    @Test
    public void plantFoodIsNotSpentOnPlantWithoutAnEffect() {
        PlantZombieGame game = Main.loadApplication().createGame();
        assertTrue(game.plant("Appease-mint", new Position(0, 0)));
        assertTrue(game.getPlantFoodSystem().addPlantFood());
        assertFalse(game.getPlantFoodSystem().feedPlant(new Position(0, 0)));
        assertEquals(1, game.getPlantFoodSystem().getPlantFoodAmount());
    }
}
