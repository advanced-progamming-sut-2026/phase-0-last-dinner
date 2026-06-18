package model.zombie.behavior;

import model.Plant;
import model.mechanism.Board;
import model.zombie.Zombie;
import model.zombie.ZombieDefinition;
import model.zombie.ZombieFactory;

public class GargantuarBehavior implements ZombieBehavior {
    private ZombieDefinition impDefinition;
    private ZombieFactory zombieFactory;
    private double throwHealthThreshold;
    private boolean impThrown;

    public GargantuarBehavior(ZombieDefinition impDefinition, ZombieFactory zombieFactory, double throwHealthThreshold) {
        this.impDefinition = impDefinition;
        this.zombieFactory = zombieFactory;
        this.throwHealthThreshold = throwHealthThreshold;
    }

    @Override
    public void onTick(Zombie zombie, Board board) {
        if (!this.impThrown && zombie != null && zombie.getDefinition() != null
                && zombie.getHealth() <= zombie.getDefinition().getHitpoints() * this.throwHealthThreshold) {
            this.throwImp(zombie, board);
        }
    }

    @Override
    public void attack(Zombie zombie, Plant plant, Board board) {
        if (plant != null && board != null && board.getCombatSystem() != null) {
            board.getCombatSystem().destroyPlant(plant);
        }
    }

    @Override
    public void activate(Zombie zombie, Board board) {
        this.throwImp(zombie, board);
    }

    public Zombie throwImp(Zombie zombie, Board board) {
        if (this.impThrown || zombie == null || board == null
                || this.zombieFactory == null || this.impDefinition == null) {
            return null;
        }

        Zombie imp = this.zombieFactory.create(this.impDefinition, zombie.getPosition());
        board.addZombie(imp, zombie.getPosition());
        this.impThrown = true;
        return imp;
    }
}
