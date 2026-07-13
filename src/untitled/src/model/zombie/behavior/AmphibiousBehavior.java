package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.plant.Projectile;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

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

        Tile tile = board == null ? null : board.getTile(zombie.getPosition());
        boolean inWater = tile != null && tile.getTerrainType() == TerrainType.WATER;
        this.submerged = inWater && !zombie.isAttacking();
        zombie.setCurrentSpeed(inWater ? this.waterSpeed : this.landSpeed);

        if (this.submerged) {
            zombie.addCondition(ZombieCondition.SUBMERGED);
        } else {
            zombie.removeCondition(ZombieCondition.SUBMERGED);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        this.submerged = false;
        if (zombie != null) {
            zombie.removeCondition(ZombieCondition.SUBMERGED);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.submerged = true;
        if (zombie != null) {
            zombie.addCondition(ZombieCondition.SUBMERGED);
        }
    }

    @Override
    public boolean canBeHitBy(Zombie zombie, Projectile projectile) {
        if (!this.submerged || this.targetableWhileSubmerged) {
            return true;
        }
        return projectile != null && projectile.isLobbed();
    }
}
