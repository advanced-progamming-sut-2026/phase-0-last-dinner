package model.mechanism;

import lombok.Getter;import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;
import model.zombie.behavior.ZombieBehavior;
import view.GameEventListener;

import java.util.ArrayList;import java.util.List;
import java.util.Random;
@Getter
public class ZombieSpawner {
    private ZombieFactory zombieFactory;
    private ZombieDefinitionRepository definitionRepository;
    private Board board;
    private GameEventListener listener;
    private Random random;
    public ZombieSpawner(ZombieFactory zombieFactory,
                         ZombieDefinitionRepository definitionRepository,
                         Board board) {
        this.zombieFactory = zombieFactory;
        this.definitionRepository = definitionRepository;
        this.board = board;
        this.random = new Random();
    }
    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }

    public List<Zombie> spawnWave(Wave wave) {
        List<Zombie> spawnedZombies = new ArrayList<>();
        int remainingCost = (int) wave.getDifficulty();
        while (remainingCost > 0) {
            ZombieDefinition definition = chooseZombieDefinition(remainingCost);
            if (definition == null) break;
            int row = chooseRandomRow();
            Zombie zombie = spawnZombie(definition, null, row);
            // تعریف کردن  behavior کار زامبی فکتوریه و ما به این سیگنچر نیاز نداریم
            // اما برای اینکه ممکنه شما جایی ازش استفاده کرده باشید میذارم باشه و بهش مقداری پاس نمیدم
            if (zombie != null) {
                wave.addZombie(zombie);
                spawnedZombies.add(zombie);
                remainingCost -= definition.getWavePointCost();
                fireEvent("Zombie " + definition.getDisplayName()
                        + " spawned at wave " + wave.getNumber()
                        + " in lane " + row
                        + " which costed " + definition.getWavePointCost() + ".");
            }
        }
        return spawnedZombies;
    }

    public Zombie spawnZombie(ZombieDefinition definition,
            ZombieBehavior behavior, int row) {
        Position spawnPosition = new Position(8, row);
        Zombie zombie = zombieFactory.create(definition, spawnPosition);

        if (zombie != null) {
            board.addZombie(zombie, spawnPosition);
        }
        return zombie;
    }

    public int chooseRandomRow() {
        return random.nextInt(5);
    }

    public ZombieDefinition chooseZombieDefinition(int remainingCost) {
        List<ZombieDefinition> all = definitionRepository.findAll();
        // فیلتر کن زامبی‌هایی که cost شون از remainingCost بیشتر نیست
        List<ZombieDefinition> affordable = new ArrayList<>();
        for (ZombieDefinition def : all) {
            if (def.getWavePointCost() <= remainingCost) {
                affordable.add(def);
            }
        }
        if (affordable.isEmpty()) return null;
        // تصادفی با توجه به weight انتخاب کن
        int totalWeight = 0;
        for (ZombieDefinition def : affordable) {
            totalWeight += def.getWeight();
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (ZombieDefinition def : affordable) {
            cumulative += def.getWeight();
            if (roll < cumulative) return def;
        }
        return affordable.get(affordable.size() - 1);
    }
}
