package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.plant.Projectile;
import model.zombie.BarrelObstacle;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;

public class BarrelRollerBehavior implements ZombieBehavior {
    private static final int BARREL_LEAD_TILES = 1;

    private final ZombieDefinition impDefinition;
    private final ZombieFactory zombieFactory;
    private BarrelObstacle barrel;
    // barrel ba marge owner baghi mimone va in flag spawn tekrar ro migire
    private boolean barrelDeployed;

    public BarrelRollerBehavior(
            ZombieDefinition impDefinition,
            ZombieFactory zombieFactory
    ) {
        this.impDefinition = impDefinition;
        this.zombieFactory = zombieFactory;
    }

    @Override
    public void onTick(Zombie owner, Board board) {
        this.ensureBarrel(owner, board);
        this.pushBarrel(owner, board);
        this.crushPlantsUnderBarrel(board);
    }

    @Override
    public void activate(Zombie owner, Board board) {
        this.ensureBarrel(owner, board);
    }

    @Override
    public void onDeath(Zombie owner, Board board) {
        if (owner != null && owner.isHypnotized()) {
            return;
        }

        // agar owner ghabl az tick aval bemire barrel inja sakhte mishe
        this.ensureBarrel(owner, board);
    }

    @Override
    public boolean onProjectileHit(Zombie owner, Projectile projectile, Board board) {
        if (projectile == null || projectile.isLobbed()) {
            return false;
        }

        this.ensureBarrel(owner, board);

        if (this.barrel == null || this.barrel.isDead() || this.barrel.getBoard() != board
                || this.barrel.getBehavior() == null) {
            return false;
        }

        // shot straight aval be barrel jeloye owner mire
        return this.barrel.getBehavior().onProjectileHit(this.barrel, projectile, board);
    }

    public BarrelObstacle getBarrel() {
        return this.barrel;
    }

    private void ensureBarrel(Zombie owner, Board board) {
        if (this.barrelDeployed || owner == null || owner.getPosition() == null || board == null) {
            return;
        }

        Position barrelPosition = this.getPositionAheadOf(owner);
        BarrelObstacle candidate = new BarrelObstacle(
                barrelPosition,
                this.impDefinition,
                this.zombieFactory
        );
        board.addZombie(candidate, barrelPosition);

        if (candidate.getBoard() == board) {
            this.barrel = candidate;
            this.barrelDeployed = true;

            if (owner.getWave() != null) {
                // barrel ham ozve wave ast ta wave zudtar tamam nashe
                owner.getWave().addZombie(candidate);
            }
        }
    }

    private void pushBarrel(Zombie owner, Board board) {
        if (owner == null || owner.isDead() || board == null || this.barrel == null
                || this.barrel.isDead() || this.barrel.getBoard() != board) {
            return;
        }

        Position destination = this.getPositionAheadOf(owner);

        if (destination.equals(this.barrel.getPosition())) {
            return;
        }

        if (board.removeZombie(this.barrel)) {
            board.addZombie(this.barrel, destination);
        }
    }

    private void crushPlantsUnderBarrel(Board board) {
        if (board == null || board.getCombatSystem() == null || this.barrel == null
                || this.barrel.isDead() || this.barrel.getPosition() == null) {
            return;
        }

        for (Plant plant : board.getPlantsAt(this.barrel.getPosition())) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    private Position getPositionAheadOf(Zombie owner) {
        int direction = owner.hasCondition(ZombieCondition.HYPNOTIZED) ? 1 : -1;
        int x = owner.getPosition().getX() + direction * BARREL_LEAD_TILES;
        x = Math.max(0, Math.min(8, x));
        return new Position(x, owner.getPosition().getY());
    }
}
