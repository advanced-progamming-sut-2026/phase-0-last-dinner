package model.zombie;

import lombok.Getter;

@Getter
public class ZombieArmor {
    private ZombieArmorDefinition definition;
    private int currentHealth;
    private boolean dropped;

    public ZombieArmor(ZombieArmorDefinition definition) {
        this.definition = definition;
        this.currentHealth = definition == null ? 0 : definition.getBaseHealth();
    }

    public void takeDamage(int amount) {
        if (amount <= 0 || this.dropped || this.isDestroyed()) {
            return;
        }

        this.currentHealth -= amount;

        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
            this.drop();
        }
    }

    public boolean isDestroyed() {
        return this.currentHealth <= 0;
    }

    public void drop() {
        this.dropped = true;
    }
}
