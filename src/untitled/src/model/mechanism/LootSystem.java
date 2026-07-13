package model.mechanism;

import model.zombie.Zombie;
import model.zombie.ZombieType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootSystem {
    private List<Loot> droppedLoot;
    private int coinAmount;
    private int diamondAmount;
    private int potAmount;
    private Random random;

    public LootSystem() {
        this.droppedLoot = new ArrayList<>();
        this.random = new Random();
    }

    public Loot generateZombieDrop(Zombie zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return null;
        }

        int roll = this.random.nextInt(100);
        Loot loot = null;

        if (zombie.getDefinition().getType() == ZombieType.BOSS) {
            loot = new Loot(LootType.DIAMOND, 3, zombie.getPosition());
        } else if (zombie.getDefinition().getType() == ZombieType.GARGANTUAR) {
            loot = new Loot(LootType.POT, 1, zombie.getPosition());
        } else if (roll < 10) {
            loot = new Loot(LootType.DIAMOND, 1, zombie.getPosition());
        } else if (roll < 35) {
            loot = new Loot(LootType.COIN, 25, zombie.getPosition());
        }

        if (loot != null) {
            this.droppedLoot.add(loot);
        }

        return loot;
    }

    public void collectLoot(Loot loot) {
        if (loot == null || loot.isCollected()) {
            return;
        }

        loot.collect();

        if (loot.getType() == LootType.COIN) {
            this.coinAmount += loot.getAmount();
        } else if (loot.getType() == LootType.DIAMOND) {
            this.diamondAmount += loot.getAmount();
        } else if (loot.getType() == LootType.POT) {
            this.potAmount += loot.getAmount();
        }
    }

    public int getCoinAmount() {
        return this.coinAmount;
    }

    public int getDiamondAmount() {
        return this.diamondAmount;
    }

    public int getPotAmount() {
        return this.potAmount;
    }
}
