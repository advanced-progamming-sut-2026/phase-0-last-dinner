import controller.ApplicationController;
import model.Greenhouse.GreenhouseBoostService;
import model.Menu.MenuType;
import model.User.User;
import model.User.UserRepository;
import model.mechanism.PlantZombieGame;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PagesNineToTwentyTwoTest {
    @Test
    public void settingsChapterAndDifficultyReachTheRunningGame() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-pages-9-22").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        controller.execute(
                "register -u shayan -p Strong#123 Strong#123 "
                        + "-n Shayan -e shayan@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u shayan -p Strong#123");

        assertEquals("SETTINGS_MENU", controller.execute("menu enter settings"));
        controller.execute("menu settings change-difficulty -l 5");
        assertEquals(5, controller.getCurrentUser().getDifficultyLevel());

        assertEquals("MAIN_MENU", controller.execute("menu exit"));
        assertEquals("GAME_MENU", controller.execute("menu enter game"));
        controller.execute("menu enter chapter -c Ancient Egypt");
        assertEquals(MenuType.CHAPTER_MENU, controller.getCurrentMenu());

        User user = controller.getCurrentUser();
        PlantZombieGame game = new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                new ZombieFactory(application.getZombieDefinitions()),
                user.getPlantUpgradeService(),
                new GreenhouseBoostService(user.getGreenhouse()),
                user
        );
        ZombieDefinition definition = application.getZombieDefinitions().findByAlias("ZombieDefault");
        Zombie zombie = game.spawnZombie("ZombieDefault", 0);

        assertNotNull(definition);
        assertNotNull(zombie);
        assertEquals(
                (int) Math.round(definition.getHitpoints() * 5.0 / 3.0),
                zombie.getHealth()
        );

        game.advanceTime(3);
        assertEquals(5, game.getEngine().getClock().getCurrentTick());
    }
}
