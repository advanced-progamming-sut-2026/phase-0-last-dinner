import model.Plant;
import model.mechanism.PlantZombieGame;
import model.mechanism.Position;
import model.mechanism.Sun;
import model.mechanism.SunSystem;
import model.mechanism.SunType;
import model.mechanism.TerrainType;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.behavior.CrystalSkullBehavior;
import model.zombie.behavior.FlyingBehavior;
import model.zombie.behavior.PianoBehavior;
import model.zombie.behavior.ProjectileReflectorBehavior;
import model.zombie.behavior.ProspectorBehavior;
import model.zombie.behavior.SunStealerBehavior;
import model.zombie.behavior.TorchBearerBehavior;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpecialZombieBehaviorTest {
    @Test
    public void prospectorRelocatesToHouseEndAndIceCancelsLaunch() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie prospector = game.spawnZombie("ZombieProspector", 2);
        ProspectorBehavior behavior = prospector.findBehavior(ProspectorBehavior.class);

        assertNotNull(behavior);
        behavior.activate(prospector, game.getBoard());
        assertEquals(new Position(0, 2), prospector.getPosition());
        assertEquals(1, behavior.getMovementDirection(prospector));

        double xBeforeMoving = prospector.getExactX();
        prospector.move();
        assertTrue(prospector.getExactX() > xBeforeMoving);

        PlantZombieGame cancelledGame = Main.loadApplication().createGame();
        Zombie cancelled = cancelledGame.spawnZombie("ZombieProspector", 1);
        ProspectorBehavior cancelledBehavior = cancelled.findBehavior(ProspectorBehavior.class);
        cancelledBehavior.onProjectileHit(
                cancelled,
                this.projectile("20", ProjectileType.ICE),
                cancelledGame.getBoard()
        );
        cancelledBehavior.activate(cancelled, cancelledGame.getBoard());

        assertEquals(new Position(8, 1), cancelled.getPosition());
        assertEquals(-1, cancelledBehavior.getMovementDirection(cancelled));
    }

    @Test
    public void crystalSkullDetectsInTwoDimensionsButFiresStraightAhead() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Peashooter", new Position(4, 4)));
        assertTrue(game.plant("Sunflower", new Position(4, 0)));

        Plant offLane = game.getBoard().getPlantsAt(new Position(4, 4)).get(0);
        Plant inLane = game.getBoard().getPlantsAt(new Position(4, 0)).get(0);
        Zombie crystalSkull = game.spawnZombie("ZombieCrystalSkull", 0);
        CrystalSkullBehavior behavior = crystalSkull.findBehavior(CrystalSkullBehavior.class);

        behavior.onTick(crystalSkull, game.getBoard());
        assertFalse(behavior.canMove(crystalSkull, game.getBoard()));

        behavior.activate(crystalSkull, game.getBoard());
        assertTrue(inLane.isDead());
        assertFalse(offLane.isDead());
    }

    @Test
    public void pianoMovesZombiesAcrossTheWholeBoardToAdjacentLanes() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie pianist = game.spawnZombie("ZombiePiano", 2);
        Zombie distantDancer = game.spawnZombie("ZombieDefault", 0);
        Position distantPosition = new Position(0, 0);
        assertTrue(game.getBoard().moveZombie(distantDancer, distantPosition));
        distantDancer.setPosition(distantPosition);

        PianoBehavior behavior = pianist.findBehavior(PianoBehavior.class);
        behavior.activate(pianist, game.getBoard());

        assertEquals(new Position(0, 1), distantDancer.getPosition());
    }

    @Test
    public void explorerTorchRespondsToPlantAndProjectileElements() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Snow Pea", new Position(6, 0)));
        assertTrue(game.plant("Fire Peashooter", new Position(5, 0)));

        Plant icePlant = game.getBoard().getPlantsAt(new Position(6, 0)).get(0);
        Plant firePlant = game.getBoard().getPlantsAt(new Position(5, 0)).get(0);
        Zombie explorer = game.spawnZombie("ZombieExplorer", 0);
        TorchBearerBehavior behavior = explorer.findBehavior(TorchBearerBehavior.class);

        behavior.attack(explorer, icePlant, game.getBoard());
        assertFalse(behavior.isTorchLit());
        assertFalse(icePlant.isDead());

        behavior.attack(explorer, firePlant, game.getBoard());
        assertTrue(behavior.isTorchLit());
        assertTrue(firePlant.isDead());

        behavior.onProjectileHit(explorer, this.projectile("20", ProjectileType.ICE), game.getBoard());
        assertFalse(behavior.isTorchLit());
        behavior.onProjectileHit(explorer, this.projectile("20", ProjectileType.FIRE), game.getBoard());
        assertTrue(behavior.isTorchLit());
    }

    @Test
    public void jugglerReflectsToSameLaneAndIceBuildsBreakableCover() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Peashooter", new Position(4, 1)));
        assertTrue(game.plant("Sunflower", new Position(7, 2)));

        Plant sameLane = game.getBoard().getPlantsAt(new Position(4, 1)).get(0);
        Plant otherLane = game.getBoard().getPlantsAt(new Position(7, 2)).get(0);
        Zombie juggler = game.spawnZombie("ZombieDarkJuggler", 1);
        ProjectileReflectorBehavior behavior = juggler.findBehavior(ProjectileReflectorBehavior.class);
        int sameLaneHealth = sameLane.getHealth();
        int otherLaneHealth = otherLane.getHealth();
        int jugglerHealth = juggler.getHealth();

        Projectile normal = new Projectile(
                "20",
                new Position(7, 1),
                1.0,
                ProjectileType.NORMAL,
                null
        );
        game.getBoard().addProjectile(normal);
        game.advanceTime(1);

        assertTrue(normal.isHostileToPlants());
        assertEquals(-1, normal.getHorizontalDirection());
        assertEquals(jugglerHealth, juggler.getHealth());
        assertEquals(sameLaneHealth, sameLane.getHealth());
        this.advanceUntilProjectileResolves(game, normal);
        assertEquals(sameLaneHealth - 20, sameLane.getHealth());
        assertEquals(otherLaneHealth, otherLane.getHealth());

        for (int hit = 0; hit < 3; hit++) {
            Projectile ice = new Projectile(
                    "20",
                    juggler.getPosition(),
                    1.0,
                    ProjectileType.ICE,
                    null
            );
            assertTrue(behavior.onProjectileHit(juggler, ice, game.getBoard()));
            game.getBoard().addProjectile(ice);
            this.advanceUntilProjectileResolves(game, ice);

            if (hit < 2) {
                assertFalse(sameLane.isDisabled());
            }
        }

        assertTrue(game.getBoard().getPlantCoverSystem().isCovered(sameLane));
        assertTrue(sameLane.isDisabled());
    }

    @Test
    public void reflectedProjectileUsesTerrainAndPlantCoverCollisionPipeline() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Wall-nut", new Position(4, 1)));

        Plant wallNut = game.getBoard().getPlantsAt(new Position(4, 1)).get(0);
        Zombie juggler = game.spawnZombie("ZombieDarkJuggler", 1);
        ProjectileReflectorBehavior behavior = juggler.findBehavior(ProjectileReflectorBehavior.class);
        Position gravePosition = new Position(6, 1);
        game.getBoard().setTerrain(gravePosition, TerrainType.GRAVE);
        int graveHealth = game.getBoard().getTile(gravePosition).getTerrainHealth();
        int plantHealth = wallNut.getHealth();

        Projectile terrainShot = new Projectile(
                "20",
                juggler.getPosition(),
                1.0,
                ProjectileType.NORMAL,
                null
        );
        assertTrue(behavior.onProjectileHit(juggler, terrainShot, game.getBoard()));
        game.getBoard().addProjectile(terrainShot);
        this.advanceUntilProjectileResolves(game, terrainShot);

        assertEquals(graveHealth - 20, game.getBoard().getTile(gravePosition).getTerrainHealth());
        assertEquals(plantHealth, wallNut.getHealth());

        game.getBoard().setTerrain(gravePosition, TerrainType.CLASSIC);
        game.getBoard().getPlantCoverSystem().coverWithOctopus(wallNut);
        int coverHealth = game.getBoard().getPlantCoverSystem().getCoverHealth(wallNut);
        Projectile coverShot = new Projectile(
                "20",
                juggler.getPosition(),
                1.0,
                ProjectileType.NORMAL,
                null
        );
        assertTrue(behavior.onProjectileHit(juggler, coverShot, game.getBoard()));
        game.getBoard().addProjectile(coverShot);
        this.advanceUntilProjectileResolves(game, coverShot);

        assertEquals(coverHealth - 20, game.getBoard().getPlantCoverSystem().getCoverHealth(wallNut));
        assertEquals(plantHealth, wallNut.getHealth());
    }

    @Test
    public void projectileShieldsConsumeBlockedShotsButTakeUnblockedDamage() {
        this.assertShieldCollision(
                "ZombieDarkImpDragon",
                ProjectileType.FIRE,
                false
        );
        this.assertShieldCollision(
                "ZombieLostCityJane",
                ProjectileType.LOBBED,
                true
        );
    }

    @Test
    public void raStealsOnIntervalAndRefundsEveryStolenSunOnDeath() {
        PlantZombieGame game = Main.loadApplication().createGame();
        SunSystem sunSystem = game.getSunSystem();
        Sun specialSun = new Sun(SunType.SPECIAL, new Position(3, 2), 0);
        specialSun.reachGround();
        sunSystem.getSuns().add(specialSun);

        Zombie ra = game.spawnZombie("ZombieRa", 2);
        SunStealerBehavior behavior = ra.findBehavior(SunStealerBehavior.class);
        int walletBefore = sunSystem.getSunAmount();

        behavior.attack(ra, null, game.getBoard());
        assertTrue(sunSystem.getSuns().contains(specialSun));
        for (int tick = 0; tick < 9; tick++) {
            behavior.onTick(ra, game.getBoard());
        }
        assertTrue(sunSystem.getSuns().contains(specialSun));

        behavior.onTick(ra, game.getBoard());
        assertFalse(sunSystem.getSuns().contains(specialSun));
        ra.die();
        assertEquals(walletBefore + specialSun.getValue(), sunSystem.getSunAmount());
    }

    @Test
    public void dodoFlightIsLimitedBlocksTallNutAndAvoidsSlipperyTiles() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(1000);
        assertTrue(game.plant("Wall-nut", new Position(7, 0)));
        assertTrue(game.plant("Tall-nut", new Position(7, 1)));

        Zombie wallNutDodo = game.spawnZombie("ZombieIceAgeDodo", 0);
        FlyingBehavior wallNutFlight = wallNutDodo.findBehavior(FlyingBehavior.class);
        Plant wallNut = game.getBoard().getPlantsAt(new Position(7, 0)).get(0);

        assertFalse(wallNutFlight.canAttackPlant(wallNutDodo, wallNut, game.getBoard()));
        assertTrue(wallNutDodo.hasCondition(ZombieCondition.FLYING));
        wallNutDodo.setPosition(new Position(6, 0));
        wallNutFlight.onTick(wallNutDodo, game.getBoard());
        assertFalse(wallNutDodo.hasCondition(ZombieCondition.FLYING));

        Zombie tallNutDodo = game.spawnZombie("ZombieIceAgeDodo", 1);
        FlyingBehavior tallNutFlight = tallNutDodo.findBehavior(FlyingBehavior.class);
        Plant tallNut = game.getBoard().getPlantsAt(new Position(7, 1)).get(0);
        assertTrue(tallNutFlight.canAttackPlant(tallNutDodo, tallNut, game.getBoard()));
        assertFalse(tallNutDodo.hasCondition(ZombieCondition.FLYING));

        game.getBoard().setTerrain(new Position(7, 2), TerrainType.SLIPPERY_UP);
        Zombie slipperyDodo = game.spawnZombie("ZombieIceAgeDodo", 2);
        FlyingBehavior slipperyFlight = slipperyDodo.findBehavior(FlyingBehavior.class);
        slipperyFlight.onTick(slipperyDodo, game.getBoard());
        assertTrue(slipperyDodo.hasCondition(ZombieCondition.FLYING));

        for (int tick = 0; tick < 6; tick++) {
            slipperyDodo.onTick();
        }
        assertEquals(2, slipperyDodo.getPosition().getY());
    }

    private Projectile projectile(String damage, ProjectileType type) {
        return new Projectile(damage, new Position(7, 0), 1.0, type, null);
    }

    private void advanceUntilProjectileResolves(PlantZombieGame game, Projectile projectile) {
        for (int tick = 0; tick < 20 && game.getBoard().getProjectiles().contains(projectile); tick++) {
            game.advanceTime(1);
        }
        assertFalse(game.getBoard().getProjectiles().contains(projectile));
    }

    private void assertShieldCollision(
            String shieldAlias,
            ProjectileType blockedType,
            boolean lobbed
    ) {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie shieldHolder = game.spawnZombie(shieldAlias, 3);
        Zombie zombieBehind = game.spawnZombie("ZombieDefault", 3);
        game.getBoard().removeZombie(shieldHolder);
        game.getBoard().addZombie(shieldHolder, new Position(5, 3));
        game.getBoard().removeZombie(zombieBehind);
        game.getBoard().addZombie(zombieBehind, new Position(7, 3));
        int shieldHealth = shieldHolder.getHealth();
        int behindHealth = zombieBehind.getHealth();

        Projectile blocked = new Projectile(
                "20",
                new Position(4, 3),
                1.0,
                blockedType,
                null
        );
        blocked.setLobbed(lobbed);
        assertTrue(shieldHolder.getBehavior().canBeHitBy(shieldHolder, blocked));
        game.getBoard().addProjectile(blocked);
        game.getCombatSystem().onTick();

        assertFalse(game.getBoard().getProjectiles().contains(blocked));
        assertEquals(shieldHealth, shieldHolder.getHealth());
        assertEquals(behindHealth, zombieBehind.getHealth());

        Projectile unblocked = new Projectile(
                "20",
                new Position(4, 3),
                1.0,
                ProjectileType.NORMAL,
                null
        );
        game.getBoard().addProjectile(unblocked);
        game.getCombatSystem().onTick();

        assertFalse(game.getBoard().getProjectiles().contains(unblocked));
        assertEquals(shieldHealth - 20, shieldHolder.getHealth());
        assertEquals(behindHealth, zombieBehind.getHealth());
    }
}
