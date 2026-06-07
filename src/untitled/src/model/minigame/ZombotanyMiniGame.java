package model.minigame;

import model.zombie.ZombieDefinition;

import java.util.List;
import java.util.Map;

public class ZombotanyMiniGame extends MiniGame {
    private Map<ZombieDefinition, ZombotanyTrait> zombieTraits;
    private List<ZombieDefinition> availableZombies;

    public ZombotanyMiniGame() {
        super(MiniGameType.ZOMBOTANY);
    }

    public ZombotanyTrait getTrait(ZombieDefinition definition) {
        return null;
    }

    @Override
    public void start() {
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
