package model.zombie;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.Tickable;
import model.zombie.behavior.ZombieBehavior;

import java.util.List;

public class Zombie implements Tickable {
    private ZombieDefinition definition;
    private Position position;
    private int health;
    private double currentSpeed;
    private boolean glowing;
    private boolean attacking;
    private boolean dead;
    private List<ZombieArmor> armors;
    private List<ZombieCondition> conditions;
    private ZombieBehavior behavior;
    private Board board;

    @Override
    public void onTick() {
    }

    public void move() {
    }

    public void attack(Plant plant) {
    }

    public void activateAbility() {
    }

    public void takeDamage(int amount) {
    }

    public void die() {
    }

    public ZombieArmor getActiveArmor() {
        return null;
    }

    public boolean isDead() {
        return false;
    }
}
