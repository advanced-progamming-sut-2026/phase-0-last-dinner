package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.SunSystem;
import model.zombie.Zombie;

public class CrystalSkullBehavior implements ZombieBehavior {
    private static final int STEAL_INTERVAL_TICKS = 10;

    private int detectionRadius;
    private long chargingDurationTicks;
    private long cooldownDurationTicks;
    private long chargingTicks;
    private long cooldownTicks;
    private int stolenSun;
    private boolean charging;

    public CrystalSkullBehavior(int detectionRadius, long chargingDurationTicks, long cooldownDurationTicks) {
        this.detectionRadius = Math.max(1, detectionRadius);
        this.chargingDurationTicks = Math.max(1, chargingDurationTicks);
        this.cooldownDurationTicks = Math.max(0, cooldownDurationTicks);
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            this.charging = false;
            return;
        }

        if (!this.hasPlantInRange(zombie, board)) {
            this.charging = false;
            this.chargingTicks = 0;
            return;
        }

        this.charging = true;
        this.chargingTicks++;
        if (this.chargingTicks % STEAL_INTERVAL_TICKS == 0) {
            this.stealSun(board, 25);
        }
        if (this.chargingTicks >= this.chargingDurationTicks) {
            this.fireLaser(zombie, board);
            this.charging = false;
            this.chargingTicks = 0;
            this.cooldownTicks = this.cooldownDurationTicks;
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.fireLaser(zombie, board);
    }

    @Override
    public boolean canMove(Zombie zombie, Board board) {
        return !this.charging;
    }

    @Override
    public void onDeath(Zombie zombie, Board board) {
        SunSystem sunSystem = board == null ? null : board.getSunSystem();
        if (sunSystem != null && this.stolenSun > 0) {
            sunSystem.addSun(this.stolenSun / 2);
        }
        this.stolenSun = 0;
    }

    private boolean hasPlantInRange(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return false;
        }
        for (Plant plant : board.getPlantsInRadius(zombie.getPosition(), this.detectionRadius)) {
            if (plant != null && !plant.isDead() && plant.getPosition() != null) {
                return true;
            }
        }
        return false;
    }

    private void stealSun(Board board, int amount) {
        SunSystem sunSystem = board == null ? null : board.getSunSystem();
        if (sunSystem == null) {
            return;
        }
        int stolen = Math.min(Math.max(0, amount), Math.max(0, sunSystem.getSunAmount()));
        if (stolen > 0) {
            sunSystem.addSun(-stolen);
            this.stolenSun += stolen;
        }
    }

    private void fireLaser(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null || board.getCombatSystem() == null) {
            return;
        }
        for (int distance = 1; distance <= this.detectionRadius; distance++) {
            Position position = new Position(
                    zombie.getPosition().getX() - distance,
                    zombie.getPosition().getY()
            );
            for (Plant plant : board.getPlantsAt(position)) {
                if (plant != null && !plant.isDead()) {
                    board.getCombatSystem().destroyPlant(plant);
                }
            }
        }
    }
}
