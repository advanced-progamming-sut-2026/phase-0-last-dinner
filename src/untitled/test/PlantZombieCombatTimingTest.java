import college.java.project.Main;
import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.Position;
import model.plant.PlantFactory;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.plant.behavior.ExplosiveBehavior;
import model.plant.behavior.ExplosivePattern;
import model.zombie.Zombie;
import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PlantZombieCombatTimingTest {
    @Test
    public void repeaterProjectilesLeaveOneTickApart() {
        Main application = Main.loadApplication();
        Board board = new Board();
        CombatSystem combat = new CombatSystem(board);
        Plant repeater = this.addPlant(application, board, "Repeater", 2, 2);
        this.addZombie(application, board, "ZombieDefault", 8, 2);

        repeater.useAbility();

        assertEquals(2, board.getProjectiles().size());
        Projectile first = board.getProjectiles().get(0);
        Projectile second = board.getProjectiles().get(1);
        assertEquals(0, first.getSpawnDelayTicks());
        assertEquals(1, second.getSpawnDelayTicks());

        combat.onTick();

        assertEquals(3.0, first.getExactX(), 0.0001);
        assertEquals(2.0, second.getExactX(), 0.0001);

        combat.onTick();

        assertEquals(4.0, first.getExactX(), 0.0001);
        assertEquals(3.0, second.getExactX(), 0.0001);
    }

    @Test
    public void repeaterPlantFoodSpreadsTheWholeVolley() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant repeater = this.addPlant(application, board, "Repeater", 2, 2);
        this.addZombie(application, board, "ZombieDefault", 8, 2);

        repeater.receivePlantFood();

        assertEquals(11, board.getProjectiles().size());
        long maximumDelay = board.getProjectiles().stream()
                .map(Projectile::getSpawnDelayTicks)
                .max(Comparator.naturalOrder())
                .orElse(0L);
        assertEquals(10, maximumDelay);
        assertEquals(11, board.getProjectiles().stream()
                .mapToLong(Projectile::getSpawnDelayTicks)
                .distinct()
                .count());
        assertTrue(board.getProjectiles().stream().anyMatch(projectile -> projectile.getSpawnDelayTicks() > 0));
    }

    @Test
    public void megaGatlingPlantFoodKeepsEveryProjectileInOrder() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant megaGatling = this.addPlant(application, board, "Mega Gatling Pea", 2, 2);
        this.addZombie(application, board, "ZombieDefault", 8, 2);

        megaGatling.receivePlantFood();

        assertEquals(36, board.getProjectiles().size());
        assertEquals(36, board.getProjectiles().stream()
                .mapToLong(Projectile::getSpawnDelayTicks)
                .distinct()
                .count());
        assertEquals(35L, board.getProjectiles().stream()
                .mapToLong(Projectile::getSpawnDelayTicks)
                .max()
                .orElse(-1L));
    }

    @Test
    public void bonkChoyHitsFrontAndBackWithoutHittingAnotherLane() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant bonkChoy = this.addPlant(application, board, "Bonk Choy", 4, 2);
        Zombie front = this.addZombie(application, board, "ZombieDefault", 5, 2);
        Zombie back = this.addZombie(application, board, "ZombieDefault", 3, 2);
        Zombie otherLane = this.addZombie(application, board, "ZombieDefault", 5, 1);
        int frontHealth = front.getHealth();
        int backHealth = back.getHealth();
        int otherLaneHealth = otherLane.getHealth();

        bonkChoy.onTick();
        bonkChoy.onTick();
        bonkChoy.onTick();

        assertEquals(frontHealth - 15, front.getHealth());
        assertEquals(backHealth - 15, back.getHealth());
        assertEquals(otherLaneHealth, otherLane.getHealth());
    }

    @Test
    public void bonkChoyKeepsHittingAnOverlappingZombie() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant bonkChoy = this.addPlant(application, board, "Bonk Choy", 4, 2);
        Zombie overlapping = this.addZombie(application, board, "ZombieDefault", 4, 2);
        int health = overlapping.getHealth();

        bonkChoy.onTick();
        bonkChoy.onTick();
        bonkChoy.onTick();

        assertEquals(health - 15, overlapping.getHealth());
    }

    @Test
    public void homingProjectileSkipsAnUntargetableSubmergedZombie() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant catTail = this.addPlant(application, board, "Cat-tail", 1, 2);
        Zombie snorkel = this.addZombie(application, board, "ZombieBeachSnorkel", 3, 2);
        Zombie target = this.addZombie(application, board, "ZombieDefault", 6, 2);
        snorkel.activateAbility();

        catTail.useAbility();

        assertEquals(1, board.getProjectiles().size());
        assertSame(target, board.getProjectiles().get(0).getTarget());
    }

    @Test
    public void splashTargetsDoNotConsumeProjectileBounces() {
        Main application = Main.loadApplication();
        Board board = new Board();
        CombatSystem combat = new CombatSystem(board);
        Projectile projectile = new Projectile(
                "20", new Position(0, 2), 1.0, ProjectileType.NORMAL, null
        );
        projectile.setBounceCount(2);
        projectile.setSplashRadius(1);
        board.addProjectile(projectile);
        this.addZombie(application, board, "ZombieDefault", 1, 2);
        this.addZombie(application, board, "ZombieDefault", 1, 1);
        this.addZombie(application, board, "ZombieDefault", 1, 3);
        this.addZombie(application, board, "ZombieDefault", 2, 2);
        Zombie nextTarget = this.addZombie(application, board, "ZombieDefault", 5, 2);

        combat.onTick();

        assertTrue(board.getProjectiles().contains(projectile));
        assertSame(nextTarget, projectile.getTarget());
    }

    @Test
    public void grapeshotBurstCoversEightDifferentDirections() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant grapeshot = this.addPlant(application, board, "Grapeshot", 4, 2);
        Projectile grape = new Projectile("100", null, 1.0, ProjectileType.NORMAL, null);
        ExplosiveBehavior behavior = new ExplosiveBehavior(
                "0", 0, false, ExplosivePattern.RADIUS, 0, true
        );
        behavior.setSecondaryProjectileBurst(grape, 8);

        behavior.activate(grapeshot, board);

        assertEquals(8, board.getProjectiles().size());
        assertEquals(8, board.getProjectiles().stream()
                .map(projectile -> projectile.getHorizontalDirection() + ":" + projectile.getVerticalDirection())
                .distinct()
                .count());
        assertTrue(board.getProjectiles().stream().anyMatch(projectile -> projectile.getHorizontalDirection() == 0
                && projectile.getVerticalDirection() == 1));
        assertTrue(board.getProjectiles().stream().anyMatch(projectile -> projectile.getHorizontalDirection() == 0
                && projectile.getVerticalDirection() == -1));
    }

    @Test
    public void contactPlantDoesNotTriggerFromTheNextTile() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant iceberg = this.addPlant(application, board, "Iceberg Lettuce", 4, 2);
        this.addZombie(application, board, "ZombieDefault", 5, 2);

        iceberg.onTick();

        assertNotNull(iceberg.getPosition());
        assertTrue(board.getPlantsAt(new Position(4, 2)).contains(iceberg));
    }

    @Test
    public void repeaterAutomaticFireKeepsItsFifteenTickCycle() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant repeater = this.addPlant(application, board, "Repeater", 2, 2);
        this.addZombie(application, board, "ZombieDefault", 8, 2);

        for (int tick = 0; tick < 14; tick++) {
            repeater.onTick();
        }
        assertEquals(0, board.getProjectiles().size());

        repeater.onTick();
        assertEquals(2, board.getProjectiles().size());
        assertEquals(0, board.getProjectiles().get(0).getSpawnDelayTicks());
        assertEquals(1, board.getProjectiles().get(1).getSpawnDelayTicks());

        for (int tick = 0; tick < 14; tick++) {
            repeater.onTick();
        }
        assertEquals(2, board.getProjectiles().size());

        repeater.onTick();
        assertEquals(4, board.getProjectiles().size());
    }

    @Test
    public void shooterStaysReadyUntilAValidTargetAppears() {
        Main application = Main.loadApplication();
        Board board = new Board();
        new CombatSystem(board);
        Plant repeater = this.addPlant(application, board, "Repeater", 2, 2);

        for (int tick = 0; tick < 30; tick++) {
            repeater.onTick();
        }
        assertEquals(0, board.getProjectiles().size());

        this.addZombie(application, board, "ZombieDefault", 8, 2);
        repeater.onTick();

        assertEquals(2, board.getProjectiles().size());
    }

    private Plant addPlant(Main application, Board board, String name, int column, int row) {
        Plant plant = new PlantFactory().create(application.getPlantDefinitions().findByName(name));
        Position position = new Position(column, row);
        plant.setPosition(position);
        plant.setBoard(board);
        board.getTile(position).addPlant(plant);
        return plant;
    }

    private Zombie addZombie(Main application, Board board, String alias, int column, int row) {
        Position position = new Position(column, row);
        Zombie zombie = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias(alias),
                position
        );
        board.addZombie(zombie, position);
        return zombie;
    }
}
