import college.java.project.Main;import controller.ApplicationController;
import controller.PlantPickController;
import model.Plant;
import model.User.User;
import model.User.UserRepository;
import model.chapters.ChapterAncientEgypt;
import model.level.Level;
import model.level.LevelType;
import model.mechanism.PlantZombieGame;
import model.plant.PlantFactory;
import model.plant.PlantUnlockService;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StarterPlantProgressionTest {
    @Test
    public void newUserStartsWithPeashooterAndKeepsItAfterReload() throws Exception {
        Main application = Main.loadApplication();
        Path usersFile = Files.createTempDirectory("pvz-starter-plant").resolve("users.json");
        ApplicationController controller = new ApplicationController(
                new UserRepository(usersFile),
                application.getPlantDefinitions(),
                application.getZombieDefinitions()
        );

        this.registerAndLogin(controller);
        List<Plant> unlockedPlants = controller.getCurrentUser().getUnlockedPlants();

        assertEquals(1, unlockedPlants.size());
        assertEquals("Peashooter", unlockedPlants.get(0).getName());
        assertEquals(
                java.util.Collections.singletonList("Peashooter"),
                new PlantPickController(
                        controller.getCurrentUser(),
                        application.getPlantDefinitions()
                ).showAvailablePlants()
        );

        controller.close();
        User reloadedUser = new UserRepository(usersFile).findByUsername("starter");
        assertEquals("Peashooter", reloadedUser.getUnlockedPlants().get(0).getName());
    }

    @Test
    public void uniqueAdventureWinsUnlockPlantsInOrderOnlyOnce() {
        Main application = Main.loadApplication();
        User user = this.createUserWithPeashooter(application);
        PlantZombieGame game = this.createGame(application, user);
        game.configureChapter(new ChapterAncientEgypt());

        game.configureLevel(new WinningLevel(LevelType.NORMAL));
        game.advanceTime(1);
        assertTrue(this.hasPlant(user, "Sunflower"));
        assertEquals(1, this.plantUnlockNewsCount(user));

        game.configureLevel(new WinningLevel(LevelType.NORMAL));
        game.advanceTime(1);
        assertEquals(2, user.getUnlockedPlants().size());
        assertEquals(1, this.plantUnlockNewsCount(user));

        game.configureLevel(new WinningLevel(LevelType.CONVEYOR_BELT));
        game.advanceTime(1);
        assertTrue(this.hasPlant(user, "Wall-nut"));
        assertEquals(2, this.plantUnlockNewsCount(user));
    }

    @Test
    public void anAlreadyOwnedRewardIsSkippedWithoutCreatingDuplicates() {
        Main application = Main.loadApplication();
        User user = this.createUserWithPeashooter(application);
        PlantFactory factory = new PlantFactory(user.getPlantUpgradeService());
        PlantUnlockService.unlock(user, factory.create(
                application.getPlantDefinitions().findByName("Sunflower")
        ));
        PlantZombieGame game = this.createGame(application, user);
        game.configureChapter(new ChapterAncientEgypt());

        game.configureLevel(new WinningLevel(LevelType.NORMAL));
        game.advanceTime(1);

        assertEquals(3, user.getUnlockedPlants().size());
        assertTrue(this.hasPlant(user, "Wall-nut"));
        assertEquals(1, this.plantUnlockNewsCount(user));
    }

    private void registerAndLogin(ApplicationController controller) {
        controller.execute(
                "register -u starter -p Strong#123 Strong#123 "
                        + "-n Starter -e starter@example.com -g male"
        );
        controller.execute("pick question -q 1 -a blue -c blue");
        controller.execute("login -u starter -p Strong#123");
    }

    private User createUserWithPeashooter(Main application) {
        User user = new User();
        user.initializeMissingFields();
        PlantFactory factory = new PlantFactory(user.getPlantUpgradeService());
        PlantUnlockService.unlock(user, factory.create(
                application.getPlantDefinitions().findByName("Peashooter")
        ));
        return user;
    }

    private PlantZombieGame createGame(Main application, User user) {
        return new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory(),
                user.getPlantUpgradeService(),
                null,
                user
        );
    }

    private boolean hasPlant(User user, String plantName) {
        for (Plant plant : user.getUnlockedPlants()) {
            if (plant != null && plantName.equalsIgnoreCase(plant.getName())) {
                return true;
            }
        }
        return false;
    }

    private int plantUnlockNewsCount(User user) {
        int count = 0;
        for (String news : user.getAllNews()) {
            if (news != null && news.startsWith("New plant unlocked:")) {
                count++;
            }
        }
        return count;
    }

    private static final class WinningLevel extends Level {
        private WinningLevel(LevelType levelType) {
            super(levelType);
        }

        @Override
        public void start() {
            this.setStarted(true);
        }

        @Override
        public boolean isWinConditionMet() {
            return true;
        }

        @Override
        public boolean isLoseConditionMet() {
            return false;
        }
    }
}
