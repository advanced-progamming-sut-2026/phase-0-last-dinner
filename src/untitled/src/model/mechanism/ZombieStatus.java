package model.mechanism;

import java.util.LinkedHashMap;
import java.util.Map;

// etelaat lahzei baraye dastoor zombies info ro negah midare
public final class ZombieStatus {
    private final String zombieType;
    private final Position position;
    private final double exactX;
    private final int health;
    private final Map<String, Integer> armorHealth;
    private final Map<String, Long> effectRemainingTicks;

    public ZombieStatus(String zombieType, Position position, double exactX, int health) {
        this.zombieType = zombieType;
        this.position = position;
        this.exactX = exactX;
        this.health = health;
        this.armorHealth = new LinkedHashMap<>();
        this.effectRemainingTicks = new LinkedHashMap<>();
    }

    public String getZombieType() {
        return this.zombieType;
    }

    public Position getPosition() {
        return this.position;
    }

    public double getExactX() {
        return this.exactX;
    }

    public int getHealth() {
        return this.health;
    }

    public Map<String, Integer> getArmorHealth() {
        return this.armorHealth;
    }

    public Map<String, Long> getEffectRemainingTicks() {
        return this.effectRemainingTicks;
    }

    public void addArmor(String armorName, int remainingHealth) {
        if (armorName != null && remainingHealth > 0) {
            this.armorHealth.put(armorName, remainingHealth);
        }
    }

    public void addEffect(String effectName, Long remainingTicks) {
        if (effectName != null) {
            this.effectRemainingTicks.put(effectName, remainingTicks);
        }
    }
}
