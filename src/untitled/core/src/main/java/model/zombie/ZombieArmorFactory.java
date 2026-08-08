package model.zombie;

public class ZombieArmorFactory {
    public ZombieArmor create(ZombieArmorDefinition definition) {
        return new ZombieArmor(definition);
    }
}
