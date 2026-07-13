import controller.MidGameController;
import model.Plant;
import model.mechanism.PlantStatus;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.plant.PlantFactory;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MidGameStatusCommandTest {
    @Test
    public void plantAvailabilityRequiresEnoughSunAndFinishedCooldown() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        Plant peashooter = new PlantFactory().create(
                application.getPlantDefinitions().findByName("Peashooter")
        );

        List<PlantStatus> statuses = game.getGameStatusService()
                .getPlantsStatus(Arrays.asList(peashooter));
        assertEquals(1, statuses.size());
        assertFalse(statuses.get(0).isAvailable());

        game.getSunSystem().addSun(peashooter.getSunCost());
        statuses = game.getGameStatusService().getPlantsStatus(Arrays.asList(peashooter));
        assertTrue(statuses.get(0).isAvailable());

        assertTrue(game.getPlantingSystem().canPlant(peashooter, new Position(0, 0)));
        game.getPlantingSystem().plant(peashooter, new Position(0, 0));
        statuses = game.getGameStatusService().getPlantsStatus(Arrays.asList(peashooter));
        assertFalse(statuses.get(0).isAvailable());
        assertTrue(statuses.get(0).getRemainingCooldownTicks() > 0);
    }

    @Test
    public void zombieInfoShowsExactPositionArmorAndTimedEffects() {
        PlantZombieGame game = Main.loadApplication().createGame();
        MidGameController controller = new MidGameController(game);
        Zombie zombie = controller.spawnZombieCheat("ZombieArmor1", 3, 4);

        assertTrue(zombie != null);
        assertTrue(game.getBoard().getZombiesAt(new Position(2, 3)).contains(zombie));

        zombie.move();
        zombie.takeDamage(100);
        zombie.addCondition(ZombieCondition.CHILLED, 32);

        String output = controller.executeCommand("zombies info");
        assertTrue(output.contains("position: 2.815, 4"));
        assertTrue(output.contains("health: " + zombie.getHealth()));
        assertTrue(output.contains("cone: 270"));
        assertTrue(output.contains("chilled: 3.2s"));
    }

    @Test
    public void spawnCommandUsesOneBasedCoordinatesAndRejectsInvalidInput() {
        PlantZombieGame game = Main.loadApplication().createGame();
        MidGameController controller = new MidGameController(game);

        String result = controller.executeCommand(
                "cheat spawn-zombie -t ZombieDefault -l (9, 5)"
        );

        assertTrue(result.contains("spawned at 9, 5"));
        assertEquals(1, game.getBoard().getZombiesAt(new Position(8, 4)).size());
        assertNull(controller.spawnZombieCheat("missing-zombie", 3, 2));
        assertNull(controller.spawnZombieCheat("ZombieDefault", 0, 2));
        assertEquals("Invalid mid-game command.", controller.executeCommand("something else"));
    }
}
