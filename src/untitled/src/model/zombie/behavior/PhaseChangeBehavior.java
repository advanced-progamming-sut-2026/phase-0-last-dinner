package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.ArmorType;
import model.zombie.Zombie;
import model.zombie.ZombieArmor;

public class PhaseChangeBehavior implements ZombieBehavior {
    private double transitionHealthPercent;
    private double secondPhaseSpeedMultiplier;
    private double secondPhaseDamageMultiplier = 1.0;
    // agar trigger armor set bashe phase dovom ba shekastan haman armor faal mishe
    private ArmorType triggerArmorType;
    private boolean transitioned;

    public PhaseChangeBehavior(
            ZombieBehavior firstPhase,
            ZombieBehavior secondPhase,
            double transitionHealthPercent,
            double secondPhaseSpeedMultiplier
    ) {
        this.transitionHealthPercent = transitionHealthPercent;
        this.secondPhaseSpeedMultiplier = secondPhaseSpeedMultiplier;
    }

    public PhaseChangeBehavior(
            ArmorType triggerArmorType,
            double secondPhaseSpeedMultiplier,
            double secondPhaseDamageMultiplier
    ) {
        this.triggerArmorType = triggerArmorType;
        this.secondPhaseSpeedMultiplier = secondPhaseSpeedMultiplier;
        this.secondPhaseDamageMultiplier = secondPhaseDamageMultiplier;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (this.transitioned || zombie == null) {
            return;
        }

        boolean shouldTransition = this.triggerArmorType == null
                ? zombie.getDefinition() != null
                    && zombie.getHealth() <= zombie.getDefinition().getHitpoints() * this.transitionHealthPercent
                : !this.hasIntactTriggerArmor(zombie);

        if (shouldTransition) {
            this.activate(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        // bite ro basic behavior mizane in behavior faghat phase ro avaz mikone
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.transitioned = true;

        if (zombie != null) {
            zombie.setCurrentSpeed(zombie.getCurrentSpeed() * this.secondPhaseSpeedMultiplier);
            BasicZombieBehavior basicBehavior = zombie.findBehavior(BasicZombieBehavior.class);
            if (basicBehavior != null) {
                basicBehavior.multiplyDamage(this.secondPhaseDamageMultiplier);
            }
        }
    }

    private boolean hasIntactTriggerArmor(Zombie zombie) {
        if (zombie.getArmors() == null) {
            return false;
        }

        for (ZombieArmor armor : zombie.getArmors()) {
            if (armor != null && armor.getDefinition() != null
                    && armor.getDefinition().getType() == this.triggerArmorType
                    && !armor.isDestroyed() && !armor.isDropped()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean runsWhileHypnotized() {
        return true;
    }
}
