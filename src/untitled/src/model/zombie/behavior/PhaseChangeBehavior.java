package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class PhaseChangeBehavior implements ZombieBehavior {
    private ZombieBehavior firstPhase;
    private ZombieBehavior secondPhase;
    private double transitionHealthPercent;
    private double secondPhaseSpeedMultiplier;
    private boolean transitioned;

    @Override
    public void onTick(Zombie zombie, Board board) {
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }
}
