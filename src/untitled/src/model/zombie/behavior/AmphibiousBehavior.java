package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;

public class AmphibiousBehavior implements ZombieBehavior {
    private double waterSpeed;
    private double landSpeed;
    private boolean submerged;
    private boolean targetableWhileSubmerged;

    public AmphibiousBehavior(double waterSpeed, double landSpeed, boolean targetableWhileSubmerged) {
        this.waterSpeed = waterSpeed;
        this.landSpeed = landSpeed;
        this.targetableWhileSubmerged = targetableWhileSubmerged;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie == null) {
            return;
        }

        zombie.setCurrentSpeed(this.submerged ? this.waterSpeed : this.landSpeed);
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().applyDamageToPlant(plant, 1);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.submerged = !this.targetableWhileSubmerged;
    }
}
