package model.mechanism;

import model.zombie.Zombie;

import java.util.List;

public class LootSystem {
    private List<Loot> droppedLoot;
    private int coinAmount;
    private int diamondAmount;
    private int potAmount;

    public Loot generateZombieDrop(Zombie zombie) {
        return null;
    }

    public void collectLoot(Loot loot) {
    }

    public int getCoinAmount() {
        return 0;
    }

    public int getDiamondAmount() {
        return 0;
    }

    public int getPotAmount() {
        return 0;
    }
}
