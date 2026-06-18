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

    public PhaseChangeBehavior(
            ZombieBehavior firstPhase,
            ZombieBehavior secondPhase,
            double transitionHealthPercent,
            double secondPhaseSpeedMultiplier
    ) {
        this.firstPhase = firstPhase;
        this.secondPhase = secondPhase;
        this.transitionHealthPercent = transitionHealthPercent;
        this.secondPhaseSpeedMultiplier = secondPhaseSpeedMultiplier;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (!this.transitioned && zombie != null && zombie.getDefinition() != null
                && zombie.getHealth() <= zombie.getDefinition().getHitpoints() * this.transitionHealthPercent) {
            this.activate(zombie, board);
        }

        if (this.transitioned && this.secondPhase != null) {
            this.secondPhase.onTick(zombie, board);
        } else if (this.firstPhase != null) {
            this.firstPhase.onTick(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (this.transitioned && this.secondPhase != null) {
            this.secondPhase.attack(zombie, plant, board);
        } else if (this.firstPhase != null) {
            this.firstPhase.attack(zombie, plant, board);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.transitioned = true;

        if (zombie != null) {
            zombie.setCurrentSpeed(zombie.getCurrentSpeed() * this.secondPhaseSpeedMultiplier);
        }
    }
}
