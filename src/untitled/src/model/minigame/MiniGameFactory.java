package model.minigame;

import model.minigame.beghouledminigame.BeghouledMiniGame;
import model.minigame.beghouledminigame.PlantZombieBeghouledIntegration;
import model.minigame.izombieminigame.IZombieMiniGame;
import model.minigame.izombieminigame.PlantZombieIZombieIntegration;
import model.minigame.vasebreakerminigame.PlantZombieVasebreakerIntegration;
import model.minigame.vasebreakerminigame.VasebreakerMiniGame;
import model.minigame.wallnutbowlingminigame.PlantZombieWallnutBowlingIntegration;
import model.minigame.wallnutbowlingminigame.WallnutBowlingMiniGame;
import model.minigame.zombotanyminigame.PlantZombieZombotanyIntegration;
import model.minigame.zombotanyminigame.ZombotanyMiniGame;
import model.plant.PlantDefinitionRepository;
import model.zombie.ZombieDefinitionRepository;
import model.zombie.ZombieFactory;

public class MiniGameFactory {

    private final PlantDefinitionRepository plantDefinitions;

    private final ZombieDefinitionRepository zombieDefinitions;

    private final ZombieFactory zombieFactory;

    public MiniGameFactory() {
        this(null, null, null);
    }

    public MiniGameFactory(
            PlantDefinitionRepository plantDefinitions,
            ZombieDefinitionRepository zombieDefinitions,
            ZombieFactory zombieFactory
    ) {
        this.plantDefinitions = plantDefinitions;
        this.zombieDefinitions = zombieDefinitions;
        this.zombieFactory = zombieFactory;
    }

    public MiniGame create(MiniGameType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case VASEBREAKER ->
                    createVasebreaker();

            case WALLNUT_BOWLING ->
                    createWallnutBowling();

            case I_ZOMBIE ->
                    createIZombie();

            case BEGHOULED ->
                    createBeghouled();

            case ZOMBOTANY -> createZombotany();
        };
    }

    private VasebreakerMiniGame createVasebreaker() {
        if (!hasSharedGameDependencies()) {
            return new VasebreakerMiniGame();
        }

        return new VasebreakerMiniGame(
                new PlantZombieVasebreakerIntegration(
                        plantDefinitions,
                        zombieDefinitions,
                        zombieFactory
                )
        );
    }

    private WallnutBowlingMiniGame createWallnutBowling() {
        if (!hasSharedGameDependencies()) {
            return new WallnutBowlingMiniGame();
        }

        return new WallnutBowlingMiniGame(
                new PlantZombieWallnutBowlingIntegration(
                        plantDefinitions,
                        zombieDefinitions,
                        zombieFactory
                )
        );
    }

    private IZombieMiniGame createIZombie() {
        if (!hasSharedGameDependencies()) {
            return new IZombieMiniGame();
        }

        return new IZombieMiniGame(
                new PlantZombieIZombieIntegration(
                        plantDefinitions,
                        zombieDefinitions,
                        zombieFactory
                )
        );
    }

    private BeghouledMiniGame createBeghouled() {
        if (!hasSharedGameDependencies()) {
            return new BeghouledMiniGame();
        }

        return new BeghouledMiniGame(
                new PlantZombieBeghouledIntegration(
                        plantDefinitions,
                        zombieDefinitions,
                        zombieFactory
                )
        );
    }

    private MiniGame createZombotany() {
        if (plantDefinitions != null
                && zombieDefinitions != null) {
            return new ZombotanyMiniGame(
                    new PlantZombieZombotanyIntegration(
                            plantDefinitions,
                            zombieDefinitions,
                            zombieFactory
                    )
            );
        }

        return new ZombotanyMiniGame();
    }

    private boolean hasSharedGameDependencies() {
        return plantDefinitions != null
                && zombieDefinitions != null
                && zombieFactory != null;
    }
}