package model.mechanism;

import lombok.Getter;
import lombok.Setter;
import model.Plant;
import model.zombie.Zombie;
import view.GameEventListener;import java.util.List;
@Getter
@Setter
public class CombatSystem implements Tickable {
    private Board board;
    private GameEventListener listener;
    public CombatSystem(Board board) {
        this.board = board;
    }
    private void fireEvent(String message) {
        if (listener != null) listener.onGameEvent(message);
    }

    @Override
    public void onTick() {
        if (board == null || board.getTiles() == null) return;
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || zombie.isDead()) continue;
            Plant target = getNearestPlantInLane(zombie);
            if (target == null) continue;
            zombie.attack(target);
            //مهم
            // از اونجایی که منطق اتک برای هر زامبی متفاوته بودن متود اتک میک سنس نمیکنه زیاد توی این کلاس
            //بهتره هرکاری که قراره انجام بشه توی رفتار زامبی ها هندل شه
            // و نکته مهم توی متود اتک مخصوص هر زامبی چک بشه که فاصله مجاز باشه برای حمله
        }
    }

    public void attack(Zombie zombie) {
    }
    private Plant getNearestPlantInLane(Zombie zombie) {
        List<Plant> plantsInLane = board.getPlantsInLane(zombie.getPosition());
        Plant nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Plant plant : plantsInLane) {
            if (plant == null || plant.isDead()) continue;
            int distance = Math.abs(plant.getPosition().getX()
                    - zombie.getPosition().getX());
            if (distance < nearestDistance) {
                nearest = plant;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public void applyDamageToZombie(Zombie zombie, int damage) {
        if (zombie == null || damage <= 0) {
            return;
        }

        zombie.takeDamage(damage);

        if (zombie.isDead()) {
            fireEvent("Zombie of type " + zombie.getDefinition().getDisplayName()
                    + " is dead at (" + zombie.getPosition().getX()
                    + ", " + zombie.getPosition().getY() + ")");
            this.removeZombieFromBoard(zombie);
        }
    }

    public void applyDamageToPlant(Plant plant, int damage) {
        if (plant == null || damage <= 0) {
            return;
        }
        plant.takeDamage(damage);
        if (plant.isDead()) {
            fireEvent("Plant " + plant.getName()
                    + " at (" + plant.getPosition().getX()
                    + ", " + plant.getPosition().getY() + ") is destroyed.");
            removePlantFromBoard(plant);
        }
    }

    public void destroyPlant(Plant plant) {
        if (plant == null) {
            return;
        }
        plant.takeDamage(plant.getHealth());
        applyDamageToPlant(plant, plant.getHealth());
    }

    public void killZombie(Zombie zombie) {
        if (zombie == null) {
            return;
        }
        applyDamageToZombie(zombie, zombie.getHealth());
        zombie.die();
    }
    public void applyRadioactiveSunExplosion(Position center) {
        for (Zombie zombie : board.getZombiesInRadius(center, 2)) {
            applyDamageToZombie(zombie, 150);
        }
        for (Plant plant : board.getPlantsInRadius(center, 1)) {
            applyDamageToPlant(plant, 80);
        }
    }

    private void removeZombieFromBoard(Zombie zombie) {
        if (zombie == null || zombie.getBoard() == null || zombie.getPosition() == null) {
            return;
        }

        Tile tile = zombie.getBoard().getTile(zombie.getPosition());

        if (tile != null) {
            tile.removeZombie(zombie);
        }
    }
    private void removePlantFromBoard(Plant plant) {
        if (plant == null || plant.getPosition() == null) return;
        Tile tile = board.getTile(plant.getPosition());
        if (tile != null) tile.removePlant(plant);
    }
}
