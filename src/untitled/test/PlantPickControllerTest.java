import college.java.project.Main;import controller.ApplicationController;
import controller.LoginController;
import controller.PlantPickController;
import model.Menu.MenuType;
import model.Plant;
import model.User.User;
import model.User.UserGender;
import model.User.UserRepository;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlantPickControllerTest {
    @Test
    public void listsAndSelectsOnlyUnlockedLevelPlants() {
        Main application = Main.loadApplication();
        User user = this.createUser();
        PlantFactory factory = new PlantFactory(user.getPlantUpgradeService());
        PlantDefinition peashooter = application.getPlantDefinitions().findByName("Peashooter");
        PlantDefinition sunflower = application.getPlantDefinitions().findByName("Sunflower");
        Plant peashooterPlant = factory.create(peashooter);
        Plant sunflowerPlant = factory.create(sunflower);

        user.getUnlockedPlants().add(peashooterPlant);

        PlantPickController controller = new PlantPickController(
                user,
                application.getPlantDefinitions(),
                Arrays.asList(peashooterPlant),
                1
        );

        assertTrue(controller.showAllPlants().contains("Sunflower"));
        assertEquals(Arrays.asList("Peashooter"), controller.showAvailablePlants());
        assertEquals("Plant is locked.", controller.addPlant("Sunflower"));

        user.getUnlockedPlants().add(sunflowerPlant);
        assertEquals(
                "Plant is not available in this level.",
                controller.addPlant("sunflower")
        );
        assertEquals("Peashooter was added.", controller.addPlant("peashooter"));
        assertEquals("Plant is already selected.", controller.addPlant("Peashooter"));
        assertEquals("Peashooter was removed.", controller.removePlant("PEASHOOTER"));
        assertEquals("Plant is not selected.", controller.removePlant("Peashooter"));
    }

    @Test
    public void boostCostsTwoDiamondsAndAppliesWheneverPlantIsUsed() {
        Main application = Main.loadApplication();
        User user = this.createUser();
        user.setDiamond(3);

        PlantDefinition sunflower = application.getPlantDefinitions().findByName("Sunflower");
        PlantFactory factory = new PlantFactory(user.getPlantUpgradeService());
        user.getUnlockedPlants().add(factory.create(sunflower));

        PlantPickController controller = new PlantPickController(
                user,
                application.getPlantDefinitions()
        );

        assertEquals("Sunflower was added.", controller.addPlant("Sunflower"));
        assertEquals("Sunflower was boosted.", controller.boostPlant("Sunflower"));
        assertEquals(1, user.getDiamond());
        assertEquals("Plant is already boosted.", controller.boostPlant("sunflower"));
        assertEquals(1, user.getDiamond());
        assertEquals("Game started.", controller.startGame());

        PlantZombieGame game = application.createGame();
        game.configurePlantSelection(
                controller.getSelectedPlants(),
                controller.getBoostedPlantNames()
        );
        game.getSunSystem().addSun(300);

        assertFalse(game.plant("Peashooter", new Position(0, 0)));
        assertTrue(game.plant("Sunflower", new Position(0, 0)));
        int sunAfterFirstPlant = game.getSunSystem().getSunAmount();
        assertTrue(sunAfterFirstPlant > 250);

        game.advanceTime(50);
        assertTrue(game.plant("Sunflower", new Position(1, 0)));
        assertTrue(game.getSunSystem().getSunAmount() > sunAfterFirstPlant);
    }

    @Test
    public void cannotStartWithoutASelectedPlant() {
        Main application = Main.loadApplication();
        PlantPickController controller = new PlantPickController(
                this.createUser(),
                application.getPlantDefinitions()
        );

        assertEquals(
                "Select at least one plant before starting the game.",
                controller.startGame()
        );
        assertFalse(controller.isStarted());
    }

    @Test
    public void applicationRoutesPlantPickCommandsIntoTheSelectedGame() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-plant-pick").resolve("users.json");
        UserRepository repository = new UserRepository(usersFile) {
            @Override
            public void save() {
            }
        };
        ApplicationController controller = new ApplicationController(
                repository,
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        User user = this.createUser();
        Field currentUser = ApplicationController.class.getDeclaredField("currentUser");
        currentUser.setAccessible(true);
        currentUser.set(controller, user);
        Field loginControllerField = ApplicationController.class.getDeclaredField("loginController");
        loginControllerField.setAccessible(true);
        LoginController loginController = (LoginController) loginControllerField.get(controller);
        Field loggedInUser = LoginController.class.getDeclaredField("currentUser");
        loggedInUser.setAccessible(true);
        loggedInUser.set(loginController, user);
        controller.getMenuContext().login();

        PlantDefinition sunflower = application.getPlantDefinitions().findByName("Sunflower");
        user.getUnlockedPlants().add(
                new PlantFactory(user.getPlantUpgradeService()).create(sunflower)
        );
        user.setDiamond(2);

        assertEquals("GAME_MENU", controller.execute("menu enter game"));
        assertEquals("CHAPTER_MENU", controller.execute("menu enter chapter -c Ancient Egypt"));
        assertEquals("PLANT_PICK_MENU", controller.execute("select level -t NORMAL"));
        controller.execute("add plant -t Sunflower");
        controller.execute("boost plant -t Sunflower");
        controller.execute("start game");

        assertEquals(MenuType.MID_GAME_MENU, controller.getCurrentMenu());
        assertTrue(controller.getCurrentGame().getBoostedPlantNames().contains("sunflower"));
        assertFalse(controller.getCurrentGame().plant("Peashooter", new Position(0, 0)));
    }

    private User createUser() {
        User user = new User(
                "shayan",
                "hash",
                "shayan",
                "shayan@example.com",
                1,
                "answer",
                UserGender.MALE
        );
        user.setUnlockedPlants(new ArrayList<Plant>());
        return user;
    }
}
