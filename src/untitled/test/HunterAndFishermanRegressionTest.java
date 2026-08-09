import college.java.project.Main;import model.Plant;
import model.chapters.ChapterBigWaveBeach;
import model.mechanism.Board;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.ZombieSpawner;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HunterAndFishermanRegressionTest {
    @Test
    public void hunterHitsTheNearestPlantEvenWhenItIsMoreThanFourTilesAway() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Peashooter", new Position(0, 0)));
        assertTrue(game.plant("Sunflower", new Position(2, 0)));
        Plant distantPlant = game.getBoard().getPlantsAt(new Position(0, 0)).get(0);
        Plant nearestPlant = game.getBoard().getPlantsAt(new Position(2, 0)).get(0);
        Zombie hunter = game.spawnZombie("ZombieIceAgeHunter", 0);

        hunter.activateAbility();
        hunter.activateAbility();
        hunter.activateAbility();

        assertTrue(game.getBoard().getPlantCoverSystem().isCovered(nearestPlant));
        assertFalse(game.getBoard().getPlantCoverSystem().isCovered(distantPlant));
    }

    @Test
    public void beachFishermanIgnoresLowBeachSpawnAndStaysAtTheRightEdge() {
        Main application = Main.loadApplication();
        ChapterBigWaveBeach chapter = new LowBeachChapter();
        Board board = chapter.buildBoard();
        ZombieSpawner spawner = new ZombieSpawner(
                application.getZombieFactory(),
                application.getZombieDefinitions(),
                board
        );
        spawner.setChapter(chapter);
        ZombieDefinition fishermanDefinition = application.getZombieDefinitions()
                .findByAlias("ZombieBeachFisherman");

        Zombie fisherman = spawner.spawnZombie(fishermanDefinition, null, 1);

        assertNotNull(fisherman);
        assertEquals(new Position(8, 1), fisherman.getPosition());
        for (int tick = 0; tick < 50; tick++) {
            fisherman.onTick();
        }
        assertEquals(new Position(8, 1), fisherman.getPosition());
    }

    private static final class LowBeachChapter extends ChapterBigWaveBeach {
        @Override
        public Position resolveZombieSpawnPosition(int row, boolean isFinalWave) {
            return new Position(5, row);
        }
    }
}
