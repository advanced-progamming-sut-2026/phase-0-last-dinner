import model.Plant;
import model.mechanism.Board;
import model.mechanism.CombatSystem;
import model.mechanism.PlantingSystem;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Wave;
import model.plant.PlantDefinition;
import model.plant.PlantFactory;
import model.plant.Projectile;
import model.plant.behavior.ExplosiveBehavior;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlantFoodAndTangleKelpTest {
    @Test
    public void kernelPlantFoodButtersAndStunsEveryEnemy() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant kernel = this.plant(application, board, "Kernel-pult", new Position(1, 0));
        Zombie first = this.zombie(application, board, "ZombieDefault", new Position(4, 0));
        Zombie second = this.zombie(application, board, "ZombieDefault", new Position(5, 1));
        int firstHealth = first.getHealth();
        int secondHealth = second.getHealth();

        kernel.receivePlantFood();

        assertTrue(first.hasCondition(ZombieCondition.STUNNED));
        assertTrue(second.hasCondition(ZombieCondition.STUNNED));
        assertEquals(firstHealth - 40, first.getHealth());
        assertEquals(secondHealth - 40, second.getHealth());
    }

    @Test
    public void fumePlantFoodDamagesAndKnocksBackOnlyItsLane() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant fume = this.plant(application, board, "Fume-shroom", new Position(1, 0));
        Zombie sameLane = this.zombie(application, board, "ZombieDefault", new Position(4, 0));
        Zombie otherLane = this.zombie(application, board, "ZombieDefault", new Position(4, 1));
        int sameLaneHealth = sameLane.getHealth();
        int otherLaneHealth = otherLane.getHealth();

        fume.receivePlantFood();

        assertEquals(sameLaneHealth - 20, sameLane.getHealth());
        assertEquals(6, sameLane.getPosition().getX());
        assertEquals(otherLaneHealth, otherLane.getHealth());
        assertEquals(4, otherLane.getPosition().getX());
    }

    @Test
    public void repeaterPlantFoodAddsOneTwentyTimesGiantPea() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant repeater = this.plant(application, board, "Repeater", new Position(1, 0));
        this.zombie(application, board, "ZombieDefault", new Position(8, 0));

        repeater.receivePlantFood();

        assertEquals(11, board.getProjectiles().size());
        assertEquals(1, this.projectileCount(board, "400"));
        assertEquals(10, this.projectileCount(board, "20"));
    }

    @Test
    public void peaPodPlantFoodLaunchesOneGiantPeaPerHead() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Position position = new Position(1, 1);
        Plant firstHead = this.plant(application, board, "Pea Pod", position);
        this.plant(application, board, "Pea Pod", position);
        this.plant(application, board, "Pea Pod", position);
        this.zombie(application, board, "ZombieDefault", new Position(8, 1));

        firstHead.receivePlantFood();

        assertEquals(3, board.getProjectiles().size());
        assertEquals(3, this.projectileCount(board, "400"));
    }

    @Test
    public void hypnoPlantFoodReplacesTargetWithAlliedGargantuar() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant hypno = this.plant(application, board, "Hypno-shroom", new Position(1, 2));
        Zombie original = this.zombie(application, board, "ZombieDefault", new Position(4, 2));
        Wave wave = new Wave(1, 1.0, false);
        wave.addZombie(original);

        hypno.receivePlantFood();

        assertFalse(board.getAllZombies().contains(original));
        assertEquals(1, board.getAllZombies().size());
        Zombie ally = board.getAllZombies().get(0);
        assertEquals("ZombieGargantuar", ally.getDefinition().getAlias());
        assertTrue(ally.hasCondition(ZombieCondition.HYPNOTIZED));
        assertFalse(wave.getZombies().contains(original));
        assertTrue(wave.getZombies().contains(ally));
        assertEquals(null, original.getWave());
        assertEquals(wave, ally.getWave());
        assertEquals(0.0, wave.getRemainingHealthPercentage(), 0.0);

        Zombie enemy = this.zombie(application, board, "ZombieDefault", new Position(5, 2));
        int enemyHealth = enemy.getHealth();
        ally.onTick();
        assertTrue(enemy.getHealth() < enemyHealth);

        Plant wallNut = this.plant(application, board, "Wall-nut", new Position(3, 2));
        int plantHealth = wallNut.getHealth();
        ally.attack(wallNut);
        assertEquals(plantHealth, wallNut.getHealth());
    }

    @Test
    public void freezeAndPoisonPlantFoodConditionsExpireAfterThreeSeconds() {
        Main application = Main.loadApplication();

        this.assertTimedPlantFoodCondition(application, "Iceberg Lettuce", ZombieCondition.FROZEN);
        this.assertTimedPlantFoodCondition(application, "Snow Pea", ZombieCondition.FROZEN);
        this.assertTimedPlantFoodCondition(application, "Goo Peashooter", ZombieCondition.POISONED);
    }

    @Test
    public void plantFoodClonesUseNormalTerrainAndOccupancyRules() {
        Main application = Main.loadApplication();
        Board potatoBoard = this.boardWithCombat();
        Plant potato = this.plant(application, potatoBoard, "Potato Mine", new Position(4, 2));
        this.plant(application, potatoBoard, "Wall-nut", new Position(3, 1));
        this.plant(application, potatoBoard, "Wall-nut", new Position(4, 1));

        potato.receivePlantFood();

        assertEquals(1, potatoBoard.getPlantsAt(new Position(3, 1)).size());
        assertEquals(1, potatoBoard.getPlantsAt(new Position(4, 1)).size());
        assertEquals(3, potatoBoard.getPlantsByName("Potato Mine").size());

        Board lilyBoard = this.boardWithCombat();
        Position originalPosition = new Position(4, 2);
        Position occupiedWater = new Position(3, 1);
        Position emptyWater = new Position(4, 1);
        lilyBoard.setTerrain(originalPosition, TerrainType.WATER);
        lilyBoard.setTerrain(occupiedWater, TerrainType.WATER);
        lilyBoard.setTerrain(emptyWater, TerrainType.WATER);
        this.plant(application, lilyBoard, "Lily Pad", occupiedWater);
        Plant lily = this.plant(application, lilyBoard, "Lily Pad", originalPosition);

        lily.receivePlantFood();

        assertEquals(1, lilyBoard.getPlantsAt(occupiedWater).size());
        assertEquals(1, lilyBoard.getPlantsAt(emptyWater).size());
        assertEquals(3, lilyBoard.getPlantsByName("Lily Pad").size());
    }

    @Test
    public void garlicPlantFoodMovesOnlyHostileGroundZombie() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant garlic = this.plant(application, board, "Garlic", new Position(1, 2));
        Zombie hostile = this.zombie(application, board, "ZombieDefault", new Position(4, 2));
        Zombie ally = this.zombie(application, board, "ZombieDefault", new Position(5, 2));
        ally.addCondition(ZombieCondition.HYPNOTIZED);
        Zombie dodo = this.zombie(application, board, "ZombieIceAgeDodo", new Position(6, 2));
        Zombie submerged = this.zombie(application, board, "ZombieBeachSnorkel", new Position(7, 2));
        submerged.addCondition(ZombieCondition.SUBMERGED);

        garlic.receivePlantFood();

        assertEquals(1, hostile.getPosition().getY());
        assertEquals(2, ally.getPosition().getY());
        assertEquals(2, dodo.getPosition().getY());
        assertEquals(2, submerged.getPosition().getY());
    }

    @Test
    public void endurianPlantFoodAddsArmorAndDoublesReflection() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant endurian = this.plant(application, board, "Endurian", new Position(4, 3));
        Zombie attacker = this.zombie(application, board, "ZombieDefault", new Position(5, 3));
        int attackerHealth = attacker.getHealth();

        endurian.receivePlantFood();
        endurian.takeDamage(10);

        assertEquals(6000, endurian.getMaximumHealth());
        assertEquals(attackerHealth - 40, attacker.getHealth());
    }

    @Test
    public void explodeONutPlantFoodExplodesWhenMetalArmorBreaks() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant explodeONut = this.plant(application, board, "Explode-o-nut", new Position(4, 4));
        Zombie target = this.zombie(application, board, "ZombieDefault", new Position(5, 4));

        explodeONut.receivePlantFood();
        explodeONut.takeDamage(2999);
        assertFalse(target.isDead());

        explodeONut.takeDamage(1);

        assertEquals(7000, explodeONut.getMaximumHealth());
        assertTrue(target.isDead());
    }

    @Test
    public void tangleKelpRequiresWaterAndOnlyPullsSubmergedWaterZombie() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        PlantDefinition definition = application.getPlantDefinitions().findByName("Tangle Kelp");
        PlantingSystem planting = new PlantingSystem(board, null, null);
        Position waterPosition = new Position(4, 2);

        assertFalse(planting.canPlant(new PlantFactory().create(definition), new Position(4, 1)));
        board.setTerrain(waterPosition, TerrainType.WATER);
        Plant kelp = this.plant(application, board, "Tangle Kelp", waterPosition);
        Zombie submerged = this.zombie(application, board, "ZombieBeachSnorkel", waterPosition);
        submerged.addCondition(ZombieCondition.SUBMERGED);
        Zombie landZombie = this.zombie(application, board, "ZombieDefault", new Position(5, 2));

        kelp.onTick();

        assertTrue(submerged.isDead());
        assertFalse(landZombie.isDead());
        assertFalse(board.getPlantsAt(waterPosition).contains(kelp));
    }

    @Test
    public void tangleKelpPlantFoodPullsThreeRandomSubmergedWaterZombies() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Position kelpPosition = new Position(0, 2);
        board.setTerrain(kelpPosition, TerrainType.WATER);
        Plant kelp = this.plant(application, board, "Tangle Kelp", kelpPosition);
        List<Zombie> waterZombies = new ArrayList<>();

        for (int x = 3; x <= 6; x++) {
            Position position = new Position(x, 2);
            board.setTerrain(position, TerrainType.WATER);
            Zombie zombie = this.zombie(application, board, "ZombieBeachSnorkel", position);
            zombie.addCondition(ZombieCondition.SUBMERGED);
            waterZombies.add(zombie);
        }

        Zombie landZombie = this.zombie(application, board, "ZombieDefault", new Position(7, 2));
        kelp.receivePlantFood();

        int deadWaterZombies = 0;

        for (Zombie zombie : waterZombies) {
            if (zombie.isDead()) {
                deadWaterZombies++;
            }
        }

        assertEquals(3, deadWaterZombies);
        assertFalse(landZombie.isDead());
    }

    @Test
    public void dodoDoesNotTriggerTrapBeforeFlyingBehaviorGetsItsFirstTick() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Position position = new Position(5, 0);
        Plant mine = this.plant(application, board, "Potato Mine", position);
        ((ExplosiveBehavior) mine.getBehavior()).armNow();
        Zombie dodo = this.zombie(application, board, "ZombieIceAgeDodo", position);

        assertFalse(dodo.hasCondition(ZombieCondition.FLYING));
        mine.onTick();

        assertFalse(dodo.isDead());
        assertTrue(board.getPlantsAt(position).contains(mine));
    }

    @Test
    public void defenderContactEffectIgnoresDodoBeforeFirstTick() {
        Main application = Main.loadApplication();
        Board board = this.boardWithCombat();
        Plant endurian = this.plant(application, board, "Endurian", new Position(4, 0));
        Zombie dodo = this.zombie(application, board, "ZombieIceAgeDodo", new Position(5, 0));
        Plant sweetPotato = this.plant(application, board, "Sweet Potato", new Position(4, 2));
        Zombie nearbyDodo = this.zombie(application, board, "ZombieIceAgeDodo", new Position(5, 1));
        int health = dodo.getHealth();

        assertFalse(dodo.hasCondition(ZombieCondition.FLYING));
        assertFalse(nearbyDodo.hasCondition(ZombieCondition.FLYING));
        endurian.takeDamage(10);
        sweetPotato.onTick();

        assertEquals(health, dodo.getHealth());
        assertEquals(1, nearbyDodo.getPosition().getY());
    }

    private Board boardWithCombat() {
        Board board = new Board();
        new CombatSystem(board);
        return board;
    }

    private Plant plant(Main application, Board board, String name, Position position) {
        Plant plant = new PlantFactory().create(application.getPlantDefinitions().findByName(name));
        new PlantingSystem(board, null, null).plant(plant, position);
        assertTrue(board.getPlantsAt(position).contains(plant));
        return plant;
    }

    private Zombie zombie(Main application, Board board, String alias, Position position) {
        Zombie zombie = application.getZombieFactory().create(
                application.getZombieDefinitions().findByAlias(alias),
                position
        );
        board.addZombie(zombie, position);
        return zombie;
    }

    private int projectileCount(Board board, String damageExpression) {
        int count = 0;

        for (Projectile projectile : board.getProjectiles()) {
            if (projectile != null && damageExpression.equals(projectile.getDamageExpression())) {
                count++;
            }
        }

        return count;
    }

    private void assertTimedPlantFoodCondition(
            Main application,
            String plantName,
            ZombieCondition condition
    ) {
        Board board = this.boardWithCombat();
        Plant plant = this.plant(application, board, plantName, new Position(1, 0));
        Zombie target = this.zombie(application, board, "ZombieDefault", new Position(4, 0));
        target.setCurrentSpeed(0);

        plant.receivePlantFood();
        assertTrue(target.hasCondition(condition));

        for (int tick = 0; tick < 29; tick++) {
            target.onTick();
        }

        assertTrue(target.hasCondition(condition));
        target.onTick();
        assertFalse(target.hasCondition(condition));
    }
}
