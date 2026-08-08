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
        this.absorbDamage(amount);
    }

    public int absorbDamage(int amount) {
        if (amount <= 0 || this.dropped || this.isDestroyed()) {
            return Math.max(0, amount);
        }

        int absorbedDamage = Math.min(amount, this.currentHealth);
        this.currentHealth -= amount;

        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
            this.drop();
        }

        return amount - absorbedDamage;
    }

    public boolean isDestroyed() {
        return this.currentHealth <= 0;
    }

    public void drop() {
        this.dropped = true;
    }
}
