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
    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }

    public Loot generateZombieDrop(Zombie zombie) {
        if (zombie == null) return null;
        if (random.nextInt(100) >= 10) return null;
        LootType type;
        int amount;
        int roll = random.nextInt(3);
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
        droppedLoot.add(loot);
        return loot;
    }

    public void collectLoot(Loot loot) {
        if (loot == null || loot.isCollected()) return;

        loot.collect();

        switch (loot.getType()) {
            case COIN:
                coinAmount += loot.getAmount();
                fireEvent("A zombie dropeed a coin; you have " + coinAmount + " coins now.");
                break;
            case DIAMOND:
                diamondAmount += loot.getAmount();
                fireEvent("A zombie dropeed a diamond; you have " + diamondAmount + " diamonds now.");
                break;
            case POT:
                potAmount += loot.getAmount();
                fireEvent("A zombie dropeed a pot; you have " + potAmount + " pots now.");
        }
    }
}
