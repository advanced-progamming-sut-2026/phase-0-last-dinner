package model.mechanism;

import model.Plant;
import model.zombie.Zombie;

public class CombatSystem implements Tickable {
    private Board board;

    @Override
    public void onTick() {
    }

    public void attack() {
    }

    public void applyDamageToZombie(Zombie zombie, int damage) {
        if (zombie == null || damage <= 0) {
            return;
        }

        zombie.takeDamage(damage);

        if (zombie.isDead()) {
            this.removeZombieFromBoard(zombie);
        }
    }

    public void applyDamageToPlant(Plant plant, int damage) {
        if (plant == null || damage <= 0) {
            return;
        }

        plant.takeDamage(damage);
    }

    public void destroyPlant(Plant plant) {
        if (plant == null) {
            return;
        }

        plant.takeDamage(plant.getHealth());
    }

    public void killZombie(Zombie zombie) {
        if (zombie == null) {
            return;
        }

        zombie.die();
        this.removeZombieFromBoard(zombie);
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
}
