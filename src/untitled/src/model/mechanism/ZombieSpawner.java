package model.mechanism;

import lombok.Getter;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.behavior.ZombieBehavior;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
public class ZombieSpawner {
    private ZombieFactory zombieFactory;
    private ZombieDefinitionRepository definitionRepository;
    private Board board;
    private GameEventListener listener;
    private Random random;

    public ZombieSpawner() {
        this(null, null, null);
    }

    public ZombieSpawner(
            ZombieFactory zombieFactory,
            ZombieDefinitionRepository definitionRepository,
            Board board
    ) {
        this.definitionRepository = definitionRepository;
        this.zombieFactory = zombieFactory == null ? new ZombieFactory(definitionRepository) : zombieFactory;
        this.board = board;
        this.random = new Random();
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    public List<Zombie> spawnWave(Wave wave) {
        List<Zombie> spawnedZombies = new ArrayList<>();

        if (wave == null) {
            return spawnedZombies;
        }

        int remainingCost = Math.max(0, (int) wave.getDifficulty());

        while (remainingCost > 0) {
            ZombieDefinition definition = this.chooseZombieDefinition(remainingCost);

            if (definition == null) {
                break;
            }

            Zombie zombie = this.spawnZombie(definition, null, this.chooseRandomRow());

            if (zombie == null) {
                break;
            }

            wave.addZombie(zombie);
            spawnedZombies.add(zombie);
            remainingCost -= Math.max(1, definition.getWavePointCost());
            this.fireEvent("Zombie " + definition.getDisplayName() + " spawned in wave "
                    + wave.getNumber() + ".");
        }

        return spawnedZombies;
    }

    public Zombie spawnZombie(
            ZombieDefinition definition,
            ZombieBehavior behavior,
            int row
    ) {
        if (definition == null || this.board == null) {
            return null;
        }

        int safeRow = Math.max(0, Math.min(4, row));
        Position spawnPosition = new Position(8, safeRow);
        Zombie zombie = this.zombieFactory.create(definition, spawnPosition);

        if (zombie != null) {
            this.board.addZombie(zombie, spawnPosition);
        }

        return zombie;
    }

    public int chooseRandomRow() {
        return this.random.nextInt(5);
    }

    public ZombieDefinition chooseZombieDefinition(int remainingCost) {
        if (this.definitionRepository == null) {
            return null;
        }

        List<ZombieDefinition> affordableDefinitions = new ArrayList<>();

        for (ZombieDefinition definition : this.definitionRepository.findAll()) {
            if (definition != null && definition.getWavePointCost() <= remainingCost) {
                affordableDefinitions.add(definition);
            }
        }

        if (affordableDefinitions.isEmpty()) {
            return null;
        }

        int totalWeight = 0;

        for (ZombieDefinition definition : affordableDefinitions) {
            totalWeight += Math.max(1, definition.getWeight());
        }

        int roll = this.random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (ZombieDefinition definition : affordableDefinitions) {
            cumulativeWeight += Math.max(1, definition.getWeight());

            if (roll < cumulativeWeight) {
                return definition;
            }
        }

        return affordableDefinitions.get(affordableDefinitions.size() - 1);
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
