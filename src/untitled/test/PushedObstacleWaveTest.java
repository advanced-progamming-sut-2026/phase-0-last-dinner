import model.Plant;
import model.mechanism.PlantZombieGame;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.Wave;
import model.plant.PlantFactory;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.BarrelObstacle;
import model.zombie.PushedObstacle;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.behavior.BarrelRollerBehavior;
import model.zombie.behavior.BlockPusherBehavior;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PushedObstacleWaveTest {
    @Test
    public void troglobiteDeploysThreeIndependentPositionedIceBlocks() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie troglobite = game.spawnZombie("ZombieIceAgeTroglobite", 0);
        BlockPusherBehavior behavior = troglobite.findBehavior(BlockPusherBehavior.class);

        game.advanceTime(1);

        List<PushedObstacle> blocks = behavior.getObstacles();
        assertEquals(3, blocks.size());
        assertEquals(new Position(7, 0), blocks.get(0).getPosition());
        assertEquals(new Position(6, 0), blocks.get(1).getPosition());
        assertEquals(new Position(5, 0), blocks.get(2).getPosition());

        for (PushedObstacle block : blocks) {
            assertSame(game.getBoard(), block.getBoard());
            assertTrue(game.getBoard().getAllZombies().contains(block));
        }
    }

    @Test
    public void pushedObjectPersistsAfterOwnerAndKillsGroundCollisions() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        Zombie arcade = game.spawnZombie("ZombieArcade", 1);
        BlockPusherBehavior behavior = arcade.findBehavior(BlockPusherBehavior.class);
        assertEquals(1100, arcade.getHealth());
        game.getCombatSystem().killZombie(arcade);
        PushedObstacle machine = behavior.getObstacles().get(0);

        assertFalse(machine.isDead());
        assertSame(game.getBoard(), machine.getBoard());

        Plant wallNut = new PlantFactory().create(
                application.getPlantDefinitions().findByName("Wall-nut")
        );
        new PlantingSystem(game.getBoard(), null, null).plant(wallNut, machine.getPosition());
        Zombie hypnotized = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                machine.getPosition()
        );
        hypnotized.addCondition(ZombieCondition.HYPNOTIZED);
        game.getBoard().addZombie(hypnotized, hypnotized.getPosition());

        behavior.onTick(arcade, game.getBoard());

        assertTrue(wallNut.isDead());
        assertTrue(hypnotized.isDead());
    }

    @Test
    public void straightProjectileDamagesClosestIceBlockInsteadOfPrivateShieldCounter() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie troglobite = game.spawnZombie("ZombieIceAgeTroglobite", 2);
        BlockPusherBehavior behavior = troglobite.findBehavior(BlockPusherBehavior.class);
        troglobite.activateAbility();
        PushedObstacle frontBlock = behavior.getObstacles().get(2);
        int health = frontBlock.getHealth();
        game.getBoard().addProjectile(new Projectile(
                "20",
                new Position(4, 2),
                1.0,
                ProjectileType.NORMAL,
                null
        ));

        game.getCombatSystem().onTick();

        assertEquals(health - 20, frontBlock.getHealth());
        assertEquals(troglobite.getDefinition().getHitpoints(), troglobite.getHealth());
    }

    @Test
    public void barrelAndReleasedImpsRemainMembersOfTheirWave() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie owner = game.spawnZombie("ZombieBarrelRoller", 3);
        Wave wave = new Wave(1, 600, false);
        wave.addZombie(owner);
        wave.start();
        owner.activateAbility();
        BarrelObstacle barrel = owner.findBehavior(BarrelRollerBehavior.class).getBarrel();

        assertTrue(wave.getZombies().contains(barrel));
        barrel.takeDirectDamage(BarrelObstacle.DEFAULT_HEALTH);
        owner.die();

        int impCount = 0;
        for (Zombie zombie : wave.getZombies()) {
            if (zombie.getDefinition() != null
                    && "ZombieImp".equalsIgnoreCase(zombie.getDefinition().getAlias())) {
                impCount++;
            }
        }

        assertEquals(2, impCount);
        assertTrue(wave.getRemainingHealthPercentage() > 0);
    }
}
