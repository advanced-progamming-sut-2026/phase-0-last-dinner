import college.java.project.Main;import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.minigame.vasebreakerminigame.PlantZombieVasebreakerIntegration;
import model.minigame.vasebreakerminigame.VasebreakerActionStatus;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.plant.PlantDefinition;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class VasebreakerIntegrationTest {
    @Test
    public void defaultVasebreakerUsesReadyRuntime() {
        VasebreakerMiniGame game = new VasebreakerMiniGame();

        assertTrue(game.isIntegrationReady());
        assertEquals(VasebreakerActionStatus.STARTED, game.startStage(1).getStatus());
        assertFalse(game.getVases().isEmpty());
    }

    @Test
    public void translatesBothOneBasedCoordinates() {
        Main application = Main.loadApplication();
        PlantZombieVasebreakerIntegration integration = this.createIntegration(application);
        ZombieDefinition zombieDefinition = integration.chooseRegularZombieDefinition(1);

        assertNotNull(zombieDefinition);
        assertTrue(integration.releaseZombie(zombieDefinition, new Position(9, 5)));
        assertEquals(1, integration.getBoard().getZombiesAt(new Position(8, 4)).size());

        PlantDefinition repeater = application.getPlantDefinitions().findByName("Repeater");
        assertTrue(integration.plantFromSeedPacket(repeater, new Position(8, 5)));
        assertEquals(1, integration.getBoard().getPlantsAt(new Position(7, 4)).size());
    }

    @Test
    public void seedPacketsBypassSunAndCooldownButKeepStackRules() {
        Main application = Main.loadApplication();
        PlantZombieVasebreakerIntegration integration = this.createIntegration(application);
        PlantDefinition repeater = application.getPlantDefinitions().findByName("Repeater");
        int sunBefore = integration.getSunSystem().getSunAmount();

        assertTrue(repeater.getCost() > sunBefore);
        assertTrue(integration.plantFromSeedPacket(repeater, new Position(1, 1)));
        assertTrue(integration.plantFromSeedPacket(repeater, new Position(2, 1)));
        assertEquals(sunBefore, integration.getSunSystem().getSunAmount());
        assertFalse(integration.plantFromSeedPacket(repeater, new Position(1, 1)));

        PlantDefinition peaPod = application.getPlantDefinitions().findByName("Pea Pod");
        assertTrue(integration.plantFromSeedPacket(peaPod, new Position(3, 1)));
        assertTrue(integration.plantFromSeedPacket(peaPod, new Position(3, 1)));
        assertEquals(2, integration.getBoard().getPlantsAt(new Position(2, 0)).size());
    }

    @Test
    public void tracksOnlyLivingZombiesReleasedFromVases() {
        Main application = Main.loadApplication();
        PlantZombieVasebreakerIntegration integration = this.createIntegration(application);
        ZombieDefinition definition = integration.chooseRegularZombieDefinition(1);

        assertTrue(integration.releaseZombie(definition, new Position(9, 1)));
        Zombie released = integration.getBoard().getZombiesAt(new Position(8, 0)).get(0);

        Zombie unrelated = application.getZombieFactory().create(definition, new Position(7, 0));
        integration.getBoard().addZombie(unrelated, new Position(7, 0));
        assertTrue(integration.hasAliveVasebreakerZombies());

        released.die();
        assertFalse(integration.hasAliveVasebreakerZombies());
        assertFalse(unrelated.isDead());
    }

    @Test
    public void reportsBrainStateFromTheRuntimeBoard() {
        Main application = Main.loadApplication();
        PlantZombieVasebreakerIntegration integration = this.createIntegration(application);
        ZombieDefinition definition = integration.chooseRegularZombieDefinition(1);

        assertTrue(integration.releaseZombie(definition, new Position(1, 1)));
        Zombie firstZombie = integration.getBoard().getZombiesAt(new Position(0, 0)).get(0);
        assertTrue(integration.getBoard().handleZombieAtHouse(firstZombie));
        assertFalse(integration.isBrainEaten());

        assertTrue(integration.releaseZombie(definition, new Position(1, 1)));
        Zombie secondZombie = integration.getBoard().getZombiesAt(new Position(0, 0)).get(0);
        assertFalse(integration.getBoard().handleZombieAtHouse(secondZombie));
        assertTrue(integration.isBrainEaten());
    }

    @Test
    public void advancesExactlyOneTickAndPrepareStageResetsRuntime() {
        Main application = Main.loadApplication();
        PlantZombieVasebreakerIntegration integration = this.createIntegration(application);
        long tickBefore = integration.getEngine().getClock().getCurrentTick();

        integration.advanceOneTick();
        assertEquals(tickBefore + 1, integration.getEngine().getClock().getCurrentTick());

        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        ZombieDefinition zombie = integration.chooseRegularZombieDefinition(1);
        assertTrue(integration.plantFromSeedPacket(peashooter, new Position(2, 2)));
        assertTrue(integration.releaseZombie(zombie, new Position(9, 2)));
        Board oldBoard = integration.getBoard();

        integration.prepareStage(2);

        assertNotSame(oldBoard, integration.getBoard());
        assertEquals(2, integration.getStageNumber());
        assertEquals(0, integration.getEngine().getClock().getCurrentTick());
        assertTrue(integration.getBoard().getAllPlants().isEmpty());
        assertTrue(integration.getBoard().getAllZombies().isEmpty());
        assertFalse(integration.isBrainEaten());
        assertFalse(integration.hasAliveVasebreakerZombies());
        assertFalse(integration.getSunSystem().isAutomaticSunEnabled());
    }

    @Test
    public void disablesSkySunWithoutDisablingPlantSun() {
        Main application = Main.loadApplication();
        PlantZombieVasebreakerIntegration integration = this.createIntegration(application);

        for (int i = 0; i < 250; i++) {
            integration.advanceOneTick();
        }
        assertTrue(integration.getSunSystem().getSuns().isEmpty());

        PlantDefinition sunflower = application.getPlantDefinitions().findByName("Sunflower");
        assertTrue(integration.plantFromSeedPacket(sunflower, new Position(2, 2)));

        for (int i = 0; i < 240; i++) {
            integration.advanceOneTick();
        }

        assertFalse(integration.getSunSystem().getSuns().isEmpty());
        Plant producer = integration.getBoard().getPlantsAt(new Position(1, 1)).get(0);
        assertTrue(integration.getSunSystem().hasUncollectedSunFrom(producer));
    }

    private PlantZombieVasebreakerIntegration createIntegration(Main application) {
        return new PlantZombieVasebreakerIntegration(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory()
        );
    }
}
