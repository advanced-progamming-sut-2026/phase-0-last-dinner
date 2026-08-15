import college.java.project.graphics.ControllerPlantCollectionDataSource;
import college.java.project.graphics.ControllerZombieCollectionDataSource;
import college.java.project.graphics.PlantCollectionFilter;
import controller.CollectionController;
import model.User.User;
import model.collection.CollectionActionResult;
import model.collection.PlantCollectionState;
import model.collection.ZombieCollectionState;
import model.plant.CsvPlantDefinitionRepository;
import model.plant.PlantDefinition;
import model.plant.PlantDefinitionRepository;
import model.plant.PlantFactory;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.ZombieDefinitionRepository;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionGraphicsIntegrationTest {
    @Test
    public void graphicalDataSourcesUseTheLoggedInUsersCollectionState() throws Exception {
        PlantDefinitionRepository plants = plants();
        ZombieDefinitionRepository zombies = zombies();
        User user = new User();
        user.initializeMissingFields();
        user.setGold(3000);
        PlantDefinition peashooter = plants.findByName("Peashooter");
        user.getUnlockedPlants().add(
                new PlantFactory(user.getPlantUpgradeService()).create(peashooter)
        );
        user.recordEncounteredZombie("ZombieDefault");

        CollectionController controller = new CollectionController(user, plants, zombies);
        ControllerPlantCollectionDataSource plantSource =
                new ControllerPlantCollectionDataSource(controller);
        ControllerZombieCollectionDataSource zombieSource =
                new ControllerZombieCollectionDataSource(controller);

        List<PlantCollectionState> plantStates = plantSource.getPlants();
        List<ZombieCollectionState> zombieStates = zombieSource.loadZombies();

        assertEquals(69, plantStates.size());
        assertTrue(findPlant(plantStates, "Peashooter").isUnlocked());
        assertEquals(28, zombieStates.size());
        assertTrue(findZombie(zombieStates, "ZombieDefault").isEncountered());
        assertFalse(findZombie(zombieStates, "ZombieArmor1").isEncountered());
    }

    @Test
    public void successfulCollectionActionsAreSavedImmediately() throws Exception {
        PlantDefinitionRepository plants = plants();
        User user = new User();
        user.initializeMissingFields();
        user.setGold(3000);
        AtomicInteger saves = new AtomicInteger();
        CollectionController controller = new CollectionController(user, plants, zombies());
        ControllerPlantCollectionDataSource source = new ControllerPlantCollectionDataSource(
                controller,
                null,
                false,
                saves::incrementAndGet
        );

        CollectionActionResult result = source.purchasePlant("Sunflower");

        assertTrue(result.isSuccessful());
        assertEquals(1, saves.get());
        assertEquals(1000, user.getGold());
    }

    @Test
    public void upgradeableFilterAlsoChecksAvailableCoins() throws Exception {
        PlantDefinitionRepository plants = plants();
        User user = new User();
        user.initializeMissingFields();
        PlantDefinition peashooter = plants.findByName("Peashooter");
        user.getUnlockedPlants().add(
                new PlantFactory(user.getPlantUpgradeService()).create(peashooter)
        );
        user.getPlantUpgradeService().addSeedPackets("Peashooter", 10);
        CollectionController controller = new CollectionController(user, plants, zombies());
        PlantCollectionState state = findPlant(
                controller.onShowAllPlantsRequested().getPlants(),
                "Peashooter"
        );

        assertFalse(PlantCollectionFilter.UPGRADEABLE.matches(state, 999));
        assertTrue(PlantCollectionFilter.UPGRADEABLE.matches(state, 1000));
    }

    private PlantDefinitionRepository plants() throws Exception {
        return CsvPlantDefinitionRepository.fromClasspath("data/plants.csv");
    }

    private ZombieDefinitionRepository zombies() throws Exception {
        return JsonZombieDefinitionRepository.fromClasspath(
                "data/zombies.json",
                "data/ArmorTypeData.json"
        );
    }

    private PlantCollectionState findPlant(List<PlantCollectionState> states, String name) {
        return states.stream()
                .filter(state -> state.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private ZombieCollectionState findZombie(List<ZombieCollectionState> states, String alias) {
        return states.stream()
                .filter(state -> state.getAlias().equalsIgnoreCase(alias))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }
}
