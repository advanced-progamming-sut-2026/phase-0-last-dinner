package model.minigame.zombotanyminigame;

import lombok.Getter;
import model.mechanism.Position;
import model.minigame.zombotanyminigame.ZombotanyTrait;

@Getter
public class ZombotanyZombieState {

    private final String zombieName;
    private final Position position;
    private final int health;
    private final ZombotanyTrait trait;

    public ZombotanyZombieState(
            String zombieName,
            Position position,
            int health,
            ZombotanyTrait trait
    ) {
        this.zombieName =
                zombieName == null
                        ? "Zombie"
                        : zombieName;

        this.position = position;
        this.health = Math.max(0, health);
        this.trait = trait;
    }
}