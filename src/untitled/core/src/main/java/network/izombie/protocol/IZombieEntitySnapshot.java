package network.izombie.protocol;

import java.util.Collections;
import java.util.List;

public record IZombieEntitySnapshot(long entityId, IZombieEntityKind kind, String definitionKey, double x, double y,
                                    int health, int maximumHealth, boolean attacking, boolean dead,
                                    List<String> states) {
    public IZombieEntitySnapshot(long entityId, IZombieEntityKind kind, String definitionKey, double x, double y,
                                 int health, int maximumHealth, boolean attacking, boolean dead, List<String> states) {
        this.entityId = entityId;
        this.kind = kind;
        this.definitionKey = definitionKey;
        this.x = x;
        this.y = y;
        this.health = health;
        this.maximumHealth = maximumHealth;
        this.attacking = attacking;
        this.dead = dead;
        this.states = copyStates(states);
    }

    private List<String> copyStates(List<String> source) {
        if (source == null) {
            return Collections.emptyList();
        }

        return List.copyOf(source);
    }
}
