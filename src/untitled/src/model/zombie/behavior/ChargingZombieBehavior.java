package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class ChargingZombieBehavior implements ZombieBehavior {
    private double runningSpeedMultiplier;
    private double exhaustedSpeedMultiplier;
    private double baseSpeed = -1;
    private boolean chargeSpent;

    public ChargingZombieBehavior(double runningSpeedMultiplier, double exhaustedSpeedMultiplier) {
        this.runningSpeedMultiplier = runningSpeedMultiplier;
        this.exhaustedSpeedMultiplier = exhaustedSpeedMultiplier;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie == null) {
            return;
        }

        if (this.baseSpeed < 0) {
            this.baseSpeed = zombie.getDefinition() == null
                    ? zombie.getCurrentSpeed()
                    : zombie.getDefinition().getSpeed();
        }
        zombie.setCurrentSpeed(this.baseSpeed * (this.chargeSpent
                ? this.exhaustedSpeedMultiplier
                : this.runningSpeedMultiplier));

        if (!this.chargeSpent && board != null && board.getCombatSystem() != null
                && zombie.getPosition() != null) {
            for (Zombie other : board.getZombiesInLane(zombie.getPosition())) {
                if (other == null || other == zombie || other.isDead() || other.getPosition() == null
                        || !other.hasCondition(ZombieCondition.HYPNOTIZED)) {
                    continue;
                }

                if (Math.abs(other.getPosition().getX() - zombie.getPosition().getX()) <= 1) {
                    board.getCombatSystem().killZombie(other);
                    this.spendCharge(zombie);
                    break;
                }
            }
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (!this.chargeSpent && plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
            this.spendCharge(zombie);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
    }

    private void spendCharge(Zombie zombie) {
        this.chargeSpent = true;
        if (zombie != null && this.baseSpeed >= 0) {
            zombie.setCurrentSpeed(this.baseSpeed * this.exhaustedSpeedMultiplier);
        }
    }
}
