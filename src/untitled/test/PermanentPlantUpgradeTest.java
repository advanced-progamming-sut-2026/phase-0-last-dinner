import model.Plant;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import model.plant.PlantUpgradeData;
import model.plant.PlantUpgradeResult;
import model.plant.PlantUpgradeService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermanentPlantUpgradeTest {
    @Test
    public void upgradeRequiresAndSpendsBothSeedPacketsAndCoins() {
        Main application = Main.loadApplication();
        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        PlantUpgradeService upgrades = new PlantUpgradeService();

        assertEquals(
                PlantUpgradeResult.NOT_ENOUGH_SEED_PACKETS,
                upgrades.upgrade(peashooter)
        );

        upgrades.addSeedPackets("Peashooter", 10);
        upgrades.addCoins(999);
        assertEquals(PlantUpgradeResult.NOT_ENOUGH_COINS, upgrades.upgrade(peashooter));
        assertEquals(10, upgrades.getSeedPackets("Peashooter"));
        assertEquals(999, upgrades.getCoins());

        upgrades.addCoins(1);
        assertEquals(PlantUpgradeResult.SUCCESS, upgrades.upgrade(peashooter));
        assertEquals(2, upgrades.getLevel("peashooter"));
        assertEquals(0, upgrades.getSeedPackets("Peashooter"));
        assertEquals(0, upgrades.getCoins());

        PlantUpgradeData nextUpgrade =
                new PlantFactory(upgrades).create(peashooter).getUpgradeData();
        assertEquals(20, nextUpgrade.getRequiredSeedPackets());
        assertEquals(2000, nextUpgrade.getRequiredCoins());
        assertFalse(nextUpgrade.canUpgrade());
    }

    @Test
    public void factoryAppliesAllStoredUpgradeEffectsToEveryNewPlant() {
        Main application = Main.loadApplication();
        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        PlantDefinition imitater = application.getPlantDefinitions().findByName("Imitater");
        PlantUpgradeService upgrades = new PlantUpgradeService(6000);
        upgrades.addSeedPackets("Peashooter", 60);

        assertEquals(PlantUpgradeResult.SUCCESS, upgrades.upgrade(peashooter));
        assertEquals(PlantUpgradeResult.SUCCESS, upgrades.upgrade(peashooter));
        assertEquals(PlantUpgradeResult.SUCCESS, upgrades.upgrade(peashooter));
        assertEquals(PlantUpgradeResult.MAXIMUM_LEVEL_REACHED, upgrades.upgrade(peashooter));

        PlantFactory factory = new PlantFactory(upgrades);
        Plant first = factory.create(peashooter);
        Plant second = factory.create(peashooter);
        Plant copied = factory.createImitater(imitater, peashooter);

        assertPermanentLevelFourStats(first);
        assertPermanentLevelFourStats(second);
        assertEquals(1, copied.getLevel());
        assertEquals(300, copied.getMaximumHealth());
        assertEquals(100, copied.getSunCost());

        first.setPosition(new Position(0, 0));
        first.setBoard(new model.mechanism.Board());
        first.getBoard().addZombie(
                application.getZombieFactory().create(
                        application.getZombieDefinitions().findByAlias("ZombieDefault"),
                        new Position(8, 0)
                ),
                new Position(8, 0)
        );
        first.useAbility();
        assertEquals("30", first.getBoard().getProjectiles().get(0).getDamageExpression());
    }

    @Test
    public void applicationSharesCollectionProgressAcrossGames() {
        Main application = Main.loadApplication();
        PlantUpgradeService upgrades = application.getPlantUpgradeService();
        upgrades.addSeedPackets("Peashooter", 10);
        upgrades.addCoins(1000);

        assertEquals(
                PlantUpgradeResult.SUCCESS,
                application.getCollectionController().upgradePlant("Peashooter")
        );

        PlantZombieGame firstGame = application.createGame();
        PlantZombieGame secondGame = application.createGame();
        firstGame.getSunSystem().addSun(100);
        secondGame.getSunSystem().addSun(100);

        assertTrue(firstGame.plant("Peashooter", new Position(0, 0)));
        assertTrue(secondGame.plant("Peashooter", new Position(0, 0)));
        assertEquals(2, firstGame.getBoard().getPlantsAt(new Position(0, 0)).get(0).getLevel());
        assertEquals(2, secondGame.getBoard().getPlantsAt(new Position(0, 0)).get(0).getLevel());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void boardInstanceCannotUpgradeItselfForFree() {
        Main application = Main.loadApplication();
        PlantUpgradeService upgrades = application.getPlantUpgradeService();
        Plant plant = new PlantFactory(upgrades).create(
                application.getPlantDefinitions().findByName("Peashooter")
        );

        assertFalse(plant.upgrade());
        assertEquals(1, plant.getLevel());
        assertEquals(1, upgrades.getLevel("Peashooter"));
    }

    private static void assertPermanentLevelFourStats(Plant plant) {
        assertEquals(4, plant.getLevel());
        assertEquals(450, plant.getMaximumHealth());
        assertEquals(450, plant.getHealth());
        assertEquals(75, plant.getSunCost());
    }
}
