package model.plant.behavior;

import model.Plant;
import model.mechanism.Board;
import model.plant.Projectile;
import model.plant.ShootingDirection;

public class ShooterBehavior implements PlantBehavior {
    private Projectile projectileTemplate;
    private ShootingDirection direction;
    private int forwardShotCount;
    private int backwardShotCount;
    private int laneCount;

    @Override
    public void onTick(Plant plant, Board board) {
    }

    @Override
    public void activate(Plant plant, Board board) {
    }

    private void shootForward(Plant plant, Board board) {
    }

    private void shootBackward(Plant plant, Board board) {
    }
}
