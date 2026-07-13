package model.zombie;

import model.mechanism.Position;
import model.zombie.behavior.PushedObstacleBehavior;

import java.util.ArrayList;
import java.util.Locale;

// obstacle az zombie ers mibare ta collision va lifecycle wave yeksan bemone
public final class PushedObstacle extends Zombie {
    public PushedObstacle(String name, int health, ZombieChapter chapter, Position position) {
        super(
                createDefinition(name, health, chapter),
                position,
                Math.max(1, health),
                0,
                new ArrayList<ZombieArmor>(),
                new ArrayList<ZombieCondition>(),
                new PushedObstacleBehavior()
        );
    }

    private static ZombieDefinition createDefinition(String name, int health, ZombieChapter chapter) {
        String displayName = name == null || name.trim().isEmpty() ? "Pushed Obstacle" : name.trim();
        String alias = displayName.replaceAll("[^A-Za-z0-9]", "");

        if (alias.isEmpty()) {
            alias = "PushedObstacle";
        }

        return new ZombieDefinition(
                alias.toLowerCase(Locale.ROOT),
                displayName,
                "Destructible ground obstacle pushed by a zombie",
                ZombieType.SPECIAL,
                chapter == null ? ZombieChapter.ALL_CHAPTERS : chapter,
                Math.max(1, health),
                0,
                0,
                0,
                0,
                false,
                new ArrayList<ZombieArmorDefinition>(),
                new ArrayList<ConditionResistance>()
        );
    }
}
