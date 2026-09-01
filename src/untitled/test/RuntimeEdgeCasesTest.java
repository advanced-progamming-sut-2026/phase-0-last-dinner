import college.java.project.Main;import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.PlantZombieGame;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.ArmorFlag;
import model.zombie.ArmorType;
import model.zombie.ConditionResistance;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;
import model.zombie.ZombieArmorDefinition;
import model.zombie.ZombieChapter;
import model.zombie.ZombieCondition;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuntimeEdgeCasesTest {
    @Test
    public void frozenCoverCombinesHunterHitsAndSurvivesTheirDeath() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(500);
        assertTrue(game.plant("Peashooter", new Position(4, 0)));
        Plant plant = game.getBoard().getPlantsAt(new Position(4, 0)).get(0);

        Zombie firstHunter = game.spawnZombie("ZombieIceAgeHunter", 0);
        Zombie secondHunter = game.spawnZombie("ZombieIceAgeHunter", 0);
        firstHunter.activateAbility();
        secondHunter.activateAbility();
        firstHunter.activateAbility();

        assertTrue(game.getBoard().getPlantCoverSystem().isCovered(plant));
        assertTrue(plant.isDisabled());
        assertEquals(600, game.getBoard().getPlantCoverSystem().getCoverHealth(plant));

        game.getCombatSystem().killZombie(firstHunter);
        game.getCombatSystem().killZombie(secondHunter);
        game.getBoard().addProjectile(this.projectile("20", ProjectileType.NORMAL, 3, 0));
        game.getCombatSystem().onTick();

        assertEquals(580, game.getBoard().getPlantCoverSystem().getCoverHealth(plant));
        game.getBoard().addProjectile(this.projectile("20", ProjectileType.FIRE, 3, 0));
        game.getCombatSystem().onTick();

        assertFalse(game.getBoard().getPlantCoverSystem().isCovered(plant));
        assertFalse(plant.isDisabled());
    }

    @Test
    public void octopusCoverSurvivesThrowerAndTakesProjectileDamage() {
        PlantZombieGame game = Main.loadApplication().createGame();
        game.getSunSystem().addSun(500);
        assertTrue(game.plant("Sunflower", new Position(4, 1)));
        Plant plant = game.getBoard().getPlantsAt(new Position(4, 1)).get(0);
        Zombie octopus = game.spawnZombie("ZombieBeachOctopus", 1);

        octopus.activateAbility();
        game.getCombatSystem().killZombie(octopus);
        assertTrue(plant.isDisabled());

        game.getBoard().addProjectile(this.projectile("300", ProjectileType.NORMAL, 3, 1));
        game.getCombatSystem().onTick();

        assertFalse(game.getBoard().getPlantCoverSystem().isCovered(plant));
        assertFalse(plant.isDisabled());
    }

    @Test
    public void graveAndFrozenTerrainBlockStraightShotsAndCanBeDestroyed() {
        Board board = new Board();
        CombatSystem combat = new CombatSystem(board);
        Position position = new Position(4, 0);
        board.setTerrain(position, TerrainType.GRAVE);
        Tile tile = board.getTile(position);

        board.addProjectile(this.projectile("699", ProjectileType.NORMAL, 3, 0));
        combat.onTick();
        assertEquals(TerrainType.GRAVE, tile.getTerrainType());
        assertEquals(1, tile.getTerrainHealth());

        board.addProjectile(this.projectile("1", ProjectileType.NORMAL, 3, 0));
        combat.onTick();
        assertEquals(TerrainType.CLASSIC, tile.getTerrainType());

        board.setTerrain(position, TerrainType.FROZEN);
        board.addProjectile(this.projectile("1", ProjectileType.FIRE, 3, 0));
        combat.onTick();
        assertEquals(TerrainType.CLASSIC, tile.getTerrainType());
    }

    @Test
    public void frozenTerrainDisablesOccupantsAndHotPotatoMeltsIt() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Position position = new Position(3, 2);
        assertTrue(game.plant("Sunflower", position));
        Plant sunflower = game.getBoard().getPlantsAt(position).get(0);

        game.getBoard().setTerrain(position, TerrainType.FROZEN);
        assertTrue(sunflower.isDisabled());
        assertTrue(game.plant("Hot Potato", position));

        assertEquals(TerrainType.CLASSIC, game.getBoard().getTile(position).getTerrainType());
        assertFalse(sunflower.isDisabled());
        assertEquals(1, game.getBoard().getPlantsAt(position).size());
    }

    @Test
    public void adjacentFirePlantThawsFrozenTerrainAtSixtyHealthPerSecond() {
        Main application = Main.loadApplication();
        Board board = new Board();
        CombatSystem combat = new CombatSystem(board);
        Position frozenPosition = new Position(4, 2);
        board.setTerrain(frozenPosition, TerrainType.FROZEN);

        PlantDefinition definition = application.getPlantDefinitions().findByName("Fire Peashooter");
        Plant firePlant = new PlantFactory().create(definition);
        new PlantingSystem(board, null, null).plant(firePlant, new Position(3, 1));

        for (int tick = 0; tick < 10; tick++) {
            combat.onTick();
        }

        assertEquals(540, board.getTile(frozenPosition).getTerrainHealth());
    }

    @Test
    public void stackedPeaPodsProduceOneScaledProjectile() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        PlantDefinition peaPodDefinition = application.getPlantDefinitions().findByName("Pea Pod");
        PlantingSystem planting = new PlantingSystem(game.getBoard(), null, null);
        Position position = new Position(1, 3);
        planting.plant(new PlantFactory().create(peaPodDefinition), position);
        planting.plant(new PlantFactory().create(peaPodDefinition), position);
        game.spawnZombie("ZombieDefault", 3);

        game.advanceTime(15);

        assertEquals(1, game.getBoard().getProjectiles().size());
        assertEquals("40", game.getBoard().getProjectiles().get(0).getDamageExpression());
    }

    @Test
    public void torchwoodTransformsOnlyPeaProjectilesAndOnlyOnce() {
        Main application = Main.loadApplication();
        Board board = new Board();
        PlantFactory factory = new PlantFactory();
        Plant peashooter = factory.create(application.getPlantDefinitions().findByName("Peashooter"));
        Plant torchwood = factory.create(application.getPlantDefinitions().findByName("Torchwood"));
        PlantingSystem planting = new PlantingSystem(board, null, null);
        planting.plant(peashooter, new Position(0, 0));
        planting.plant(torchwood, new Position(1, 0));

        Zombie zombie = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                new Position(8, 0)
        );
        board.addZombie(zombie, zombie.getPosition());
        peashooter.useAbility();
        Projectile pea = board.getProjectiles().get(0);
        pea.move();
        torchwood.onTick();
        torchwood.onTick();

        assertEquals(ProjectileType.FIRE, pea.getType());
        assertEquals("40", pea.getDamageExpression());

        Projectile ordinaryShot = this.projectile("20", ProjectileType.NORMAL, 0, 0);
        ordinaryShot.move();
        board.addProjectile(ordinaryShot);
        torchwood.onTick();
        assertEquals(ProjectileType.NORMAL, ordinaryShot.getType());
        assertEquals("20", ordinaryShot.getDamageExpression());
    }

    @Test
    public void damageOverflowCrossesEveryArmorBeforeBody() {
        ZombieArmor first = new ZombieArmor(new ZombieArmorDefinition(
                "first", ArmorType.CONE, 50, EnumSet.of(ArmorFlag.DAMAGEABLE)
        ));
        ZombieArmor second = new ZombieArmor(new ZombieArmorDefinition(
                "second", ArmorType.BUCKET, 50, EnumSet.of(ArmorFlag.DAMAGEABLE)
        ));
        Zombie zombie = new Zombie(
                this.definition("Armored", 100),
                new Position(8, 0),
                100,
                0,
                Arrays.asList(first, second),
                new ArrayList<ZombieCondition>(),
                null
        );

        zombie.takeDamage(120);

        assertEquals(0, first.getCurrentHealth());
        assertEquals(0, second.getCurrentHealth());
        assertEquals(80, zombie.getHealth());
    }

    @Test
    public void plantProjectilePassesHypnotizedAllyAndHitsEnemyBehindIt() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        ZombieDefinition definition = application.getZombieDefinitions().findByAlias("ZombieDefault");
        Zombie ally = application.getZombieFactory().create(definition, new Position(5, 4));
        ally.addCondition(ZombieCondition.HYPNOTIZED);
        Zombie enemy = application.getZombieFactory().create(definition, new Position(6, 4));
        game.getBoard().addZombie(ally, ally.getPosition());
        game.getBoard().addZombie(enemy, enemy.getPosition());
        int allyHealth = ally.getHealth();
        int enemyHealth = enemy.getHealth();
        game.getBoard().addProjectile(this.projectile("20", ProjectileType.NORMAL, 4, 4));

        game.getCombatSystem().onTick();
        game.getCombatSystem().onTick();

        assertEquals(allyHealth, ally.getHealth());
        assertEquals(enemyHealth - 20, enemy.getHealth());
    }

    @Test
    public void submergedSnorkelIgnoresDirectDamageButLobbedProjectileHits() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie snorkel = game.spawnZombie("ZombieBeachSnorkel", 2);
        snorkel.activateAbility();
        int health = snorkel.getHealth();

        game.getCombatSystem().applyDamageToZombie(snorkel, 100);
        game.getCombatSystem().applyDirectDamageToZombie(snorkel, 100);
        assertEquals(health, snorkel.getHealth());

        Projectile straight = this.projectile("20", ProjectileType.NORMAL, 7, 2);
        game.getBoard().addProjectile(straight);
        game.getCombatSystem().onTick();
        assertEquals(health, snorkel.getHealth());

        Projectile lobbed = this.projectile("20", ProjectileType.NORMAL, 7, 2);
        lobbed.setLobbed(true);
        game.getBoard().addProjectile(lobbed);
        game.getCombatSystem().onTick();
        assertEquals(health - 20, snorkel.getHealth());
    }

    @Test
    public void frozenZombieCannotBiteOrUseItsBehavior() {
        PlantZombieGame game = Main.loadApplication().createGame();
        assertTrue(game.plant("Wall-nut", new Position(7, 0)));
        Plant wallNut = game.getBoard().getPlantsAt(new Position(7, 0)).get(0);
        Zombie zombie = game.spawnZombie("ZombieDefault", new Position(7, 0));
        int health = wallNut.getHealth();
        zombie.addCondition(ZombieCondition.FROZEN, 10);

        for (int tick = 0; tick < 5; tick++) {
            zombie.onTick();
        }

        assertEquals(health, wallNut.getHealth());
        zombie.removeCondition(ZombieCondition.FROZEN);
        zombie.onTick();
        assertTrue(wallNut.getHealth() < health);
    }

    @Test
    public void chilledZombieBitesAtHalfDamagePerSecond() {
        PlantZombieGame game = Main.loadApplication().createGame();
        assertTrue(game.plant("Wall-nut", new Position(7, 1)));
        Plant wallNut = game.getBoard().getPlantsAt(new Position(7, 1)).get(0);
        Zombie zombie = game.spawnZombie("ZombieDefault", new Position(7, 1));
        int health = wallNut.getHealth();
        zombie.addCondition(ZombieCondition.CHILLED, 20);

        for (int tick = 0; tick < 10; tick++) {
            zombie.onTick();
        }

        assertEquals(health - zombie.getDefinition().getEatDamagePerSecond() / 2, wallNut.getHealth());
    }

    @Test
    public void reversedProspectorLeavesBoardAtTheRightEdge() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie prospector = game.spawnZombie("ZombieProspector", 3);

        game.advanceTime(600);

        assertTrue(prospector.isDead());
        assertFalse(game.getBoard().getAllZombies().contains(prospector));
    }

    @Test
    public void kingDoesNotKnightAHypnotizedAlly() {
        PlantZombieGame game = Main.loadApplication().createGame();
        Zombie king = game.spawnZombie("ZombieDarkKing", 2);
        Zombie ally = game.spawnZombie("ZombieDefault", 2);
        ally.addCondition(ZombieCondition.HYPNOTIZED);

        king.activateAbility();

        assertTrue(ally.getArmors().isEmpty());
    }

    @Test
    public void sweetPotatoPullsOnlyHostileGroundZombies() {
        Main application = Main.loadApplication();
        PlantZombieGame game = application.createGame();
        game.getSunSystem().addSun(200);
        Position sweetPotatoPosition = new Position(4, 2);
        assertTrue(game.plant("Sweet Potato", sweetPotatoPosition));
        Plant sweetPotato = game.getBoard().getPlantsAt(sweetPotatoPosition).get(0);
        Zombie ally = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                new Position(4, 1)
        );
        ally.addCondition(ZombieCondition.HYPNOTIZED);
        game.getBoard().addZombie(ally, ally.getPosition());
        Zombie hostile = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                new Position(5, 1)
        );
        game.getBoard().addZombie(hostile, hostile.getPosition());
        Zombie dodo = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieIceAgeDodo"),
                new Position(3, 1)
        );
        game.getBoard().addZombie(dodo, dodo.getPosition());
        Zombie snorkel = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieBeachSnorkel"),
                new Position(5, 3)
        );
        snorkel.addCondition(ZombieCondition.SUBMERGED);
        game.getBoard().addZombie(snorkel, snorkel.getPosition());

        sweetPotato.onTick();

        assertEquals(new Position(4, 1), ally.getPosition());
        assertEquals(new Position(5, 2), hostile.getPosition());
        assertEquals(new Position(3, 1), dodo.getPosition());
        assertEquals(new Position(5, 3), snorkel.getPosition());
    }

    @Test
    public void temporaryPlantLifespanContinuesWhileCovered() {
        Main application = Main.loadApplication();
        Board board = new Board();
        Position position = new Position(2, 2);
        Plant puffShroom = new PlantFactory().create(
                application.getPlantDefinitions().findByName("Puff-shroom")
        );
        new PlantingSystem(board, null, null).plant(puffShroom, position);
        puffShroom.setCovered(true);

        for (int tick = 0; tick < 600; tick++) {
            puffShroom.onTick();
        }

        assertFalse(board.getPlantsAt(position).contains(puffShroom));
    }

    @Test
    public void hypnoShroomDoesNotConsumeItselfForAnExistingAlly() {
        Main application = Main.loadApplication();
        Board board = new Board();
        Position position = new Position(4, 2);
        Plant hypnoShroom = new PlantFactory().create(
                application.getPlantDefinitions().findByName("Hypno-shroom")
        );
        new PlantingSystem(board, null, null).plant(hypnoShroom, position);
        Zombie ally = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias("ZombieDefault"),
                position
        );
        ally.addCondition(ZombieCondition.HYPNOTIZED);
        board.addZombie(ally, position);

        hypnoShroom.takeDamage(1);

        assertTrue(board.getPlantsAt(position).contains(hypnoShroom));
    }

    private Projectile projectile(String damage, ProjectileType type, int x, int row) {
        return new Projectile(damage, new Position(x, row), 1.0, type, null);
    }

    private ZombieDefinition definition(String alias, int health) {
        return new ZombieDefinition(
                alias,
                alias,
                "",
                ZombieType.BASIC,
                ZombieChapter.ALL_CHAPTERS,
                health,
                0,
                0,
                0,
                1,
                false,
                Collections.<ZombieArmorDefinition>emptyList(),
                Collections.<ConditionResistance>emptyList()
        );
    }
}
