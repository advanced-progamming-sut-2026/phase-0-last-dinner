package model.mechanism;

import model.zombie.Zombie;

import java.util.List;

public class LawnMower {
    private int row;
    private boolean active;
    private boolean used;

    public List<Zombie> trigger(List<Zombie> zombies) {
        return null;
    }

    public boolean canTrigger() {
        return false;
    }

    public boolean isUsed() {
        return false;
    }
}
