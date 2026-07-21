package model.mechanism;

import lombok.Getter;
import model.chapters.Chapter;
import model.zombie.Zombie;
import model.zombie.ZombieChapter;
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
    private ZombieChapter activeChapter;
    private Chapter chapter;
    private DifficultyConfig difficultyConfig;

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
        this.activeChapter = ZombieChapter.ALL_CHAPTERS;
        this.difficultyConfig = new DifficultyConfig(null);
    }
    public List<Zombie> spawnWave(Wave wave) {
        List<Zombie> spawnedZombies = new ArrayList<>();

        if (wave == null) {
            return spawnedZombies;
        }

        int requestedCost = Math.max(0, (int) wave.getDifficulty());
        List<ZombieDefinition> definitions = this.getEligibleDefinitions(requestedCost);
        boolean[] reachableCosts = this.calculateReachableCosts(requestedCost, definitions);
        int remainingCost = this.findLargestReachableCost(requestedCost, reachableCosts);

        while (remainingCost > 0) {
            ZombieDefinition definition = this.chooseZombieDefinition(
                    remainingCost,
                    definitions,
                    reachableCosts
            );

            if (definition == null) {
                break;
            }

            int row = this.chooseRandomRow();
            Zombie zombie = this.spawnZombie(definition, null, row);

            if (zombie == null) {
                break;
            }

            wave.addZombie(zombie);
            spawnedZombies.add(zombie);
            remainingCost -= this.getAdjustedWavePointCost(definition);
            this.fireEvent("Zombie " + this.getDefinitionName(definition)
                    + " spawned at wave " + wave.getNumber()
                    + " in lane " + (row + 1)
                    + " which costed " + this.getAdjustedWavePointCost(definition) + ".");
        }

        return spawnedZombies;
    }

    public Zombie spawnZombie(
            ZombieDefinition definition,
            ZombieBehavior behavior,
            int row
    ) {
        return this.spawnZombie(definition, behavior, row, false);
        // این تیکه رو برای طوفان عوض کردم اما دوتا سیگنچر رو نگه داشتم بقیه جاها بهم نریزه
    }
    public Zombie spawnZombie(
            ZombieDefinition definition,
            ZombieBehavior behavior,
            int row,
            boolean isFinalWave
    ) {
        if (definition == null || this.board == null) {
            return null;
        }

        int safeRow = Math.max(0, Math.min(4, row));
        Position spawnPosition = this.chapter != null
                ? this.chapter.resolveZombieSpawnPosition(safeRow, isFinalWave)
                : new Position(8, safeRow);
        Zombie zombie = this.zombieFactory.create(definition, spawnPosition);

        if (zombie != null) {
            zombie.applyDifficulty(this.difficultyConfig.getMultiplier());
            this.board.addZombie(zombie, spawnPosition);
        }

        return zombie;
    }

    public int chooseRandomRow() {
        return this.random.nextInt(5);
    }

    public ZombieDefinition chooseZombieDefinition(int remainingCost) {
        if (remainingCost <= 0) {
            return null;
        }

        List<ZombieDefinition> definitions = this.getEligibleDefinitions(remainingCost);
        boolean[] reachableCosts = this.calculateReachableCosts(remainingCost, definitions);

        if (!reachableCosts[remainingCost]) {
            return null;
        }

        return this.chooseZombieDefinition(remainingCost, definitions, reachableCosts);
    }
    public ZombieDefinition chooseRandomSpawnableDefinition() {
        List<ZombieDefinition> definitions = this.getEligibleDefinitions(Integer.MAX_VALUE);

        if (definitions.isEmpty()) {
            return null;
        }

        return definitions.get(this.random.nextInt(definitions.size()));
    }

    public void setActiveChapter(ZombieChapter activeChapter) {
        this.activeChapter = activeChapter == null
                ? ZombieChapter.ALL_CHAPTERS
                : activeChapter;
    }

    public void setRandom(Random random) {
        this.random = random == null ? new Random() : random;
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void setDifficultyConfig(DifficultyConfig difficultyConfig) {
        this.difficultyConfig = difficultyConfig == null
                ? new DifficultyConfig(null)
                : difficultyConfig;
    }

    private ZombieDefinition chooseZombieDefinition(
            int remainingCost,
            List<ZombieDefinition> definitions,
            boolean[] reachableCosts
    ) {
        List<ZombieDefinition> validChoices = new ArrayList<>();

        for (ZombieDefinition definition : definitions) {
            int cost = this.getAdjustedWavePointCost(definition);

            if (cost <= remainingCost && reachableCosts[remainingCost - cost]) {
                validChoices.add(definition);
            }
        }

        if (validChoices.isEmpty()) {
            return null;
        }

        long totalWeight = 0;

        for (ZombieDefinition definition : validChoices) {
            totalWeight += Math.max(1, definition.getWeight());
        }

        long roll = (long) (this.random.nextDouble() * totalWeight);
        long cumulativeWeight = 0;

        for (ZombieDefinition definition : validChoices) {
            cumulativeWeight += Math.max(1, definition.getWeight());

            if (roll < cumulativeWeight) {
                return definition;
            }
        }

        return validChoices.get(validChoices.size() - 1);
    }

    private List<ZombieDefinition> getEligibleDefinitions(int maximumCost) {
        List<ZombieDefinition> definitions = new ArrayList<>();

        if (this.definitionRepository == null || maximumCost <= 0) {
            return definitions;
        }

        List<ZombieDefinition> allDefinitions = this.definitionRepository.findAll();

        if (allDefinitions == null) {
            return definitions;
        }

        for (ZombieDefinition definition : allDefinitions) {
            if (definition == null || definition.getWavePointCost() <= 0
                    || this.getAdjustedWavePointCost(definition) > maximumCost
                    || !this.isAvailableInActiveChapter(definition)) {
                continue;
            }

            definitions.add(definition);
        }

        return definitions;
    }

    private boolean isAvailableInActiveChapter(ZombieDefinition definition) {
        if (this.activeChapter == ZombieChapter.ALL_CHAPTERS) {
            return true;
        }

        ZombieChapter definitionChapter = definition.getChapter();
        return definitionChapter == null
                || definitionChapter == ZombieChapter.ALL_CHAPTERS
                || definitionChapter == this.activeChapter;
    }

    // hazine haye ghabele sakht ro ba tarkib zombie ha peyda mikone
    private boolean[] calculateReachableCosts(int maximumCost, List<ZombieDefinition> definitions) {
        boolean[] reachable = new boolean[Math.max(0, maximumCost) + 1];
        reachable[0] = true;

        for (int cost = 1; cost <= maximumCost; cost++) {
            for (ZombieDefinition definition : definitions) {
                int zombieCost = this.getAdjustedWavePointCost(definition);

                if (zombieCost <= cost && reachable[cost - zombieCost]) {
                    reachable[cost] = true;
                    break;
                }
            }
        }

        return reachable;
    }

    private int getAdjustedWavePointCost(ZombieDefinition definition) {
        if (definition == null) {
            return 0;
        }

        return Math.max(1, (int) Math.round(
                definition.getWavePointCost() * this.difficultyConfig.getInverseMultiplier()
        ));
    }

    private int findLargestReachableCost(int requestedCost, boolean[] reachableCosts) {
        for (int cost = requestedCost; cost > 0; cost--) {
            if (reachableCosts[cost]) {
                return cost;
            }
        }

        return 0;
    }

    private String getDefinitionName(ZombieDefinition definition) {
        if (definition == null) {
            return "Zombie";
        }

        String displayName = definition.getDisplayName();

        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }

        String alias = definition.getAlias();
        return alias == null || alias.trim().isEmpty() ? "Zombie" : alias.trim();
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
