package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.PlantTag;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;

public class TorchBearerBehavior implements ZombieBehavior {
    private boolean torchLit = true;

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        this.updateTorchFromPlant(plant);

        if (this.torchLit && plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.torchLit = true;
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        if (projectile == null) {
            return false;
        }

        if (projectile.getType() == ProjectileType.ICE) {
            this.torchLit = false;
        } else if (projectile.getType() == ProjectileType.FIRE) {
            this.torchLit = true;
        }
        return false;
    }

    public boolean isTorchLit() {
        return this.torchLit;
    }

    private void updateTorchFromPlant(Plant plant) {
        if (plant == null || plant.getTags() == null) {
            return;
        }

        if (plant.getTags().contains(PlantTag.ICE)) {
            this.torchLit = false;
        } else if (plant.getTags().contains(PlantTag.FIRE)) {
            this.torchLit = true;
        }
    }
}
