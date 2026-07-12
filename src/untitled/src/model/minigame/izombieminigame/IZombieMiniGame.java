package model.minigame.izombieminigame;

import model.mechanism.Position;
import model.minigame.MiniGame;
import model.minigame.MiniGameType;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;

import java.util.List;
import java.util.Map;

public class IZombieMiniGame extends MiniGame {
    private int sunAmount;
    private List<ZombieDefinition> availableZombies;
    private Map<ZombieDefinition, Integer> zombieCosts;
    private List<Zombie> placedZombies;
    private List<Brain> brains;
    private List<Zombie> sunProducerZombies;

    public IZombieMiniGame() {
        super(MiniGameType.I_ZOMBIE);
    }

    public Zombie placeZombie(
            ZombieDefinition definition,
            Position position
    ) {
        return null;
    }

    public void addSun(int amount) {
    }

    public boolean canAfford(ZombieDefinition definition) {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean isWinConditionMet() {
        return false;
    }

    @Override
    public boolean isLoseConditionMet() {
        return false;
    }
}
