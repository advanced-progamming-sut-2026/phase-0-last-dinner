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

    public Wave() {
        this(1, 0, false);
    }

    public Wave(int number, double difficulty, boolean finalWave) {
        this.number = number;
        this.difficulty = difficulty;
        this.finalWave = finalWave;
        this.zombies = new ArrayList<>();
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null) {
            this.zombies.add(zombie);
        }
    }

    public void start() {
        this.started = true;
    }

    public double getRemainingHealthPercentage() {
        if (this.zombies == null || this.zombies.isEmpty()) {
            return 0;
        }

        int totalMaximumHealth = 0;
        int totalCurrentHealth = 0;

        for (Zombie zombie : this.zombies) {
            if (zombie == null || zombie.getDefinition() == null) {
                continue;
            }

            totalMaximumHealth += zombie.getDefinition().getHitpoints();
            totalCurrentHealth += Math.max(0, zombie.getHealth());
        }

        if (totalMaximumHealth <= 0) {
            return 0;
        }

        return (double) totalCurrentHealth / totalMaximumHealth;
    }

    public boolean canStartNextWave() {
        return this.started && this.getRemainingHealthPercentage() <= 0.25;
    }
}
