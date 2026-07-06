package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class Wave {
    private int number;
    private double difficulty;
    private boolean finalWave;
    private List<Zombie> zombies;
    private boolean started;
    public Wave(int number, double difficulty, boolean finalWave) {
        this.number = number;
        this.difficulty = difficulty;
        this.finalWave = finalWave;
        this.zombies = new ArrayList<>();
        this.started = false;
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null) zombies.add(zombie);
    }
    public void start() {
        this.started = true;
    }

    public double getRemainingHealthPercentage() {
        if (zombies.isEmpty()) return 0;

        int totalMax = 0;
        int totalCurrent = 0;
        for (Zombie zombie : zombies) {
            totalMax += zombie.getDefinition().getHitpoints();
            totalCurrent += zombie.getHealth();
        }
        if (totalMax == 0) return 0;
        return (double) totalCurrent / totalMax;
    }

    public boolean canStartNextWave() {
        return getRemainingHealthPercentage() <= 0.25;
    }
}
