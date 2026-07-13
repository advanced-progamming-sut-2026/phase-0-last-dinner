import model.mechanism.Board;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.BarrelObstacle;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieType;
import model.zombie.behavior.BarrelRollerBehavior;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BarrelRollerZombieTest {
    @Test
    public void bundledDefinitionUsesDedicatedBehavior() {
        Main application = Main.loadApplication();
        ZombieDefinition definition = application.getZombieDefinitions().findByAlias("ZombieBarrelRoller");

        assertNotNull(definition);
        assertEquals(ZombieType.SPECIAL, definition.getType());

        Zombie zombie = application.getZombieFactory().create(definition, new Position(8, 0));
        assertNotNull(zombie.findBehavior(BarrelRollerBehavior.class));
    }

    @Test
    public void barrelStopsPiercingProjectileBeforeOwner() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie owner = game.spawnZombie("ZombieBarrelRoller", 1);
        BarrelRollerBehavior behavior = owner.findBehavior(BarrelRollerBehavior.class);
        int ownerHealth = owner.getHealth();

        game.advanceTime(1);
        BarrelObstacle barrel = behavior.getBarrel();
        assertNotNull(barrel);
        assertEquals(new Position(7, 1), barrel.getPosition());

        Projectile projectile = this.projectile("300", 1);
        projectile.setPierceCount(5);
        game.getBoard().addProjectile(projectile);
        game.advanceTime(1);

        assertEquals(ownerHealth, owner.getHealth());
        assertEquals(BarrelObstacle.DEFAULT_HEALTH - 300, barrel.getHealth());
        assertTrue(game.getBoard().getProjectiles().isEmpty());
    }

    @Test
    public void ownerKeepsBarrelOneTileAheadWhileMoving() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie owner = game.spawnZombie("ZombieBarrelRoller", 0);
        BarrelRollerBehavior behavior = owner.findBehavior(BarrelRollerBehavior.class);

        game.advanceTime(4);

        BarrelObstacle barrel = behavior.getBarrel();
        assertNotNull(barrel);
        assertEquals(owner.getPosition().getX() - 1, barrel.getPosition().getX());
        assertEquals(owner.getPosition().getY(), barrel.getPosition().getY());
    }

    @Test
    public void barrelSurvivesOwnerAndReleasesTwoMovingImpsWhenBroken() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie owner = game.spawnZombie("ZombieBarrelRoller", 2);
        BarrelRollerBehavior behavior = owner.findBehavior(BarrelRollerBehavior.class);

        assertNull(behavior.getBarrel());
        game.getCombatSystem().killZombie(owner);

        BarrelObstacle barrel = behavior.getBarrel();
        assertNotNull(barrel);
        assertFalse(barrel.isDead());
        assertSame(game.getBoard(), barrel.getBoard());
        assertEquals(new Position(7, 2), barrel.getPosition());

        this.fireAtBarrel(game, "550", 2);
        assertEquals(550, barrel.getHealth());
        this.fireAtBarrel(game, "550", 2);

        assertTrue(barrel.isDead());
        assertFalse(game.getBoard().getAllZombies().contains(barrel));

        List<Zombie> imps = this.findZombies(game.getBoard(), "ZombieImp");
        assertEquals(2, imps.size());
        assertEquals(new Position(7, 2), imps.get(0).getPosition());
        assertEquals(new Position(7, 2), imps.get(1).getPosition());

        double impX = imps.get(0).getExactX();
        game.advanceTime(1);
        assertTrue(imps.get(0).getExactX() < impX);
        assertTrue(imps.get(1).getExactX() < impX);
    }

    @Test
    public void regularZombiesPassBarrelWhileHypnotizedZombieDamagesIt() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        Zombie owner = game.spawnZombie("ZombieBarrelRoller", 3);
        BarrelRollerBehavior behavior = owner.findBehavior(BarrelRollerBehavior.class);
        game.getCombatSystem().killZombie(owner);
        BarrelObstacle barrel = behavior.getBarrel();

        ZombieDefinition regularDefinition = application.getZombieDefinitions().findByAlias("ZombieDefault");
        Zombie regular = application.getZombieFactory().create(regularDefinition, new Position(8, 3));
        game.getBoard().addZombie(regular, regular.getPosition());

        game.advanceTime(3);
        assertFalse(regular.isDead());
        assertEquals(new Position(7, 3), regular.getPosition());
        assertEquals(new Position(7, 3), barrel.getPosition());
        assertEquals(BarrelObstacle.DEFAULT_HEALTH, barrel.getHealth());

        Zombie hypnotized = application.getZombieFactory().create(regularDefinition, new Position(6, 3));
        hypnotized.addCondition(ZombieCondition.HYPNOTIZED);
        game.getBoard().addZombie(hypnotized, hypnotized.getPosition());
        game.advanceTime(1);

        assertTrue(barrel.getHealth() < BarrelObstacle.DEFAULT_HEALTH);
    }

    private Projectile projectile(String damage, int row) {
        return new Projectile(
                damage,
                new Position(6, row),
                1.0,
                ProjectileType.NORMAL,
                null
        );
    }

    private void fireAtBarrel(PlantZombieGame game, String damage, int row) {
        game.getBoard().addProjectile(this.projectile(damage, row));
        game.advanceTime(1);
    }

    private List<Zombie> findZombies(Board board, String alias) {
        List<Zombie> matches = new ArrayList<>();

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie != null && zombie.getDefinition() != null
                    && alias.equalsIgnoreCase(zombie.getDefinition().getAlias())) {
                matches.add(zombie);
            }
        }

        return matches;
    }
}
