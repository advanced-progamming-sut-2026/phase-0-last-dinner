package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.User.User;
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
    private User user;

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
            if (this.user != null) {
                this.user.setGold(this.safeAdd(this.user.getGold(), loot.getAmount()));
                this.coinAmount = this.user.getGold();
            } else {
                this.coinAmount = this.safeAdd(this.coinAmount, loot.getAmount());
            }
            this.fireEvent("A zombie dropped a coin; you have " + this.coinAmount + " coins now.");
        } else if (loot.getType() == LootType.DIAMOND) {
            if (this.user != null) {
                this.user.setDiamond(this.safeAdd(this.user.getDiamond(), loot.getAmount()));
                this.diamondAmount = this.user.getDiamond();
            } else {
                this.diamondAmount = this.safeAdd(this.diamondAmount, loot.getAmount());
            }
            this.fireEvent("A zombie dropped a diamond; you have " + this.diamondAmount + " diamonds now.");
        } else if (loot.getType() == LootType.POT) {
            this.potAmount = this.safeAdd(this.potAmount, loot.getAmount());
            if (this.user != null && this.user.getGreenhouse() != null) {
                this.user.getGreenhouse().unlockNextPot();
            }
            this.fireEvent("A zombie dropped a pot; you have " + this.potAmount + " pots now.");
        }
    }

    private int safeAdd(int value, int amount) {
        long result = (long) value + amount;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private void fireEvent(String message) {
        if (this.listener != null) {
            this.listener.onGameEvent(message);
        }
    }
}
