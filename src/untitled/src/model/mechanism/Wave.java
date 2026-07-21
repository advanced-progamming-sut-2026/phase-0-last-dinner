package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;

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
        if (zombie == null) {
            return;
        }

        if (this.zombies == null) {
            this.zombies = new ArrayList<>();
        }

        if (!this.zombies.contains(zombie)) {
            this.zombies.add(zombie);
            zombie.setWave(this);
        }
    }

    // zombie tabdil shode ro dar haman wave jaygozin mikone
    public void replaceZombie(Zombie original, Zombie replacement) {
        if (original == null || replacement == null || original == replacement) {
            return;
        }

        if (this.zombies == null) {
            this.zombies = new ArrayList<>();
        }

        this.zombies.remove(replacement);
        int originalIndex = this.zombies.indexOf(original);

        if (originalIndex >= 0) {
            this.zombies.set(originalIndex, replacement);
        } else {
            this.zombies.add(replacement);
        }

        original.setWave(null);
        replacement.setWave(this);
    }

    public void start() {
        this.started = true;
    }

    // armor ro ham hesab mikone va zombie haye ally ro kenar mizare
    public double getRemainingHealthPercentage() {
        if (this.zombies == null || this.zombies.isEmpty()) {
            return 0;
        }

        int totalMaximumHealth = 0;
        int totalCurrentHealth = 0;

        for (Zombie zombie : this.zombies) {
            if (zombie == null || zombie.getDefinition() == null || zombie.isHypnotized()) {
                continue;
            }

            totalMaximumHealth += zombie.getMaximumHealth();
            totalCurrentHealth += Math.max(0, zombie.getHealth());

            if (zombie.getArmors() != null) {
                for (ZombieArmor armor : zombie.getArmors()) {
                    if (armor == null || armor.getDefinition() == null) {
                        continue;
                    }

                    totalMaximumHealth += Math.max(0, armor.getDefinition().getBaseHealth());

                    if (!armor.isDropped()) {
                        totalCurrentHealth += Math.max(0, armor.getCurrentHealth());
                    }
                }
            }
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
