package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Wave;
import model.zombie.JsonZombieDefinitionRepository;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;
import model.zombie.behavior.BasicZombieBehavior;

import java.io.IOException;

// nazdiktarin doshman ro ba gargantuar ham peyman jaygozin mikone
public class AlliedGargantuarPlantFoodBehavior implements PlantFoodBehavior {
    private static final String ZOMBIES_RESOURCE = "data/zombies.json";
    private static final String ARMOR_RESOURCE = "data/ArmorTypeData.json";
    private static final String GARGANTUAR_ALIAS = "ZombieGargantuar";
    private static final int ALLIED_DAMAGE_PER_SECOND = 500;

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null) {
            return;
        }

        Zombie target = this.findNearestEnemy(plant, board);

        if (target == null || target.getPosition() == null) {
            return;
        }

        Position position = target.getPosition();
        Wave wave = target.getWave();
        JsonZombieDefinitionRepository repository = this.loadDefinitions();
        ZombieDefinition gargantuarDefinition = repository.findByAlias(GARGANTUAR_ALIAS);

        if (gargantuarDefinition == null) {
            throw new IllegalStateException("Bundled Gargantuar definition is missing");
        }

        board.removeZombie(target);
        target.addCondition(ZombieCondition.TRANSFORMED);

        Zombie gargantuar = new ZombieFactory(repository).create(gargantuarDefinition, position);
        gargantuar.addCondition(ZombieCondition.HYPNOTIZED);
        BasicZombieBehavior basicBehavior = gargantuar.findBehavior(BasicZombieBehavior.class);

        if (basicBehavior != null) {
            basicBehavior.ensureMinimumDamagePerSecond(ALLIED_DAMAGE_PER_SECOND);
        }

        board.addZombie(gargantuar, position);

        // jaygozini dar wave jeloye tamoom shodan ghalat moj ro migire
        if (wave != null) {
            wave.replaceZombie(target, gargantuar);
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        return new AlliedGargantuarPlantFoodBehavior();
    }

    private Zombie findNearestEnemy(Plant plant, Board board) {
        for (Zombie zombie : board.getNearestZombies(plant.getPosition(), board.getAllZombies().size())) {
            if (zombie != null && !zombie.isDead() && !zombie.isHypnotized()) {
                return zombie;
            }
        }

        return null;
    }

    private JsonZombieDefinitionRepository loadDefinitions() {
        try {
            return JsonZombieDefinitionRepository.fromClasspath(ZOMBIES_RESOURCE, ARMOR_RESOURCE);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled Gargantuar definition", e);
        }
    }
}
