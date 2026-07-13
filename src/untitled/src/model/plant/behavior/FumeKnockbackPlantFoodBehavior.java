package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.PlantUpgradeEffect;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class FumeKnockbackPlantFoodBehavior implements PlantFoodBehavior {
    private int damage;
    private int knockbackTiles;

    public FumeKnockbackPlantFoodBehavior(int damage, int knockbackTiles) {
        this.damage = Math.max(0, damage);
        this.knockbackTiles = Math.max(1, knockbackTiles);
    }

    @Override
    public void activate(Plant plant, Board board) {
        if (plant == null || plant.getPosition() == null || board == null) {
            return;
        }

        for (Zombie zombie : board.getZombiesInLane(plant.getPosition())) {
            if (!this.canAffect(zombie)) {
                continue;
            }

            if (this.damage > 0 && board.getCombatSystem() != null) {
                board.getCombatSystem().applyDamageToZombie(zombie, this.damage);
            }

            if (!zombie.isDead()) {
                this.pushBack(board, zombie);
            }
        }
    }

    @Override
    public PlantFoodBehavior copy() {
        return new FumeKnockbackPlantFoodBehavior(this.damage, this.knockbackTiles);
    }

    @Override
    public void applyUpgrade(PlantUpgradeEffect effect) {
        if (effect != null) {
            this.damage += Math.max(0, effect.getDamageBonus());
        }
    }

    private boolean canAffect(Zombie zombie) {
        return zombie != null
                && !zombie.isDead()
                && !zombie.isHypnotized()
                && zombie.getPosition() != null
                && !zombie.hasCondition(ZombieCondition.SUBMERGED);
    }

    private void pushBack(Board board, Zombie zombie) {
        Position current = zombie.getPosition();
        int destinationX = Math.min(8, current.getX() + this.knockbackTiles);
        Position destination = new Position(destinationX, current.getY());

        if (board.moveZombie(zombie, destination)) {
            zombie.setPosition(destination);
        }
    }
}
