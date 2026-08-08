package model.zombie;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class ZombieArmorDefinition {
    private String alias;
    private ArmorType type;
    private int baseHealth;
    private Set<ArmorFlag> flags;
}
