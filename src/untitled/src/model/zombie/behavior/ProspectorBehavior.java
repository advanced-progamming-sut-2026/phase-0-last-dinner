package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.Projectile;
import model.plant.ProjectileType;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

public class ProspectorBehavior implements ZombieBehavior {
    private long launchCountdownTicks;
    private long elapsedTicks;
    private boolean dynamiteExtinguished;
    // reversed yani dynamite amal karde va zombie az samte khane be rast barmigarde
    private boolean reversed;
    private boolean attackingPlantToTheRight;

    public ProspectorBehavior(long launchCountdownTicks) {
        this.launchCountdownTicks = Math.max(1, launchCountdownTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie != null && zombie.isHypnotized()) {
            return;
        }

        if (!this.dynamiteExtinguished && !this.reversed) {
            this.elapsedTicks++;
            if (this.elapsedTicks >= this.launchCountdownTicks) {
                this.launchToHouseEnd(zombie, board);
            }
        }

        this.attackingPlantToTheRight = false;
        if (this.reversed && zombie != null && zombie.getPosition() != null && board != null) {
            Position targetPosition = new Position(
                    zombie.getPosition().getX() + 1,
                    zombie.getPosition().getY()
            );
            for (Plant plant : board.getPlantsAt(targetPosition)) {
                if (plant != null && !plant.isDead()) {
                    this.attackingPlantToTheRight = true;
                    zombie.setAttacking(true);
                    zombie.attack(plant);
                    break;
                }
            }
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        if (!this.dynamiteExtinguished && (zombie == null || !zombie.isHypnotized())) {
            this.launchToHouseEnd(zombie, board);
        }
    }

    @Override
    public boolean onProjectileHit(Zombie zombie, Projectile projectile, Board board) {
        if (projectile != null && projectile.getType() == ProjectileType.ICE) {
            this.dynamiteExtinguished = true;
        }
        return false;
    }

    @Override
    public boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        if (!this.reversed || zombie == null || zombie.getPosition() == null
                || plant == null || plant.getPosition() == null) {
            return true;
        }
        return plant.getPosition().getX() >= zombie.getPosition().getX();
    }

    @Override
    public boolean canMove(Zombie zombie, Board board) {
        return !this.attackingPlantToTheRight;
    }

    @Override
    public int getMovementDirection(Zombie zombie) {
        if (zombie != null && zombie.hasCondition(ZombieCondition.HYPNOTIZED)) {
            return 1;
        }
        return this.reversed ? 1 : -1;
    }

    @Override
    public boolean runsWhileHypnotized() {
        return true;
    }

    private void launchToHouseEnd(Zombie zombie, Board board) {
        if (this.reversed || zombie == null || zombie.getPosition() == null || board == null) {
            return;
        }

        Position houseEnd = new Position(0, zombie.getPosition().getY());
        if (board.moveZombie(zombie, houseEnd)) {
            // set position exact x ro ham ba tile jadid sync mikone
            zombie.setPosition(houseEnd);
            zombie.setAttacking(false);
            this.reversed = true;
        }
    }
}
