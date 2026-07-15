package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.zombie.Zombie;
import view.GameEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class LootSystem {
    private List<Loot> droppedLoot;
    private int coinAmount;
    private int diamondAmount;
    private int potAmount;
    private GameEventListener listener;
    private Random random;

    public LootSystem() {
        this.droppedLoot = new ArrayList<>();
        this.random = new Random();
    }

    public Loot generateZombieDrop(Zombie zombie) {
        if (zombie == null || this.random.nextInt(100) >= 10) {
            return null;
        }

        LootType type;
        int amount;
        int roll = this.random.nextInt(3);

        if (roll == 0) {
            type = LootType.COIN;
            amount = 50;
        } else if (roll == 1) {
            type = LootType.DIAMOND;
            amount = 1;
        } else {
            type = LootType.POT;
            amount = 1;
        }

        Loot loot = new Loot(type, amount, zombie.getPosition());
        this.droppedLoot.add(loot);
        return loot;
    }

    public void collectLoot(Loot loot) {
        if (loot == null || loot.isCollected()) {
            return;
        }

        loot.collect();

        if (loot.getType() == LootType.COIN) {
            this.coinAmount += loot.getAmount();
            this.fireEvent("A zombie dropped a coin; you have " + this.coinAmount + " coins now.");
        } else if (loot.getType() == LootType.DIAMOND) {
            this.diamondAmount += loot.getAmount();
            this.fireEvent("A zombie dropped a diamond; you have " + this.diamondAmount + " diamonds now.");
        } else if (loot.getType() == LootType.POT) {
            this.potAmount += loot.getAmount();
            this.fireEvent("A zombie dropped a pot; you have " + this.potAmount + " pots now.");
        }
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
