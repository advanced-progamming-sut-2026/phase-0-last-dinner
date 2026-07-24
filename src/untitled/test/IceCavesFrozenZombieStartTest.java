import model.Plant;
import model.chapters.ChapterIceCaves;
import model.level.DeadlineLevel;
import model.level.NormalLevel;
import model.mechanism.Board;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IceCavesFrozenZombieStartTest {
    @Test
    public void iceCavesStartsWithFrozenZombiesOnConfiguredIceTiles() {
        Main application = Main.loadApplication();
        ChapterIceCaves chapter = new ChapterIceCaves();
        Board board = chapter.buildBoard();
        PlantZombieGame game = new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory(),
                application.getPlantUpgradeService(),
                null,
                null,
                board
        );
        DeadlineLevel level = new DeadlineLevel(null, Collections.<Plant>emptyList(), 0);

        game.configureChapter(chapter);
        game.configureLevel(level);
        game.advanceTime(1);

        assertEquals(4, chapter.getFrozenZombies().size());
        assertEquals(4, board.getAllZombies().size());
        assertTrue(Collections.disjoint(
                level.getWaves().get(0).getZombies(),
                chapter.getFrozenZombies()
        ));

        for (Zombie zombie : chapter.getFrozenZombies()) {
            assertEquals(TerrainType.FROZEN, board.getTile(zombie.getPosition()).getTerrainType());
            assertTrue(zombie.isTerrainFrozen());
        }

        game.configureLevel(new DeadlineLevel(null, Collections.<Plant>emptyList(), 0));
        game.advanceTime(1);
        assertEquals(4, chapter.getFrozenZombies().size());
        assertEquals(4, board.getAllZombies().size());
    }

    @Test
    public void frozenZombieMovesOnlyAfterItsIceIsDestroyed() {
        Main application = Main.loadApplication();
        ChapterIceCaves chapter = new ChapterIceCaves();
        Board board = chapter.buildBoard();
        PlantZombieGame game = new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory(),
                application.getPlantUpgradeService(),
                null,
                null,
                board
        );

        game.configureChapter(chapter);
        game.configureLevel(new DeadlineLevel(null, Collections.<Plant>emptyList(), 0));
        game.advanceTime(1);

        Zombie zombie = board.getZombiesAt(new Position(6, 0)).get(0);
        zombie.onTick();
        assertEquals(new Position(6, 0), zombie.getPosition());

        board.addProjectile(new Projectile(
                "600",
                new Position(5, 0),
                1,
                ProjectileType.NORMAL,
                null
        ));
        game.getCombatSystem().onTick();

        assertEquals(TerrainType.CLASSIC, board.getTile(new Position(6, 0)).getTerrainType());
        assertFalse(zombie.isTerrainFrozen());

        double xBeforeMoving = zombie.getExactX();
        zombie.onTick();
        assertTrue(zombie.getExactX() < xBeforeMoving);
    }

    @Test
    public void normalIceCavesLevelDoesNotAddFrozenStartingZombies() {
        Main application = Main.loadApplication();
        ChapterIceCaves chapter = new ChapterIceCaves();
        Board board = chapter.buildBoard();
        PlantZombieGame game = new PlantZombieGame(
                application.getPlantDefinitions(),
                application.getZombieDefinitions(),
                application.getZombieFactory(),
                application.getPlantUpgradeService(),
                null,
                null,
                board
        );

        game.configureChapter(chapter);
        game.configureLevel(new NormalLevel(null, Collections.<Plant>emptyList(), 0));
        game.advanceTime(1);

        assertTrue(chapter.getFrozenZombies().isEmpty());
        assertTrue(board.getAllZombies().isEmpty());
    }
}
