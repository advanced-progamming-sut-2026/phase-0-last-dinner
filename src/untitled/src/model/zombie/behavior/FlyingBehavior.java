package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.mechanism.Position;
import model.mechanism.TerrainType;
import model.mechanism.Tile;
import model.plant.PlantCategory;
import model.plant.PlantTag;
import model.zombie.Zombie;
import model.zombie.ZombieCondition;

import java.util.Locale;

public class FlyingBehavior implements ZombieBehavior {
    private static final double MAXIMUM_FLIGHT_TILES = 2.0;

    private boolean ignoresGroundObstacles;
    private boolean flying;
    private double flightStartX;

    public FlyingBehavior(boolean ignoresGroundObstacles) {
        this.ignoresGroundObstacles = ignoresGroundObstacles;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (zombie == null) {
            return;
        }

        if (this.flying && Math.abs(zombie.getExactX() - this.flightStartX) >= MAXIMUM_FLIGHT_TILES) {
            this.stopFlying(zombie);
        }

        if (!this.flying && this.ignoresGroundObstacles && this.hasSlipperyTileAhead(zombie, board)) {
            this.startFlying(zombie);
        }

        if (this.flying) {
            zombie.addCondition(ZombieCondition.FLYING);
        } else {
            zombie.removeCondition(ZombieCondition.FLYING);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.ignoresGroundObstacles = true;
        this.startFlying(zombie);
    }

    @Override
    public boolean canAttackPlant(Zombie zombie, Plant plant, Board board) {
        if (!this.ignoresGroundObstacles || plant == null || plant.getName() == null) {
            return true;
        }

        String name = plant.getName().toLowerCase(Locale.ROOT);
        if (name.contains("tall-nut") || name.contains("tall nut")) {
            this.stopFlying(zombie);
            return true;
        }

        boolean fliesOverPlant = plant.getCategories() != null
                && plant.getCategories().contains(PlantCategory.DEFENDER);
        fliesOverPlant = fliesOverPlant || plant.getTags() != null
                && (plant.getTags().contains(PlantTag.TRAP)
                || plant.getTags().contains(PlantTag.MOVE_ZOMBIES));

        if (!fliesOverPlant) {
            this.stopFlying(zombie);
            return true;
        }

        if (!this.flying) {
            this.startFlying(zombie);
        }

        if (zombie != null && Math.abs(zombie.getExactX() - this.flightStartX) >= MAXIMUM_FLIGHT_TILES) {
            this.stopFlying(zombie);
            return true;
        }

        return false;
    }

    @Override
    public boolean runsWhileHypnotized() {
        return true;
    }

    private void startFlying(Zombie zombie) {
        if (this.flying || zombie == null) {
            return;
        }

        this.flying = true;
        this.flightStartX = zombie.getExactX();
        zombie.addCondition(ZombieCondition.FLYING);
    }

    private void stopFlying(Zombie zombie) {
        this.flying = false;
        if (zombie != null) {
            zombie.removeCondition(ZombieCondition.FLYING);
        }
    }

    private boolean hasSlipperyTileAhead(Zombie zombie, Board board) {
        if (zombie == null || zombie.getPosition() == null || board == null) {
            return false;
        }

        int direction = zombie.isHypnotized() ? 1 : -1;
        Position nextPosition = new Position(
                zombie.getPosition().getX() + direction,
                zombie.getPosition().getY()
        );
        Tile tile = board.getTile(nextPosition);

        return tile != null && (tile.getTerrainType() == TerrainType.SLIPPERY_UP
                || tile.getTerrainType() == TerrainType.SLIPPERY_DOWN);
    }
}
