package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class KernelButterPlantFoodBehavior implements PlantFoodBehavior {
    private int damage;
    private long stunTicks;

    public KernelButterPlantFoodBehavior(int damage, long stunTicks) {
        this.damage = Math.max(0, damage);
        this.stunTicks = Math.max(1, stunTicks);
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (board == null) {
            return;
        }

        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()) {
                continue;
            }

            zombie.addCondition(ZombieCondition.STUNNED, this.stunTicks);
            this.applyLobbedDamage(board, zombie);
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        return new KernelButterPlantFoodBehavior(this.damage, this.stunTicks);
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect == null) {
            return;
        }

        this.damage += Math.max(0, effect.getDamageBonus());
        this.stunTicks += Math.max(0, effect.getDurationBonusTicks());
    }

    private void applyLobbedDamage(Board board, Zombie zombie) {
        if (this.damage <= 0 || board.getCombatSystem() == null) {
            return;
        }

        if (zombie.hasCondition(ZombieCondition.SUBMERGED)) {
            zombie.takeDamage(this.damage);

            if (zombie.isDead()) {
                board.getCombatSystem().killZombieIgnoringAllegiance(zombie);
            }
        } else {
            board.getCombatSystem().applyDamageToZombie(zombie, this.damage);
        }
    }
}
