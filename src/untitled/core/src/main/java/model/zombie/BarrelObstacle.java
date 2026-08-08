package model.zombie;

import model.mechanism.Position;
import model.zombie.behavior.BarrelObstacleBehavior;

import java.util.ArrayList;

// barrel az zombie ers mibare ta collision va lifecycle wave yeksan bemone
public final class BarrelObstacle extends Zombie {
    // chon doc adad nadade health barrel ba obstacle arcade yeksan gerefte shode
    public static final int DEFAULT_HEALTH = 1100;

    private static final ZombieDefinition DEFINITION = new ZombieDefinition(
            "BarrelObstacle",
            "Barrel",
            "Destructible barrel pushed by a Barrel Roller Zombie",
            ZombieType.SPECIAL,
            ZombieChapter.ALL_CHAPTERS,
            DEFAULT_HEALTH,
            0,
            0,
            0,
            0,
            false,
            new ArrayList<ZombieArmorDefinition>(),
            new ArrayList<ConditionResistance>()
    );

    public BarrelObstacle(
            Position position,
            ZombieDefinition impDefinition,
            ZombieFactory zombieFactory
    ) {
        super(
                DEFINITION,
                position,
                DEFAULT_HEALTH,
                0,
                new ArrayList<ZombieArmor>(),
                new ArrayList<ZombieCondition>(),
                new BarrelObstacleBehavior(impDefinition, zombieFactory)
        );
    }
}
