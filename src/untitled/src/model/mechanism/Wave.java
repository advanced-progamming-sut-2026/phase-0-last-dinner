package model.mechanism;

import model.zombie.Zombie;

import java.util.List;

public class Wave {
    private int number;
    private double difficulty;
    private boolean finalWave;
    private List<Zombie> zombies;
    private boolean started;

    public void start() {
    }

    public double getRemainingHealthPercentage() {
        return 0;
    }

    public boolean canStartNextWave() {
        return false;
    }

    public boolean isFinalWave() {
        return false;
    }
}
