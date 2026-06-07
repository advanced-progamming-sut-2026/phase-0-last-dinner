package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public interface ZombieBehavior {
    void onTick(Zombie zombie, Board board);

    void attack(Zombie zombie, Plant plant, Board board);

    void activate(Zombie zombie, Board board);
}
